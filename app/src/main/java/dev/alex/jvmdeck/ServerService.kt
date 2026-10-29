package dev.alex.jvmdeck

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import java.io.File
import kotlin.concurrent.thread

class ServerService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                goForeground()
                val args = intent.getStringArrayExtra(EXTRA_ARGS)?.toList() ?: emptyList()
                val dir = File(intent.getStringExtra(EXTRA_DIR) ?: filesDir.absolutePath)
                val javaVer = intent.getIntExtra(EXTRA_JAVA_VER, ServerRuntime.effectiveJavaVersion)
                startJava(args, dir, javaVer)
            }
            ACTION_SEND -> {
                val line = intent.getStringExtra(EXTRA_LINE)
                if (!line.isNullOrBlank()) {
                    ServerRuntime.append("> $line")
                    ServerRuntime.sendCommand(line)
                }
            }
            ACTION_STOP -> {
                ServerRuntime.append("Зупинка сервера...")
                ServerRuntime.stopServer()
            }
        }
        return START_NOT_STICKY
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val pm = getSystemService(PowerManager::class.java)
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "dev.alex.jvmdeck:ServerWakeLock")
        }
        if (wakeLock?.isHeld != true) {
            wakeLock?.acquire()
        }
    }

    private fun releaseWakeLock() {
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
    }

    override fun onDestroy() {
        releaseWakeLock()
        super.onDestroy()
    }

    private fun startJava(args: List<String>, dir: File, javaVer: Int) {
        val runner = ServerRuntime.runner(this, javaVer)
        if (runner.isRunning) { ServerRuntime.append("Сервер вже запущено"); return }
        ServerRuntime.running.value = true
        ServerRuntime.status.value = ServerStatus.STARTING
        acquireWakeLock()
        thread(name = "java-bootstrap") {
            try {
                StaleProcessKiller.killStaleJava(this)
                val jre = runner.jre
                if (!jre.isInstalled()) {
                    ServerRuntime.append("Unpacking JRE (${jre.jreDirName})...")
                    jre.install()
                }
                runner.start(
                    args, dir,
                    onLine = ServerRuntime::append,
                    onExit = { code ->
                        mainHandler.post {
                            ServerRuntime.append("--- exit code $code ---")
                            ServerRuntime.running.value = false
                            ServerRuntime.status.value = ServerStatus.OFFLINE
                            releaseWakeLock()
                            runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
                            runCatching { stopSelf() }
                        }
                    }
                )
            } catch (t: Throwable) {
                mainHandler.post {
                    ServerRuntime.append("BOOTSTRAP ERROR: $t")
                    ServerRuntime.running.value = false
                    ServerRuntime.status.value = ServerStatus.OFFLINE
                    releaseWakeLock()
                    stopSelf()
                }
            }
        }
    }

    private fun goForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Java server", NotificationManager.IMPORTANCE_LOW)
        )
        val n = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Java server running")
            .setOngoing(true)
            .build()
        ServiceCompat.startForeground(this, 1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
    }

    companion object {
        private const val CHANNEL = "jvm_host"
        const val ACTION_START = "dev.alex.jvmdeck.START"
        const val ACTION_STOP = "dev.alex.jvmdeck.STOP"
        const val ACTION_SEND = "dev.alex.jvmdeck.SEND"
        const val EXTRA_ARGS = "args"
        const val EXTRA_DIR = "dir"
        const val EXTRA_LINE = "line"
        const val EXTRA_JAVA_VER = "java_ver"

        fun start(ctx: Context, args: List<String>, dir: File, javaVer: Int = ServerRuntime.effectiveJavaVersion) =
            ctx.startForegroundService(
                Intent(ctx, ServerService::class.java)
                    .setAction(ACTION_START)
                    .putExtra(EXTRA_ARGS, args.toTypedArray())
                    .putExtra(EXTRA_DIR, dir.absolutePath)
                    .putExtra(EXTRA_JAVA_VER, javaVer)
            )

        fun stop(ctx: Context) =
            ctx.startService(Intent(ctx, ServerService::class.java).setAction(ACTION_STOP))

        fun send(ctx: Context, line: String) =
            ctx.startService(
                Intent(ctx, ServerService::class.java).setAction(ACTION_SEND).putExtra(EXTRA_LINE, line)
            )
    }
}

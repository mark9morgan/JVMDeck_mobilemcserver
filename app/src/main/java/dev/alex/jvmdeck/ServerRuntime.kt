package dev.alex.jvmdeck

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

enum class ServerStatus {
    OFFLINE,  // 🔴 Вимкнено
    STARTING, // 🟡 Запуск...
    ONLINE    // 🟢 Онлайн
}

object ServerRuntime {
    val log = MutableStateFlow<List<String>>(emptyList())
    val running = MutableStateFlow(false)
    val status = MutableStateFlow(ServerStatus.OFFLINE)
    val autoJavaVersion = MutableStateFlow(21)
    val selectedJavaMode = MutableStateFlow("auto") // "auto", "8", "17", "21", "25"

    val effectiveJavaVersion: Int
        get() {
            val mode = selectedJavaMode.value
            return if (mode == "auto") {
                autoJavaVersion.value
            } else {
                mode.toIntOrNull() ?: autoJavaVersion.value
            }
        }

    @Volatile private var currentRunner: JavaRunner? = null

    fun append(line: String) {
        log.update { (it + line).takeLast(3000) }
        val upper = line.uppercase()
        if (upper.contains("ADDRESS ALREADY IN USE") || upper.contains("FAILED TO BIND TO PORT")) {
            log.update { (it + "Порт зайнятий: схоже, працює старий процес java. Перезапусти запуск, додаток прибере старі процеси.").takeLast(3000) }
        }
        if (status.value == ServerStatus.STARTING) {
            if (line.contains("Done (") || line.contains("For help, type") || line.contains("Server started") || line.contains("Ready!")) {
                status.value = ServerStatus.ONLINE
            }
        }
    }

    fun runner(ctx: Context, javaVer: Int = effectiveJavaVersion): JavaRunner {
        val existing = currentRunner
        if (existing != null && existing.isRunning) {
            return existing
        }
        val jre = JreManager.forJavaVersion(ctx.applicationContext, javaVer)
        val newRunner = JavaRunner(jre)
        currentRunner = newRunner
        return newRunner
    }

    fun sendCommand(line: String) {
        val r = currentRunner
        if (r != null && r.isRunning) {
            r.send(line)
        }
    }

    fun stopServer() {
        val r = currentRunner
        if (r != null && r.isRunning) {
            r.stop()
        }
    }

    fun jre(ctx: Context, javaVer: Int = effectiveJavaVersion) = JreManager.forJavaVersion(ctx.applicationContext, javaVer)
}

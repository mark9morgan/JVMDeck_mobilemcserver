package dev.alex.jvmdeck

import android.os.Build
import android.system.Os
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/** Уся логіка запуску JVM тут. Якщо exec-схема не спрацює, міняти треба лише цей клас. */
class JavaRunner(val jre: JreManager) {
    @Volatile private var process: Process? = null

    val isRunning get() = process?.isAlive == true

    fun start(
        args: List<String>,
        workDir: File,
        onLine: (String) -> Unit,
        onExit: (Int) -> Unit,
    ) {
        workDir.mkdirs()

        // Переконуємось, що JRE встановлено та лінки створено
        jre.install()
        jre.ensureBinJavaSymlink()

        // Використовуємо симлінк bin/java у jreHome для збігу шляху з JAVA_HOME
        val binJava = File(jre.jreHome, "bin/java")
        val executable = if (binJava.exists()) binJava.absolutePath else jre.javaBin.absolutePath

        // Видаляємо застаріле посилання на агент та створюємо свіже без знаків '=' у шляху
        val nativeAgent = File(jre.javaBin.parentFile, "libnotbi.so")
        val agentSymlink = File(jre.jreHome, "libnotbi.so")
        if (nativeAgent.exists()) {
            runCatching {
                agentSymlink.delete()
                Os.symlink(nativeAgent.absolutePath, agentSymlink.absolutePath)
            }
        }
        val agentPath = if (agentSymlink.exists()) agentSymlink.absolutePath else nativeAgent.absolutePath
        val cmd = listOf(executable, "-agentpath:$agentPath") + args
        val pb = ProcessBuilder(cmd).directory(workDir).redirectErrorStream(true)

        // Збираємо всі можливі папки з бінарними бібліотеками всередині jreHome
        val libSearchPaths = mutableListOf<String>()
        val serverLibDir = File(jre.jreHome, "lib/server")
        if (serverLibDir.exists()) libSearchPaths.add(serverLibDir.absolutePath)

        val rootLibDir = File(jre.jreHome, "lib")
        if (rootLibDir.exists()) {
            libSearchPaths.add(rootLibDir.absolutePath)
            rootLibDir.walkTopDown().filter { it.isDirectory }.forEach { dir ->
                libSearchPaths.add(dir.absolutePath)
            }
        }
        jre.javaBin.parentFile?.absolutePath?.let { libSearchPaths.add(it) }

        val ldLibraryPath = libSearchPaths.distinct().joinToString(":")

        pb.environment().apply {
            put("JAVA_HOME", jre.jreHome.absolutePath)
            put("LD_LIBRARY_PATH", ldLibraryPath)
            put("HOME", workDir.absolutePath)
            put("TMPDIR", File(workDir, "tmp").apply { mkdirs() }.absolutePath)
        }
        onLine("$ ${cmd.joinToString(" ")}")

        val p = try {
            pb.start()
        } catch (e: IOException) {
            onLine("START FAILED: ${e.message}")
            onExit(-1)
            return
        }
        process = p
        thread(name = "java-out") {
            runCatching {
                p.inputStream.bufferedReader().forEachLine(onLine)
            }
            onExit(p.waitFor())
        }
    }

    /** Текст у stdin процесу (консоль Minecraft-сервера). */
    fun send(line: String) {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return
        process?.outputStream?.let { out ->
            runCatching {
                out.write((trimmed + "\n").toByteArray(Charsets.UTF_8))
                out.flush()
            }
        }
    }

    /** Ввічливо ("stop" у консоль), а якщо не вийшов за [graceSec] секунд, то силою. */
    fun stop(graceCmd: String? = "stop", graceSec: Long = 10) {
        val p = process ?: return
        if (graceCmd != null) {
            send(graceCmd)
        }
        thread(name = "java-stop") {
            try {
                if (!p.waitFor(graceSec, TimeUnit.SECONDS)) {
                    p.destroy()
                    if (!p.waitFor(3, TimeUnit.SECONDS)) {
                        p.destroyForcibly()
                    }
                }
            } catch (_: Exception) {
                p.destroyForcibly()
            }
        }
    }

    /** Діагностика: що бачить додаток, коли "не запускається". */
    fun diagnostics(): List<String> = buildList {
        add("SDK=${Build.VERSION.SDK_INT}  ABIs=${Build.SUPPORTED_ABIS.joinToString()}")
        add("javaBin=${jre.javaBin}")
        add("  exists=${jre.javaBin.exists()} canExecute=${jre.javaBin.canExecute()}")
        val nl = jre.javaBin.parentFile
        add("nativeLibraryDir: ${nl?.list()?.joinToString() ?: "?"}")
        add("jreHome=${jre.jreHome} installed=${jre.isInstalled()}")
        listOf("lib/jvm.cfg", "lib/server/libjvm.so", "lib/libjli.so", "lib/modules").forEach {
            add("  $it exists=${File(jre.jreHome, it).exists()}")
        }
    }
}

package dev.alex.jvmdeck

import android.content.Context
import android.os.Process
import dev.alex.jvmdeck.model.LanguageManager
import java.io.File

object StaleProcessKiller {

    fun listJava(ctx: Context): List<Int> {
        val pids = mutableListOf<Int>()
        val procDir = File("/proc")
        val files = procDir.listFiles() ?: return pids

        val nativeDir = ctx.applicationInfo.nativeLibraryDir
        val jreBinPath = ctx.filesDir.absolutePath + "/jre"

        for (file in files) {
            val name = file.name
            if (!name.all { it.isDigit() }) continue
            val pid = name.toIntOrNull() ?: continue

            runCatching {
                val cmdlineFile = File(file, "cmdline")
                if (!cmdlineFile.exists()) return@runCatching
                val bytes = cmdlineFile.readBytes()
                if (bytes.isEmpty()) return@runCatching

                val nullIdx = bytes.indexOf(0.toByte())
                val argv0 = if (nullIdx != -1) {
                    String(bytes, 0, nullIdx, Charsets.UTF_8)
                } else {
                    String(bytes, Charsets.UTF_8)
                }

                val matchesNative = nativeDir.isNotBlank() && argv0.startsWith(nativeDir)
                val matchesJre = argv0.startsWith(jreBinPath)

                if ((matchesNative || matchesJre) && pid != Process.myPid()) {
                    pids.add(pid)
                }
            }
        }
        return pids
    }

    fun killStaleJava(ctx: Context, exceptPid: Int? = null): Int {
        val pids = listJava(ctx)
        var killedCount = 0

        for (pid in pids) {
            if (pid == Process.myPid() || pid == exceptPid) continue
            runCatching {
                Process.killProcess(pid)
                ServerRuntime.append(LanguageManager.getString("killed_stale_java", pid))
                killedCount++
            }
        }

        if (killedCount > 0) {
            runCatching { Thread.sleep(500) }
        }

        return killedCount
    }
}

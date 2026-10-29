package dev.alex.jvmdeck.model

import android.os.Handler
import android.os.Looper
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

object InstalledModsManager {

    private val mainHandler = Handler(Looper.getMainLooper())

    fun getModsDir(serverDir: File): File {
        val modsDir = File(serverDir, "mods")
        modsDir.mkdirs()
        return modsDir
    }

    fun getInstalledMods(serverDir: File): List<InstalledPlugin> {
        val dir = getModsDir(serverDir)
        val files = dir.listFiles() ?: return emptyList()

        return files.filter {
            it.isFile && (it.name.endsWith(".jar", ignoreCase = true) || it.name.endsWith(".jar.disabled", ignoreCase = true))
        }.map { file ->
            val isEnabled = file.name.endsWith(".jar", ignoreCase = true)
            InstalledPlugin(
                fileName = file.name,
                isEnabled = isEnabled,
                sizeBytes = file.length(),
                lastModified = file.lastModified()
            )
        }.sortedBy { it.fileName.lowercase() }
    }

    fun toggleModState(serverDir: File, fileName: String): Boolean {
        val dir = getModsDir(serverDir)
        val target = File(dir, fileName)
        if (!target.exists()) return false

        return if (fileName.endsWith(".jar", ignoreCase = true)) {
            val disabledFile = File(dir, "$fileName.disabled")
            target.renameTo(disabledFile)
        } else if (fileName.endsWith(".jar.disabled", ignoreCase = true)) {
            val enabledName = fileName.removeSuffix(".disabled")
            val enabledFile = File(dir, enabledName)
            target.renameTo(enabledFile)
        } else {
            false
        }
    }

    fun deleteMod(serverDir: File, fileName: String): Boolean {
        val dir = getModsDir(serverDir)
        val target = File(dir, fileName)
        return target.delete()
    }

    fun downloadAndInstallMod(
        serverDir: File,
        downloadUrl: String,
        fileName: String,
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ) {
        thread(name = "mod-downloader") {
            try {
                val dir = getModsDir(serverDir)
                val targetFile = File(dir, fileName)
                require(targetFile.canonicalPath.startsWith(dir.canonicalPath)) { "path traversal detected: $fileName" }

                val conn = URL(downloadUrl).openConnection() as HttpURLConnection
                conn.setRequestProperty("User-Agent", "JvmDeck-AndroidApp/1.0")
                conn.connectTimeout = 10000
                conn.readTimeout = 30000

                conn.inputStream.use { input ->
                    targetFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                mainHandler.post { onComplete() }
            } catch (e: Exception) {
                val msg = "Помилка завантаження мода: ${e.message}"
                mainHandler.post { onError(msg) }
            }
        }
    }
}

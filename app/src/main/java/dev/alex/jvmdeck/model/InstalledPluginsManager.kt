package dev.alex.jvmdeck.model

import android.os.Handler
import android.os.Looper
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

data class InstalledPlugin(
    val fileName: String,
    val isEnabled: Boolean,
    val sizeBytes: Long,
    val lastModified: Long
)

object InstalledPluginsManager {

    private val mainHandler = Handler(Looper.getMainLooper())

    fun getPluginsDir(serverDir: File): File {
        val pluginsDir = File(serverDir, "plugins")
        pluginsDir.mkdirs()
        return pluginsDir
    }

    fun getInstalledPlugins(serverDir: File): List<InstalledPlugin> {
        val dir = getPluginsDir(serverDir)
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

    fun togglePluginState(serverDir: File, fileName: String): Boolean {
        val dir = getPluginsDir(serverDir)
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

    fun deletePlugin(serverDir: File, fileName: String): Boolean {
        val dir = getPluginsDir(serverDir)
        val target = File(dir, fileName)
        return target.delete()
    }

    fun downloadAndInstallPlugin(
        serverDir: File,
        downloadUrl: String,
        fileName: String,
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ) {
        thread(name = "plugin-downloader") {
            try {
                val dir = getPluginsDir(serverDir)
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
                val msg = "Помилка завантаження плагіна: ${e.message}"
                mainHandler.post { onError(msg) }
            }
        }
    }
}

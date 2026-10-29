package dev.alex.jvmdeck.model

import android.content.Context
import dev.alex.jvmdeck.JreManager
import dev.alex.jvmdeck.ServerRuntime
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

object ServerDownloader {

    fun deduceJavaVersion(version: String, type: String = "paper"): Int {
        val cleanVer = version.trim().removePrefix("v")
        return when {
            cleanVer.startsWith("1.16.") && type.lowercase() == "paper" -> 17
            cleanVer == "1.1" || cleanVer.startsWith("1.1.") ||
            cleanVer == "1.2" || cleanVer.startsWith("1.2.") ||
            cleanVer == "1.3" || cleanVer.startsWith("1.3.") ||
            cleanVer == "1.4" || cleanVer.startsWith("1.4.") ||
            cleanVer == "1.5" || cleanVer.startsWith("1.5.") ||
            cleanVer == "1.6" || cleanVer.startsWith("1.6.") ||
            cleanVer == "1.7" || cleanVer.startsWith("1.7.") ||
            cleanVer == "1.8" || cleanVer.startsWith("1.8.") ||
            cleanVer == "1.9" || cleanVer.startsWith("1.9.") ||
            cleanVer == "1.10" || cleanVer.startsWith("1.10.") ||
            cleanVer == "1.11" || cleanVer.startsWith("1.11.") -> 8

            cleanVer == "1.12" || cleanVer.startsWith("1.12.") ||
            cleanVer == "1.13" || cleanVer.startsWith("1.13.") ||
            cleanVer == "1.14" || cleanVer.startsWith("1.14.") ||
            cleanVer == "1.15" || cleanVer.startsWith("1.15.") ||
            cleanVer == "1.16" || cleanVer.startsWith("1.16.") -> 11

            cleanVer == "1.17" || cleanVer.startsWith("1.17.") -> 16

            cleanVer == "1.18" || cleanVer.startsWith("1.18.") ||
            cleanVer == "1.19" || cleanVer.startsWith("1.19.") ||
            cleanVer == "1.20" || cleanVer == "1.20.1" || cleanVer == "1.20.2" ||
            cleanVer == "1.20.3" || cleanVer == "1.20.4" -> 17

            cleanVer == "1.20.5" || cleanVer.startsWith("1.20.6") ||
            cleanVer == "1.21" || cleanVer.startsWith("1.21.") -> 21

            cleanVer.startsWith("26.") || cleanVer.startsWith("1.22") || cleanVer == "26" -> 25

            else -> 21
        }
    }

    fun downloadServer(
        context: Context,
        type: String,
        version: String,
        serverDir: File,
        onComplete: (() -> Unit)? = null
    ) {
        val appContext = context.applicationContext
        thread(name = "server-downloader") {
            try {
                serverDir.mkdirs()
                File(serverDir, "eula.txt").writeText("eula=true\n")

                val targetJar = File(serverDir, "server.jar")
                val reqJava = deduceJavaVersion(version)
                ServerRuntime.autoJavaVersion.value = reqJava

                when (type.lowercase()) {
                    "paper" -> downloadPaper(version, targetJar, onComplete)
                    "vanilla" -> downloadVanilla(version, targetJar, onComplete)
                    "spigot" -> downloadSpigot(version, targetJar, onComplete)
                    "forge" -> downloadForge(appContext, version, targetJar, serverDir, onComplete)
                    "neoforge" -> downloadNeoForge(appContext, version, targetJar, serverDir, onComplete)
                    else -> downloadPaper(version, targetJar, onComplete)
                }
            } catch (e: Exception) {
                ServerRuntime.append("Помилка завантаження $type $version: ${e.message}")
            }
        }
    }

    private fun downloadPaper(version: String, targetJar: File, onComplete: (() -> Unit)?) {
        ServerRuntime.append("Запитування інформації для Paper $version...")
        val versionUrl = URL("https://fill.papermc.io/v3/projects/paper/versions/$version")
        val vConn = versionUrl.openConnection() as HttpURLConnection
        vConn.setRequestProperty("User-Agent", "JvmDeck-Android")
        vConn.connectTimeout = 10000
        vConn.readTimeout = 10000
        val vJsonText = vConn.inputStream.bufferedReader().readText()
        val vJson = JSONObject(vJsonText)

        val javaVer = vJson.optJSONObject("version")
            ?.optJSONObject("java")
            ?.optJSONObject("version")
            ?.optInt("minimum", deduceJavaVersion(version))
            ?: deduceJavaVersion(version)

        ServerRuntime.autoJavaVersion.value = javaVer

        val builds = vJson.getJSONArray("builds")
        if (builds.length() == 0) {
            ServerRuntime.append("Помилка: білди відсутні для Paper $version")
            return
        }
        val latestBuild = builds.getInt(0)

        val buildUrl = URL("https://fill.papermc.io/v3/projects/paper/versions/$version/builds/$latestBuild")
        val bConn = buildUrl.openConnection() as HttpURLConnection
        bConn.setRequestProperty("User-Agent", "JvmDeck-Android")
        val bJsonText = bConn.inputStream.bufferedReader().readText()
        val bJson = JSONObject(bJsonText)
        val downloadsObj = bJson.getJSONObject("downloads")
        val downloadItem = if (downloadsObj.has("server:default")) {
            downloadsObj.getJSONObject("server:default")
        } else {
            downloadsObj.getJSONObject(downloadsObj.keys().next())
        }
        val fileUrl = downloadItem.getString("url")

        ServerRuntime.append("Завантаження Paper $version (build #$latestBuild)...")
        downloadFileFromUrl(fileUrl, targetJar)
        ServerRuntime.append("Успішно імпортовано: Paper $version -> server.jar")
        onComplete?.invoke()
    }

    private fun downloadVanilla(version: String, targetJar: File, onComplete: (() -> Unit)?) {
        ServerRuntime.append("Запитування офіційного маніфесту Mojang Vanilla $version...")
        val manifestUrl = URL("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json")
        val conn = manifestUrl.openConnection() as HttpURLConnection
        conn.setRequestProperty("User-Agent", "JvmDeck-Android")
        val jsonText = conn.inputStream.bufferedReader().readText()
        val json = JSONObject(jsonText)
        val versionsArr = json.getJSONArray("versions")

        var pkgUrl: String? = null
        for (i in 0 until versionsArr.length()) {
            val vObj = versionsArr.getJSONObject(i)
            if (vObj.getString("id") == version) {
                pkgUrl = vObj.getString("url")
                break
            }
        }

        if (pkgUrl == null) {
            ServerRuntime.append("Помилка: версію Vanilla $version не знайдено в маніфесті Mojang")
            return
        }

        val pkgConn = URL(pkgUrl).openConnection() as HttpURLConnection
        pkgConn.setRequestProperty("User-Agent", "JvmDeck-Android")
        val pkgJson = JSONObject(pkgConn.inputStream.bufferedReader().readText())
        val serverUrl = pkgJson.getJSONObject("downloads").getJSONObject("server").getString("url")

        ServerRuntime.append("Завантаження офіційного ядра Vanilla $version...")
        downloadFileFromUrl(serverUrl, targetJar)
        ServerRuntime.append("Успішно імпортовано: Vanilla $version -> server.jar")
        onComplete?.invoke()
    }

    private fun downloadSpigot(version: String, targetJar: File, onComplete: (() -> Unit)?) {
        ServerRuntime.append("Завантаження Spigot $version...")
        val url = "https://download.getbukkit.org/spigot/spigot-$version.jar"
        downloadFileFromUrl(url, targetJar)
        ServerRuntime.append("Успішно імпортовано: Spigot $version -> server.jar")
        onComplete?.invoke()
    }

    private fun downloadForge(context: Context, version: String, targetJar: File, serverDir: File, onComplete: (() -> Unit)?) {
        ServerRuntime.append("Запитування версії Forge для $version...")
        try {
            val promoUrl = URL("https://files.minecraftforge.net/net/minecraftforge/forge/promotions_slim.json")
            val conn = promoUrl.openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "JvmDeck-Android")
            val jsonText = conn.inputStream.bufferedReader().readText()
            val json = JSONObject(jsonText)
            val promos = json.getJSONObject("promos")

            val key = "$version-recommended"
            val altKey = "$version-latest"
            val forgeVer = if (promos.has(key)) {
                promos.getString(key)
            } else if (promos.has(altKey)) {
                promos.getString(altKey)
            } else {
                null
            }

            if (forgeVer == null) {
                ServerRuntime.append("Помилка: Forge не знайдено для версії $version")
                return
            }

            val fullVersion = "$version-$forgeVer"
            val installerUrl = "https://maven.minecraftforge.net/net/minecraftforge/forge/$fullVersion/forge-$fullVersion-installer.jar"
            val installerFile = File(serverDir, "forge-installer.jar")

            ServerRuntime.append("Завантаження інсталятора Forge $fullVersion...")
            downloadFileFromUrl(installerUrl, installerFile)

            ServerRuntime.append("Встановлення Forge через Java...")
            val javaVer = deduceJavaVersion(version)
            val jre = JreManager(context, javaVer)
            jre.ensureBinJavaSymlink()
            val javaExe = File(jre.jreHome, "bin/java")
            if (!javaExe.exists()) {
                jre.install()
            }

            val process = ProcessBuilder(javaExe.absolutePath, "-jar", "forge-installer.jar", "--installServer")
                .directory(serverDir)
                .redirectErrorStream(true)
                .start()

            process.inputStream.bufferedReader().readText()
            process.waitFor()

            installerFile.delete()

            val files = serverDir.listFiles() ?: emptyArray()
            val foundJar = files.firstOrNull { it.name.startsWith("forge-") && it.name.endsWith(".jar") && !it.name.contains("installer") }
            if (foundJar != null) {
                foundJar.renameTo(targetJar)
            } else {
                File(serverDir, "forge-$fullVersion.jar").renameTo(targetJar)
            }

            ServerRuntime.append("Успішно встановлено: Forge $version -> server.jar")
            onComplete?.invoke()
        } catch (e: Exception) {
            ServerRuntime.append("Помилка встановлення Forge: ${e.message}")
        }
    }

    private fun downloadNeoForge(context: Context, version: String, targetJar: File, serverDir: File, onComplete: (() -> Unit)?) {
        ServerRuntime.append("Запитування версії NeoForge для $version...")
        try {
            val metaUrl = URL("https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml")
            val conn = metaUrl.openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "JvmDeck-Android")
            val xml = conn.inputStream.bufferedReader().readText()

            val cleanVer = version.removePrefix("v")
            val parts = cleanVer.split(".")
            val majorMinor = if (parts.size >= 2) "${parts[0].toInt() - 1}.${parts[1]}" else cleanVer

            val regex = Regex("<version>($majorMinor\\.[^<]+)</version>")
            val matches = regex.findAll(xml).map { it.groupValues[1] }.toList()
            val neoVer = matches.lastOrNull() ?: matches.firstOrNull()

            if (neoVer == null) {
                ServerRuntime.append("Помилка: NeoForge не знайдено для версії $version")
                return
            }

            val installerUrl = "https://maven.neoforged.net/releases/net/neoforged/neoforge/$neoVer/neoforge-$neoVer-installer.jar"
            val installerFile = File(serverDir, "neoforge-installer.jar")

            ServerRuntime.append("Завантаження інсталятора NeoForge $neoVer...")
            downloadFileFromUrl(installerUrl, installerFile)

            ServerRuntime.append("Встановлення NeoForge через Java...")
            val javaVer = deduceJavaVersion(version)
            val jre = JreManager(context, javaVer)
            jre.ensureBinJavaSymlink()
            val javaExe = File(jre.jreHome, "bin/java")
            if (!javaExe.exists()) {
                jre.install()
            }

            val process = ProcessBuilder(javaExe.absolutePath, "-jar", "neoforge-installer.jar", "--installServer")
                .directory(serverDir)
                .redirectErrorStream(true)
                .start()

            process.inputStream.bufferedReader().readText()
            process.waitFor()

            installerFile.delete()

            val files = serverDir.listFiles() ?: emptyArray()
            val foundJar = files.firstOrNull { it.name.startsWith("neoforge-") && it.name.endsWith(".jar") && !it.name.contains("installer") }
            if (foundJar != null) {
                foundJar.renameTo(targetJar)
            } else {
                File(serverDir, "neoforge-$neoVer.jar").renameTo(targetJar)
            }

            ServerRuntime.append("Успішно встановлено: NeoForge $version -> server.jar")
            onComplete?.invoke()
        } catch (e: Exception) {
            ServerRuntime.append("Помилка встановлення NeoForge: ${e.message}")
        }
    }

    private fun downloadFileFromUrl(urlStr: String, targetFile: File) {
        val conn = URL(urlStr).openConnection() as HttpURLConnection
        conn.setRequestProperty("User-Agent", "JvmDeck-Android")
        conn.connectTimeout = 15000
        conn.readTimeout = 60000

        conn.inputStream.use { input ->
            targetFile.outputStream().use { output -> input.copyTo(output) }
        }
    }
}

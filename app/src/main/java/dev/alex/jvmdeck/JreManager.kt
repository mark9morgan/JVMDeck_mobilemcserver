package dev.alex.jvmdeck

import android.content.Context
import android.system.Os
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import java.io.File

/**
 * Виконуваний файл "java" лежить у jniLibs/arm64-v8a/libjava_XX.so (перейменований bin/java),
 * бо Android 10+ забороняє execve з filesDir.
 * Решта JRE (lib/, conf/ ...) це дані, їх розпаковуємо з assets в filesDir/<jreDirName>.
 */
class JreManager(
    private val ctx: Context,
    val majorVersion: Int = 21,
    val assetDir: String = JRE_ASSET_DIR,
    val jreDirName: String = "jre-$majorVersion"
) {
    val javaBin: File
        get() {
            val nativeDir = File(ctx.applicationInfo.nativeLibraryDir)
            val versionedBin = File(nativeDir, "libjava_$majorVersion.so")
            if (versionedBin.exists()) return versionedBin

            val fallbacks = listOf("libjava_21.so", "libjava_17.so", "libjava_8.so", "libjava_25.so", "libjava.so")
            for (f in fallbacks) {
                val candidate = File(nativeDir, f)
                if (candidate.exists()) return candidate
            }
            return File(nativeDir, "libjava.so")
        }

    val jreHome: File get() = File(ctx.filesDir, jreDirName)
    private val marker: File get() = File(jreHome, ".installed_v${JRE_VERSION}_$assetDir")

    fun isInstalled() = marker.exists()

    fun install() {
        if (isInstalled()) return
        jreHome.deleteRecursively()
        jreHome.mkdirs()

        val effectiveAssetDir = try {
            val list = ctx.assets.list(assetDir)
            if (list != null && list.isNotEmpty()) assetDir else JRE_ASSET_DIR
        } catch (_: Exception) {
            JRE_ASSET_DIR
        }

        val assetFiles = try { ctx.assets.list(effectiveAssetDir)?.toList() ?: emptyList() } catch (_: Exception) { emptyList() }
        val targets = listOf("jre.tar.xz", "universal.tar.xz", "bin-arm64.tar.xz").filter { assetFiles.contains(it) }

        if (targets.isEmpty()) {
            unpackTarXz("$effectiveAssetDir/universal.tar.xz")
            unpackTarXz("$effectiveAssetDir/bin-arm64.tar.xz")
        } else {
            for (archive in targets) {
                unpackTarXz("$effectiveAssetDir/$archive")
            }
        }

        flattenNestedDir()
        ensureLibraryLinks()
        ensureBinJavaSymlink()

        marker.createNewFile()
    }

    private fun flattenNestedDir() {
        listOf("jre", "jdk").forEach { dirName ->
            val nested = File(jreHome, dirName)
            if (nested.exists() && nested.isDirectory) {
                nested.listFiles()?.forEach { file ->
                    val target = File(jreHome, file.name)
                    if (!target.exists()) {
                        file.renameTo(target)
                    }
                }
            }
        }
    }

    fun ensureBinJavaSymlink() {
        val binDir = File(jreHome, "bin")
        binDir.mkdirs()
        val javaSymlink = File(binDir, "java")
        val targetBin = javaBin // points to nativeLibraryDir/libjava_XX.so
        if (targetBin.exists()) {
            runCatching {
                javaSymlink.delete()
                Os.symlink(targetBin.absolutePath, javaSymlink.absolutePath)
            }
        }
    }

    private fun ensureLibraryLinks() {
        val libDir = File(jreHome, "lib")
        val serverDir = File(libDir, "server")
        libDir.mkdirs()
        serverDir.mkdirs()

        // Створюємо посилання у lib/ та lib/server/ для всіх .so бібліотек (наприклад, libmanagement.so, libnet.so, libnio.so)
        jreHome.walkTopDown().filter { it.isFile && it.name.endsWith(".so") }.forEach { soFile ->
            val targetInLib = File(libDir, soFile.name)
            if (!targetInLib.exists()) {
                runCatching { Os.symlink(soFile.absolutePath, targetInLib.absolutePath) }
                if (!targetInLib.exists()) {
                    runCatching { soFile.copyTo(targetInLib, overwrite = true) }
                }
            }
            if (soFile.name == "libjvm.so" || soFile.name == "libjsig.so") {
                val targetInServer = File(serverDir, soFile.name)
                if (!targetInServer.exists()) {
                    runCatching { Os.symlink(soFile.absolutePath, targetInServer.absolutePath) }
                    if (!targetInServer.exists()) {
                        runCatching { soFile.copyTo(targetInServer, overwrite = true) }
                    }
                }
            }
        }
    }

    private fun unpackTarXz(assetPath: String) {
        val root = jreHome.canonicalPath
        try {
            ctx.assets.open(assetPath).use { raw ->
                XZCompressorInputStream(raw.buffered()).use { xz ->
                    TarArchiveInputStream(xz).use { tar ->
                        var entry: TarArchiveEntry? = tar.nextEntry
                        while (entry != null) {
                            var name = entry.name
                            if (name.startsWith("./")) {
                                name = name.substring(2)
                            } else if (name.startsWith("/")) {
                                name = name.substring(1)
                            }

                            // Видаляємо вкладені префікси jre/ або jdk/ з шляху архіву
                            if (name.startsWith("jre/")) {
                                name = name.substring(4)
                            } else if (name.startsWith("jdk/")) {
                                name = name.substring(4)
                            } else if (name == "jre" || name == "jdk") {
                                name = ""
                            }

                            val isBinFile = name == "bin" || name.startsWith("bin/")
                            if (!isBinFile && name.isNotEmpty()) {
                                val out = File(jreHome, name)
                                require(out.canonicalPath.startsWith(root)) { "path traversal: ${entry.name}" }

                                if (entry.isSymbolicLink) {
                                    out.parentFile?.mkdirs()
                                    out.delete()
                                    Os.symlink(entry.linkName, out.absolutePath)
                                } else if (entry.isDirectory) {
                                    out.mkdirs()
                                } else {
                                    out.parentFile?.mkdirs()
                                    out.outputStream().use { tar.copyTo(it) }
                                }
                            }
                            entry = tar.nextEntry
                        }
                    }
                }
            }
        } catch (e: Exception) {
            ServerRuntime.append("Помилка розпакування $assetPath: ${e.message}")
            throw e
        }
    }

    companion object {
        const val JRE_ASSET_DIR = "jre-new"
        const val JRE_VERSION = 9

        fun forJavaVersion(ctx: Context, majorVersion: Int): JreManager {
            val assetList = try { ctx.assets.list("")?.toList() ?: emptyList() } catch (_: Exception) { emptyList() }
            
            val effectiveVer = when {
                majorVersion <= 11 -> 8
                majorVersion in 12..20 -> 17
                majorVersion in 21..24 -> 21
                else -> 25
            }

            val assetDir = when (effectiveVer) {
                8 -> "jre"
                17 -> if (assetList.contains("jre-17")) "jre-17" else "jre-new"
                21 -> if (assetList.contains("jre-21")) "jre-21" else "jre-new"
                25 -> if (assetList.contains("jre-25")) "jre-25" else "jre-new"
                else -> JRE_ASSET_DIR
            }
            val targetDirName = "jre-$effectiveVer"
            return JreManager(ctx, majorVersion = effectiveVer, assetDir = assetDir, jreDirName = targetDirName)
        }
    }
}

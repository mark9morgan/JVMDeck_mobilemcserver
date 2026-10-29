package dev.alex.jvmdeck

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.alex.jvmdeck.model.ServerConfig
import dev.alex.jvmdeck.model.ServerConfigManager
import dev.alex.jvmdeck.model.ServerDownloader
import dev.alex.jvmdeck.ui.components.BottomNavBar
import dev.alex.jvmdeck.ui.components.NavScreen
import dev.alex.jvmdeck.ui.screens.*
import dev.alex.jvmdeck.ui.theme.DarkBg
import dev.alex.jvmdeck.ui.theme.JvmDeckTheme
import java.io.File
import java.net.Inet4Address
import java.net.NetworkInterface

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!ServerRuntime.running.value) {
            kotlin.concurrent.thread(name = "stale-killer-startup") {
                StaleProcessKiller.killStaleJava(applicationContext)
            }
        }
        hideSystemNavBar()
        setContent {
            JvmDeckTheme {
                MainAppScreen()
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemNavBar()
        }
    }

    private fun hideSystemNavBar() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.navigationBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun getFileNameFromUri(context: Context, uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        result = cursor.getString(index)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result ?: "imported_file.jar"
    }

    private fun getLocalIpAddress(): String {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return "127.0.0.1"
            for (intf in interfaces) {
                if (!intf.isUp || intf.isLoopback) continue
                val addrs = intf.inetAddresses
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress ?: continue
                    }
                }
            }
            "127.0.0.1"
        } catch (_: Exception) {
            "127.0.0.1"
        }
    }

    private fun isIgnoringBatteryOptimizations(ctx: Context): Boolean {
        val pm = ctx.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
        return pm.isIgnoringBatteryOptimizations(ctx.packageName)
    }

    @Composable
    private fun MainAppScreen() {
        val context = LocalContext.current
        val log by ServerRuntime.log.collectAsState()
        val running by ServerRuntime.running.collectAsState()
        val serverStatus by ServerRuntime.status.collectAsState()

        var serversList by remember {
            mutableStateOf(ServerConfigManager.loadAllServers(filesDir))
        }

        var activeServer by remember(serversList) {
            mutableStateOf(serversList.firstOrNull() ?: ServerConfig())
        }

        var currentNavScreen by remember { mutableStateOf(NavScreen.SERVERS) }
        var showCreateDialog by remember { mutableStateOf(false) }
        var editingServer by remember { mutableStateOf<ServerConfig?>(null) }
        var showPluginsScreen by remember { mutableStateOf(false) }
        var showModsScreen by remember { mutableStateOf(false) }
        var showFilesScreen by remember { mutableStateOf(false) }

        val activeServerDir = remember(activeServer.id) {
            ServerConfigManager.getServerDir(filesDir, activeServer.id)
        }

        val availableVersions = remember {
            listOf(
                "26.3",
                "1.21.4", "1.21.3", "1.21.2", "1.21.1", "1.21",
                "1.20.6", "1.20.5", "1.20.4", "1.20.3", "1.20.2", "1.20.1", "1.20",
                "1.19.4", "1.19.3", "1.19.2", "1.19.1", "1.19",
                "1.18.2", "1.18.1", "1.18",
                "1.17.1", "1.17",
                "1.16.5", "1.16.4", "1.16.3", "1.16.2", "1.16.1", "1.16",
                "1.15.2", "1.15.1", "1.15",
                "1.14.4", "1.14.3", "1.14.2", "1.14.1", "1.14",
                "1.13.2", "1.13.1", "1.13",
                "1.12.2"
            )
        }

        var isBatteryOk by remember { mutableStateOf(isIgnoringBatteryOptimizations(context)) }
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    isBatteryOk = isIgnoringBatteryOptimizations(context)
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        val notifPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
        LaunchedEffect(Unit) { notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS) }

        var filePickerTargetDir by remember { mutableStateOf<File?>(null) }

        val pickCustomFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri ?: return@rememberLauncherForActivityResult
            val targetDir = filePickerTargetDir ?: activeServerDir
            val fileName = getFileNameFromUri(context, uri)
            runCatching {
                contentResolver.openInputStream(uri)?.use { i ->
                    File(targetDir, fileName).outputStream().use { o -> i.copyTo(o) }
                }
                ServerRuntime.append("Завантажено $fileName в ${targetDir.name}/")
                Toast.makeText(context, "Файл $fileName завантажено", Toast.LENGTH_SHORT).show()
            }
        }

        fun runServerAction(config: ServerConfig) {
            activeServer = config
            val targetDir = ServerConfigManager.getServerDir(filesDir, config.id)
            ServerConfigManager.updateServerConfig(filesDir, config)

            val targetJava = if (config.javaVersionMode == "auto") {
                ServerDownloader.deduceJavaVersion(config.version, config.type)
            } else {
                config.javaVersionMode.toIntOrNull() ?: ServerDownloader.deduceJavaVersion(config.version, config.type)
            }

            ServerRuntime.selectedJavaMode.value = config.javaVersionMode
            ServerRuntime.autoJavaVersion.value = targetJava

            val jarFile = File(targetDir, "server.jar")

            val startBlock = {
                val ram = config.ramMb
                val jvmArgs = mutableListOf(
                    "-Xmx${ram}M",
                    "-Xms256M",
                    "-XX:+UseG1GC",
                    "-Dio.netty.noNative=true",
                    "-Dio.netty.transport.noNative=true",
                    "-Dlog4j2.formatMsgNoLookups=true",
                    "-Dpaper.ignoreJavaVersion=true",
                    "-DPaper.IgnoreJavaVersion=true"
                )
                if (targetJava == 8) {
                    jvmArgs.add("-XX:-UseCompressedOops")
                }
                jvmArgs.addAll(listOf("-jar", "server.jar", "nogui"))

                ServerService.start(
                    this@MainActivity,
                    jvmArgs,
                    targetDir,
                    targetJava
                )
            }

            if (jarFile.exists()) {
                startBlock()
            } else {
                ServerRuntime.append("server.jar відсутній. Автозавантаження ядра ${config.type} ${config.version}...")
                ServerDownloader.downloadServer(context, config.type, config.version, targetDir) {
                    startBlock()
                }
            }
        }

        Scaffold(
            bottomBar = {
                if (!showCreateDialog && editingServer == null && !showPluginsScreen && !showModsScreen && !showFilesScreen) {
                    BottomNavBar(
                        currentScreen = currentNavScreen,
                        onSelectScreen = { currentNavScreen = it }
                    )
                }
            },
            containerColor = DarkBg
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                if (showCreateDialog) {
                    CreateServerDialog(
                        availableVersions = availableVersions,
                        onDismiss = { showCreateDialog = false },
                        onCreateServer = { name, type, version, javaMode, ramMb ->
                            val newCfg = ServerConfigManager.createNewServer(filesDir, name, type, version, javaMode, ramMb)
                            serversList = ServerConfigManager.loadAllServers(filesDir)
                            activeServer = newCfg
                            showCreateDialog = false

                            val newTargetDir = ServerConfigManager.getServerDir(filesDir, newCfg.id)
                            ServerDownloader.downloadServer(context, type, version, newTargetDir)
                            Toast.makeText(context, "Сервер $name створено!", Toast.LENGTH_SHORT).show()
                        }
                    )
                } else if (editingServer != null) {
                    ServerSettingsScreen(
                        config = editingServer!!,
                        isRunning = running && editingServer!!.id == activeServer.id,
                        onSaveConfig = { updated ->
                            ServerConfigManager.updateServerConfig(filesDir, updated)
                            serversList = ServerConfigManager.loadAllServers(filesDir)
                            if (activeServer.id == updated.id) {
                                activeServer = updated
                            }
                            editingServer = null
                            Toast.makeText(context, "Налаштування збережено", Toast.LENGTH_SHORT).show()
                        },
                        onOpenFiles = {
                            activeServer = editingServer!!
                            editingServer = null
                            showFilesScreen = true
                        },
                        onOpenPlugins = {
                            activeServer = editingServer!!
                            editingServer = null
                            showPluginsScreen = true
                        },
                        onOpenMods = {
                            activeServer = editingServer!!
                            editingServer = null
                            showModsScreen = true
                        },
                        onBack = { editingServer = null }
                    )
                } else if (showPluginsScreen) {
                    PluginsScreen(
                        serverName = activeServer.name,
                        mcVersion = activeServer.version,
                        serverDir = activeServerDir,
                        onBack = { showPluginsScreen = false }
                    )
                } else if (showModsScreen) {
                    ModsScreen(
                        serverName = activeServer.name,
                        mcVersion = activeServer.version,
                        serverType = activeServer.type,
                        serverDir = activeServerDir,
                        onBack = { showModsScreen = false }
                    )
                } else if (showFilesScreen) {
                    FileManagerScreen(
                        serverName = activeServer.name,
                        serverDir = activeServerDir,
                        onImportFileToDir = { targetDir ->
                            filePickerTargetDir = targetDir
                            pickCustomFile.launch(arrayOf("*/*"))
                        },
                        onBack = { showFilesScreen = false }
                    )
                } else {
                    when (currentNavScreen) {
                        NavScreen.SERVERS -> {
                            ServersScreen(
                                servers = serversList,
                                activeServerId = activeServer.id,
                                serverStatus = serverStatus,
                                localIpAddress = getLocalIpAddress(),
                                onStartServer = { cfg -> runServerAction(cfg) },
                                onStopServer = { ServerService.stop(this@MainActivity) },
                                onCreateServerClick = { showCreateDialog = true },
                                onSelectServer = { cfg -> editingServer = cfg },
                                onOpenConsole = { cfg ->
                                    activeServer = cfg
                                    currentNavScreen = NavScreen.CONSOLE
                                },
                                onOpenFiles = { cfg ->
                                    activeServer = cfg
                                    showFilesScreen = true
                                },
                                onOpenPlugins = { cfg ->
                                    activeServer = cfg
                                    showPluginsScreen = true
                                },
                                onOpenMods = { cfg ->
                                    activeServer = cfg
                                    showModsScreen = true
                                },
                                onDeleteServer = { cfg ->
                                    serversList = ServerConfigManager.deleteServer(filesDir, cfg.id)
                                    if (activeServer.id == cfg.id) {
                                        activeServer = serversList.firstOrNull() ?: ServerConfig()
                                    }
                                    Toast.makeText(context, "Server deleted", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }

                        NavScreen.CONSOLE -> {
                            ConsoleScreen(
                                serverName = activeServer.name,
                                logLines = log,
                                serverStatus = serverStatus,
                                onSendCommand = { cmd -> ServerService.send(this@MainActivity, cmd) },
                                onStartServer = { runServerAction(activeServer) },
                                onStopServer = { ServerService.stop(this@MainActivity) },
                                onClearLog = { ServerRuntime.log.value = emptyList() }
                            )
                        }

                        NavScreen.MONITOR -> {
                            MonitoringScreen()
                        }

                        NavScreen.SETTINGS -> {
                            AppSettingsScreen(
                                isBatteryOk = isBatteryOk,
                                onRunDiagnostics = {
                                    currentNavScreen = NavScreen.CONSOLE
                                    ServerRuntime.runner(this@MainActivity).diagnostics().forEach(ServerRuntime::append)
                                    ServerRuntime.append("java processes found: ${StaleProcessKiller.listJava(this@MainActivity).size}")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

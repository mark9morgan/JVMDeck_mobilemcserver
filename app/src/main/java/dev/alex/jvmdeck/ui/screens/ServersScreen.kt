package dev.alex.jvmdeck.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.alex.jvmdeck.ServerStatus
import dev.alex.jvmdeck.model.LanguageManager
import dev.alex.jvmdeck.model.ServerConfig
import dev.alex.jvmdeck.ui.components.StatusBadge
import dev.alex.jvmdeck.ui.components.StyledCard
import dev.alex.jvmdeck.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServersScreen(
    servers: List<ServerConfig>,
    activeServerId: String?,
    serverStatus: ServerStatus,
    localIpAddress: String,
    onStartServer: (ServerConfig) -> Unit,
    onStopServer: () -> Unit,
    onCreateServerClick: () -> Unit,
    onSelectServer: (ServerConfig) -> Unit,
    onOpenConsole: (ServerConfig) -> Unit,
    onOpenFiles: (ServerConfig) -> Unit,
    onOpenPlugins: (ServerConfig) -> Unit,
    onOpenMods: (ServerConfig) -> Unit,
    onDeleteServer: (ServerConfig) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = LanguageManager.getString("my_servers"),
                            color = TextWhite,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = onCreateServerClick,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(CardBorder)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = LanguageManager.getString("create_server_title"),
                                tint = TextWhite,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                actions = {},
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        },
        containerColor = DarkBg
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(servers.size) { index ->
                val server = servers[index]
                val currentCardStatus = if (activeServerId == server.id) serverStatus else ServerStatus.OFFLINE

                ServerCardItem(
                    server = server,
                    cardStatus = currentCardStatus,
                    localIp = "$localIpAddress:${server.port}",
                    onTogglePlay = {
                        if (currentCardStatus != ServerStatus.OFFLINE) {
                            onStopServer()
                        } else {
                            onStartServer(server)
                        }
                    },
                    onCopyIp = {
                        clipboardManager.setText(AnnotatedString("$localIpAddress:${server.port}"))
                    },
                    onClick = { onSelectServer(server) },
                    onOpenConsole = { onOpenConsole(server) },
                    onOpenFiles = { onOpenFiles(server) },
                    onOpenPlugins = { onOpenPlugins(server) },
                    onOpenMods = { onOpenMods(server) },
                    onDeleteServer = { onDeleteServer(server) }
                )
            }
        }
    }
}

@Composable
private fun ServerCardItem(
    server: ServerConfig,
    cardStatus: ServerStatus,
    localIp: String,
    onTogglePlay: () -> Unit,
    onCopyIp: () -> Unit,
    onClick: () -> Unit,
    onOpenConsole: () -> Unit,
    onOpenFiles: () -> Unit,
    onOpenPlugins: () -> Unit,
    onOpenMods: () -> Unit,
    onDeleteServer: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val isRunningOrStarting = cardStatus != ServerStatus.OFFLINE

    StyledCard(onClick = onClick) {
        Column {
            // Banner Background Image / Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1E3A8A),
                                CardBg
                            )
                        )
                    )
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = server.name,
                            color = TextWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        StatusBadge(status = cardStatus)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = onTogglePlay,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(if (isRunningOrStarting) StatusRed else PrimaryBlue)
                        ) {
                            Icon(
                                imageVector = if (isRunningOrStarting) Icons.Default.Close else Icons.Default.PlayArrow,
                                contentDescription = if (isRunningOrStarting) LanguageManager.getString("stop") else LanguageManager.getString("start"),
                                tint = TextWhite,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Box {
                            IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = TextWhite)
                            }

                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(LanguageManager.getString("console"), color = TextWhite) },
                                    leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = TextMuted) },
                                    onClick = {
                                        menuExpanded = false
                                        onOpenConsole()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(LanguageManager.getString("files_title"), color = TextWhite) },
                                    leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = TextMuted) },
                                    onClick = {
                                        menuExpanded = false
                                        onOpenFiles()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(LanguageManager.getString("plugins_title"), color = TextWhite) },
                                    leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, tint = TextMuted) },
                                    onClick = {
                                        menuExpanded = false
                                        onOpenPlugins()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(LanguageManager.getString("mods_title"), color = TextWhite) },
                                    leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, tint = TextMuted) },
                                    onClick = {
                                        menuExpanded = false
                                        onOpenMods()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(LanguageManager.getString("settings"), color = TextWhite) },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = TextMuted) },
                                    onClick = {
                                        menuExpanded = false
                                        onClick()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(LanguageManager.getString("delete"), color = StatusRed) },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = StatusRed) },
                                    onClick = {
                                        menuExpanded = false
                                        onDeleteServer()
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Info details row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "0/${server.maxPlayers} ${LanguageManager.getString("players")}",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${server.version} (${server.type}) • ${LanguageManager.getString("port")} ${server.port}",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // IP banner if server is active
            AnimatedVisibility(visible = isRunningOrStarting) {
                Column {
                    HorizontalDivider(color = CardBorder)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(InputBg)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = LanguageManager.getString("local_ip"),
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                            Text(
                                text = localIp,
                                color = TextWhite,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        IconButton(onClick = onCopyIp, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Copy IP",
                                tint = PrimaryBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

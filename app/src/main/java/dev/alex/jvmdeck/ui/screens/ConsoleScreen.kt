package dev.alex.jvmdeck.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.alex.jvmdeck.ServerStatus
import dev.alex.jvmdeck.model.LanguageManager
import dev.alex.jvmdeck.ui.components.StatusBadge
import dev.alex.jvmdeck.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsoleScreen(
    serverName: String,
    logLines: List<String>,
    serverStatus: ServerStatus,
    onSendCommand: (String) -> Unit,
    onStartServer: () -> Unit,
    onStopServer: () -> Unit,
    onClearLog: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val listState = rememberLazyListState()
    var cmdText by remember { mutableStateOf("") }
    val isRunningOrStarting = serverStatus != ServerStatus.OFFLINE

    LaunchedEffect(logLines.size) {
        if (logLines.isNotEmpty()) {
            listState.scrollToItem(logLines.lastIndex)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(LanguageManager.getString("console"), color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            StatusBadge(status = serverStatus)
                        }
                        Text(serverName, color = TextMuted, fontSize = 12.sp)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        clipboardManager.setText(AnnotatedString(logLines.joinToString("\n")))
                    }) {
                        Icon(Icons.Default.Share, contentDescription = LanguageManager.getString("copy_log"), tint = TextMuted)
                    }
                    IconButton(onClick = onClearLog) {
                        Icon(Icons.Default.Clear, contentDescription = LanguageManager.getString("clear_console"), tint = TextMuted)
                    }
                    IconButton(onClick = {
                        if (isRunningOrStarting) onStopServer() else onStartServer()
                    }) {
                        Icon(
                            imageVector = if (isRunningOrStarting) Icons.Default.Clear else Icons.Default.PlayArrow,
                            contentDescription = if (isRunningOrStarting) LanguageManager.getString("stop") else LanguageManager.getString("start"),
                            tint = if (isRunningOrStarting) StatusRed else StatusGreen
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        },
        containerColor = DarkBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Log Container
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = ConsoleBg,
                border = BorderStroke(1.dp, CardBorder)
            ) {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(logLines) { line ->
                        val textColor = when {
                            line.startsWith("$") -> ConsoleGreen
                            line.contains("ERROR") || line.contains("FAILED") || line.contains("Exception") -> ConsoleRed
                            line.contains("WARN") || line.contains("server.properties") -> ConsoleYellow
                            line.contains("Ready!") || line.contains("started!") -> ConsoleGreen
                            else -> TextWhite
                        }

                        SelectionContainer {
                            Text(
                                text = line,
                                color = textColor,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Command Input Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = cmdText,
                    onValueChange = { cmdText = it },
                    placeholder = { Text(LanguageManager.getString("search_placeholder"), color = TextMuted, fontSize = 14.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = CardBorder,
                        focusedContainerColor = InputBg,
                        unfocusedContainerColor = InputBg,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    )
                )

                IconButton(
                    onClick = {
                        val trimmed = cmdText.trim()
                        if (trimmed.isNotEmpty()) {
                            onSendCommand(trimmed)
                            cmdText = ""
                        }
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .background(PrimaryBlue, shape = RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = LanguageManager.getString("start"),
                        tint = TextWhite
                    )
                }
            }

            Spacer(modifier = Modifier.height(70.dp))
        }
    }
}

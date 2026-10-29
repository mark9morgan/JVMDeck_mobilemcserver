package dev.alex.jvmdeck.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.alex.jvmdeck.model.LanguageManager
import dev.alex.jvmdeck.ui.components.StyledCard
import dev.alex.jvmdeck.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileManagerScreen(
    serverName: String,
    serverDir: File,
    onImportFileToDir: (targetDir: File) -> Unit,
    onBack: () -> Unit
) {
    var currentDir by remember(serverDir) { mutableStateOf(serverDir) }
    var editingFile by remember { mutableStateOf<File?>(null) }
    var editContent by remember { mutableStateOf("") }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var showFabMenu by remember { mutableStateOf(false) }

    // Гарантуємо наявність стандартних папок сервера (plugins, world, logs, config, mods)
    LaunchedEffect(serverDir) {
        serverDir.mkdirs()
        listOf("plugins", "world", "logs", "config", "mods").forEach { dirName ->
            File(serverDir, dirName).mkdirs()
        }
    }

    val filesList = remember(currentDir, editingFile, showNewFolderDialog) {
        currentDir.listFiles()?.sortedWith(
            compareBy({ !it.isDirectory }, { it.name.lowercase() })
        ) ?: emptyList()
    }

    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(LanguageManager.getString("files_header_title", serverName), color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        val relPath = currentDir.absolutePath.substringAfter(serverDir.absolutePath).ifEmpty { "/" }
                        Text(relPath, color = TextMuted, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentDir.absolutePath != serverDir.absolutePath && currentDir.parentFile != null) {
                            currentDir = currentDir.parentFile!!
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = LanguageManager.getString("back"), tint = TextWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        },
        floatingActionButton = {
            Box {
                FloatingActionButton(
                    onClick = { showFabMenu = true },
                    containerColor = PrimaryBlue,
                    contentColor = TextWhite,
                    modifier = Modifier.padding(bottom = 70.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = LanguageManager.getString("add"))
                }

                DropdownMenu(
                    expanded = showFabMenu,
                    onDismissRequest = { showFabMenu = false },
                    modifier = Modifier.padding(bottom = 70.dp)
                ) {
                    DropdownMenuItem(
                        text = { Text(LanguageManager.getString("upload_file"), color = TextWhite) },
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, tint = TextMuted) },
                        onClick = {
                            showFabMenu = false
                            onImportFileToDir(currentDir)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(LanguageManager.getString("new_folder"), color = TextWhite) },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = TextMuted) },
                        onClick = {
                            showFabMenu = false
                            newFolderName = ""
                            showNewFolderDialog = true
                        }
                    )
                }
            }
        },
        containerColor = DarkBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                items(filesList) { file ->
                    var menuExpanded by remember { mutableStateOf(false) }

                    StyledCard(
                        onClick = {
                            if (file.isDirectory) {
                                currentDir = file
                            } else if (file.length() < 1024 * 1024) { // Edit text files < 1MB
                                runCatching {
                                    editContent = file.readText(Charsets.UTF_8)
                                    editingFile = file
                                }
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.size(40.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (file.isDirectory) PrimaryBlue.copy(alpha = 0.2f) else CardBorder
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = if (file.isDirectory) "📁" else "📄",
                                            fontSize = 20.sp
                                        )
                                    }
                                }

                                Column {
                                    Text(
                                        text = file.name,
                                        color = TextWhite,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    val sizeStr = if (file.isDirectory) LanguageManager.getString("elements_count", file.listFiles()?.size ?: 0) else formatFileSize(file.length())
                                    Text(
                                        text = "$sizeStr • ${dateFormat.format(Date(file.lastModified()))}",
                                        color = TextMuted,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Box {
                                IconButton(onClick = { menuExpanded = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = LanguageManager.getString("menu"), tint = TextMuted)
                                }

                                DropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = { menuExpanded = false }
                                ) {
                                    if (!file.isDirectory) {
                                        DropdownMenuItem(
                                            text = { Text(LanguageManager.getString("edit"), color = TextWhite) },
                                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = TextMuted) },
                                            onClick = {
                                                menuExpanded = false
                                                runCatching {
                                                    editContent = file.readText(Charsets.UTF_8)
                                                    editingFile = file
                                                }
                                            }
                                        )
                                    }

                                    DropdownMenuItem(
                                        text = { Text(LanguageManager.getString("delete"), color = StatusRed) },
                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = StatusRed) },
                                        onClick = {
                                            menuExpanded = false
                                            file.deleteRecursively()
                                            currentDir = File(currentDir.absolutePath) // Refresh
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Text File Editor Dialog
    if (editingFile != null) {
        AlertDialog(
            onDismissRequest = { editingFile = null },
            title = {
                Text(
                    text = editingFile?.name ?: LanguageManager.getString("files_title"),
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                OutlinedTextField(
                    value = editContent,
                    onValueChange = { editContent = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = CardBorder,
                        focusedContainerColor = ConsoleBg,
                        unfocusedContainerColor = ConsoleBg,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        editingFile?.writeText(editContent, Charsets.UTF_8)
                        editingFile = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text(LanguageManager.getString("save"), color = TextWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingFile = null }) {
                    Text(LanguageManager.getString("cancel"), color = TextMuted)
                }
            },
            containerColor = CardBg
        )
    }

    // New Folder Dialog
    if (showNewFolderDialog) {
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = {
                Text(LanguageManager.getString("new_folder"), color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text(LanguageManager.getString("folder_name")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = CardBorder,
                        focusedContainerColor = InputBg,
                        unfocusedContainerColor = InputBg,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFolderName.isNotBlank()) {
                            File(currentDir, newFolderName.trim()).mkdirs()
                            currentDir = File(currentDir.absolutePath)
                        }
                        showNewFolderDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text(LanguageManager.getString("create"), color = TextWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) {
                    Text(LanguageManager.getString("cancel"), color = TextMuted)
                }
            },
            containerColor = CardBg
        )
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format(Locale.US, "%.1f MB", mb)
    val gb = mb / 1024.0
    return String.format(Locale.US, "%.1f GB", gb)
}

package dev.alex.jvmdeck.ui.screens

import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import dev.alex.jvmdeck.model.*
import dev.alex.jvmdeck.ui.components.StyledCard
import dev.alex.jvmdeck.ui.theme.*
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginsScreen(
    serverName: String,
    mcVersion: String,
    serverDir: File,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("cat_popular") }

    var searchResults by remember { mutableStateOf<List<PluginItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var installedPlugins by remember { mutableStateOf(InstalledPluginsManager.getInstalledPlugins(serverDir)) }
    var installingPluginId by remember { mutableStateOf<String?>(null) }

    val categories = listOf("cat_popular", "cat_recommended", "cat_administration", "cat_optimization", "cat_economy", "cat_installed")

    fun refreshInstalled() {
        installedPlugins = InstalledPluginsManager.getInstalledPlugins(serverDir)
    }

    fun performSearch() {
        if (selectedCategory == "cat_installed") return
        isLoading = true
        val apiCat = when (selectedCategory) {
            "cat_administration" -> "admin"
            "cat_optimization" -> "optimization"
            "cat_economy" -> "economy"
            "cat_recommended" -> "recommended"
            else -> selectedCategory
        }
        ModrinthApi.searchPlugins(
            query = searchQuery,
            mcVersion = mcVersion,
            category = apiCat,
            onResult = { items ->
                searchResults = items
                isLoading = false
            },
            onError = { err ->
                isLoading = false
                Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
            }
        )
    }

    LaunchedEffect(searchQuery, selectedCategory) {
        if (selectedCategory != "cat_installed") {
            performSearch()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(LanguageManager.getString("plugins_mods_title"), color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("$serverName ($mcVersion)", color = TextMuted, fontSize = 12.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = LanguageManager.getString("back"), tint = TextWhite)
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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Search Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(LanguageManager.getString("search_plugins"), color = TextMuted, fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                modifier = Modifier.fillMaxWidth(),
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

            // Category Chips Row
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                categories.forEach { catKey ->
                    val isSelected = catKey == selectedCategory
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = catKey },
                        label = { Text(LanguageManager.getString(catKey), color = if (isSelected) TextWhite else TextMuted, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryBlue,
                            containerColor = InputBg
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = CardBorder,
                            selectedBorderColor = PrimaryBlue,
                            enabled = true,
                            selected = isSelected
                        )
                    )
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
            } else if (selectedCategory == "cat_installed") {
                // Installed Plugins Tab
                if (installedPlugins.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(LanguageManager.getString("no_plugins_folder"), color = TextMuted, fontSize = 14.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(installedPlugins) { plugin ->
                            StyledCard {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = plugin.fileName,
                                            color = TextWhite,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (plugin.isEnabled) LanguageManager.getString("enabled") else LanguageManager.getString("disabled_suffix"),
                                            color = if (plugin.isEnabled) StatusGreen else StatusRed,
                                            fontSize = 12.sp
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Switch(
                                            checked = plugin.isEnabled,
                                            onCheckedChange = {
                                                InstalledPluginsManager.togglePluginState(serverDir, plugin.fileName)
                                                refreshInstalled()
                                            }
                                        )

                                        IconButton(onClick = {
                                            InstalledPluginsManager.deletePlugin(serverDir, plugin.fileName)
                                            refreshInstalled()
                                            Toast.makeText(context, LanguageManager.getString("plugin_deleted"), Toast.LENGTH_SHORT).show()
                                        }) {
                                            Icon(Icons.Default.Delete, contentDescription = LanguageManager.getString("delete"), tint = StatusRed)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Online Store Search Tab
                if (searchResults.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(LanguageManager.getString("no_plugins_found"), color = TextMuted, fontSize = 14.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(searchResults) { item ->
                            val isInstalling = installingPluginId == item.id
                            val isAlreadyInstalled = installedPlugins.any {
                                it.fileName.contains(item.title, ignoreCase = true) || it.fileName.contains(item.slug, ignoreCase = true)
                            }

                            StyledCard {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (item.iconUrl != null) {
                                            AsyncImage(
                                                model = item.iconUrl,
                                                contentDescription = null,
                                                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp))
                                            )
                                        } else {
                                            Surface(
                                                modifier = Modifier.size(44.dp),
                                                shape = RoundedCornerShape(10.dp),
                                                color = CardBorder
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text("🧩", fontSize = 22.sp)
                                                }
                                            }
                                        }

                                        Column {
                                            Text(
                                                text = item.title,
                                                color = TextWhite,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = item.description,
                                                color = TextMuted,
                                                fontSize = 12.sp,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = LanguageManager.getString("downloads_count", item.downloads),
                                                color = PrimaryBlue,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Button(
                                        onClick = {
                                            if (isInstalling) return@Button
                                            installingPluginId = item.id
                                            ModrinthApi.getLatestVersion(
                                                projectId = item.id,
                                                mcVersion = mcVersion,
                                                onResult = { verInfo ->
                                                    InstalledPluginsManager.downloadAndInstallPlugin(
                                                        serverDir = serverDir,
                                                        downloadUrl = verInfo.downloadUrl,
                                                        fileName = verInfo.fileName,
                                                        onComplete = {
                                                            installingPluginId = null
                                                            refreshInstalled()
                                                            Toast.makeText(context, "${item.title} ${LanguageManager.getString("plugin_installed")}", Toast.LENGTH_SHORT).show()
                                                        },
                                                        onError = { err ->
                                                            installingPluginId = null
                                                            Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                                        }
                                                    )
                                                },
                                                onError = { err ->
                                                    installingPluginId = null
                                                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        },
                                        enabled = !isInstalling,
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isAlreadyInstalled) StatusGreen else PrimaryBlue
                                        ),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        if (isInstalling) {
                                            CircularProgressIndicator(
                                                color = TextWhite,
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp
                                            )
                                        } else if (isAlreadyInstalled) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text(LanguageManager.getString("installed"), fontSize = 12.sp)
                                            }
                                        } else {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text(LanguageManager.getString("install"), fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

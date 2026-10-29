package dev.alex.jvmdeck.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.alex.jvmdeck.model.LanguageManager
import dev.alex.jvmdeck.model.ServerConfig
import dev.alex.jvmdeck.ui.components.PrimaryActionButton
import dev.alex.jvmdeck.ui.components.StyledCard
import dev.alex.jvmdeck.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerSettingsScreen(
    config: ServerConfig,
    isRunning: Boolean,
    onSaveConfig: (ServerConfig) -> Unit,
    onOpenFiles: () -> Unit,
    onOpenPlugins: () -> Unit,
    onOpenMods: () -> Unit,
    onBack: () -> Unit
) {
    var name by remember(config) { mutableStateOf(config.name) }
    var portText by remember(config) { mutableStateOf(config.port.toString()) }
    var maxPlayersText by remember(config) { mutableStateOf(config.maxPlayers.toString()) }
    var gamemode by remember(config) { mutableStateOf(config.gamemode) }
    var difficulty by remember(config) { mutableStateOf(config.difficulty) }
    var onlineMode by remember(config) { mutableStateOf(config.onlineMode) }
    var autoRestart by remember(config) { mutableStateOf(config.autoRestart) }
    var lanVisible by remember(config) { mutableStateOf(config.lanVisible) }
    var ramMb by remember(config) { mutableStateOf(config.ramMb) }

    var gamemodeExpanded by remember { mutableStateOf(false) }
    var difficultyExpanded by remember { mutableStateOf(false) }

    val gamemodes = listOf("Survival", "Creative", "Adventure", "Spectator")
    val difficulties = listOf("Peaceful", "Easy", "Normal", "Hard")

    val coreType = config.type.lowercase()
    val supportsPlugins = coreType in listOf("paper", "spigot", "purpur", "bukkit", "mohist", "arclight", "magma")
    val supportsMods = coreType in listOf("fabric", "forge", "neoforge", "mohist", "arclight", "magma")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(LanguageManager.getString("server_settings_title"), color = TextWhite, fontWeight = FontWeight.Bold)
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
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Server Header Card
            StyledCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                            Text(
                                text = LanguageManager.getString("type_version_info", config.type, config.version),
                                fontSize = 13.sp,
                                color = TextMuted
                            )
                        }
                        Icon(Icons.Default.Edit, contentDescription = null, tint = TextMuted)
                    }
                }
            }

            // Quick Tools Row (Adaptive depending on whether server supports plugins, mods, or both)
            Text(LanguageManager.getString("server_tools"), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextWhite)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onOpenFiles,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = InputBg),
                    contentPadding = PaddingValues(4.dp)
                ) {
                    Text("📁 ${LanguageManager.getString("files_title")}", color = TextWhite, fontSize = 12.sp)
                }

                if (supportsPlugins) {
                    Button(
                        onClick = onOpenPlugins,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Text("🧩 ${LanguageManager.getString("plugins_title")}", color = TextWhite, fontSize = 12.sp)
                    }
                }

                if (supportsMods) {
                    Button(
                        onClick = onOpenMods,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Text("⚔️ ${LanguageManager.getString("mods_title")}", color = TextWhite, fontSize = 12.sp)
                    }
                }
            }

            Text(LanguageManager.getString("basic_settings"), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextWhite)

            // Server Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(LanguageManager.getString("server_name")) },
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

            // Port
            OutlinedTextField(
                value = portText,
                onValueChange = { portText = it.filter { c -> c.isDigit() } },
                label = { Text(LanguageManager.getString("port_label")) },
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

            // Max Players
            OutlinedTextField(
                value = maxPlayersText,
                onValueChange = { maxPlayersText = it.filter { c -> c.isDigit() } },
                label = { Text(LanguageManager.getString("max_players")) },
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

            // Gamemode Dropdown
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { gamemodeExpanded = true }
            ) {
                OutlinedTextField(
                    value = gamemode,
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    label = { Text(LanguageManager.getString("gamemode")) },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = { Text("▼", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledBorderColor = CardBorder,
                        disabledContainerColor = InputBg,
                        disabledTextColor = TextWhite,
                        disabledLabelColor = TextMuted
                    )
                )

                DropdownMenu(
                    expanded = gamemodeExpanded,
                    onDismissRequest = { gamemodeExpanded = false }
                ) {
                    gamemodes.forEach { gm ->
                        DropdownMenuItem(
                            text = { Text(gm, color = TextWhite) },
                            onClick = {
                                gamemode = gm
                                gamemodeExpanded = false
                            }
                        )
                    }
                }
            }

            // Difficulty Dropdown
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { difficultyExpanded = true }
            ) {
                OutlinedTextField(
                    value = difficulty,
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    label = { Text(LanguageManager.getString("difficulty")) },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = { Text("▼", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledBorderColor = CardBorder,
                        disabledContainerColor = InputBg,
                        disabledTextColor = TextWhite,
                        disabledLabelColor = TextMuted
                    )
                )

                DropdownMenu(
                    expanded = difficultyExpanded,
                    onDismissRequest = { difficultyExpanded = false }
                ) {
                    difficulties.forEach { diff ->
                        DropdownMenuItem(
                            text = { Text(diff, color = TextWhite) },
                            onClick = {
                                difficulty = diff
                                difficultyExpanded = false
                            }
                        )
                    }
                }
            }

            // RAM Slider Box
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardBg, shape = RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = LanguageManager.getString("ram_allocation"),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextWhite
                    )
                    Text(
                        text = "$ramMb MB (${String.format(Locale.US, "%.1f", ramMb / 1024.0)} GB)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isRunning) TextMuted else PrimaryBlue
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = ramMb.toFloat(),
                    onValueChange = { ramMb = it.toInt() },
                    valueRange = 512f..4096f,
                    steps = ((4096 - 512) / 256) - 1,
                    enabled = !isRunning,
                    colors = SliderDefaults.colors(
                        thumbColor = PrimaryBlue,
                        activeTrackColor = PrimaryBlue,
                        inactiveTrackColor = CardBorder,
                        disabledThumbColor = CardBorder,
                        disabledActiveTrackColor = CardBorder
                    )
                )
                Text(
                    text = if (isRunning) LanguageManager.getString("ram_locked_warning") else LanguageManager.getString("recommended_ram"),
                    fontSize = 11.sp,
                    color = if (isRunning) StatusRed else TextMuted
                )
            }

            Text(LanguageManager.getString("additional_settings"), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextWhite)

            // Switches
            StyledCard {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(LanguageManager.getString("online_mode"), color = if (isRunning) TextMuted else TextWhite, fontSize = 14.sp)
                            if (isRunning) {
                                Text(LanguageManager.getString("online_mode_locked_warning"), color = StatusRed, fontSize = 10.sp)
                            }
                        }
                        Switch(
                            checked = onlineMode,
                            onCheckedChange = { if (!isRunning) onlineMode = it },
                            enabled = !isRunning
                        )
                    }

                    HorizontalDivider(color = CardBorder, modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(LanguageManager.getString("auto_restart"), color = TextWhite, fontSize = 14.sp)
                        Switch(checked = autoRestart, onCheckedChange = { autoRestart = it })
                    }

                    HorizontalDivider(color = CardBorder, modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(LanguageManager.getString("lan_visible"), color = TextWhite, fontSize = 14.sp)
                        Switch(checked = lanVisible, onCheckedChange = { lanVisible = it })
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            PrimaryActionButton(
                text = LanguageManager.getString("save"),
                onClick = {
                    val updated = config.copy(
                        name = name,
                        port = portText.toIntOrNull() ?: 25565,
                        maxPlayers = maxPlayersText.toIntOrNull() ?: 20,
                        gamemode = gamemode,
                        difficulty = difficulty,
                        onlineMode = onlineMode,
                        autoRestart = autoRestart,
                        lanVisible = lanVisible,
                        ramMb = ramMb
                    )
                    onSaveConfig(updated)
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

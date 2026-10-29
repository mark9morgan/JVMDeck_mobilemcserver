package dev.alex.jvmdeck.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.alex.jvmdeck.model.LanguageManager
import dev.alex.jvmdeck.ui.components.PrimaryActionButton
import dev.alex.jvmdeck.ui.theme.*
import java.util.Locale

data class ServerTypeOption(
    val id: String,
    val title: String,
    val subtitleKey: String
)

val SERVER_TYPES = listOf(
    ServerTypeOption("Vanilla", "Vanilla", "server_type_vanilla_desc"),
    ServerTypeOption("Paper", "Paper", "server_type_paper_desc"),
    ServerTypeOption("Spigot", "Spigot", "server_type_spigot_desc"),
    ServerTypeOption("Forge", "Forge", "server_type_forge_desc"),
    ServerTypeOption("NeoForge", "NeoForge", "server_type_neoforge_desc")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateServerDialog(
    availableVersions: List<String>,
    onDismiss: () -> Unit,
    onCreateServer: (name: String, type: String, version: String, javaMode: String, ramMb: Int) -> Unit
) {
    var serverName by remember { mutableStateOf(LanguageManager.getString("default_server_name")) }
    var selectedType by remember { mutableStateOf("Paper") }

    val versionsForType = remember(selectedType) {
        if (selectedType.lowercase() == "paper") {
            availableVersions.filter { it != "1.16.5" }
        } else {
            availableVersions
        }
    }

    var selectedVersion by remember(selectedType) { mutableStateOf(versionsForType.firstOrNull() ?: "1.20.4") }
    var selectedJavaMode by remember { mutableStateOf("auto") }
    var ramMb by remember { mutableStateOf(1024) }

    var versionMenuExpanded by remember { mutableStateOf(false) }
    var javaMenuExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = DarkBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = LanguageManager.getString("cancel"),
                        tint = TextWhite
                    )
                }
                Text(
                    text = LanguageManager.getString("create_server_title"),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = serverName,
                        onValueChange = { serverName = it },
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
                }

                item {
                    Text(
                        text = LanguageManager.getString("server_type_title"),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextWhite
                    )
                }

                items(SERVER_TYPES.size) { index ->
                    val option = SERVER_TYPES[index]
                    val isSelected = option.id == selectedType

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedType = option.id },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isSelected) InputBg else CardBg),
                        border = BorderStroke(1.dp, if (isSelected) PrimaryBlue else CardBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = option.title,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite
                                )
                                Text(
                                    text = LanguageManager.getString(option.subtitleKey),
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = LanguageManager.getString("minecraft_version"),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextWhite
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Box {
                        OutlinedTextField(
                            value = selectedVersion,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { versionMenuExpanded = true },
                            trailingIcon = {
                                Text("▼", color = TextMuted, modifier = Modifier.clickable { versionMenuExpanded = true })
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryBlue,
                                unfocusedBorderColor = CardBorder,
                                focusedContainerColor = InputBg,
                                unfocusedContainerColor = InputBg,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            )
                        )

                        DropdownMenu(
                            expanded = versionMenuExpanded,
                            onDismissRequest = { versionMenuExpanded = false }
                        ) {
                            versionsForType.take(20).forEach { ver ->
                                DropdownMenuItem(
                                    text = { Text(ver, color = TextWhite) },
                                    onClick = {
                                        selectedVersion = ver
                                        versionMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = LanguageManager.getString("java_runtime_version"),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextWhite
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Box {
                        val javaDisplay = if (selectedJavaMode == "auto") LanguageManager.getString("auto_java") else "Java $selectedJavaMode"
                        OutlinedTextField(
                            value = javaDisplay,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { javaMenuExpanded = true },
                            trailingIcon = {
                                Text("▼", color = TextMuted, modifier = Modifier.clickable { javaMenuExpanded = true })
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryBlue,
                                unfocusedBorderColor = CardBorder,
                                focusedContainerColor = InputBg,
                                unfocusedContainerColor = InputBg,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            )
                        )

                        DropdownMenu(
                            expanded = javaMenuExpanded,
                            onDismissRequest = { javaMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(LanguageManager.getString("auto_java_create"), color = TextWhite) },
                                onClick = {
                                    selectedJavaMode = "auto"
                                    javaMenuExpanded = false
                                }
                            )
                            listOf("8", "17", "21", "25").forEach { jVer ->
                                DropdownMenuItem(
                                    text = { Text("Java $jVer", color = TextWhite) },
                                    onClick = {
                                        selectedJavaMode = jVer
                                        javaMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
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
                                text = LanguageManager.getString("ram_allocation_title"),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextWhite
                            )
                            Text(
                                text = "$ramMb MB (${String.format(Locale.US, "%.1f", ramMb / 1024.0)} GB)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlue
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = ramMb.toFloat(),
                            onValueChange = { ramMb = it.toInt() },
                            valueRange = 512f..4096f,
                            steps = ((4096 - 512) / 256) - 1,
                            colors = SliderDefaults.colors(
                                thumbColor = PrimaryBlue,
                                activeTrackColor = PrimaryBlue,
                                inactiveTrackColor = CardBorder
                            )
                        )
                        Text(
                            text = LanguageManager.getString("recommended_ram"),
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            PrimaryActionButton(
                text = LanguageManager.getString("create"),
                onClick = {
                    onCreateServer(serverName, selectedType, selectedVersion, selectedJavaMode, ramMb)
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

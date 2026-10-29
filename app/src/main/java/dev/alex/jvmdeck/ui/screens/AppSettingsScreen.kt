package dev.alex.jvmdeck.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.alex.jvmdeck.model.LanguageManager
import dev.alex.jvmdeck.ui.components.StyledCard
import dev.alex.jvmdeck.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen(
    isBatteryOk: Boolean,
    onRunDiagnostics: () -> Unit
) {
    val context = LocalContext.current
    var notificationsEnabled by remember { mutableStateOf(true) }
    var autoStartEnabled by remember { mutableStateOf(false) }
    var languageExpanded by remember { mutableStateOf(false) }

    val currentLang = LanguageManager.currentLanguage
    val langDisplay = if (currentLang == "en") "English" else "Українська"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(LanguageManager.getString("settings"), color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
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
            Text(LanguageManager.getString("general_section"), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextWhite)

            StyledCard {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Language Switcher Row
                    Box {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { languageExpanded = true }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = TextMuted)
                                Text(LanguageManager.getString("language_label"), color = TextWhite, fontSize = 15.sp)
                            }
                            Text("$langDisplay >", color = PrimaryBlue, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }

                        DropdownMenu(
                            expanded = languageExpanded,
                            onDismissRequest = { languageExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("🇺🇦 Українська", color = TextWhite) },
                                onClick = {
                                    LanguageManager.setLanguage("uk")
                                    languageExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("🇬🇧 English", color = TextWhite) },
                                onClick = {
                                    LanguageManager.setLanguage("en")
                                    languageExpanded = false
                                }
                            )
                        }
                    }

                    HorizontalDivider(color = CardBorder, modifier = Modifier.padding(vertical = 4.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Build, contentDescription = null, tint = TextMuted)
                            Text(LanguageManager.getString("theme_label"), color = TextWhite, fontSize = 15.sp)
                        }
                        Text(LanguageManager.getString("dark_theme"), color = TextMuted, fontSize = 14.sp)
                    }

                    HorizontalDivider(color = CardBorder, modifier = Modifier.padding(vertical = 4.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = TextMuted)
                            Text(LanguageManager.getString("notifications_label"), color = TextWhite, fontSize = 15.sp)
                        }
                        Switch(checked = notificationsEnabled, onCheckedChange = { notificationsEnabled = it })
                    }

                    HorizontalDivider(color = CardBorder, modifier = Modifier.padding(vertical = 4.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = TextMuted)
                            Text(LanguageManager.getString("autostart_label"), color = TextWhite, fontSize = 15.sp)
                        }
                        Switch(checked = autoStartEnabled, onCheckedChange = { autoStartEnabled = it })
                    }
                }
            }

            Text(LanguageManager.getString("optimization_section"), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextWhite)

            StyledCard {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val intent = Intent(
                                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(LanguageManager.getString("battery_optimization"), color = TextWhite, fontSize = 15.sp)
                        Text(
                            text = if (isBatteryOk) LanguageManager.getString("allowed") else LanguageManager.getString("permission_required"),
                            color = if (isBatteryOk) StatusGreen else StatusRed,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    HorizontalDivider(color = CardBorder, modifier = Modifier.padding(vertical = 4.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onRunDiagnostics() }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(LanguageManager.getString("native_diagnostics"), color = TextWhite, fontSize = 15.sp)
                        Text(LanguageManager.getString("run_action"), color = PrimaryBlue, fontSize = 14.sp)
                    }
                }
            }

            Text(LanguageManager.getString("info_section"), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextWhite)

            StyledCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(LanguageManager.getString("app_version_label"), color = TextWhite, fontSize = 15.sp)
                        Text("1.0.0", color = TextMuted, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(LanguageManager.getString("supported_java_label"), color = TextWhite, fontSize = 15.sp)
                        Text("Java 8, 17, 21, 25", color = PrimaryBlue, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

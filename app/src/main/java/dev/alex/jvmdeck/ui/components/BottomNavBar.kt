package dev.alex.jvmdeck.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.alex.jvmdeck.model.LanguageManager
import dev.alex.jvmdeck.ui.theme.*

enum class NavScreen(val labelKey: String, val icon: ImageVector) {
    SERVERS("servers", Icons.Default.Home),
    CONSOLE("console", Icons.AutoMirrored.Filled.List),
    MONITOR("monitoring", Icons.Default.Memory),
    SETTINGS("settings", Icons.Default.Settings)
}

@Composable
fun BottomNavBar(
    currentScreen: NavScreen,
    onSelectScreen: (NavScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp),
        color = CardBg,
        shadowElevation = 8.dp,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavScreen.entries.forEach { screen ->
                val isSelected = currentScreen == screen
                val labelText = LanguageManager.getString(screen.labelKey)

                NavigationBarItem(
                    selected = isSelected,
                    onClick = { onSelectScreen(screen) },
                    icon = {
                        Icon(
                            imageVector = screen.icon,
                            contentDescription = labelText,
                            tint = if (isSelected) PrimaryBlue else TextMuted
                        )
                    },
                    label = {
                        Text(
                            text = labelText,
                            color = if (isSelected) PrimaryBlue else TextMuted,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = PrimaryBlue.copy(alpha = 0.15f)
                    )
                )
            }
        }
    }
}

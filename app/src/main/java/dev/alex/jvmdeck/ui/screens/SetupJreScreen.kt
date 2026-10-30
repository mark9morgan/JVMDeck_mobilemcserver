package dev.alex.jvmdeck.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.alex.jvmdeck.JreManager
import kotlin.concurrent.thread

@Composable
fun SetupJreScreen(onJreInstalled: () -> Unit) {
    val context = LocalContext.current
    var progress by remember { mutableStateOf(0f) }
    var statusText by remember { mutableStateOf("Підготовка до встановлення...") }
    var isDownloading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Автоматично запускаємо встановлення при відкритті екрана
    LaunchedEffect(Unit) {
        if (!isDownloading) {
            isDownloading = true
            errorMessage = null
            thread(name = "jre-auto-download") {
                try {
                    val manager = JreManager.forJavaVersion(context, 21)
                    val releaseBaseUrl = "https://github.com/mark9morgan/JVMDeck_mobilemcserver/releases/download/jre"
                    manager.downloadAndInstall(releaseBaseUrl) { p, s ->
                        progress = p
                        statusText = s
                    }
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        onJreInstalled()
                    }
                } catch (e: Exception) {
                    isDownloading = false
                    errorMessage = "Помилка встановлення: ${e.localizedMessage}"
                }
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Налаштування JvmDeck",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Будь ласка, зачекайте, поки ми підготуємо необхідні системні компоненти для запуску серверів.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(36.dp))

            if (isDownloading) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = errorMessage!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        isDownloading = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Спробувати знову")
                }
            }
        }
    }
}

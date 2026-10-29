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
    var downloadUrl by remember { mutableStateOf("http://10.0.2.2:8000/universal.tar.xz") }
    var progress by remember { mutableStateOf(0f) }
    var statusText by remember { mutableStateOf("Очікування посилання для завантаження JRE...") }
    var isDownloading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Ласкаво просимо в JvmDeck!",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Для запуску Java-серверів (Minecraft тощо) потрібен Java Runtime Environment (JRE). Щоб додаток був легким і відповідав вимогам Google Play, ви можете завантажити JRE з локального сервера на вашому ПК.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = downloadUrl,
                onValueChange = { downloadUrl = it },
                label = { Text("URL архіву JRE (напр. universal.tar.xz)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isDownloading
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("💡 Як підняти локальний сервер на ПК для тесту:", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "1. Покладіть архів `universal.tar.xz` у будь-яку папку на ПК.\n" +
                        "2. У цій папці відкрийте термінал і виконайте:\n" +
                        "   `python -m http.server 8000`\n" +
                        "3. Переконайтеся, що телефон і ПК в одній Wi-Fi мережі (або використовуйте `10.0.2.2` для емулятора).",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (isDownloading) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(statusText, style = MaterialTheme.typography.bodySmall)
            } else {
                Button(
                    onClick = {
                        if (downloadUrl.isBlank()) {
                            errorMessage = "Введіть дійсний URL"
                            return@Button
                        }
                        isDownloading = true
                        errorMessage = null
                        thread(name = "jre-download") {
                            try {
                                val manager = JreManager.forJavaVersion(context, 21)
                                manager.downloadAndInstall(downloadUrl) { p, s ->
                                    progress = p
                                    statusText = s
                                }
                                android.os.Handler(android.os.Looper.getMainLooper()).post {
                                    onJreInstalled()
                                }
                            } catch (e: Exception) {
                                isDownloading = false
                                errorMessage = "Помилка: ${e.localizedMessage}"
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Завантажити та встановити JRE")
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

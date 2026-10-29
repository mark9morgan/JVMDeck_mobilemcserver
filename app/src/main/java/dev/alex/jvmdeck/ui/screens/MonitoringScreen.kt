package dev.alex.jvmdeck.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.alex.jvmdeck.MonitoringRuntime
import dev.alex.jvmdeck.model.LanguageManager
import dev.alex.jvmdeck.ui.components.PerformanceChartCard
import dev.alex.jvmdeck.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitoringScreen() {
    val cpuHist by MonitoringRuntime.cpuHistory.collectAsState()
    val ramHist by MonitoringRuntime.ramHistory.collectAsState()

    val ramTotalMb = MonitoringRuntime.ramTotalMb
    val ramUsedMb = MonitoringRuntime.ramUsedMb
    val appHeapUsedMb = MonitoringRuntime.appHeapUsedMb
    val cpuUsagePercent = MonitoringRuntime.cpuUsagePercent

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(LanguageManager.getString("monitoring_title"), color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        },
        containerColor = DarkBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // RAM Performance Chart Card
            PerformanceChartCard(
                title = LanguageManager.getString("ram_usage"),
                subtitle = LanguageManager.getString("total_ram", ramTotalMb, appHeapUsedMb),
                currentValueText = "$ramUsedMb MB",
                history = ramHist,
                lineColor = PrimaryBlue
            )

            // CPU Performance ChartCard
            PerformanceChartCard(
                title = LanguageManager.getString("cpu_usage"),
                subtitle = LanguageManager.getString("cpu_activity"),
                currentValueText = String.format(Locale.US, "%.1f%%", cpuUsagePercent),
                history = cpuHist,
                lineColor = StatusGreen
            )

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

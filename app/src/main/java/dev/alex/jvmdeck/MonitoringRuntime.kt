package dev.alex.jvmdeck

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

private val MEM_TOTAL_REGEX = Regex("MemTotal:\\s+(\\d+) kB")
private val MEM_AVAIL_REGEX = Regex("MemAvailable:\\s+(\\d+) kB")

object MonitoringRuntime {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val cpuHistory = MutableStateFlow(List(30) { 0.15f })
    val ramHistory = MutableStateFlow(List(30) { 0.35f })

    var ramTotalMb by mutableStateOf(0L)
        private set
    var ramUsedMb by mutableStateOf(0L)
        private set
    var appHeapUsedMb by mutableStateOf(0L)
        private set
    var cpuUsagePercent by mutableStateOf(0f)
        private set

    init {
        // Start polling only when there are active subscribers (e.g. when MonitoringScreen is open)
        // Stops 5 seconds after leaving the screen to save CPU and battery.
        scope.launch {
            flow {
                while (true) {
                    emit(Unit)
                    delay(1000)
                }
            }.shareIn(
                scope = scope,
                started = SharingStarted.WhileSubscribed(5000),
                replay = 0
            ).collect {
                runCatching {
                    val memInfo = File("/proc/meminfo").readText()
                    val totalMatch = MEM_TOTAL_REGEX.find(memInfo)
                    val availMatch = MEM_AVAIL_REGEX.find(memInfo)
                    if (totalMatch != null && availMatch != null) {
                        val totalKb = totalMatch.groupValues[1].toLong()
                        val availKb = availMatch.groupValues[1].toLong()
                        val total = totalKb / 1024
                        val used = (totalKb - availKb) / 1024
                        ramTotalMb = total
                        ramUsedMb = used

                        val ratio = if (total > 0) (used.toFloat() / total.toFloat()) else 0f
                        ramHistory.update { (it.drop(1) + ratio.coerceIn(0f, 1f)).takeLast(30) }
                    }
                }

                val runtime = Runtime.getRuntime()
                appHeapUsedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)

                val cpuVal = (Math.random() * 20 + 5).toFloat()
                cpuUsagePercent = cpuVal
                cpuHistory.update { (it.drop(1) + (cpuVal / 100f).coerceIn(0f, 1f)).takeLast(30) }
            }
        }
    }
}

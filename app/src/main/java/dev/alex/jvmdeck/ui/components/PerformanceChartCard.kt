package dev.alex.jvmdeck.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.alex.jvmdeck.ui.theme.*

@Composable
fun PerformanceChartCard(
    title: String,
    subtitle: String,
    currentValueText: String,
    history: List<Float>, // values between 0f and 1f
    lineColor: Color,
    modifier: Modifier = Modifier
) {
    StyledCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = title, color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(text = subtitle, color = TextMuted, fontSize = 12.sp)
                }
                Text(text = currentValueText, color = lineColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Windows Task Manager style Line Chart Canvas
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                val width = size.width
                val height = size.height

                // Draw background grid lines (horizontal)
                val gridColor = CardBorder.copy(alpha = 0.4f)
                val steps = 4
                for (i in 0..steps) {
                    val y = (height / steps) * i
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                if (history.size < 2) return@Canvas

                val path = Path()
                val fillPath = Path()

                val dx = width / (history.size - 1).coerceAtLeast(1)

                fillPath.moveTo(0f, height)
                path.moveTo(0f, height - (history[0].coerceIn(0f, 1f) * height))

                for (i in history.indices) {
                    val x = i * dx
                    val y = height - (history[i].coerceIn(0f, 1f) * height)
                    path.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }

                fillPath.lineTo(width, height)
                fillPath.close()

                // Draw filled gradient area under curve
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(lineColor.copy(alpha = 0.4f), lineColor.copy(alpha = 0.0f)),
                        startY = 0f,
                        endY = height
                    )
                )

                // Draw line curve
                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }
    }
}

package com.sleep.snore.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class PieSlice(
    val label: String,
    val value: Float,
    val color: Color
)

@Composable
fun SnoreTypePieChart(
    slices: List<PieSlice>,
    modifier: Modifier = Modifier
) {
    if (slices.isEmpty()) return

    val total = slices.sumOf { it.value.toDouble() }.toFloat()
    if (total <= 0f) return

    Column(modifier = modifier) {
        Text(
            "鼾声类型分布",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Canvas(modifier = Modifier.size(120.dp)) {
                var startAngle = -90f
                slices.forEach { slice ->
                    val sweepAngle = (slice.value / total) * 360f
                    drawArc(
                        color = slice.color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = true,
                        size = Size(size.width, size.height),
                        topLeft = Offset.Zero
                    )
                    startAngle += sweepAngle
                }
            }

            Spacer(Modifier.width(24.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                slices.forEach { slice ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Canvas(modifier = Modifier.size(12.dp)) {
                            drawCircle(color = slice.color)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "${slice.label} ${(slice.value / total * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

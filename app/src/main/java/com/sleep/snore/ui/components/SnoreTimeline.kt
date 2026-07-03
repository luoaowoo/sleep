package com.sleep.snore.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * 整夜鼾声时间线柱状图
 *
 * @param hourlyData 每小时数据：Pair(小时标签, 事件数)
 * @param maxValue 最大值 (用于归一化)
 */
@Composable
fun SnoreTimeline(
    hourlyData: List<Pair<String, Int>>,
    maxValue: Int = 20,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            "整夜时间线",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        ) {
            if (hourlyData.isEmpty()) return@Canvas

            val barWidth = size.width / hourlyData.size * 0.7f
            val gap = size.width / hourlyData.size * 0.3f
            val maxH = size.height * 0.85f

            hourlyData.forEachIndexed { index, (_, count) ->
                val heightFraction = (count.toFloat() / maxValue).coerceIn(0.05f, 1f)
                val barHeight = maxH * heightFraction
                val x = index * (barWidth + gap) + gap / 2
                val y = size.height - barHeight

                val fraction = count.toFloat() / maxValue
                val color = when {
                    fraction < 0.25 -> Color(0xFF4CAF50)
                    fraction < 0.5 -> Color(0xFFFFC107)
                    fraction < 0.75 -> Color(0xFFFF9800)
                    else -> Color(0xFFF44336)
                }

                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(4f, 4f)
                )
            }
        }

        if (hourlyData.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                hourlyData.forEach { (label, _) ->
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

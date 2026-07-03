package com.sleep.snore.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.sleep.snore.ui.theme.snoreScoreColor

/**
 * SnoreScore 趋势折线图
 *
 * @param data 分数列表 (越往后越新)
 * @param labels X轴标签
 */
@Composable
fun TrendChart(
    data: List<Int>,
    labels: List<String> = emptyList(),
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            "SnoreScore 趋势",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            if (data.size < 2) return@Canvas

            val padding = 24f
            val w = size.width - padding * 2
            val h = size.height - padding * 2
            val stepX = if (data.size > 1) w / (data.size - 1) else 0f

            for (i in 0..4) {
                val y = padding + h * i / 4
                drawLine(
                    Color.LightGray.copy(alpha = 0.3f),
                    Offset(padding, y),
                    Offset(padding + w, y),
                    strokeWidth = 1f
                )
            }

            val path = Path()
            data.forEachIndexed { index, score ->
                val x = padding + index * stepX
                val y = padding + h * (1 - score / 100f)
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }

            drawPath(
                path,
                color = Color(0xFF6750A4),
                style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            data.forEachIndexed { index, score ->
                val x = padding + index * stepX
                val y = padding + h * (1 - score / 100f)
                drawCircle(
                    color = snoreScoreColor(score).toArgb().let { Color(it) },
                    radius = 5f,
                    center = Offset(x, y)
                )
            }
        }

        if (labels.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                labels.forEach { label ->
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

fun androidx.compose.ui.graphics.Color.toArgb(): Int {
    return (this.alpha * 255).toInt() shl 24 or
            (this.red * 255).toInt() shl 16 or
            (this.green * 255).toInt() shl 8 or
            (this.blue * 255).toInt()
}

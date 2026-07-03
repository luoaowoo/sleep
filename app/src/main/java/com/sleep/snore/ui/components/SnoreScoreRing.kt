package com.sleep.snore.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sleep.snore.data.model.Severity
import com.sleep.snore.data.model.severityFromScore
import com.sleep.snore.ui.theme.snoreScoreColor

@Composable
fun SnoreScoreRing(
    score: Int,
    size: Dp = 160.dp,
    modifier: Modifier = Modifier
) {
    val animatedScore by animateFloatAsState(
        targetValue = score / 100f,
        animationSpec = tween(800)
    )
    val color = snoreScoreColor(score)
    val severity = severityFromScore(score)
    val strokeWidth = size / 12

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val sweepAngle = animatedScore * 270f
            val startAngle = 135f
            val arcSize = Size(size.toPx() - strokeWidth.toPx(), size.toPx() - strokeWidth.toPx())
            val topLeft = Offset(strokeWidth.toPx() / 2, strokeWidth.toPx() / 2)

            // 背景弧
            drawArc(
                color = color.copy(alpha = 0.15f),
                startAngle = startAngle,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )

            // 进度弧
            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$score",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = color
            )
            Text(
                text = severity.label,
                style = MaterialTheme.typography.labelMedium,
                color = color
            )
        }
    }
}

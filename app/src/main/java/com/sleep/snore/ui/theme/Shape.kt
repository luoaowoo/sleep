package com.sleep.snore.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val SleepSnoreShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/** 药丸形圆角 (完全圆角) */
val PillShape = RoundedCornerShape(percent = 50)

/** SnoreScore 大圆环卡片圆角 */
val HeroCardShape = RoundedCornerShape(28.dp)

package com.sleep.snore.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp
import com.sleep.snore.data.model.CardCornerStyle

/** 默认 Shapes（标准圆角），保留以兼容旧引用 */
val SleepSnoreShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/** 药丸形圆角 (完全圆角) */
val PillShape = RoundedCornerShape(percent = 50)

/**
 * 根据卡片圆角风格生成对应的 Shapes。
 * - STANDARD: 28dp（大圆角，默认）
 * - SOFT: 20dp（柔和）
 * - SHARP: 12dp（锐利）
 *
 * extraSmall / small / medium / large 保持一致，仅 extraLarge 随风格变化，
 * 这样 HeroCard 等使用 `MaterialTheme.shapes.extraLarge` 的组件会响应风格切换。
 *
 * @param style 卡片圆角风格
 */
fun cardShapes(style: CardCornerStyle): Shapes {
    val extraLargeDp = when (style) {
        CardCornerStyle.STANDARD -> 28.dp
        CardCornerStyle.SOFT -> 20.dp
        CardCornerStyle.SHARP -> 12.dp
    }
    return Shapes(
        extraSmall = RoundedCornerShape(4.dp),
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(12.dp),
        large = RoundedCornerShape(16.dp),
        extraLarge = RoundedCornerShape(extraLargeDp)
    )
}

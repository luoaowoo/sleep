package com.sleep.snore.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.sleep.snore.data.model.FontScale

/**
 * 根据字号缩放选项生成对应的 Typography。
 * - 对所有 TextStyle 的 fontSize 应用缩放系数（label 系列除外，保持原值）
 * - lineHeight 同步应用缩放系数，并保证 lineHeight ≥ fontSize * 1.2
 * - 字重、字间距等其它 TextStyle 属性保持原值
 *
 * @param scale 字号缩放档位
 */
fun scaledTypography(scale: FontScale): Typography {
    val mult = when (scale) {
        FontScale.SMALL -> 0.9f
        FontScale.STANDARD -> 1.0f
        FontScale.LARGE -> 1.15f
    }
    return Typography(
        // === Display ===
        displayLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = (57f * mult).sp,
            lineHeight = (70f * mult).sp,
            letterSpacing = (-0.25).sp
        ),
        displayMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = (45f * mult).sp,
            lineHeight = (56f * mult).sp,
            letterSpacing = 0.sp
        ),
        displaySmall = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = (36f * mult).sp,
            lineHeight = (44f * mult).sp,
            letterSpacing = 0.sp
        ),

        // === Headline ===
        headlineLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = (32f * mult).sp,
            lineHeight = (40f * mult).sp,
            letterSpacing = 0.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = (28f * mult).sp,
            lineHeight = (36f * mult).sp,
            letterSpacing = 0.sp
        ),
        headlineSmall = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = (24f * mult).sp,
            lineHeight = (32f * mult).sp,
            letterSpacing = 0.sp
        ),

        // === Title ===
        titleLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = (22f * mult).sp,
            lineHeight = (28f * mult).sp,
            letterSpacing = 0.sp
        ),
        titleMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = (16f * mult).sp,
            lineHeight = (24f * mult).sp,
            letterSpacing = 0.15.sp
        ),
        titleSmall = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = (14f * mult).sp,
            lineHeight = (20f * mult).sp,
            letterSpacing = 0.1.sp
        ),

        // === Body ===
        bodyLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = (16f * mult).sp,
            lineHeight = (24f * mult).sp,
            letterSpacing = 0.5.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = (14f * mult).sp,
            lineHeight = (20f * mult).sp,
            letterSpacing = 0.25.sp
        ),
        bodySmall = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = (12f * mult).sp,
            lineHeight = (16f * mult).sp,
            letterSpacing = 0.4.sp
        ),

        // === Label (fontSize 保持原值不缩放，lineHeight 同步缩放) ===
        labelLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = (20f * mult).sp,
            letterSpacing = 0.1.sp
        ),
        labelMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = (16f * mult).sp,
            letterSpacing = 0.5.sp
        ),
        labelSmall = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = (16f * mult).sp,
            letterSpacing = 0.5.sp
        )
    )
}

/** 默认 Typography（标准字号），保留以兼容旧引用 */
val SleepSnoreTypography: Typography = scaledTypography(FontScale.STANDARD)

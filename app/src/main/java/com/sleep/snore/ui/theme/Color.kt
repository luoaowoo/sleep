package com.sleep.snore.ui.theme

import androidx.compose.ui.graphics.Color

// ========== Light Theme Tonal Palette (Seed: #6750A4 靛蓝紫) ==========

// Primary
val PrimaryLight = Color(0xFF6750A4)
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFEADDFF)
val OnPrimaryContainerLight = Color(0xFF21005D)

// Secondary
val SecondaryLight = Color(0xFF625B71)
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFE8DEF8)
val OnSecondaryContainerLight = Color(0xFF1D192B)

// Tertiary
val TertiaryLight = Color(0xFF7D5260)
val OnTertiaryLight = Color(0xFFFFFFFF)
val TertiaryContainerLight = Color(0xFFFFD8E4)
val OnTertiaryContainerLight = Color(0xFF31111D)

// Error
val ErrorLight = Color(0xFFB3261E)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFF9DEDC)
val OnErrorContainerLight = Color(0xFF410E0B)

// Surface & Background
val SurfaceLight = Color(0xFFFFFBFE)
val OnSurfaceLight = Color(0xFF1C1B1F)
val SurfaceVariantLight = Color(0xFFE7E0EC)
val OnSurfaceVariantLight = Color(0xFF49454F)

val BackgroundLight = Color(0xFFFFFBFE)
val OnBackgroundLight = Color(0xFF1C1B1F)

val OutlineLight = Color(0xFF79747E)
val OutlineVariantLight = Color(0xFFCAC4D0)

// Inverse
val InverseSurfaceLight = Color(0xFF313033)
val InverseOnSurfaceLight = Color(0xFFF4EFF4)
val InversePrimaryLight = Color(0xFFD0BCFF)

val SurfaceTintLight = Color(0xFF6750A4)
val SurfaceDimLight = Color(0xFFDED8E1)

// Scrim
val ScrimLight = Color(0xFF000000)

// ========== Dark Theme Tonal Palette ==========

val PrimaryDark = Color(0xFFD0BCFF)
val OnPrimaryDark = Color(0xFF381E72)
val PrimaryContainerDark = Color(0xFF4F378B)
val OnPrimaryContainerDark = Color(0xFFEADDFF)

val SecondaryDark = Color(0xFFCCC2DC)
val OnSecondaryDark = Color(0xFF332D41)
val SecondaryContainerDark = Color(0xFF4A4458)
val OnSecondaryContainerDark = Color(0xFFE8DEF8)

val TertiaryDark = Color(0xFFEFB8C8)
val OnTertiaryDark = Color(0xFF492532)
val TertiaryContainerDark = Color(0xFF633B48)
val OnTertiaryContainerDark = Color(0xFFFFD8E4)

val ErrorDark = Color(0xFFF2B8B5)
val OnErrorDark = Color(0xFF601410)
val ErrorContainerDark = Color(0xFF8C1D18)
val OnErrorContainerDark = Color(0xFFF9DEDC)

val SurfaceDark = Color(0xFF1C1B1F)
val OnSurfaceDark = Color(0xFFE6E1E5)
val SurfaceVariantDark = Color(0xFF49454F)
val OnSurfaceVariantDark = Color(0xFFCAC4D0)

val BackgroundDark = Color(0xFF1C1B1F)
val OnBackgroundDark = Color(0xFFE6E1E5)

val OutlineDark = Color(0xFF938F99)
val OutlineVariantDark = Color(0xFF49454F)

val InverseSurfaceDark = Color(0xFFE6E1E5)
val InverseOnSurfaceDark = Color(0xFF313033)
val InversePrimaryDark = Color(0xFF6750A4)

val SurfaceTintDark = Color(0xFFD0BCFF)
val SurfaceDimDark = Color(0xFF141316)

val ScrimDark = Color(0xFF000000)

// ========== SnoreScore 渐变色 ==========
// SnoreScore 0-30 (良好): 绿色
val SnoreScoreGood = Color(0xFF4CAF50)
// SnoreScore 31-60 (轻度): 黄色
val SnoreScoreMild = Color(0xFFFFC107)
// SnoreScore 61-80 (中度): 橙色
val SnoreScoreModerate = Color(0xFFFF9800)
// SnoreScore 81-100 (重度): 红色
val SnoreScoreSevere = Color(0xFFF44336)

/** 根据 SnoreScore 返回对应颜色 */
fun snoreScoreColor(score: Int): Color = when {
    score <= 30 -> SnoreScoreGood
    score <= 60 -> SnoreScoreMild
    score <= 80 -> SnoreScoreModerate
    else -> SnoreScoreSevere
}

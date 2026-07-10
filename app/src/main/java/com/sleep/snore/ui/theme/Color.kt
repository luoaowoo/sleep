package com.sleep.snore.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.sleep.snore.data.model.AccentColor

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

// ========== 主题 ColorScheme 集合 (4 组强调色 × light/dark) ==========

// ---------- INDIGO（靛紫，默认） ----------
val IndigoLightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    inverseSurface = InverseSurfaceLight,
    inverseOnSurface = InverseOnSurfaceLight,
    inversePrimary = InversePrimaryLight,
    surfaceTint = SurfaceTintLight,
    scrim = ScrimLight,
    surfaceDim = SurfaceDimLight
)

val IndigoDarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    inverseSurface = InverseSurfaceDark,
    inverseOnSurface = InverseOnSurfaceDark,
    inversePrimary = InversePrimaryDark,
    surfaceTint = SurfaceTintDark,
    scrim = ScrimDark,
    surfaceDim = SurfaceDimDark
)

// ---------- BLUE（蓝） ----------
val BlueLightColorScheme = lightColorScheme(
    primary = Color(0xFF2C638B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCBE6FF),
    onPrimaryContainer = Color(0xFF001E2E),
    secondary = Color(0xFF51606F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD5E4F7),
    onSecondaryContainer = Color(0xFF0D1D29),
    tertiary = Color(0xFF695779),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF1DAFF),
    onTertiaryContainer = Color(0xFF231533),
    background = Color(0xFFF7F9FC),
    onBackground = Color(0xFF191C1F),
    surface = Color(0xFFF7F9FC),
    onSurface = Color(0xFF191C1F),
    surfaceVariant = Color(0xFFDDE3EA),
    onSurfaceVariant = Color(0xFF41474D),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF72787E),
    surfaceDim = Color(0xFFD9E2EC)
)

val BlueDarkColorScheme = darkColorScheme(
    primary = Color(0xFF95CCF4),
    onPrimary = Color(0xFF00344C),
    primaryContainer = Color(0xFF094B6D),
    onPrimaryContainer = Color(0xFFCBE6FF),
    secondary = Color(0xFFB9C8DA),
    onSecondary = Color(0xFF23323F),
    secondaryContainer = Color(0xFF394856),
    onSecondaryContainer = Color(0xFFD5E4F7),
    tertiary = Color(0xFFD5BEE5),
    onTertiary = Color(0xFF3A2948),
    tertiaryContainer = Color(0xFF514060),
    onTertiaryContainer = Color(0xFFF1DAFF),
    background = Color(0xFF101417),
    onBackground = Color(0xFFE2E2E5),
    surface = Color(0xFF101417),
    onSurface = Color(0xFFE2E2E5),
    surfaceVariant = Color(0xFF41474D),
    onSurfaceVariant = Color(0xFFC1C7CE),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF8B9198),
    surfaceDim = Color(0xFF0C1216)
)

// ---------- GREEN（绿） ----------
val GreenLightColorScheme = lightColorScheme(
    primary = Color(0xFF4C662C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCDEDA3),
    onPrimaryContainer = Color(0xFF111F00),
    secondary = Color(0xFF586249),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDCE7C8),
    onSecondaryContainer = Color(0xFF151E0A),
    tertiary = Color(0xFF386663),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFBCECE8),
    onTertiaryContainer = Color(0xFF00201E),
    background = Color(0xFFF9FAEF),
    onBackground = Color(0xFF1A1C16),
    surface = Color(0xFFF9FAEF),
    onSurface = Color(0xFF1A1C16),
    surfaceVariant = Color(0xFFE1E4D5),
    onSurfaceVariant = Color(0xFF44483D),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF75796C),
    surfaceDim = Color(0xFFDDE2D0)
)

val GreenDarkColorScheme = darkColorScheme(
    primary = Color(0xFFB1D089),
    onPrimary = Color(0xFF1F3701),
    primaryContainer = Color(0xFF354E16),
    onPrimaryContainer = Color(0xFFCDEDA3),
    secondary = Color(0xFFBFCBAD),
    onSecondary = Color(0xFF2A331E),
    secondaryContainer = Color(0xFF404A33),
    onSecondaryContainer = Color(0xFFDCE7C8),
    tertiary = Color(0xFFA0D0CC),
    onTertiary = Color(0xFF003734),
    tertiaryContainer = Color(0xFF1F4E4B),
    onTertiaryContainer = Color(0xFFBCECE8),
    background = Color(0xFF12140E),
    onBackground = Color(0xFFE2E3D8),
    surface = Color(0xFF12140E),
    onSurface = Color(0xFFE2E3D8),
    surfaceVariant = Color(0xFF44483D),
    onSurfaceVariant = Color(0xFFC5C8BA),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF8E9285),
    surfaceDim = Color(0xFF0C0F08)
)

// ---------- ORANGE（橙） ----------
val OrangeLightColorScheme = lightColorScheme(
    primary = Color(0xFF8D4E00),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDCC2),
    onPrimaryContainer = Color(0xFF2E1500),
    secondary = Color(0xFF735943),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFEDCBE),
    onSecondaryContainer = Color(0xFF2A1707),
    tertiary = Color(0xFF5B6237),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFDFE7AF),
    onTertiaryContainer = Color(0xFF181E00),
    background = Color(0xFFFFF8F4),
    onBackground = Color(0xFF221A14),
    surface = Color(0xFFFFF8F4),
    onSurface = Color(0xFF221A14),
    surfaceVariant = Color(0xFFF4DED3),
    onSurfaceVariant = Color(0xFF52443C),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF85746B),
    surfaceDim = Color(0xFFE8D9CC)
)

val OrangeDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB872),
    onPrimary = Color(0xFF4C2600),
    primaryContainer = Color(0xFF6B3900),
    onPrimaryContainer = Color(0xFFFFDCC2),
    secondary = Color(0xFFE0C0A4),
    onSecondary = Color(0xFF402B18),
    secondaryContainer = Color(0xFF59412C),
    onSecondaryContainer = Color(0xFFFEDCBE),
    tertiary = Color(0xFFC3CB94),
    onTertiary = Color(0xFF2D330F),
    tertiaryContainer = Color(0xFF434A24),
    onTertiaryContainer = Color(0xFFDFE7AF),
    background = Color(0xFF1A120D),
    onBackground = Color(0xFFEFE0D4),
    surface = Color(0xFF1A120D),
    onSurface = Color(0xFFEFE0D4),
    surfaceVariant = Color(0xFF52443C),
    onSurfaceVariant = Color(0xFFD7C3B8),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF9E8D83),
    surfaceDim = Color(0xFF100A07)
)

// ---------- RED（红） ----------
val RedLightColorScheme = lightColorScheme(
    primary = Color(0xFFB3261E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDAD6),
    onPrimaryContainer = Color(0xFF410002),
    secondary = Color(0xFF775651),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDAD6),
    onSecondaryContainer = Color(0xFF2C1512),
    tertiary = Color(0xFF725C2E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDEA8),
    onTertiaryContainer = Color(0xFF261900),
    background = Color(0xFFFFF8F7),
    onBackground = Color(0xFF221A19),
    surface = Color(0xFFFFF8F7),
    onSurface = Color(0xFF221A19),
    surfaceVariant = Color(0xFFF4DDDA),
    onSurfaceVariant = Color(0xFF534341),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF857371),
    surfaceDim = Color(0xFFE8D6D3)
)

val RedDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB4AB),
    onPrimary = Color(0xFF690005),
    primaryContainer = Color(0xFF93000A),
    onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = Color(0xFFE7BDB6),
    onSecondary = Color(0xFF44292A),
    secondaryContainer = Color(0xFF5D3F3E),
    onSecondaryContainer = Color(0xFFFFDAD6),
    tertiary = Color(0xFFE1C28D),
    onTertiary = Color(0xFF402D05),
    tertiaryContainer = Color(0xFF584319),
    onTertiaryContainer = Color(0xFFFFDEA8),
    background = Color(0xFF1A1110),
    onBackground = Color(0xFFEFE0DE),
    surface = Color(0xFF1A1110),
    onSurface = Color(0xFFEFE0DE),
    surfaceVariant = Color(0xFF534341),
    onSurfaceVariant = Color(0xFFD8C2BE),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFFA08C89),
    surfaceDim = Color(0xFF100807)
)

// ---------- CYAN（青） ----------
val CyanLightColorScheme = lightColorScheme(
    primary = Color(0xFF00696D),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF6FF6FE),
    onPrimaryContainer = Color(0xFF002022),
    secondary = Color(0xFF4A6363),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCCE8E7),
    onSecondaryContainer = Color(0xFF051F1F),
    tertiary = Color(0xFF4B607C),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD3E4FF),
    onTertiaryContainer = Color(0xFF041C35),
    background = Color(0xFFFAFDFC),
    onBackground = Color(0xFF191C1C),
    surface = Color(0xFFFAFDFC),
    onSurface = Color(0xFF191C1C),
    surfaceVariant = Color(0xFFDAE5E3),
    onSurfaceVariant = Color(0xFF3F4948),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF6F7978),
    surfaceDim = Color(0xFFD5DBDA)
)

val CyanDarkColorScheme = darkColorScheme(
    primary = Color(0xFF4CDAD1),
    onPrimary = Color(0xFF003739),
    primaryContainer = Color(0xFF004F53),
    onPrimaryContainer = Color(0xFF6FF6FE),
    secondary = Color(0xFFB0CCCB),
    onSecondary = Color(0xFF1B3534),
    secondaryContainer = Color(0xFF324B4B),
    onSecondaryContainer = Color(0xFFCCE8E7),
    tertiary = Color(0xFFB3C8E8),
    onTertiary = Color(0xFF1B314B),
    tertiaryContainer = Color(0xFF334863),
    onTertiaryContainer = Color(0xFFD3E4FF),
    background = Color(0xFF191C1C),
    onBackground = Color(0xFFE0E3E2),
    surface = Color(0xFF191C1C),
    onSurface = Color(0xFFE0E3E2),
    surfaceVariant = Color(0xFF3F4948),
    onSurfaceVariant = Color(0xFFBEC9C7),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF899390),
    surfaceDim = Color(0xFF0E1111)
)

// ---------- PINK（粉） ----------
val PinkLightColorScheme = lightColorScheme(
    primary = Color(0xFF9D4068),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFD9E1),
    onPrimaryContainer = Color(0xFF3E001D),
    secondary = Color(0xFF75565C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFD9DE),
    onSecondaryContainer = Color(0xFF2B151B),
    tertiary = Color(0xFF7B5733),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDDB9),
    onTertiaryContainer = Color(0xFF2C1600),
    background = Color(0xFFFFF8F7),
    onBackground = Color(0xFF221920),
    surface = Color(0xFFFFF8F7),
    onSurface = Color(0xFF221920),
    surfaceVariant = Color(0xFFF3DDE0),
    onSurfaceVariant = Color(0xFF524347),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF847377),
    surfaceDim = Color(0xFFE8D6DA)
)

val PinkDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB1C6),
    onPrimary = Color(0xFF5E1133),
    primaryContainer = Color(0xFF7E294F),
    onPrimaryContainer = Color(0xFFFFD9E1),
    secondary = Color(0xFFE3BDC3),
    onSecondary = Color(0xFF44292E),
    secondaryContainer = Color(0xFF5C3F44),
    onSecondaryContainer = Color(0xFFFFD9DE),
    tertiary = Color(0xFFEDBF8E),
    onTertiary = Color(0xFF452B08),
    tertiaryContainer = Color(0xFF5E411D),
    onTertiaryContainer = Color(0xFFFFDDB9),
    background = Color(0xFF1A1114),
    onBackground = Color(0xFFEFE0E3),
    surface = Color(0xFF1A1114),
    onSurface = Color(0xFFEFE0E3),
    surfaceVariant = Color(0xFF524347),
    onSurfaceVariant = Color(0xFFD7C2C6),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF9F8C90),
    surfaceDim = Color(0xFF10080A)
)

/**
 * 根据强调色枚举返回对应 ColorScheme。
 * @param accentColor 强调色选项
 * @param darkTheme 是否为深色主题
 */
fun colorSchemeFor(accentColor: AccentColor, darkTheme: Boolean): ColorScheme = when (accentColor) {
    AccentColor.INDIGO -> if (darkTheme) IndigoDarkColorScheme else IndigoLightColorScheme
    AccentColor.BLUE -> if (darkTheme) BlueDarkColorScheme else BlueLightColorScheme
    AccentColor.GREEN -> if (darkTheme) GreenDarkColorScheme else GreenLightColorScheme
    AccentColor.ORANGE -> if (darkTheme) OrangeDarkColorScheme else OrangeLightColorScheme
    AccentColor.RED -> if (darkTheme) RedDarkColorScheme else RedLightColorScheme
    AccentColor.CYAN -> if (darkTheme) CyanDarkColorScheme else CyanLightColorScheme
    AccentColor.PINK -> if (darkTheme) PinkDarkColorScheme else PinkLightColorScheme
    AccentColor.CUSTOM -> if (darkTheme) IndigoDarkColorScheme else IndigoLightColorScheme
}

// ========== Recording 沉浸色板（更暗，专注态） ==========
val RecordingSurfaceDim = Color(0xFF0D0C0F)
val RecordingSurface = Color(0xFF141316)
val RecordingOnSurface = Color(0xFFE6E1E5)
val RecordingPrimary = Color(0xFFD0BCFF)
val RecordingOnPrimary = Color(0xFF381E72)
val RecordingError = Color(0xFFFFB4AB)

fun colorSchemeForCustomColor(accentArgb: Int, darkTheme: Boolean): ColorScheme {
    val seed = Color(accentArgb)
    return if (darkTheme) {
        darkColorScheme(
            primary = blend(seed, Color.White, 0.42f),
            onPrimary = readableOn(blend(seed, Color.White, 0.42f)),
            primaryContainer = blend(seed, Color.Black, 0.35f),
            onPrimaryContainer = readableOn(blend(seed, Color.Black, 0.35f)),
            secondary = blend(seed, Color.White, 0.28f),
            onSecondary = readableOn(blend(seed, Color.White, 0.28f)),
            secondaryContainer = blend(seed, Color.Black, 0.48f),
            onSecondaryContainer = readableOn(blend(seed, Color.Black, 0.48f)),
            tertiary = rotateApprox(seed, 0.16f, darkTheme = true),
            onTertiary = readableOn(rotateApprox(seed, 0.16f, darkTheme = true)),
            tertiaryContainer = blend(rotateApprox(seed, 0.16f, darkTheme = true), Color.Black, 0.5f),
            onTertiaryContainer = readableOn(blend(rotateApprox(seed, 0.16f, darkTheme = true), Color.Black, 0.5f)),
            background = Color(0xFF121416),
            onBackground = Color(0xFFE4E2E6),
            surface = Color(0xFF121416),
            onSurface = Color(0xFFE4E2E6),
            surfaceVariant = blend(seed, Color(0xFF202124), 0.82f),
            onSurfaceVariant = Color(0xFFC6C7CB),
            outline = Color(0xFF8F9296),
            error = ErrorDark,
            onError = OnErrorDark,
            errorContainer = ErrorContainerDark,
            onErrorContainer = OnErrorContainerDark,
            surfaceTint = blend(seed, Color.White, 0.42f),
            surfaceDim = Color(0xFF0D0F11)
        )
    } else {
        lightColorScheme(
            primary = seed,
            onPrimary = readableOn(seed),
            primaryContainer = blend(seed, Color.White, 0.78f),
            onPrimaryContainer = readableOn(blend(seed, Color.White, 0.78f)),
            secondary = blend(seed, Color(0xFF5F6368), 0.48f),
            onSecondary = readableOn(blend(seed, Color(0xFF5F6368), 0.48f)),
            secondaryContainer = blend(seed, Color.White, 0.84f),
            onSecondaryContainer = readableOn(blend(seed, Color.White, 0.84f)),
            tertiary = rotateApprox(seed, 0.18f, darkTheme = false),
            onTertiary = readableOn(rotateApprox(seed, 0.18f, darkTheme = false)),
            tertiaryContainer = blend(rotateApprox(seed, 0.18f, darkTheme = false), Color.White, 0.82f),
            onTertiaryContainer = readableOn(blend(rotateApprox(seed, 0.18f, darkTheme = false), Color.White, 0.82f)),
            background = Color(0xFFFAFCFF),
            onBackground = Color(0xFF191C1F),
            surface = Color(0xFFFAFCFF),
            onSurface = Color(0xFF191C1F),
            surfaceVariant = blend(seed, Color.White, 0.88f),
            onSurfaceVariant = Color(0xFF42474D),
            outline = Color(0xFF73777F),
            error = ErrorLight,
            onError = OnErrorLight,
            errorContainer = ErrorContainerLight,
            onErrorContainer = OnErrorContainerLight,
            surfaceTint = seed,
            surfaceDim = Color(0xFFD9DCE1)
        )
    }
}

private fun blend(start: Color, end: Color, amount: Float): Color {
    val ratio = amount.coerceIn(0f, 1f)
    return Color(
        red = start.red + (end.red - start.red) * ratio,
        green = start.green + (end.green - start.green) * ratio,
        blue = start.blue + (end.blue - start.blue) * ratio,
        alpha = 1f
    )
}

private fun readableOn(background: Color): Color {
    return if (contrastRatio(Color.Black, background) >= contrastRatio(Color.White, background)) {
        Color.Black
    } else {
        Color.White
    }
}

internal fun contrastRatio(foreground: Color, background: Color): Float {
    val foregroundLuminance = relativeLuminance(foreground)
    val backgroundLuminance = relativeLuminance(background)
    val lighter = maxOf(foregroundLuminance, backgroundLuminance)
    val darker = minOf(foregroundLuminance, backgroundLuminance)
    return (lighter + 0.05f) / (darker + 0.05f)
}

private fun relativeLuminance(color: Color): Float {
    fun channel(value: Float): Float {
        return if (value <= 0.03928f) {
            value / 12.92f
        } else {
            Math.pow(((value + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()
        }
    }
    return 0.2126f * channel(color.red) + 0.7152f * channel(color.green) + 0.0722f * channel(color.blue)
}

private fun rotateApprox(seed: Color, amount: Float, darkTheme: Boolean): Color {
    val rotated = Color(
        red = seed.green,
        green = seed.blue,
        blue = seed.red,
        alpha = 1f
    )
    return if (darkTheme) blend(rotated, Color.White, amount) else blend(rotated, Color.Black, amount)
}

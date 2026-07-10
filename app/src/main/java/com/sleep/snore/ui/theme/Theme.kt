package com.sleep.snore.ui.theme

import android.app.Activity
import android.graphics.Color
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.sleep.snore.data.model.AccentColor
import com.sleep.snore.data.model.CardCornerStyle
import com.sleep.snore.data.model.FontScale

@Composable
fun SleepSnoreTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    accentColor: AccentColor = AccentColor.INDIGO,
    customAccentColorArgb: Int? = null,
    fontScale: FontScale = FontScale.STANDARD,
    cardCornerStyle: CardCornerStyle = CardCornerStyle.STANDARD,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> colorSchemeForSettings(accentColor, customAccentColorArgb, darkTheme)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Color.TRANSPARENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = scaledTypography(fontScale),
        shapes = cardShapes(cardCornerStyle),
        content = content
    )
}

internal fun colorSchemeForSettings(
    accentColor: AccentColor,
    customAccentColorArgb: Int?,
    darkTheme: Boolean
) = if (accentColor == AccentColor.CUSTOM && customAccentColorArgb != null) {
    colorSchemeForCustomColor(customAccentColorArgb, darkTheme)
} else {
    colorSchemeFor(accentColor, darkTheme)
}

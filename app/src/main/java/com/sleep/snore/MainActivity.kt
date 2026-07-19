package com.sleep.snore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sleep.snore.data.model.AccentColor
import com.sleep.snore.data.model.CardCornerStyle
import com.sleep.snore.data.model.FontScale
import com.sleep.snore.data.preferences.SettingsPreferences
import com.sleep.snore.data.preferences.SettingsPreferencesRepository
import com.sleep.snore.navigation.SleepScaffold
import com.sleep.snore.ui.theme.LocalThemePreviewController
import com.sleep.snore.ui.theme.LocalUiPreferences
import com.sleep.snore.ui.theme.SleepSnoreTheme
import com.sleep.snore.ui.theme.ThemePreviewController
import com.sleep.snore.ui.theme.UiPreferences
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var settingsPreferencesRepository: SettingsPreferencesRepository

    private var startRoute by mutableStateOf<String?>(null)
    private var startRouteRequestId by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updateStartRoute(intent)
        enableEdgeToEdge()
        setContent {
            val settings by settingsPreferencesRepository.settings.collectAsStateWithLifecycle(SettingsPreferences())
            val accentColor by settingsPreferencesRepository.accentColor
                .collectAsStateWithLifecycle(AccentColor.INDIGO)
            val customAccentColorArgb by settingsPreferencesRepository.customAccentColorArgb
                .collectAsStateWithLifecycle(SettingsPreferencesRepository.DEFAULT_CUSTOM_ACCENT_COLOR_ARGB)
            val fontScale by settingsPreferencesRepository.fontScale
                .collectAsStateWithLifecycle(FontScale.STANDARD)
            val cardCornerStyle by settingsPreferencesRepository.cardCornerStyle
                .collectAsStateWithLifecycle(CardCornerStyle.STANDARD)
            var previewAccentColorArgb by remember { mutableStateOf<Int?>(null) }
            LaunchedEffect(
                previewAccentColorArgb,
                settings.dynamicColorEnabled,
                accentColor,
                customAccentColorArgb
            ) {
                val previewArgb = previewAccentColorArgb
                if (
                    previewArgb != null &&
                    !settings.dynamicColorEnabled &&
                    accentColor == AccentColor.CUSTOM &&
                    customAccentColorArgb == previewArgb
                ) {
                    previewAccentColorArgb = null
                }
            }
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (settings.themeMode) {
                SettingsPreferencesRepository.THEME_MODE_LIGHT -> false
                SettingsPreferencesRepository.THEME_MODE_DARK -> true
                else -> systemDark
            }
            val effectiveAccentColor = if (previewAccentColorArgb != null) AccentColor.CUSTOM else accentColor
            val effectiveCustomAccentColorArgb = previewAccentColorArgb ?: customAccentColorArgb
            val effectiveDynamicColor = settings.dynamicColorEnabled && previewAccentColorArgb == null
            SleepSnoreTheme(
                darkTheme = darkTheme,
                dynamicColor = effectiveDynamicColor,
                accentColor = effectiveAccentColor,
                customAccentColorArgb = effectiveCustomAccentColorArgb,
                fontScale = fontScale,
                cardCornerStyle = cardCornerStyle
            ) {
                CompositionLocalProvider(
                    LocalUiPreferences provides UiPreferences(
                        compactModeEnabled = settings.compactModeEnabled,
                        showTechnicalDetails = settings.showTechnicalDetails,
                        accentColor = accentColor,
                        fontScale = fontScale,
                        cardCornerStyle = cardCornerStyle
                    ),
                    LocalThemePreviewController provides ThemePreviewController(
                        previewAccentColorArgb = previewAccentColorArgb,
                        setPreviewAccentColorArgb = { previewAccentColorArgb = it },
                        clearPreviewAccentColorArgb = { previewAccentColorArgb = null }
                    )
                ) {
                    SleepScaffold(
                        startRoute = startRoute,
                        startRouteRequestId = startRouteRequestId
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        updateStartRoute(intent)
    }

    private fun updateStartRoute(intent: android.content.Intent?) {
        startRoute = intent.startRoute()
        startRouteRequestId += 1
    }

    private fun android.content.Intent?.startRoute(): String? {
        return when (this?.getStringExtra(EXTRA_START_ROUTE)) {
            START_ROUTE_SETTINGS -> START_ROUTE_SETTINGS
            else -> null
        }
    }

    companion object {
        const val EXTRA_START_ROUTE = "com.sleep.snore.extra.START_ROUTE"
        const val START_ROUTE_SETTINGS = "settings"
    }
}

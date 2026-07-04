package com.sleep.snore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sleep.snore.data.model.AccentColor
import com.sleep.snore.data.model.CardCornerStyle
import com.sleep.snore.data.model.FontScale
import com.sleep.snore.data.preferences.SettingsPreferences
import com.sleep.snore.data.preferences.SettingsPreferencesRepository
import com.sleep.snore.navigation.SleepScaffold
import com.sleep.snore.ui.theme.LocalUiPreferences
import com.sleep.snore.ui.theme.SleepSnoreTheme
import com.sleep.snore.ui.theme.UiPreferences
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var settingsPreferencesRepository: SettingsPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (settings.themeMode) {
                SettingsPreferencesRepository.THEME_MODE_LIGHT -> false
                SettingsPreferencesRepository.THEME_MODE_DARK -> true
                else -> systemDark
            }
            SleepSnoreTheme(
                darkTheme = darkTheme,
                dynamicColor = settings.dynamicColorEnabled,
                accentColor = accentColor,
                customAccentColorArgb = customAccentColorArgb,
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
                    )
                ) {
                    SleepScaffold()
                }
            }
        }
    }
}

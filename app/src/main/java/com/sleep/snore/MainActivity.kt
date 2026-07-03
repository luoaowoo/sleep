package com.sleep.snore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sleep.snore.data.preferences.SettingsPreferences
import com.sleep.snore.data.preferences.SettingsPreferencesRepository
import com.sleep.snore.navigation.SleepScaffold
import com.sleep.snore.ui.theme.SleepSnoreTheme
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
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (settings.themeMode) {
                SettingsPreferencesRepository.THEME_MODE_LIGHT -> false
                SettingsPreferencesRepository.THEME_MODE_DARK -> true
                else -> systemDark
            }
            SleepSnoreTheme(
                darkTheme = darkTheme,
                dynamicColor = settings.dynamicColorEnabled
            ) {
                SleepScaffold()
            }
        }
    }
}

package com.sleep.snore.ui.screen.settings

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.datastore.preferences.preferencesDataStore
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sleep.snore.data.model.CardCornerStyle
import com.sleep.snore.data.model.FontScale
import com.sleep.snore.data.preferences.SecretTextCipher
import com.sleep.snore.data.preferences.SettingsPreferencesRepository
import com.sleep.snore.recording.RecordingController
import com.sleep.snore.recording.RecordingStartResult
import com.sleep.snore.ui.theme.LocalUiPreferences
import com.sleep.snore.ui.theme.SleepSnoreTheme
import com.sleep.snore.ui.theme.UiPreferences
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private val Context.testSettingsDataStore by preferencesDataStore(name = "test_settings")

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun createViewModel(
        cardCornerStyle: CardCornerStyle = CardCornerStyle.STANDARD,
        fontScale: FontScale = FontScale.STANDARD,
        compactMode: Boolean = false
    ): SettingsViewModel {
        val repository = SettingsPreferencesRepository(context.testSettingsDataStore, FakeSecretTextCipher)
        runBlocking {
            repository.setCardCornerStyle(cardCornerStyle)
            repository.setFontScale(fontScale)
            repository.setCompactModeEnabled(compactMode)
        }
        return SettingsViewModel(context, repository, FakeRecordingController)
    }

    @Test
    fun sharpCornerStyle_optionSelected() {
        val viewModel = createViewModel(cardCornerStyle = CardCornerStyle.SHARP)
        composeRule.setContent {
            SleepSnoreTheme(dynamicColor = false, cardCornerStyle = CardCornerStyle.SHARP) {
                CompositionLocalProvider(
                    LocalUiPreferences provides UiPreferences(
                        cardCornerStyle = CardCornerStyle.SHARP
                    )
                ) {
                    SettingsScreen(
                        navController = rememberNavController(),
                        viewModel = viewModel
                    )
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("锐利").assertIsSelected()
    }

    @Test
    fun compactMode_switchTextVisible() {
        val viewModel = createViewModel(compactMode = true)
        composeRule.setContent {
            SleepSnoreTheme(dynamicColor = false) {
                CompositionLocalProvider(
                    LocalUiPreferences provides UiPreferences(compactModeEnabled = true)
                ) {
                    SettingsScreen(
                        navController = rememberNavController(),
                        viewModel = viewModel
                    )
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("紧凑布局").assertExists()
    }

    @Test
    fun largeFontScale_optionSelected() {
        val viewModel = createViewModel(fontScale = FontScale.LARGE)
        composeRule.setContent {
            SleepSnoreTheme(dynamicColor = false, fontScale = FontScale.LARGE) {
                CompositionLocalProvider(
                    LocalUiPreferences provides UiPreferences(fontScale = FontScale.LARGE)
                ) {
                    SettingsScreen(
                        navController = rememberNavController(),
                        viewModel = viewModel
                    )
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("大").assertIsSelected()
    }

    private object FakeSecretTextCipher : SecretTextCipher {
        override fun encrypt(plainText: String): String = "enc:$plainText"
        override fun decrypt(cipherText: String): String? = cipherText.removePrefix("enc:")
    }

    private object FakeRecordingController : RecordingController {
        override suspend fun startFromSleepTrigger(source: String): RecordingStartResult {
            return RecordingStartResult.Confirmed("started")
        }

        override suspend fun stopFromSleepTrigger(source: String, sleepEndTimeMillis: Long?): Boolean = true

        override fun isRecordingActive(): Boolean = false
    }
}

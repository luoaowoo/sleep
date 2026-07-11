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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Context.testSettingsDataStore by preferencesDataStore(name = "test_settings")

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun createViewModel(
        cardCornerStyle: CardCornerStyle = CardCornerStyle.STANDARD,
        fontScale: FontScale = FontScale.STANDARD,
        compactMode: Boolean = false,
        latestWearableSleepSession: Triple<Long, Long, String>? = null
    ): SettingsViewModel {
        val repository = SettingsPreferencesRepository(context.testSettingsDataStore, FakeSecretTextCipher)
        runBlocking {
            repository.setCardCornerStyle(cardCornerStyle)
            repository.setFontScale(fontScale)
            repository.setCompactModeEnabled(compactMode)
            latestWearableSleepSession?.let { (startMillis, endMillis, status) ->
                repository.setLatestWearableSleepSession(
                    startMillis = startMillis,
                    endMillis = endMillis,
                    status = status
                )
            }
        }
        return SettingsViewModel(
            context,
            repository,
            FakeRecordingController,
            object : WearableStandbyPrerequisiteChecker(context) {
                override suspend fun startBlocker(): String? = null
            }
        )
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

    @Test
    fun wearableLatestSleepSession_visibleWhenSynced() {
        val startMillis = 1_000L
        val endMillis = 8_000L
        val viewModel = createViewModel(
            latestWearableSleepSession = Triple(startMillis, endMillis, "已处理")
        )
        composeRule.setContent {
            SleepSnoreTheme(dynamicColor = false) {
                SettingsScreen(
                    navController = rememberNavController(),
                    viewModel = viewModel
                )
            }
        }

        composeRule.waitForIdle()

        val formatter = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
        val expectedText = "最近同步睡眠：${formatter.format(Date(startMillis))} - ${formatter.format(Date(endMillis))}（已处理）"
        composeRule.onNodeWithText(expectedText).assertExists()
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

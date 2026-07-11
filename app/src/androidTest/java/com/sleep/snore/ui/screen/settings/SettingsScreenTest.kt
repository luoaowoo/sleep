package com.sleep.snore.ui.screen.settings

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.datastore.preferences.preferencesDataStore
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sleep.snore.data.db.SleepDatabase
import com.sleep.snore.data.model.CardCornerStyle
import com.sleep.snore.data.model.FontScale
import com.sleep.snore.data.preferences.SecretTextCipher
import com.sleep.snore.data.preferences.SettingsPreferencesRepository
import com.sleep.snore.data.repository.SleepRepository
import com.sleep.snore.recording.RecordingController
import com.sleep.snore.recording.RecordingStartResult
import com.sleep.snore.ui.theme.LocalUiPreferences
import com.sleep.snore.ui.theme.SleepSnoreTheme
import com.sleep.snore.ui.theme.UiPreferences
import kotlinx.coroutines.runBlocking
import org.junit.After
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
    private val databases = mutableListOf<SleepDatabase>()

    private fun createViewModel(
        cardCornerStyle: CardCornerStyle = CardCornerStyle.STANDARD,
        fontScale: FontScale = FontScale.STANDARD,
        compactMode: Boolean = false,
        latestWearableSleepSession: LatestWearableSleepSession? = null
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
                    status = status,
                    sourcePackage = latestWearableSleepSession.sourcePackage
                )
            }
        }
        return SettingsViewModel(
            context,
            repository,
            createSleepRepository(),
            FakeRecordingController,
            object : WearableStandbyPrerequisiteChecker(context) {
                override suspend fun startBlocker(): String? = null
            }
        )
    }

    @After
    fun tearDown() {
        databases.forEach { it.close() }
        databases.clear()
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
            latestWearableSleepSession = LatestWearableSleepSession(startMillis, endMillis, "已处理")
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

    @Test
    fun wearableLatestSleepSession_showsNonXiaomiDiagnosticWarning() {
        val viewModel = createViewModel(
            latestWearableSleepSession = LatestWearableSleepSession(
                startMillis = 1_000L,
                endMillis = 8_000L,
                status = "非小米来源，仅诊断",
                sourcePackage = "com.example.sleep"
            )
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

        composeRule.onNodeWithText("自动停录判断：会被忽略", substring = true).assertExists()
        composeRule.onNodeWithText("该睡眠记录仅用于诊断", substring = true).assertExists()
    }

    @Test
    fun foregroundKeepaliveSection_clarifiesNoBackgroundMicStart() {
        val viewModel = createViewModel()
        composeRule.setContent {
            SleepSnoreTheme(dynamicColor = false) {
                SettingsScreen(
                    navController = rememberNavController(),
                    viewModel = viewModel
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText("前台检测保活").assertExists()
        composeRule.onNodeWithText("不会后台自动开麦", substring = true).assertExists()
    }

    private fun createSleepRepository(): SleepRepository {
        val database = Room.inMemoryDatabaseBuilder(
            context,
            SleepDatabase::class.java
        ).allowMainThreadQueries().build()
        databases += database
        return SleepRepository(
            context = context,
            sleepRecordDao = database.sleepRecordDao(),
            snoreEventDao = database.snoreEventDao(),
            factorLogDao = database.factorLogDao()
        )
    }

    private data class LatestWearableSleepSession(
        val startMillis: Long,
        val endMillis: Long,
        val status: String,
        val sourcePackage: String = ""
    )

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

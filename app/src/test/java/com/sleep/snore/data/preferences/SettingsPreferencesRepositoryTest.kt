package com.sleep.snore.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.common.truth.Truth.assertThat
import com.sleep.snore.data.model.AccentColor
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsPreferencesRepositoryTest {

    @Test
    fun setAccentColor_disablesDynamicColorAndStoresPresetArgb() = runTest {
        val repository = createFixture().repository

        repository.setDynamicColorEnabled(true)
        repository.setAccentColor(AccentColor.GREEN)

        val settings = repository.settings.first()
        assertThat(settings.dynamicColorEnabled).isFalse()
        assertThat(settings.customAccentColorArgb).isEqualTo(AccentColor.GREEN.defaultArgb)
    }

    @Test
    fun setCustomAccentColorArgb_disablesDynamicColorAndKeepsAlpha() = runTest {
        val repository = createFixture().repository

        repository.setDynamicColorEnabled(true)
        repository.setCustomAccentColorArgb(0x00123456)

        val settings = repository.settings.first()
        assertThat(settings.dynamicColorEnabled).isFalse()
        assertThat(settings.customAccentColorArgb).isEqualTo(0xFF123456.toInt())
        assertThat(repository.accentColor.first()).isEqualTo(AccentColor.CUSTOM)
    }

    @Test
    fun setDeepSeekApiKey_storesEncryptedValueAndClearsPlaintext() = runTest {
        val fixture = createFixture()
        val repository = fixture.repository

        repository.setDeepSeekApiKey("  sk-test  ")

        val preferences = fixture.dataStore.data.first()
        assertThat(preferences[stringPreferencesKey("deepseek_api_key")]).isNull()
        assertThat(preferences[stringPreferencesKey("deepseek_api_key_encrypted")]).isEqualTo("enc:sk-test")
        assertThat(repository.settings.first().deepSeekApiKey).isEqualTo("sk-test")
    }

    @Test
    fun settings_readsLegacyPlaintextDeepSeekApiKey() = runTest {
        val fixture = createFixture()
        val repository = fixture.repository
        fixture.dataStore.editForTest("deepseek_api_key", "legacy-key")

        assertThat(repository.settings.first().deepSeekApiKey).isEqualTo("legacy-key")
        val preferences = fixture.dataStore.data.first()
        assertThat(preferences[stringPreferencesKey("deepseek_api_key")]).isNull()
        assertThat(preferences[stringPreferencesKey("deepseek_api_key_encrypted")]).isEqualTo("enc:legacy-key")
    }

    @Test
    fun wearableSleepTriggerStatus_persistsStatusAndLastCheckTime() = runTest {
        val repository = createFixture().repository

        repository.setWearableSleepTriggerStatus("缺少 Health Connect 权限", checkedAtMillis = 1234L)

        val settings = repository.settings.first()
        assertThat(settings.wearableSleepTriggerStatus).isEqualTo("缺少 Health Connect 权限")
        assertThat(settings.wearableSleepTriggerLastCheckMillis).isEqualTo(1234L)
    }

    @Test
    fun wearableSleepTriggerMessage_doesNotChangeLastCheckTime() = runTest {
        val repository = createFixture().repository

        repository.setWearableSleepTriggerStatus("正在检查最近睡眠记录", checkedAtMillis = 1234L)
        repository.setWearableSleepTriggerMessage("Health Connect 周期检查已关闭")

        val settings = repository.settings.first()
        assertThat(settings.wearableSleepTriggerStatus).isEqualTo("Health Connect 周期检查已关闭")
        assertThat(settings.wearableSleepTriggerLastCheckMillis).isEqualTo(1234L)
    }

    @Test
    fun wearableSleepEventKey_persistsForDuplicateDetection() = runTest {
        val repository = createFixture().repository

        repository.setLastWearableSleepEventKey("SleepEnded:2:1")

        assertThat(repository.getLastWearableSleepEventKey()).isEqualTo("SleepEnded:2:1")
    }

    @Test
    fun latestWearableSleepSession_persistsStartEndTimeAndStatus() = runTest {
        val repository = createFixture().repository

        repository.setLatestWearableSleepSession(
            startMillis = 1_000L,
            endMillis = 8_000L,
            status = "已处理",
            sourcePackage = "com.xiaomi.wearable"
        )

        val settings = repository.settings.first()
        assertThat(settings.latestWearableSleepSessionStartMillis).isEqualTo(1_000L)
        assertThat(settings.latestWearableSleepSessionEndMillis).isEqualTo(8_000L)
        assertThat(settings.latestWearableSleepSessionStatus).isEqualTo("已处理")
        assertThat(settings.latestWearableSleepSessionSourcePackage).isEqualTo("com.xiaomi.wearable")
    }

    @Test
    fun activeRecordingTriggerSource_persistsAndClears() = runTest {
        val repository = createFixture().repository

        repository.setActiveRecordingTriggerSource("health_connect_sleep", startedAtMillis = 4567L)

        val activeSettings = repository.settings.first()
        assertThat(repository.getActiveRecordingTriggerSource()).isEqualTo("health_connect_sleep")
        assertThat(activeSettings.activeRecordingTriggerSource).isEqualTo("health_connect_sleep")
        assertThat(activeSettings.activeRecordingTriggerStartedAtMillis).isEqualTo(4567L)

        repository.clearActiveRecordingTriggerSource()

        val clearedSettings = repository.settings.first()
        assertThat(repository.getActiveRecordingTriggerSource()).isNull()
        assertThat(clearedSettings.activeRecordingTriggerSource).isEmpty()
        assertThat(clearedSettings.activeRecordingTriggerStartedAtMillis).isEqualTo(0L)
    }

    @Test
    fun bedtimeReminder_persistsEnabledAndClampsMinuteOfDay() = runTest {
        val repository = createFixture().repository

        repository.setBedtimeReminderEnabled(true)
        repository.setBedtimeReminderMinuteOfDay(24 * 60 + 30)

        val settings = repository.settings.first()
        assertThat(settings.bedtimeReminderEnabled).isTrue()
        assertThat(settings.bedtimeReminderMinuteOfDay)
            .isEqualTo(SettingsPreferencesRepository.MAX_BEDTIME_REMINDER_MINUTE_OF_DAY)
    }

    private fun createFixture(): RepositoryFixture {
        val dataStoreFile = File.createTempFile("sleep-settings", ".preferences_pb").apply {
            delete()
            deleteOnExit()
        }
        val dataStore = PreferenceDataStoreFactory.create(
            scope = TestScope(UnconfinedTestDispatcher()),
            produceFile = { dataStoreFile }
        )
        return RepositoryFixture(
            repository = SettingsPreferencesRepository(dataStore, FakeSecretTextCipher),
            dataStore = dataStore
        )
    }

    private suspend fun DataStore<Preferences>.editForTest(
        key: String,
        value: String
    ) {
        edit { preferences -> preferences[stringPreferencesKey(key)] = value }
    }

    private object FakeSecretTextCipher : SecretTextCipher {
        override fun encrypt(plainText: String): String = "enc:$plainText"
        override fun decrypt(cipherText: String): String? = cipherText.removePrefix("enc:")
    }

    private data class RepositoryFixture(
        val repository: SettingsPreferencesRepository,
        val dataStore: DataStore<Preferences>
    )
}

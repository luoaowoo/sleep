package com.sleep.snore.sleeptrigger

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.google.common.truth.Truth.assertThat
import com.sleep.snore.data.preferences.SecretTextCipher
import com.sleep.snore.data.preferences.SettingsPreferences
import com.sleep.snore.data.preferences.SettingsPreferencesRepository
import java.io.File
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecordingSleepEndFallbackPollerTest {

    @Test
    fun shouldRunRecordingSleepEndFallback_allowsHealthConnectAutoRecording() {
        val settings = SettingsPreferences(
            wearableSleepTriggerEnabled = true,
            wearableStopOnSleepEndEnabled = true,
            activeRecordingTriggerSource = HealthConnectSleepTriggerSource.SOURCE
        )

        assertThat(shouldRunRecordingSleepEndFallback(settings)).isTrue()
    }

    @Test
    fun shouldRunRecordingSleepEndFallback_rejectsManualRecording() {
        val settings = SettingsPreferences(
            wearableSleepTriggerEnabled = true,
            wearableStopOnSleepEndEnabled = true,
            activeRecordingTriggerSource = ""
        )

        assertThat(shouldRunRecordingSleepEndFallback(settings)).isFalse()
    }

    @Test
    fun shouldRunRecordingSleepEndFallback_rejectsDisabledStopOnSleepEnd() {
        val settings = SettingsPreferences(
            wearableSleepTriggerEnabled = true,
            wearableStopOnSleepEndEnabled = false,
            activeRecordingTriggerSource = HealthConnectSleepTriggerSource.SOURCE
        )

        assertThat(shouldRunRecordingSleepEndFallback(settings)).isFalse()
    }

    @Test
    fun pollOnce_stopsRecordingForSleepEndAndRemembersEvent() = runTest {
        val repository = createRepository()
        repository.setWearableSleepTriggerEnabled(true)
        repository.setWearableStopOnSleepEndEnabled(true)
        repository.setActiveRecordingTriggerSource(HealthConnectSleepTriggerSource.SOURCE, 1234L)
        val poller = RecordingSleepEndFallbackPoller(
            settingsRepository = repository,
            sleepSessionPoller = FakeSleepSessionPoller(
                HealthConnectSleepTriggerSource.PollResult.EventEmitted(
                    event = SleepTriggerEvent.SleepEnded(HealthConnectSleepTriggerSource.SOURCE, timestamp = 2000L),
                    eventKey = "SleepEnded:2000:1234"
                )
            )
        )

        val result = poller.pollOnce(sessionStartTimeMillis = 999L)

        assertThat(result).isEqualTo(
            RecordingSleepEndFallbackResult.StopRecording(
                statusText = "检测到睡眠结束，录音服务正在停止鼾声检测",
                eventKey = "SleepEnded:2000:1234",
                endTimeMillis = 2000L
            )
        )
        assertThat(repository.getLastWearableSleepEventKey()).isNull()
        assertThat(repository.settings.first().wearableSleepTriggerStatus)
            .isEqualTo("检测到睡眠结束，录音服务正在停止鼾声检测")
    }

    @Test
    fun pollOnce_ignoresSleepStartedWithoutRememberingEvent() = runTest {
        val repository = createRepository()
        repository.setWearableSleepTriggerEnabled(true)
        repository.setWearableStopOnSleepEndEnabled(true)
        repository.setActiveRecordingTriggerSource(HealthConnectSleepTriggerSource.SOURCE, 1234L)
        val poller = RecordingSleepEndFallbackPoller(
            settingsRepository = repository,
            sleepSessionPoller = FakeSleepSessionPoller(
                HealthConnectSleepTriggerSource.PollResult.EventEmitted(
                    event = SleepTriggerEvent.SleepStarted(
                        source = HealthConnectSleepTriggerSource.SOURCE,
                        timestamp = 1234L,
                        confidence = 0.8f
                    ),
                    eventKey = "SleepStarted:1234:1234"
                )
            )
        )

        val result = poller.pollOnce(sessionStartTimeMillis = 999L)

        assertThat(result).isEqualTo(RecordingSleepEndFallbackResult.ContinuePolling)
        assertThat(repository.getLastWearableSleepEventKey()).isNull()
    }

    @Test
    fun pollOnce_stopsPollingWhenSourceNoLongerMatches() = runTest {
        val repository = createRepository()
        repository.setWearableSleepTriggerEnabled(true)
        repository.setWearableStopOnSleepEndEnabled(true)
        val poller = RecordingSleepEndFallbackPoller(
            settingsRepository = repository,
            sleepSessionPoller = FakeSleepSessionPoller(HealthConnectSleepTriggerSource.PollResult.NoRecentSleep)
        )

        val result = poller.pollOnce(sessionStartTimeMillis = 999L)

        assertThat(result).isEqualTo(RecordingSleepEndFallbackResult.StopPolling)
    }

    @Test
    fun pollOnce_recordsPermissionMissingStatusAndContinues() = runTest {
        val repository = createRepository()
        repository.setWearableSleepTriggerEnabled(true)
        repository.setWearableStopOnSleepEndEnabled(true)
        repository.setActiveRecordingTriggerSource(HealthConnectSleepTriggerSource.SOURCE, 1234L)
        val poller = RecordingSleepEndFallbackPoller(
            settingsRepository = repository,
            sleepSessionPoller = FakeSleepSessionPoller(HealthConnectSleepTriggerSource.PollResult.PermissionMissing)
        )

        val result = poller.pollOnce(sessionStartTimeMillis = 999L)

        assertThat(result).isEqualTo(RecordingSleepEndFallbackResult.ContinuePolling)
        assertThat(repository.settings.first().wearableSleepTriggerStatus)
            .isEqualTo("录音服务等待睡眠结束：缺少 Health Connect 睡眠/后台读取权限")
    }

    @Test
    fun pollOnce_continuesWhenPollThrows() = runTest {
        val repository = createRepository()
        repository.setWearableSleepTriggerEnabled(true)
        repository.setWearableStopOnSleepEndEnabled(true)
        repository.setActiveRecordingTriggerSource(HealthConnectSleepTriggerSource.SOURCE, 1234L)
        val poller = RecordingSleepEndFallbackPoller(
            settingsRepository = repository,
            sleepSessionPoller = FakeSleepSessionPoller(error = IllegalStateException("boom"))
        )

        val result = poller.pollOnce(sessionStartTimeMillis = 999L)

        assertThat(result).isEqualTo(RecordingSleepEndFallbackResult.ContinuePolling)
        assertThat(repository.settings.first().wearableSleepTriggerStatus)
            .isEqualTo("录音服务检查睡眠结束失败，将继续重试")
    }

    private fun createRepository(): SettingsPreferencesRepository {
        val dataStoreFile = File.createTempFile("recording-sleep-end-fallback", ".preferences_pb").apply {
            delete()
            deleteOnExit()
        }
        val dataStore = PreferenceDataStoreFactory.create(
            scope = TestScope(UnconfinedTestDispatcher()),
            produceFile = { dataStoreFile }
        )
        return SettingsPreferencesRepository(dataStore, FakeSecretTextCipher)
    }

    private class FakeSleepSessionPoller(
        private val result: HealthConnectSleepTriggerSource.PollResult = HealthConnectSleepTriggerSource.PollResult.NoRecentSleep,
        private val error: Throwable? = null
    ) : HealthConnectSleepSessionPoller {
        override suspend fun pollLatestSleepSession(
            now: Instant,
            requireBackgroundRead: Boolean,
            ignoreEventsBefore: Instant?
        ): HealthConnectSleepTriggerSource.PollResult {
            error?.let { throw it }
            return result
        }
    }

    private object FakeSecretTextCipher : SecretTextCipher {
        override fun encrypt(plainText: String): String = "enc:$plainText"
        override fun decrypt(cipherText: String): String? = cipherText.removePrefix("enc:")
    }
}

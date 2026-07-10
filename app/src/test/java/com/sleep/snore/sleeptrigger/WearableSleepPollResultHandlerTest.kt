package com.sleep.snore.sleeptrigger

import com.google.common.truth.Truth.assertThat
import com.sleep.snore.data.preferences.SettingsPreferencesRepository
import com.sleep.snore.recording.RecordingController
import com.sleep.snore.recording.RecordingStartResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class WearableSleepPollResultHandlerTest {

    @Test
    fun handleWearableSleepPollResult_remembersHandledSleepStart() = runTest {
        val settingsRepository = mockk<SettingsPreferencesRepository>(relaxed = true)
        val coordinator = AutoSnoreDetectionCoordinator(
            FakeRecordingController(RecordingStartResult.Confirmed("started"))
        )
        val pollResult = HealthConnectSleepTriggerSource.PollResult.EventEmitted(
            event = SleepTriggerEvent.SleepStarted("health_connect_sleep", timestamp = 1L, confidence = 0.8f),
            eventKey = "SleepStarted:1:1"
        )

        val result = handleWearableSleepPollResult(
            pollResult = pollResult,
            stopOnSleepEnd = true,
            coordinator = coordinator,
            settingsRepository = settingsRepository,
            requireBackgroundRead = false
        )

        assertThat(result.statusText).isEqualTo("started")
        assertThat(result.emittedSleepStart).isTrue()
        assertThat(result.eventHandled).isTrue()
        coVerify { settingsRepository.setLastWearableSleepEventKey("SleepStarted:1:1") }
    }

    @Test
    fun handleWearableSleepPollResult_doesNotRememberUnconfirmedSleepStart() = runTest {
        val settingsRepository = mockk<SettingsPreferencesRepository>(relaxed = true)
        val coordinator = AutoSnoreDetectionCoordinator(
            FakeRecordingController(RecordingStartResult.Failed("missing permission"))
        )
        val pollResult = HealthConnectSleepTriggerSource.PollResult.EventEmitted(
            event = SleepTriggerEvent.SleepStarted("health_connect_sleep", timestamp = 1L, confidence = 0.8f),
            eventKey = "SleepStarted:1:1"
        )

        val result = handleWearableSleepPollResult(
            pollResult = pollResult,
            stopOnSleepEnd = true,
            coordinator = coordinator,
            settingsRepository = settingsRepository,
            requireBackgroundRead = false
        )

        assertThat(result.statusText).isEqualTo("missing permission")
        assertThat(result.emittedSleepStart).isTrue()
        assertThat(result.eventHandled).isFalse()
        coVerify(exactly = 0) { settingsRepository.setLastWearableSleepEventKey(any()) }
    }

    @Test
    fun handleWearableSleepPollResult_mapsForegroundPermissionStatus() = runTest {
        val result = handleWearableSleepPollResult(
            pollResult = HealthConnectSleepTriggerSource.PollResult.PermissionMissing,
            stopOnSleepEnd = true,
            coordinator = AutoSnoreDetectionCoordinator(FakeRecordingController()),
            settingsRepository = mockk(relaxed = true),
            requireBackgroundRead = false
        )

        assertThat(result.statusText).isEqualTo("缺少 Health Connect 睡眠读取权限")
        assertThat(result.emittedSleepStart).isFalse()
        assertThat(result.eventHandled).isFalse()
    }

    private class FakeRecordingController(
        private val startResult: RecordingStartResult = RecordingStartResult.Confirmed("started")
    ) : RecordingController {
        override suspend fun startFromSleepTrigger(source: String): RecordingStartResult = startResult
        override suspend fun stopFromSleepTrigger(source: String): Boolean = true
        override fun isRecordingActive(): Boolean = startResult.confirmed
    }
}

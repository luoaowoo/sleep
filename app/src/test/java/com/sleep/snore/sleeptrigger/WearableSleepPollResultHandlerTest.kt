package com.sleep.snore.sleeptrigger

import com.google.common.truth.Truth.assertThat
import com.sleep.snore.data.preferences.SettingsPreferencesRepository
import com.sleep.snore.recording.RecordingController
import com.sleep.snore.recording.RecordingStartResult
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
            requireBackgroundRead = false,
            allowSleepStartRecording = true
        )

        assertThat(result.statusText).isEqualTo("started")
        assertThat(result.emittedSleepStart).isTrue()
        assertThat(result.emittedSleepEnd).isFalse()
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
            requireBackgroundRead = false,
            allowSleepStartRecording = true
        )

        assertThat(result.statusText).isEqualTo("missing permission")
        assertThat(result.emittedSleepStart).isTrue()
        assertThat(result.emittedSleepEnd).isFalse()
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
        assertThat(result.emittedSleepEnd).isFalse()
        assertThat(result.eventHandled).isFalse()
    }

    @Test
    fun handleWearableSleepPollResult_doesNotStartRecordingWhenSleepStartDisallowed() = runTest {
        val settingsRepository = mockk<SettingsPreferencesRepository>(relaxed = true)
        val controller = FakeRecordingController()
        val pollResult = HealthConnectSleepTriggerSource.PollResult.EventEmitted(
            event = SleepTriggerEvent.SleepStarted("health_connect_sleep", timestamp = 1L, confidence = 0.8f),
            eventKey = "SleepStarted:1:1"
        )

        val result = handleWearableSleepPollResult(
            pollResult = pollResult,
            stopOnSleepEnd = true,
            coordinator = AutoSnoreDetectionCoordinator(controller),
            settingsRepository = settingsRepository,
            requireBackgroundRead = true,
            allowSleepStartRecording = false
        )

        assertThat(result.statusText).isEqualTo("检测到睡眠开始；后台轮询不会直接启动麦克风，请使用睡前前台检测")
        assertThat(result.emittedSleepStart).isTrue()
        assertThat(result.eventHandled).isFalse()
        assertThat(controller.started).isFalse()
        coVerify(exactly = 0) { settingsRepository.setLastWearableSleepEventKey(any()) }
    }

    @Test
    fun handleWearableSleepPollResult_doesNotStartRecordingFromSleepStartByDefault() = runTest {
        val settingsRepository = mockk<SettingsPreferencesRepository>(relaxed = true)
        val controller = FakeRecordingController()
        val pollResult = HealthConnectSleepTriggerSource.PollResult.EventEmitted(
            event = SleepTriggerEvent.SleepStarted("health_connect_sleep", timestamp = 1L, confidence = 0.8f),
            eventKey = "SleepStarted:1:1"
        )

        val result = handleWearableSleepPollResult(
            pollResult = pollResult,
            stopOnSleepEnd = true,
            coordinator = AutoSnoreDetectionCoordinator(controller),
            settingsRepository = settingsRepository,
            requireBackgroundRead = true
        )

        assertThat(result.statusText).isEqualTo("检测到睡眠开始；后台轮询不会直接启动麦克风，请使用睡前前台检测")
        assertThat(result.emittedSleepStart).isTrue()
        assertThat(result.eventHandled).isFalse()
        assertThat(controller.started).isFalse()
        coVerify(exactly = 0) { settingsRepository.setLastWearableSleepEventKey(any()) }
    }

    @Test
    fun handleWearableSleepPollResult_marksHandledSleepEnd() = runTest {
        val settingsRepository = mockk<SettingsPreferencesRepository>(relaxed = true)
        val coordinator = AutoSnoreDetectionCoordinator(FakeRecordingController())
        val pollResult = HealthConnectSleepTriggerSource.PollResult.EventEmitted(
            event = SleepTriggerEvent.SleepEnded("health_connect_sleep", timestamp = 2L),
            eventKey = "SleepEnded:2:1"
        )

        val result = handleWearableSleepPollResult(
            pollResult = pollResult,
            stopOnSleepEnd = true,
            coordinator = coordinator,
            settingsRepository = settingsRepository,
            requireBackgroundRead = true
        )

        assertThat(result.emittedSleepStart).isFalse()
        assertThat(result.emittedSleepEnd).isTrue()
        assertThat(result.eventHandled).isTrue()
        coVerify { settingsRepository.setLastWearableSleepEventKey("SleepEnded:2:1") }
    }

    @Test
    fun handleWearableSleepPollResult_doesNotRememberFailedSleepEnd() = runTest {
        val settingsRepository = mockk<SettingsPreferencesRepository>(relaxed = true)
        val coordinator = AutoSnoreDetectionCoordinator(FakeRecordingController(stopResult = false))
        val pollResult = HealthConnectSleepTriggerSource.PollResult.EventEmitted(
            event = SleepTriggerEvent.SleepEnded("health_connect_sleep", timestamp = 2L),
            eventKey = "SleepEnded:2:1"
        )

        val result = handleWearableSleepPollResult(
            pollResult = pollResult,
            stopOnSleepEnd = true,
            coordinator = coordinator,
            settingsRepository = settingsRepository,
            requireBackgroundRead = true
        )

        assertThat(result.emittedSleepEnd).isTrue()
        assertThat(result.eventHandled).isFalse()
        assertThat(result.statusText).isEqualTo("检测到睡眠结束，但未能停止鼾声检测；将继续重试")
        coVerify(exactly = 0) { settingsRepository.setLastWearableSleepEventKey(any()) }
    }

    @Test
    fun handleWearableSleepPollResult_remembersSleepEndWhenAutoStopDisabled() = runTest {
        val settingsRepository = mockk<SettingsPreferencesRepository>(relaxed = true)
        val coordinator = AutoSnoreDetectionCoordinator(FakeRecordingController())
        val pollResult = HealthConnectSleepTriggerSource.PollResult.EventEmitted(
            event = SleepTriggerEvent.SleepEnded("health_connect_sleep", timestamp = 2L),
            eventKey = "SleepEnded:2:1"
        )

        val result = handleWearableSleepPollResult(
            pollResult = pollResult,
            stopOnSleepEnd = false,
            coordinator = coordinator,
            settingsRepository = settingsRepository,
            requireBackgroundRead = true
        )

        assertThat(result.emittedSleepEnd).isTrue()
        assertThat(result.eventHandled).isFalse()
        assertThat(result.statusText).isEqualTo("检测到睡眠结束，已按设置保持鼾声检测")
        coVerify { settingsRepository.setLastWearableSleepEventKey("SleepEnded:2:1") }
    }

    private class FakeRecordingController(
        private val startResult: RecordingStartResult = RecordingStartResult.Confirmed("started"),
        private val stopResult: Boolean = true
    ) : RecordingController {
        var started = false
            private set

        override suspend fun startFromSleepTrigger(source: String): RecordingStartResult {
            started = true
            return startResult
        }
        override suspend fun stopFromSleepTrigger(source: String, sleepEndTimeMillis: Long?): Boolean = stopResult
        override fun isRecordingActive(): Boolean = startResult.confirmed
    }
}

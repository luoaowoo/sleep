package com.sleep.snore.sleeptrigger

import com.google.common.truth.Truth.assertThat
import com.sleep.snore.recording.RecordingController
import com.sleep.snore.recording.RecordingStartResult
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AutoSnoreDetectionCoordinatorTest {

    @Test
    fun handleEvent_startsRecordingForHighConfidenceSleep() = runTest {
        val controller = FakeRecordingController()
        val coordinator = AutoSnoreDetectionCoordinator(controller)

        val result = coordinator.handleEvent(
            event = SleepTriggerEvent.SleepStarted("health_connect", timestamp = 1L, confidence = 0.9f),
            enabled = true,
            stopOnSleepEnd = true
        )

        assertThat(result.handled).isTrue()
        assertThat(result.shouldRememberEvent).isTrue()
        assertThat(controller.started).isTrue()
    }

    @Test
    fun handleEvent_ignoresLowConfidenceSleep() = runTest {
        val controller = FakeRecordingController()
        val coordinator = AutoSnoreDetectionCoordinator(controller)

        val result = coordinator.handleEvent(
            event = SleepTriggerEvent.SleepStarted("health_connect", timestamp = 1L, confidence = 0.4f),
            enabled = true,
            stopOnSleepEnd = true
        )

        assertThat(result.handled).isFalse()
        assertThat(result.shouldRememberEvent).isFalse()
        assertThat(controller.started).isFalse()
    }

    @Test
    fun handleEvent_doesNotRememberUnconfirmedRecordingStart() = runTest {
        val controller = FakeRecordingController(
            startResult = RecordingStartResult.Submitted("pending")
        )
        val coordinator = AutoSnoreDetectionCoordinator(controller)

        val result = coordinator.handleEvent(
            event = SleepTriggerEvent.SleepStarted("health_connect", timestamp = 1L, confidence = 0.9f),
            enabled = true,
            stopOnSleepEnd = true
        )

        assertThat(result.handled).isFalse()
        assertThat(result.shouldRememberEvent).isFalse()
        assertThat(result.statusText).isEqualTo("pending")
    }

    @Test
    fun handleEvent_stopsRecordingWhenSleepEnds() = runTest {
        val controller = FakeRecordingController(active = true, startedFromSleepTrigger = true)
        val coordinator = AutoSnoreDetectionCoordinator(controller)

        val result = coordinator.handleEvent(
            event = SleepTriggerEvent.SleepEnded("health_connect", timestamp = 2L),
            enabled = true,
            stopOnSleepEnd = true
        )

        assertThat(result.handled).isTrue()
        assertThat(result.shouldRememberEvent).isTrue()
        assertThat(result.statusText).isEqualTo("检测到睡眠结束，已请求停止鼾声检测")
        assertThat(controller.stopped).isTrue()
    }

    @Test
    fun handleEvent_doesNotStopManualRecordingWhenSleepEnds() = runTest {
        val controller = FakeRecordingController(active = true, startedFromSleepTrigger = false)
        val coordinator = AutoSnoreDetectionCoordinator(controller)

        val result = coordinator.handleEvent(
            event = SleepTriggerEvent.SleepEnded("health_connect", timestamp = 2L),
            enabled = true,
            stopOnSleepEnd = true
        )

        assertThat(result.handled).isFalse()
        assertThat(result.shouldRememberEvent).isFalse()
        assertThat(result.statusText).isEqualTo("检测到睡眠结束，但未能停止鼾声检测；将继续重试")
        assertThat(controller.stopped).isFalse()
        assertThat(controller.isRecordingActive()).isTrue()
    }

    @Test
    fun handleEvent_keepsRecordingWhenStopOnSleepEndDisabled() = runTest {
        val controller = FakeRecordingController(active = true, startedFromSleepTrigger = true)
        val coordinator = AutoSnoreDetectionCoordinator(controller)

        val result = coordinator.handleEvent(
            event = SleepTriggerEvent.SleepEnded("health_connect", timestamp = 2L),
            enabled = true,
            stopOnSleepEnd = false
        )

        assertThat(result.handled).isFalse()
        assertThat(result.shouldRememberEvent).isFalse()
        assertThat(result.statusText).isEqualTo("检测到睡眠结束，已按设置保持鼾声检测")
        assertThat(controller.stopped).isFalse()
        assertThat(controller.isRecordingActive()).isTrue()
    }

    @Test
    fun handleEvent_sendsStopWhenAutoRecordingRuntimeStateIsNotVisible() = runTest {
        val controller = FakeRecordingController(active = false, startedFromSleepTrigger = true)
        val coordinator = AutoSnoreDetectionCoordinator(controller)

        val result = coordinator.handleEvent(
            event = SleepTriggerEvent.SleepEnded("health_connect", timestamp = 2L),
            enabled = true,
            stopOnSleepEnd = true
        )

        assertThat(result.handled).isTrue()
        assertThat(result.shouldRememberEvent).isTrue()
        assertThat(controller.stopped).isTrue()
    }

    private class FakeRecordingController(
        private var active: Boolean = false,
        private var startedFromSleepTrigger: Boolean = false,
        private val startResult: RecordingStartResult = RecordingStartResult.Confirmed("started")
    ) : RecordingController {
        var started = false
            private set
        var stopped = false
            private set

        override suspend fun startFromSleepTrigger(source: String): RecordingStartResult {
            started = true
            startedFromSleepTrigger = startResult.confirmed
            active = startResult.confirmed
            return startResult
        }

        override suspend fun stopFromSleepTrigger(source: String): Boolean {
            if (!startedFromSleepTrigger) return false
            stopped = true
            startedFromSleepTrigger = false
            active = false
            return true
        }

        override fun isRecordingActive(): Boolean = active
    }
}

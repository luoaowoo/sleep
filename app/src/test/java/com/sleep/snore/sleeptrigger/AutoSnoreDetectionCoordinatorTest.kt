package com.sleep.snore.sleeptrigger

import com.google.common.truth.Truth.assertThat
import com.sleep.snore.recording.RecordingController
import org.junit.Test

class AutoSnoreDetectionCoordinatorTest {

    @Test
    fun handleEvent_startsRecordingForHighConfidenceSleep() {
        val controller = FakeRecordingController()
        val coordinator = AutoSnoreDetectionCoordinator(controller)

        val handled = coordinator.handleEvent(
            event = SleepTriggerEvent.SleepStarted("health_connect", timestamp = 1L, confidence = 0.9f),
            enabled = true,
            stopOnSleepEnd = true
        )

        assertThat(handled).isTrue()
        assertThat(controller.started).isTrue()
    }

    @Test
    fun handleEvent_ignoresLowConfidenceSleep() {
        val controller = FakeRecordingController()
        val coordinator = AutoSnoreDetectionCoordinator(controller)

        val handled = coordinator.handleEvent(
            event = SleepTriggerEvent.SleepStarted("health_connect", timestamp = 1L, confidence = 0.4f),
            enabled = true,
            stopOnSleepEnd = true
        )

        assertThat(handled).isFalse()
        assertThat(controller.started).isFalse()
    }

    @Test
    fun handleEvent_stopsRecordingWhenSleepEnds() {
        val controller = FakeRecordingController(active = true, startedFromSleepTrigger = true)
        val coordinator = AutoSnoreDetectionCoordinator(controller)

        val handled = coordinator.handleEvent(
            event = SleepTriggerEvent.SleepEnded("health_connect", timestamp = 2L),
            enabled = true,
            stopOnSleepEnd = true
        )

        assertThat(handled).isTrue()
        assertThat(controller.stopped).isTrue()
    }

    @Test
    fun handleEvent_doesNotStopManualRecordingWhenSleepEnds() {
        val controller = FakeRecordingController(active = true, startedFromSleepTrigger = false)
        val coordinator = AutoSnoreDetectionCoordinator(controller)

        val handled = coordinator.handleEvent(
            event = SleepTriggerEvent.SleepEnded("health_connect", timestamp = 2L),
            enabled = true,
            stopOnSleepEnd = true
        )

        assertThat(handled).isFalse()
        assertThat(controller.stopped).isFalse()
        assertThat(controller.isRecordingActive()).isTrue()
    }

    private class FakeRecordingController(
        private var active: Boolean = false,
        private var startedFromSleepTrigger: Boolean = false
    ) : RecordingController {
        var started = false
            private set
        var stopped = false
            private set

        override fun startFromSleepTrigger(source: String): Boolean {
            started = true
            startedFromSleepTrigger = true
            active = true
            return true
        }

        override fun stopFromSleepTrigger(source: String): Boolean {
            if (!active || !startedFromSleepTrigger) return false
            stopped = true
            startedFromSleepTrigger = false
            active = false
            return true
        }

        override fun isRecordingActive(): Boolean = active
    }
}

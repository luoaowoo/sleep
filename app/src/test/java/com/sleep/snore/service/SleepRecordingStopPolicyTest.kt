package com.sleep.snore.service

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SleepRecordingStopPolicyTest {

    @Test
    fun shouldAcceptStopRequest_allowsManualStopWithoutExpectedSource() {
        assertThat(
            shouldAcceptStopRequest(
                activeTriggerSource = "manual",
                expectedTriggerSource = null
            )
        ).isTrue()
    }

    @Test
    fun shouldAcceptStopRequest_allowsMatchingWearableSource() {
        assertThat(
            shouldAcceptStopRequest(
                activeTriggerSource = "health_connect_sleep",
                expectedTriggerSource = "health_connect_sleep"
            )
        ).isTrue()
    }

    @Test
    fun shouldAcceptStopRequest_rejectsWearableStopForManualRecording() {
        assertThat(
            shouldAcceptStopRequest(
                activeTriggerSource = "manual",
                expectedTriggerSource = "health_connect_sleep"
            )
        ).isFalse()
    }

    @Test
    fun shouldStopWearableRecordingAfterMaxDuration_keepsRecordingBeforeLimit() {
        assertThat(
            shouldStopWearableRecordingAfterMaxDuration(
                sessionStartTimeMillis = 1_000L,
                nowMillis = 10_999L,
                maxDurationMillis = 10_000L
            )
        ).isFalse()
    }

    @Test
    fun shouldStopWearableRecordingAfterMaxDuration_stopsAtLimit() {
        assertThat(
            shouldStopWearableRecordingAfterMaxDuration(
                sessionStartTimeMillis = 1_000L,
                nowMillis = 11_000L,
                maxDurationMillis = 10_000L
            )
        ).isTrue()
    }

    @Test
    fun shouldStopWearableRecordingAfterMaxDuration_stopsAfterLimit() {
        assertThat(
            shouldStopWearableRecordingAfterMaxDuration(
                sessionStartTimeMillis = 1_000L,
                nowMillis = 12_000L,
                maxDurationMillis = 10_000L
            )
        ).isTrue()
    }

    @Test
    fun shouldStopWearableRecordingAfterMaxDuration_ignoresInvalidStartTime() {
        assertThat(
            shouldStopWearableRecordingAfterMaxDuration(
                sessionStartTimeMillis = 0L,
                nowMillis = 12_000L,
                maxDurationMillis = 10_000L
            )
        ).isFalse()
    }

    @Test
    fun shouldStopWearableRecordingAfterMaxDuration_ignoresInvalidMaxDuration() {
        assertThat(
            shouldStopWearableRecordingAfterMaxDuration(
                sessionStartTimeMillis = 1_000L,
                nowMillis = 12_000L,
                maxDurationMillis = 0L
            )
        ).isFalse()
    }

    @Test
    fun shouldStopWearableRecordingAfterMaxDuration_ignoresClockMovingBackwards() {
        assertThat(
            shouldStopWearableRecordingAfterMaxDuration(
                sessionStartTimeMillis = 12_000L,
                nowMillis = 1_000L,
                maxDurationMillis = 10_000L
            )
        ).isFalse()
    }
}

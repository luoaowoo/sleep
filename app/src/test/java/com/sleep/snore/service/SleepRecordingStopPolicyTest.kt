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
    fun shouldAcceptStopRequest_rejectsWearableStopWhenActiveSourceMissing() {
        assertThat(
            shouldAcceptStopRequest(
                activeTriggerSource = "",
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

    @Test
    fun wearableSleepEndWatcherStopAction_exitsOnlyWhenWearableSourceStillActive() {
        assertThat(
            wearableSleepEndWatcherStopAction(
                activeTriggerSource = "health_connect_sleep",
                expectedTriggerSource = "health_connect_sleep"
            )
        ).isEqualTo(WearableSleepEndWatcherStopAction.StopAndExit)

        assertThat(
            wearableSleepEndWatcherStopAction(
                activeTriggerSource = "manual",
                expectedTriggerSource = "health_connect_sleep"
            )
        ).isEqualTo(WearableSleepEndWatcherStopAction.ContinuePolling)

        assertThat(
            wearableSleepEndWatcherStopAction(
                activeTriggerSource = "",
                expectedTriggerSource = "health_connect_sleep"
            )
        ).isEqualTo(WearableSleepEndWatcherStopAction.ContinuePolling)
    }

    @Test
    fun staleActiveRecordingRecoveryAction_recoversRecordWithinLimit() {
        assertThat(
            staleActiveRecordingRecoveryAction(
                recordingAgeMillis = 9_999L,
                maxRecoveryMillis = 10_000L,
                activeTriggerSource = "health_connect_sleep",
                wearableTriggerSource = "health_connect_sleep"
            )
        ).isEqualTo(StaleActiveRecordingRecoveryAction.Recover)
    }

    @Test
    fun staleActiveRecordingRecoveryAction_defersStaleWearableRecordToFinalizerWorker() {
        assertThat(
            staleActiveRecordingRecoveryAction(
                recordingAgeMillis = 10_001L,
                maxRecoveryMillis = 10_000L,
                activeTriggerSource = "health_connect_sleep",
                wearableTriggerSource = "health_connect_sleep"
            )
        ).isEqualTo(StaleActiveRecordingRecoveryAction.DeferWearableFinalizer)
    }

    @Test
    fun staleActiveRecordingRecoveryAction_finalizesStaleManualRecordLocally() {
        assertThat(
            staleActiveRecordingRecoveryAction(
                recordingAgeMillis = 10_001L,
                maxRecoveryMillis = 10_000L,
                activeTriggerSource = "",
                wearableTriggerSource = "health_connect_sleep"
            )
        ).isEqualTo(StaleActiveRecordingRecoveryAction.FinalizeLocally)
    }

    @Test
    fun shouldDeferDestroyFinalizationToWearableFinalizer_allowsActiveWearableSession() {
        assertThat(
            shouldDeferDestroyFinalizationToWearableFinalizer(
                shouldFinalizeActiveSession = true,
                activeTriggerSource = "health_connect_sleep",
                wearableTriggerSource = "health_connect_sleep"
            )
        ).isTrue()
    }

    @Test
    fun shouldDeferDestroyFinalizationToWearableFinalizer_rejectsManualOrInactiveSession() {
        assertThat(
            shouldDeferDestroyFinalizationToWearableFinalizer(
                shouldFinalizeActiveSession = true,
                activeTriggerSource = "manual",
                wearableTriggerSource = "health_connect_sleep"
            )
        ).isFalse()
        assertThat(
            shouldDeferDestroyFinalizationToWearableFinalizer(
                shouldFinalizeActiveSession = false,
                activeTriggerSource = "health_connect_sleep",
                wearableTriggerSource = "health_connect_sleep"
            )
        ).isFalse()
    }
}

package com.sleep.snore.service

internal fun shouldAcceptStopRequest(
    activeTriggerSource: String?,
    expectedTriggerSource: String?,
    activeRecordingStartMillis: Long? = null,
    expectedActiveRecordingStartMillis: Long? = null,
    requireActiveRecordingStartToken: Boolean = false
): Boolean {
    if (expectedTriggerSource.isNullOrBlank()) return true
    if (activeTriggerSource != expectedTriggerSource) return false
    if (expectedActiveRecordingStartMillis == null || expectedActiveRecordingStartMillis <= 0L) {
        return !requireActiveRecordingStartToken
    }
    return activeRecordingStartMillis == expectedActiveRecordingStartMillis
}

internal fun shouldConfirmSleepTriggerRecording(
    detectorStarted: Boolean,
    triggerSource: String?
): Boolean {
    return detectorStarted && !triggerSource.isNullOrBlank()
}

internal fun shouldStopWearableRecordingAfterMaxDuration(
    sessionStartTimeMillis: Long,
    nowMillis: Long,
    maxDurationMillis: Long
): Boolean {
    if (sessionStartTimeMillis <= 0L) return false
    if (maxDurationMillis <= 0L) return false
    val elapsedMillis = nowMillis - sessionStartTimeMillis
    return elapsedMillis >= maxDurationMillis
}

internal enum class WearableSleepEndWatcherStopAction {
    StopAndExit,
    ContinuePolling
}

internal fun wearableSleepEndWatcherStopAction(
    activeTriggerSource: String?,
    expectedTriggerSource: String,
    activeRecordingStartMillis: Long? = null,
    expectedActiveRecordingStartMillis: Long? = null,
    requireActiveRecordingStartToken: Boolean = false
): WearableSleepEndWatcherStopAction {
    return if (
        shouldAcceptStopRequest(
            activeTriggerSource = activeTriggerSource,
            expectedTriggerSource = expectedTriggerSource,
            activeRecordingStartMillis = activeRecordingStartMillis,
            expectedActiveRecordingStartMillis = expectedActiveRecordingStartMillis,
            requireActiveRecordingStartToken = requireActiveRecordingStartToken
        )
    ) {
        WearableSleepEndWatcherStopAction.StopAndExit
    } else {
        WearableSleepEndWatcherStopAction.ContinuePolling
    }
}

internal fun wearableRecordingEndTimeWithCap(
    requestedEndTimeMillis: Long?,
    activeRecordingStartMillis: Long,
    maxDurationMillis: Long
): Long? {
    if (requestedEndTimeMillis == null || requestedEndTimeMillis <= 0L) return null
    if (activeRecordingStartMillis <= 0L || maxDurationMillis <= 0L) return requestedEndTimeMillis
    return requestedEndTimeMillis.coerceAtMost(activeRecordingStartMillis + maxDurationMillis)
}

internal enum class StaleActiveRecordingRecoveryAction {
    Recover,
    FinalizeLocally,
    DeferWearableFinalizer
}

internal fun staleActiveRecordingRecoveryAction(
    recordingAgeMillis: Long,
    maxRecoveryMillis: Long,
    activeTriggerSource: String?,
    wearableTriggerSource: String
): StaleActiveRecordingRecoveryAction {
    if (recordingAgeMillis in 0..maxRecoveryMillis) {
        return StaleActiveRecordingRecoveryAction.Recover
    }
    return if (activeTriggerSource == wearableTriggerSource) {
        StaleActiveRecordingRecoveryAction.DeferWearableFinalizer
    } else {
        StaleActiveRecordingRecoveryAction.FinalizeLocally
    }
}

internal fun shouldDeferDestroyFinalizationToWearableFinalizer(
    shouldFinalizeActiveSession: Boolean,
    activeTriggerSource: String?,
    wearableTriggerSource: String
): Boolean {
    return shouldFinalizeActiveSession && activeTriggerSource == wearableTriggerSource
}

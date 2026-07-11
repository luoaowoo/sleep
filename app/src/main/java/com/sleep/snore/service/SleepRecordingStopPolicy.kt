package com.sleep.snore.service

internal fun shouldAcceptStopRequest(
    activeTriggerSource: String?,
    expectedTriggerSource: String?
): Boolean {
    if (expectedTriggerSource.isNullOrBlank()) return true
    return activeTriggerSource == expectedTriggerSource
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
    expectedTriggerSource: String
): WearableSleepEndWatcherStopAction {
    return if (shouldAcceptStopRequest(activeTriggerSource, expectedTriggerSource)) {
        WearableSleepEndWatcherStopAction.StopAndExit
    } else {
        WearableSleepEndWatcherStopAction.ContinuePolling
    }
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

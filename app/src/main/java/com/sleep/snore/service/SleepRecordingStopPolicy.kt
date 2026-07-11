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

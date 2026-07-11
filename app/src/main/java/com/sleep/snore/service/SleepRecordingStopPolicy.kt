package com.sleep.snore.service

internal fun shouldAcceptStopRequest(
    activeTriggerSource: String?,
    expectedTriggerSource: String?
): Boolean {
    if (expectedTriggerSource.isNullOrBlank()) return true
    return activeTriggerSource == expectedTriggerSource
}

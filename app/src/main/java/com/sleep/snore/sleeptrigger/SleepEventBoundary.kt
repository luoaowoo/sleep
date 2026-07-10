package com.sleep.snore.sleeptrigger

import com.sleep.snore.data.preferences.SettingsPreferences
import java.time.Instant

internal fun sleepEventIgnoreEventsBefore(
    settings: SettingsPreferences,
    fallbackMillis: Long = 0L
): Instant? {
    val activeTriggerStartedAt = settings.activeRecordingTriggerStartedAtMillis
        .takeIf {
            it > 0L && settings.activeRecordingTriggerSource == HealthConnectSleepTriggerSource.SOURCE
        }
    val boundaryMillis = activeTriggerStartedAt ?: fallbackMillis.takeIf { it > 0L }
    return boundaryMillis?.let { Instant.ofEpochMilli(it) }
}

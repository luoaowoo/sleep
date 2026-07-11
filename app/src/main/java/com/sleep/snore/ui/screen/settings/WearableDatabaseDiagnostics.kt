package com.sleep.snore.ui.screen.settings

import com.sleep.snore.data.db.entity.SleepRecordEntity
import com.sleep.snore.data.preferences.SettingsPreferencesRepository
import com.sleep.snore.data.repository.SleepRepository
import com.sleep.snore.sleeptrigger.HealthConnectSleepTriggerSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class WearableDatabaseDiagnosticSnapshot(
    val activeRecordId: Long?,
    val activeRecordStartMillis: Long?,
    val activeRecordEndMillis: Long?,
    val activeRecordEventCount: Int?,
    val activeRecordingTriggerSource: String?,
    val nowMillis: Long?,
    val lastWearableSleepEventKey: String?
)

internal suspend fun collectWearableDatabaseDiagnostics(
    sleepRepository: SleepRepository,
    settingsRepository: SettingsPreferencesRepository
): String = withContext(Dispatchers.IO) {
    val activeRecord = runCatching {
        sleepRepository.getActiveRecordingRecord()
    }.getOrNull()
    val activeEventCount = activeRecord?.let { record ->
        runCatching {
            sleepRepository.getEventsSnapshotByRecordId(record.id).size
        }.getOrNull()
    }
    val lastEventKey = runCatching {
        settingsRepository.getLastWearableSleepEventKey()
    }.getOrNull()
    val activeTriggerSource = runCatching {
        settingsRepository.getActiveRecordingTriggerSource()
    }.getOrNull()
    wearableDatabaseDiagnosticsText(
        WearableDatabaseDiagnosticSnapshot(
            activeRecordId = activeRecord?.id,
            activeRecordStartMillis = activeRecord?.startTime,
            activeRecordEndMillis = activeRecord?.endTime,
            activeRecordEventCount = activeEventCount,
            activeRecordingTriggerSource = activeTriggerSource,
            nowMillis = System.currentTimeMillis(),
            lastWearableSleepEventKey = lastEventKey
        )
    )
}

internal fun wearableDatabaseDiagnosticsText(
    snapshot: WearableDatabaseDiagnosticSnapshot
): String {
    val activeRecordText = if (snapshot.activeRecordId == null) {
        "无"
    } else {
        val isStillActive = snapshot.activeRecordStartMillis == snapshot.activeRecordEndMillis
        val activeAgeHours = activeRecordAgeHours(
            activeRecordStartMillis = snapshot.activeRecordStartMillis,
            nowMillis = snapshot.nowMillis
        )
        val wearableCapAtMillis = wearableActiveRecordCapAtMillis(
            activeRecordStartMillis = snapshot.activeRecordStartMillis,
            activeRecordingTriggerSource = snapshot.activeRecordingTriggerSource
        )
        val overWearableCap = wearableCapAtMillis?.let { capAt ->
            (snapshot.nowMillis ?: 0L) >= capAt
        }
        "id=${snapshot.activeRecordId}, start=${snapshot.activeRecordStartMillis}, " +
            "end=${snapshot.activeRecordEndMillis}, active=$isStillActive, events=${snapshot.activeRecordEventCount ?: "未知"}, " +
            "ageHours=${activeAgeHours ?: "未知"}, wearable16hCapAt=${wearableCapAtMillis ?: "不适用"}, " +
            "over16hCap=${overWearableCap ?: "不适用"}"
    }
    val eventKeyText = snapshot.lastWearableSleepEventKey?.takeIf { it.isNotBlank() } ?: "无"
    return listOf(
        "activeRecord: $activeRecordText",
        "lastWearableSleepEventKey: $eventKeyText"
    ).joinToString(separator = "\n")
}

internal fun activeRecordAgeHours(
    activeRecordStartMillis: Long?,
    nowMillis: Long?
): Long? {
    if (activeRecordStartMillis == null || activeRecordStartMillis <= 0L) return null
    if (nowMillis == null || nowMillis < activeRecordStartMillis) return null
    return (nowMillis - activeRecordStartMillis) / 3_600_000L
}

internal fun wearableActiveRecordCapAtMillis(
    activeRecordStartMillis: Long?,
    activeRecordingTriggerSource: String?
): Long? {
    if (activeRecordingTriggerSource != HealthConnectSleepTriggerSource.SOURCE) return null
    if (activeRecordStartMillis == null || activeRecordStartMillis <= 0L) return null
    return activeRecordStartMillis + WEARABLE_ACTIVE_RECORD_CAP_MILLIS
}

private const val WEARABLE_ACTIVE_RECORD_CAP_MILLIS = 16L * 60L * 60L * 1000L

package com.sleep.snore.ui.screen.settings

import com.sleep.snore.data.db.entity.SleepRecordEntity
import com.sleep.snore.data.preferences.SettingsPreferencesRepository
import com.sleep.snore.data.repository.SleepRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class WearableDatabaseDiagnosticSnapshot(
    val activeRecordId: Long?,
    val activeRecordStartMillis: Long?,
    val activeRecordEndMillis: Long?,
    val activeRecordEventCount: Int?,
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
    wearableDatabaseDiagnosticsText(
        WearableDatabaseDiagnosticSnapshot(
            activeRecordId = activeRecord?.id,
            activeRecordStartMillis = activeRecord?.startTime,
            activeRecordEndMillis = activeRecord?.endTime,
            activeRecordEventCount = activeEventCount,
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
        "id=${snapshot.activeRecordId}, start=${snapshot.activeRecordStartMillis}, " +
            "end=${snapshot.activeRecordEndMillis}, active=$isStillActive, events=${snapshot.activeRecordEventCount ?: "未知"}"
    }
    val eventKeyText = snapshot.lastWearableSleepEventKey?.takeIf { it.isNotBlank() } ?: "无"
    return listOf(
        "activeRecord: $activeRecordText",
        "lastWearableSleepEventKey: $eventKeyText"
    ).joinToString(separator = "\n")
}

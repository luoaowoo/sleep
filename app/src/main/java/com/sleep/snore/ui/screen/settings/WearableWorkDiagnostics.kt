package com.sleep.snore.ui.screen.settings

import android.content.Context
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.sleep.snore.recording.ActiveRecordingFinalizerWorker
import com.sleep.snore.sleeptrigger.BedtimeDetectionReminderWorker
import com.sleep.snore.sleeptrigger.HealthConnectSleepTriggerWorker
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class WearableWorkDiagnosticItem(
    val name: String,
    val states: List<String>
)

internal suspend fun collectWearableWorkDiagnostics(context: Context): String = withContext(Dispatchers.IO) {
    val workManager = WorkManager.getInstance(context)
    val items = listOf(
        HealthConnectSleepTriggerWorker.WORK_NAME,
        HealthConnectSleepTriggerWorker.ONE_TIME_WORK_NAME,
        BedtimeDetectionReminderWorker.WORK_NAME,
        ActiveRecordingFinalizerWorker.WORK_NAME
    ).map { workName ->
        WearableWorkDiagnosticItem(
            name = workName,
            states = runCatching {
                workManager.getWorkInfosForUniqueWork(workName)
                    .get(2, TimeUnit.SECONDS)
                    .map { it.toDiagnosticState() }
            }.getOrElse { error ->
                listOf("读取失败：${error.message.orEmpty().ifBlank { error::class.java.simpleName }}")
            }
        )
    }
    wearableWorkDiagnosticsText(items)
}

internal fun wearableWorkDiagnosticsText(items: List<WearableWorkDiagnosticItem>): String {
    return items.joinToString(separator = "\n") { item ->
        val stateText = item.states.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: "无记录"
        "${item.name}: $stateText"
    }
}

private fun WorkInfo.toDiagnosticState(): String {
    return "${state.name}(attempt=$runAttemptCount)"
}

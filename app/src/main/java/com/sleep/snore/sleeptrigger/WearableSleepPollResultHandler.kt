package com.sleep.snore.sleeptrigger

import com.sleep.snore.data.preferences.SettingsPreferencesRepository

internal data class WearableSleepPollHandleResult(
    val statusText: String,
    val emittedSleepStart: Boolean = false,
    val emittedSleepEnd: Boolean = false,
    val eventHandled: Boolean = false
)

internal suspend fun handleWearableSleepPollResult(
    pollResult: HealthConnectSleepTriggerSource.PollResult,
    stopOnSleepEnd: Boolean,
    coordinator: AutoSnoreDetectionCoordinator,
    settingsRepository: SettingsPreferencesRepository,
    requireBackgroundRead: Boolean
): WearableSleepPollHandleResult {
    if (pollResult !is HealthConnectSleepTriggerSource.PollResult.EventEmitted) {
        return WearableSleepPollHandleResult(
            statusText = pollResult.toWearableSleepStatusText(requireBackgroundRead)
        )
    }

    val result = coordinator.handleEvent(
        event = pollResult.event,
        enabled = true,
        stopOnSleepEnd = stopOnSleepEnd
    )
    if (result.shouldRememberEvent) {
        settingsRepository.setLastWearableSleepEventKey(pollResult.eventKey)
    }
    return WearableSleepPollHandleResult(
        statusText = result.statusText ?: pollResult.toWearableSleepStatusText(requireBackgroundRead),
        emittedSleepStart = pollResult.event is SleepTriggerEvent.SleepStarted,
        emittedSleepEnd = pollResult.event is SleepTriggerEvent.SleepEnded,
        eventHandled = result.handled
    )
}

internal fun HealthConnectSleepTriggerSource.PollResult.toWearableSleepStatusText(
    requireBackgroundRead: Boolean
): String {
    return when (this) {
        HealthConnectSleepTriggerSource.PollResult.HealthConnectUnavailable -> "Health Connect 不可用"
        HealthConnectSleepTriggerSource.PollResult.PermissionMissing -> if (requireBackgroundRead) {
            "缺少 Health Connect 睡眠/后台读取权限"
        } else {
            "缺少 Health Connect 睡眠读取权限"
        }
        HealthConnectSleepTriggerSource.PollResult.NoRecentSleep -> "未发现最近睡眠记录"
        HealthConnectSleepTriggerSource.PollResult.DuplicateEvent -> "最近睡眠记录已处理，等待新记录"
        HealthConnectSleepTriggerSource.PollResult.ReadFailed -> "Health Connect 读取失败"
        is HealthConnectSleepTriggerSource.PollResult.EventEmitted -> "已处理睡眠事件"
    }
}

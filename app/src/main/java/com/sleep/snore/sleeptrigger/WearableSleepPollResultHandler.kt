package com.sleep.snore.sleeptrigger

import com.sleep.snore.data.preferences.SettingsPreferencesRepository

internal data class WearableSleepPollHandleResult(
    val statusText: String,
    val emittedSleepStart: Boolean = false,
    val emittedSleepEnd: Boolean = false,
    val eventHandled: Boolean = false,
    val eventKey: String? = null
)

internal suspend fun handleWearableSleepPollResult(
    pollResult: HealthConnectSleepTriggerSource.PollResult,
    stopOnSleepEnd: Boolean,
    coordinator: AutoSnoreDetectionCoordinator,
    settingsRepository: SettingsPreferencesRepository,
    requireBackgroundRead: Boolean,
    allowSleepStartRecording: Boolean = false
): WearableSleepPollHandleResult {
    pollResult.observedSession?.let { session ->
        settingsRepository.setLatestWearableSleepSession(
            startMillis = session.startTime.toEpochMilli(),
            endMillis = session.endTime.toEpochMilli(),
            status = pollResult.toLatestWearableSleepSessionStatus(),
            sourcePackage = session.dataOriginPackageName
        )
    }
    if (
        pollResult is HealthConnectSleepTriggerSource.PollResult.DuplicateEvent &&
        stopOnSleepEnd &&
        pollResult.eventKey.startsWith(SLEEP_ENDED_EVENT_KEY_PREFIX)
    ) {
        val activeTriggerSource = settingsRepository.getActiveRecordingTriggerSource()
        if (activeTriggerSource == HealthConnectSleepTriggerSource.SOURCE) {
            val result = coordinator.handleEvent(
                event = SleepTriggerEvent.SleepEnded(
                    source = HealthConnectSleepTriggerSource.SOURCE,
                    timestamp = pollResult.observedSession.endTime.toEpochMilli()
                ),
                enabled = true,
                stopOnSleepEnd = true
            )
            return WearableSleepPollHandleResult(
                statusText = result.statusText ?: pollResult.toWearableSleepStatusText(requireBackgroundRead),
                emittedSleepEnd = true,
                eventHandled = result.handled,
                eventKey = pollResult.eventKey
            )
        }
    }
    if (pollResult !is HealthConnectSleepTriggerSource.PollResult.EventEmitted) {
        return WearableSleepPollHandleResult(
            statusText = pollResult.toWearableSleepStatusText(requireBackgroundRead)
        )
    }
    if (pollResult.event is SleepTriggerEvent.SleepStarted && !allowSleepStartRecording) {
        return WearableSleepPollHandleResult(
            statusText = "检测到睡眠开始；后台轮询不会直接启动麦克风，请使用睡前前台检测",
            emittedSleepStart = true,
            eventKey = pollResult.eventKey
        )
    }
    if (pollResult.event is SleepTriggerEvent.SleepEnded && stopOnSleepEnd) {
        val activeTriggerSource = settingsRepository.getActiveRecordingTriggerSource()
        if (activeTriggerSource != pollResult.event.source) {
            return WearableSleepPollHandleResult(
                statusText = "检测到睡眠结束，但当前没有手环触发的前台鼾声检测；已作为诊断记录",
                emittedSleepEnd = true,
                eventHandled = false,
                eventKey = pollResult.eventKey
            )
        }
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
        eventHandled = result.handled,
        eventKey = pollResult.eventKey
    )
}

private const val SLEEP_ENDED_EVENT_KEY_PREFIX = "SleepEnded:"

private fun HealthConnectSleepTriggerSource.PollResult.toLatestWearableSleepSessionStatus(): String {
    return when (this) {
        HealthConnectSleepTriggerSource.PollResult.HealthConnectUnavailable -> "Health Connect 不可用"
        HealthConnectSleepTriggerSource.PollResult.PermissionMissing -> "缺少授权"
        HealthConnectSleepTriggerSource.PollResult.NoRecentSleep -> "未发现记录"
        is HealthConnectSleepTriggerSource.PollResult.NoActionableSleep -> when (this.reason) {
            HealthConnectSleepTriggerSource.PollResult.NoActionableSleepReason.ONGOING -> "同步中，等待结束时间"
            HealthConnectSleepTriggerSource.PollResult.NoActionableSleepReason.BEFORE_ACTIVE_RECORDING -> "早于本次检测，已忽略"
            HealthConnectSleepTriggerSource.PollResult.NoActionableSleepReason.NON_XIAOMI_SOURCE -> {
                "非小米来源，仅诊断"
            }
            HealthConnectSleepTriggerSource.PollResult.NoActionableSleepReason.INSUFFICIENT_ACTIVE_RECORDING_OVERLAP -> {
                "与本次检测重叠过短，已忽略"
            }
            HealthConnectSleepTriggerSource.PollResult.NoActionableSleepReason.SHORT_SLEEP_SESSION -> {
                "短睡眠记录，已忽略"
            }
        }
        is HealthConnectSleepTriggerSource.PollResult.DuplicateEvent -> "已处理"
        HealthConnectSleepTriggerSource.PollResult.ReadFailed -> "读取失败"
        is HealthConnectSleepTriggerSource.PollResult.EventEmitted -> when (this.event) {
            is SleepTriggerEvent.SleepEnded -> "已读取睡眠结束"
            is SleepTriggerEvent.SleepStarted -> "已读取睡眠开始"
        }
    }
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
        is HealthConnectSleepTriggerSource.PollResult.NoActionableSleep -> when (this.reason) {
            HealthConnectSleepTriggerSource.PollResult.NoActionableSleepReason.ONGOING -> {
                "发现同步中的睡眠记录，等待 Health Connect 写入结束时间"
            }
            HealthConnectSleepTriggerSource.PollResult.NoActionableSleepReason.BEFORE_ACTIVE_RECORDING -> {
                "发现睡眠记录，但早于本次睡前前台检测，已忽略"
            }
            HealthConnectSleepTriggerSource.PollResult.NoActionableSleepReason.NON_XIAOMI_SOURCE -> {
                "发现 Health Connect 睡眠记录，但来源不是已知小米伴侣，已仅用于诊断"
            }
            HealthConnectSleepTriggerSource.PollResult.NoActionableSleepReason.INSUFFICIENT_ACTIVE_RECORDING_OVERLAP -> {
                "发现睡眠结束记录，但与本次前台检测重叠不足 30 分钟，已忽略"
            }
            HealthConnectSleepTriggerSource.PollResult.NoActionableSleepReason.SHORT_SLEEP_SESSION -> {
                "发现短睡眠记录，暂不用于自动停录"
            }
        }
        is HealthConnectSleepTriggerSource.PollResult.DuplicateEvent -> "最近睡眠记录已处理，等待新记录"
        HealthConnectSleepTriggerSource.PollResult.ReadFailed -> "Health Connect 读取失败"
        is HealthConnectSleepTriggerSource.PollResult.EventEmitted -> "已处理睡眠事件"
    }
}

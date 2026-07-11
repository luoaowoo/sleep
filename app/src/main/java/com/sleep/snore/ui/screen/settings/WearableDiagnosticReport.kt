package com.sleep.snore.ui.screen.settings

internal data class WearableDiagnosticReportInput(
    val generatedAtText: String,
    val appText: String,
    val deviceText: String,
    val healthConnectStatusText: String,
    val healthConnectSdkStatusCode: Int,
    val healthConnectGrantedPermissionsText: String,
    val hasRecordAudioPermission: Boolean,
    val hasNotificationPermission: Boolean,
    val hasHealthConnectSleepReadPermission: Boolean,
    val hasHealthConnectBackgroundReadPermission: Boolean,
    val isIgnoringBatteryOptimizations: Boolean,
    val xiaomiCompanionText: String,
    val periodicCheckEnabled: Boolean,
    val stopOnSleepEndEnabled: Boolean,
    val bedtimeReminderEnabled: Boolean,
    val bedtimeReminderTimeText: String,
    val foregroundDetectionActive: Boolean,
    val recordingRuntimeText: String,
    val recordingActive: Boolean,
    val recordingStartTimeMillis: Long,
    val recordingEventCount: Int,
    val activeRecordingTriggerSource: String,
    val activeRecordingTriggerStartedAtText: String,
    val activeRecordingTriggerStartedAtMillis: Long,
    val wearableSleepTriggerStatus: String,
    val wearableSleepTriggerLastCheckText: String,
    val latestWearableSleepSessionText: String,
    val latestWearableSleepSessionStartMillis: Long,
    val latestWearableSleepSessionEndMillis: Long,
    val latestWearableSleepSessionStatus: String,
    val latestWearableSleepSessionSourcePackage: String,
    val workManagerDiagnosticsText: String,
    val databaseDiagnosticsText: String
)

internal fun wearableDiagnosticReport(input: WearableDiagnosticReportInput): String {
    return buildString {
        appendLine("SleepSnore 小米手环 / Health Connect 诊断")
        appendLine("生成时间：${input.generatedAtText}")
        appendLine("应用：${input.appText}")
        appendLine("设备：${input.deviceText}")
        appendLine("Health Connect：${input.healthConnectStatusText}")
        appendLine("Health Connect SDK 状态码：${input.healthConnectSdkStatusCode}")
        appendLine("Health Connect 已授权权限：${input.healthConnectGrantedPermissionsText.ifBlank { "无" }}")
        appendLine("小米伴侣：${input.xiaomiCompanionText}")
        appendLine("麦克风权限：${input.hasRecordAudioPermission.toYesNo()}")
        appendLine("通知权限：${input.hasNotificationPermission.toYesNo()}")
        appendLine("Health Connect 睡眠读取：${input.hasHealthConnectSleepReadPermission.toYesNo()}")
        appendLine("Health Connect 后台读取：${input.hasHealthConnectBackgroundReadPermission.toYesNo()}")
        appendLine("电池优化放行：${input.isIgnoringBatteryOptimizations.toYesNo()}")
        appendLine("周期检查：${input.periodicCheckEnabled.toOnOff()}")
        appendLine("睡眠结束自动停录：${input.stopOnSleepEndEnabled.toOnOff()}")
        appendLine("睡前提醒：${input.bedtimeReminderEnabled.toOnOff()}（${input.bedtimeReminderTimeText}）")
        appendLine("前台睡前检测：${input.foregroundDetectionActive.toActiveInactive()}")
        appendLine("录音运行态：${input.recordingRuntimeText}")
        appendLine("录音运行中：${input.recordingActive.toYesNo()}")
        appendLine("录音开始毫秒：${input.recordingStartTimeMillis}")
        appendLine("录音事件数：${input.recordingEventCount}")
        appendLine("当前录音来源：${input.activeRecordingTriggerSource.ifBlank { "无" }}")
        appendLine("当前录音触发时间：${input.activeRecordingTriggerStartedAtText}")
        appendLine("当前录音触发毫秒：${input.activeRecordingTriggerStartedAtMillis}")
        appendLine(
            "录音开始-触发差值毫秒：${
                recordingTriggerOffsetMillis(
                    input.recordingStartTimeMillis,
                    input.activeRecordingTriggerStartedAtMillis
                )?.toString() ?: "未知"
            }"
        )
        appendLine("最近状态：${input.wearableSleepTriggerStatus}")
        appendLine("最近检查：${input.wearableSleepTriggerLastCheckText}")
        appendLine("最近同步睡眠：${input.latestWearableSleepSessionText}")
        appendLine("最近睡眠开始毫秒：${input.latestWearableSleepSessionStartMillis}")
        appendLine("最近睡眠结束毫秒：${input.latestWearableSleepSessionEndMillis}")
        appendLine("最近睡眠状态：${input.latestWearableSleepSessionStatus.ifBlank { "无" }}")
        appendLine("最近睡眠来源：${input.latestWearableSleepSessionSourcePackage.ifBlank { "未知" }}")
        appendLine("最近睡眠时长分钟：${sleepDurationMinutes(input.latestWearableSleepSessionStartMillis, input.latestWearableSleepSessionEndMillis) ?: "未知"}")
        appendLine(
            "最近睡眠与触发重叠分钟：${
                sleepOverlapAfterTriggerMinutes(
                    sleepStartMillis = input.latestWearableSleepSessionStartMillis,
                    sleepEndMillis = input.latestWearableSleepSessionEndMillis,
                    triggerStartedAtMillis = input.activeRecordingTriggerStartedAtMillis
                ) ?: "未知"
            }"
        )
        appendLine(
            "最近睡眠自动停录规则判断：${
                sleepAutoStopRuleDiagnostic(
                    sleepStartMillis = input.latestWearableSleepSessionStartMillis,
                    sleepEndMillis = input.latestWearableSleepSessionEndMillis,
                    triggerStartedAtMillis = input.activeRecordingTriggerStartedAtMillis
                )
            }"
        )
        appendLine("后台任务：")
        appendLine(input.workManagerDiagnosticsText.ifBlank { "无" })
        appendLine("数据库：")
        appendLine(input.databaseDiagnosticsText.ifBlank { "无" })
        appendLine("说明：Android 后台不能可靠直接开启麦克风；推荐睡前打开前台检测，睡醒后等待小米同步睡眠结束到 Health Connect 自动停录。")
    }.trimEnd()
}

internal fun sleepDurationMinutes(startMillis: Long, endMillis: Long): Long? {
    if (startMillis <= 0L || endMillis <= startMillis) return null
    return (endMillis - startMillis) / 60_000L
}

internal fun sleepOverlapAfterTriggerMinutes(
    sleepStartMillis: Long,
    sleepEndMillis: Long,
    triggerStartedAtMillis: Long
): Long? {
    if (sleepStartMillis <= 0L || sleepEndMillis <= sleepStartMillis || triggerStartedAtMillis <= 0L) return null
    val overlapStart = maxOf(sleepStartMillis, triggerStartedAtMillis)
    val overlapMillis = sleepEndMillis - overlapStart
    return if (overlapMillis > 0L) overlapMillis / 60_000L else 0L
}

internal fun sleepAutoStopRuleDiagnostic(
    sleepStartMillis: Long,
    sleepEndMillis: Long,
    triggerStartedAtMillis: Long
): String {
    val durationMinutes = sleepDurationMinutes(sleepStartMillis, sleepEndMillis)
        ?: return "无有效最近睡眠"
    val overlapMinutes = sleepOverlapAfterTriggerMinutes(
        sleepStartMillis = sleepStartMillis,
        sleepEndMillis = sleepEndMillis,
        triggerStartedAtMillis = triggerStartedAtMillis
    ) ?: return "缺少手环触发开始时间，无法判断重叠"
    return when {
        overlapMinutes < MINIMUM_ACTIVE_RECORDING_OVERLAP_MINUTES -> {
            "会被忽略：与本次前台检测重叠不足 ${MINIMUM_ACTIVE_RECORDING_OVERLAP_MINUTES} 分钟"
        }
        durationMinutes < MINIMUM_AUTO_STOP_SLEEP_SESSION_DURATION_MINUTES -> {
            "会被忽略：睡眠时长不足 ${MINIMUM_AUTO_STOP_SLEEP_SESSION_DURATION_MINUTES} 分钟"
        }
        else -> "满足自动停录时间规则"
    }
}

internal fun recordingTriggerOffsetMillis(
    recordingStartTimeMillis: Long,
    triggerStartedAtMillis: Long
): Long? {
    if (recordingStartTimeMillis <= 0L || triggerStartedAtMillis <= 0L) return null
    return recordingStartTimeMillis - triggerStartedAtMillis
}

internal fun healthConnectStatusText(sdkStatus: Int): String {
    val blocker = healthConnectAvailabilityBlocker(sdkStatus)
    return blocker ?: "可用"
}

internal fun healthConnectGrantedPermissionsText(grantedPermissions: Set<String>): String {
    return grantedPermissions
        .filter { it.isNotBlank() }
        .sorted()
        .joinToString(separator = ", ")
        .ifBlank { "无" }
}

private fun Boolean.toYesNo(): String = if (this) "已满足" else "未满足"

private fun Boolean.toOnOff(): String = if (this) "已开启" else "已关闭"

private fun Boolean.toActiveInactive(): String = if (this) "运行中" else "未运行"

private const val MINIMUM_ACTIVE_RECORDING_OVERLAP_MINUTES = 30L
private const val MINIMUM_AUTO_STOP_SLEEP_SESSION_DURATION_MINUTES = 120L

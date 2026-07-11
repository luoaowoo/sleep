package com.sleep.snore.ui.screen.settings

internal data class WearableDiagnosticReportInput(
    val generatedAtText: String,
    val appText: String,
    val deviceText: String,
    val healthConnectStatusText: String,
    val hasRecordAudioPermission: Boolean,
    val hasNotificationPermission: Boolean,
    val hasHealthConnectSleepReadPermission: Boolean,
    val hasHealthConnectBackgroundReadPermission: Boolean,
    val isIgnoringBatteryOptimizations: Boolean,
    val xiaomiCompanionText: String,
    val periodicCheckEnabled: Boolean,
    val stopOnSleepEndEnabled: Boolean,
    val foregroundDetectionActive: Boolean,
    val recordingRuntimeText: String,
    val activeRecordingTriggerSource: String,
    val wearableSleepTriggerStatus: String,
    val wearableSleepTriggerLastCheckText: String,
    val latestWearableSleepSessionText: String
)

internal fun wearableDiagnosticReport(input: WearableDiagnosticReportInput): String {
    return buildString {
        appendLine("SleepSnore 小米手环 / Health Connect 诊断")
        appendLine("生成时间：${input.generatedAtText}")
        appendLine("应用：${input.appText}")
        appendLine("设备：${input.deviceText}")
        appendLine("Health Connect：${input.healthConnectStatusText}")
        appendLine("小米伴侣：${input.xiaomiCompanionText}")
        appendLine("麦克风权限：${input.hasRecordAudioPermission.toYesNo()}")
        appendLine("通知权限：${input.hasNotificationPermission.toYesNo()}")
        appendLine("Health Connect 睡眠读取：${input.hasHealthConnectSleepReadPermission.toYesNo()}")
        appendLine("Health Connect 后台读取：${input.hasHealthConnectBackgroundReadPermission.toYesNo()}")
        appendLine("电池优化放行：${input.isIgnoringBatteryOptimizations.toYesNo()}")
        appendLine("周期检查：${input.periodicCheckEnabled.toOnOff()}")
        appendLine("睡眠结束自动停录：${input.stopOnSleepEndEnabled.toOnOff()}")
        appendLine("前台睡前检测：${input.foregroundDetectionActive.toActiveInactive()}")
        appendLine("录音运行态：${input.recordingRuntimeText}")
        appendLine("当前录音来源：${input.activeRecordingTriggerSource.ifBlank { "无" }}")
        appendLine("最近状态：${input.wearableSleepTriggerStatus}")
        appendLine("最近检查：${input.wearableSleepTriggerLastCheckText}")
        appendLine("最近同步睡眠：${input.latestWearableSleepSessionText}")
        appendLine("说明：Android 后台不能可靠直接开启麦克风；推荐睡前打开前台检测，睡醒后等待小米同步睡眠结束到 Health Connect 自动停录。")
    }.trimEnd()
}

internal fun healthConnectStatusText(sdkStatus: Int): String {
    val blocker = healthConnectAvailabilityBlocker(sdkStatus)
    return blocker ?: "可用"
}

private fun Boolean.toYesNo(): String = if (this) "已满足" else "未满足"

private fun Boolean.toOnOff(): String = if (this) "已开启" else "已关闭"

private fun Boolean.toActiveInactive(): String = if (this) "运行中" else "未运行"

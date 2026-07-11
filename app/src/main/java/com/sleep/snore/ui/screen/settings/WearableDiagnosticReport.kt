package com.sleep.snore.ui.screen.settings

import com.sleep.snore.sleeptrigger.HealthConnectSleepTriggerSource
import com.sleep.snore.sleeptrigger.XiaomiSleepCompanionApps

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
                    triggerStartedAtMillis = input.activeRecordingTriggerStartedAtMillis,
                    sourcePackage = input.latestWearableSleepSessionSourcePackage,
                    stopOnSleepEndEnabled = input.stopOnSleepEndEnabled,
                    recordingActive = input.recordingActive,
                    activeRecordingTriggerSource = input.activeRecordingTriggerSource,
                    hasHealthConnectSleepReadPermission = input.hasHealthConnectSleepReadPermission
                )
            }"
        )
        appendLine("下一步建议：${wearableDiagnosticNextStep(input)}")
        appendLine("后台任务：")
        appendLine(input.workManagerDiagnosticsText.ifBlank { "无" })
        appendLine("数据库：")
        appendLine(input.databaseDiagnosticsText.ifBlank { "无" })
        appendLine("说明：Android 后台不能可靠直接开启麦克风；推荐睡前打开前台检测，睡醒后等待小米同步睡眠结束到 Health Connect 自动停录。本应用只读取已同步到 Health Connect 的睡眠会话；当前不是实时小米手环直连，也不承诺公开实时小米睡眠 API。")
    }.trimEnd()
}

internal fun wearableDiagnosticNextStep(input: WearableDiagnosticReportInput): String {
    return when {
        !input.hasRecordAudioPermission -> {
            "先授予麦克风权限；睡前前台检测需要用户可见地启动麦克风。"
        }
        !input.hasNotificationPermission -> {
            "先授予通知权限；前台检测依赖常驻通知保持稳定运行。"
        }
        !input.hasHealthConnectSleepReadPermission -> {
            "先授权本应用读取 Health Connect 睡眠数据。"
        }
        !input.hasHealthConnectBackgroundReadPermission -> {
            "补充授权 Health Connect 后台读取；否则周期检查和兜底停录会受限。"
        }
        input.xiaomiCompanionText.startsWith("未检测到") -> {
            "安装或打开 Mi Fitness / 小米运动健康 / Zepp Life，并确认睡眠已同步到 Health Connect。"
        }
        !input.periodicCheckEnabled -> {
            "开启 Health Connect 周期检查；它只读同步睡眠，不会后台开麦。"
        }
        !input.stopOnSleepEndEnabled -> {
            "开启“睡眠结束后自动停止”，否则睡醒后需要手动停止前台检测。"
        }
        input.latestWearableSleepSessionSourcePackage.isNotBlank() &&
            input.latestWearableSleepSessionSourcePackage !in XiaomiSleepCompanionApps.packageNames -> {
            "最近睡眠不是小米伴侣来源；打开小米伴侣确认睡眠同步到 Health Connect 后再立即检查。"
        }
        input.latestWearableSleepSessionSourcePackage.isBlank() &&
            input.latestWearableSleepSessionEndMillis > input.latestWearableSleepSessionStartMillis -> {
            "最近睡眠缺少来源信息；请打开小米伴侣确认睡眠同步到 Health Connect 后再立即检查。"
        }
        !input.recordingActive || input.activeRecordingTriggerSource != HealthConnectSleepTriggerSource.SOURCE -> {
            "睡前点击“睡前开启前台检测”；手环/Health Connect 只负责睡醒后的停止和校准。"
        }
        input.latestWearableSleepSessionEndMillis <= input.latestWearableSleepSessionStartMillis -> {
            "等待小米伴侣把睡眠结束时间同步到 Health Connect；同步后会自动停录或可点立即检查。"
        }
        else -> {
            "链路基本就绪；若真机仍异常，请连同本诊断报告和系统日志一起排查 Health Connect 同步延迟。"
        }
    }
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
    triggerStartedAtMillis: Long,
    sourcePackage: String = "",
    stopOnSleepEndEnabled: Boolean = true,
    recordingActive: Boolean = true,
    activeRecordingTriggerSource: String = HealthConnectSleepTriggerSource.SOURCE,
    hasHealthConnectSleepReadPermission: Boolean = true
): String {
    if (!stopOnSleepEndEnabled) {
        return "不会自动停录：睡眠结束自动停录开关已关闭"
    }
    if (!recordingActive) {
        return "不会自动停录：当前没有正在运行的前台鼾声检测"
    }
    if (activeRecordingTriggerSource != HealthConnectSleepTriggerSource.SOURCE) {
        return "不会自动停录：当前录音不是睡前前台检测 / Health Connect 来源"
    }
    if (!hasHealthConnectSleepReadPermission) {
        return "不会自动停录：缺少 Health Connect 睡眠读取权限"
    }
    val durationMinutes = sleepDurationMinutes(sleepStartMillis, sleepEndMillis)
        ?: return "无有效最近睡眠"
    if (sourcePackage.isBlank()) {
        return "无法自动停录：最近睡眠缺少来源信息，无法确认来自小米伴侣"
    }
    if (sourcePackage.isNotBlank() && sourcePackage !in XiaomiSleepCompanionApps.packageNames) {
        return "会被忽略：来源不是已知小米伴侣，仅用于诊断"
    }
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

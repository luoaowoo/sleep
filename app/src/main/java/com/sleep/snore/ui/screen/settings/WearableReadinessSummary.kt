package com.sleep.snore.ui.screen.settings

internal fun wearableReadinessSummary(
    hasRecordAudioPermission: Boolean,
    hasNotificationPermission: Boolean,
    hasHealthConnectPermission: Boolean,
    isIgnoringBatteryOptimizations: Boolean,
    hasXiaomiCompanion: Boolean,
    periodicCheckEnabled: Boolean,
    stopOnSleepEndEnabled: Boolean
): String {
    val missingItems = buildList {
        if (!hasRecordAudioPermission) add("麦克风权限")
        if (!hasNotificationPermission) add("通知权限")
        if (!hasHealthConnectPermission) add("Health Connect 睡眠/后台读取授权")
        if (!isIgnoringBatteryOptimizations) add("电池优化放行")
        if (!hasXiaomiCompanion) add("小米伴侣 App")
        if (!periodicCheckEnabled) add("Health Connect 周期检查")
        if (!stopOnSleepEndEnabled) add("睡眠结束后自动停止")
    }
    return if (missingItems.isEmpty()) {
        "睡前检测准备基本完成；睡前点击“睡前开启前台检测”即可开始整晚鼾声检测。"
    } else {
        "睡前检测还需处理：${missingItems.joinToString("、")}。"
    }
}

internal fun wearableIntegrationStatusSummary(
    hasXiaomiCompanion: Boolean,
    hasHealthConnectPermission: Boolean,
    periodicCheckEnabled: Boolean,
    stopOnSleepEndEnabled: Boolean,
    foregroundDetectionActive: Boolean
): String {
    return when {
        foregroundDetectionActive && !stopOnSleepEndEnabled -> {
            "小米手环/Health Connect 辅助链路运行中：前台鼾声检测已开启，但自动停录已关闭；睡醒后需要手动停止。"
        }
        foregroundDetectionActive -> {
            "小米手环/Health Connect 辅助链路运行中：前台鼾声检测已开启，应用会等待同步后的睡眠结束记录用于自动停录。"
        }
        !hasXiaomiCompanion -> {
            "小米接入待配置：请先安装并打开 Mi Fitness/小米运动健康或 Zepp Life，确认该版本支持 Health Connect 睡眠同步。"
        }
        !hasHealthConnectPermission -> {
            "小米伴侣 App 已就绪：下一步授权本应用读取 Health Connect 睡眠和后台读取数据。"
        }
        !periodicCheckEnabled -> {
            "Health Connect 睡眠读取已就绪：下一步开启周期检查，并在睡前点击“睡前开启前台检测”。"
        }
        !stopOnSleepEndEnabled -> {
            "小米伴侣 + Health Connect 链路已配置，但自动停录已关闭；睡醒后需要手动停止鼾声检测。"
        }
        else -> {
            "小米伴侣 + Health Connect 链路已配置：睡前点击“睡前开启前台检测”，睡醒后等待小米同步睡眠结束记录。"
        }
    }
}

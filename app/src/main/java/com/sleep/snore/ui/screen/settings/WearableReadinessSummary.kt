package com.sleep.snore.ui.screen.settings

internal fun wearableReadinessSummary(
    hasRecordAudioPermission: Boolean,
    hasNotificationPermission: Boolean,
    hasHealthConnectPermission: Boolean,
    isIgnoringBatteryOptimizations: Boolean,
    hasXiaomiCompanion: Boolean,
    periodicCheckEnabled: Boolean
): String {
    val missingItems = buildList {
        if (!hasRecordAudioPermission) add("麦克风权限")
        if (!hasNotificationPermission) add("通知权限")
        if (!hasHealthConnectPermission) add("Health Connect 授权")
        if (!isIgnoringBatteryOptimizations) add("电池优化放行")
        if (!hasXiaomiCompanion) add("小米伴侣 App")
        if (!periodicCheckEnabled) add("Health Connect 周期检查")
    }
    return if (missingItems.isEmpty()) {
        "睡前检测准备基本完成；睡前点击“睡前开启前台检测”即可开始整晚鼾声检测。"
    } else {
        "睡前检测还需处理：${missingItems.joinToString("、")}。"
    }
}

package com.sleep.snore.ui.screen.settings

import com.sleep.snore.sleeptrigger.XiaomiSleepCompanionApps

internal fun wearableReadinessSummary(
    hasRecordAudioPermission: Boolean,
    hasNotificationPermission: Boolean,
    hasHealthConnectSleepReadPermission: Boolean,
    hasHealthConnectBackgroundReadPermission: Boolean,
    isIgnoringBatteryOptimizations: Boolean,
    hasXiaomiCompanion: Boolean,
    periodicCheckEnabled: Boolean,
    stopOnSleepEndEnabled: Boolean,
    latestWearableSleepSessionSourcePackage: String,
    healthConnectBackgroundReadAvailable: Boolean? = true
): String {
    val requiredMissingItems = buildList {
        if (!hasRecordAudioPermission) add("麦克风权限")
        if (!hasNotificationPermission) add("通知权限")
        if (!hasHealthConnectSleepReadPermission) add("Health Connect 睡眠读取授权")
        when (healthConnectBackgroundReadAvailable) {
            null -> add("Health Connect 后台读取支持（检查中）")
            false -> add("Health Connect 后台读取支持")
            true -> {
                if (!hasHealthConnectBackgroundReadPermission) {
                    add("Health Connect 后台读取授权")
                }
            }
        }
        if (!hasXiaomiCompanion) add("小米伴侣 App")
        if (!stopOnSleepEndEnabled) add("睡眠结束后自动停止")
    }
    if (requiredMissingItems.isNotEmpty()) {
        return "睡前检测还需处理：${requiredMissingItems.joinToString("、")}。"
    }
    if (latestWearableSleepSessionSourcePackage.isNotBlank() &&
        latestWearableSleepSessionSourcePackage !in XiaomiSleepCompanionApps.packageNames
    ) {
        return "睡前检测权限已满足，但最近睡眠不是小米伴侣来源；请先确认小米伴侣已同步睡眠到 Health Connect。"
    }
    if (latestWearableSleepSessionSourcePackage.isBlank()) {
        return "睡前检测权限已满足；小米睡眠同步仍待验证，请先让小米伴侣同步一次睡眠到 Health Connect。"
    }
    val recommendedItems = buildList {
        if (!isIgnoringBatteryOptimizations) add("电池优化放行")
        if (!periodicCheckEnabled) add("Health Connect 周期检查")
    }
    return if (recommendedItems.isEmpty()) {
        "睡前检测准备基本完成；睡前点击“睡前开启前台检测”即可开始整晚鼾声检测。"
    } else {
        "可开始睡前前台检测；建议补充：${recommendedItems.joinToString("、")}。"
    }
}

internal fun wearableIntegrationStatusSummary(
    hasXiaomiCompanion: Boolean,
    hasHealthConnectSleepReadPermission: Boolean,
    hasHealthConnectBackgroundReadPermission: Boolean,
    periodicCheckEnabled: Boolean,
    stopOnSleepEndEnabled: Boolean,
    foregroundDetectionActive: Boolean,
    latestWearableSleepSessionSourcePackage: String,
    healthConnectBackgroundReadAvailable: Boolean? = true
): String {
    return when {
        foregroundDetectionActive && !stopOnSleepEndEnabled -> {
            "小米手环/Health Connect 辅助链路运行中：前台鼾声检测已开启，但自动停录已关闭；睡醒后需要手动停止。"
        }
        foregroundDetectionActive -> {
            "小米手环/Health Connect 辅助链路运行中：前台鼾声检测已开启，应用会等待同步后的睡眠结束记录用于自动停录；若睡醒后仍未停止，请先打开小米伴侣确认已同步到 Health Connect，再点立即检查。"
        }
        !hasXiaomiCompanion -> {
            "小米接入待配置：请先安装并打开 Mi Fitness/小米运动健康或 Zepp Life，确认该版本支持 Health Connect 睡眠同步。"
        }
        !hasHealthConnectSleepReadPermission -> {
            "小米伴侣 App 已就绪：下一步授权本应用读取 Health Connect 睡眠数据。"
        }
        healthConnectBackgroundReadAvailable == null -> {
            "Health Connect 后台读取支持正在检查，请稍后再点立即检查。"
        }
        !healthConnectBackgroundReadAvailable -> {
            "Health Connect 睡眠读取可用于立即检查，但当前设备或 Health Connect 版本不支持后台读取；锁屏后自动停录不稳定，睡醒后请手动确认。"
        }
        !hasHealthConnectBackgroundReadPermission -> {
            "Health Connect 睡眠读取已授权：可手动检查最近睡眠；后台周期检查和 Worker 兜底停录仍需后台读取授权；已运行的前台检测可继续低频检查睡眠结束。"
        }
        !periodicCheckEnabled -> {
            "Health Connect 睡眠读取已就绪：下一步开启周期检查，并在睡前点击“睡前开启前台检测”。"
        }
        latestWearableSleepSessionSourcePackage.isNotBlank() &&
            latestWearableSleepSessionSourcePackage !in XiaomiSleepCompanionApps.packageNames -> {
            "小米伴侣同步待验证：最近睡眠来源不是小米伴侣；请打开 Mi Fitness/小米运动健康或 Zepp Life，确认睡眠同步到 Health Connect 后再立即检查。"
        }
        latestWearableSleepSessionSourcePackage.isBlank() -> {
            "小米伴侣同步待验证：权限和开关已准备好，但还没有读取到小米来源睡眠；请先让小米伴侣同步一次睡眠到 Health Connect。"
        }
        !stopOnSleepEndEnabled -> {
            "小米伴侣 + Health Connect 链路已验证，但自动停录已关闭；睡醒后需要手动停止鼾声检测。"
        }
        else -> {
            "小米伴侣 + Health Connect 链路已验证：睡前点击“睡前开启前台检测”，睡醒后等待小米同步睡眠结束记录；若未自动停录，请打开小米伴侣确认同步后再点立即检查。"
        }
    }
}

package com.sleep.snore.ui.screen.settings

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WearableReadinessSummaryTest {

    @Test
    fun wearableReadinessSummary_reportsReadyWhenAllChecksPass() {
        val summary = wearableReadinessSummary(
            hasRecordAudioPermission = true,
            hasNotificationPermission = true,
            hasHealthConnectSleepReadPermission = true,
            hasHealthConnectBackgroundReadPermission = true,
            isIgnoringBatteryOptimizations = true,
            hasXiaomiCompanion = true,
            periodicCheckEnabled = true,
            stopOnSleepEndEnabled = true,
            latestWearableSleepSessionSourcePackage = "com.xiaomi.wearable"
        )

        assertThat(summary).contains("睡前检测准备基本完成")
        assertThat(summary).contains("睡前开启前台检测")
    }

    @Test
    fun wearableReadinessSummary_listsMissingItems() {
        val summary = wearableReadinessSummary(
            hasRecordAudioPermission = false,
            hasNotificationPermission = false,
            hasHealthConnectSleepReadPermission = false,
            hasHealthConnectBackgroundReadPermission = false,
            isIgnoringBatteryOptimizations = false,
            hasXiaomiCompanion = false,
            periodicCheckEnabled = false,
            stopOnSleepEndEnabled = false,
            latestWearableSleepSessionSourcePackage = ""
        )

        assertThat(summary).contains("麦克风权限")
        assertThat(summary).contains("通知权限")
        assertThat(summary).contains("Health Connect 睡眠读取授权")
        assertThat(summary).contains("Health Connect 后台读取授权")
        assertThat(summary).contains("小米伴侣 App")
        assertThat(summary).contains("睡眠结束后自动停止")
    }

    @Test
    fun wearableReadinessSummary_blocksMissingBackgroundReadPermission() {
        val summary = wearableReadinessSummary(
            hasRecordAudioPermission = true,
            hasNotificationPermission = true,
            hasHealthConnectSleepReadPermission = true,
            hasHealthConnectBackgroundReadPermission = false,
            isIgnoringBatteryOptimizations = true,
            hasXiaomiCompanion = true,
            periodicCheckEnabled = true,
            stopOnSleepEndEnabled = true,
            latestWearableSleepSessionSourcePackage = "com.xiaomi.wearable"
        )

        assertThat(summary).contains("睡前检测还需处理")
        assertThat(summary).contains("Health Connect 后台读取授权")
    }

    @Test
    fun wearableReadinessSummary_reportsUnsupportedBackgroundRead() {
        val summary = wearableReadinessSummary(
            hasRecordAudioPermission = true,
            hasNotificationPermission = true,
            hasHealthConnectSleepReadPermission = true,
            hasHealthConnectBackgroundReadPermission = false,
            healthConnectBackgroundReadAvailable = false,
            isIgnoringBatteryOptimizations = true,
            hasXiaomiCompanion = true,
            periodicCheckEnabled = true,
            stopOnSleepEndEnabled = true,
            latestWearableSleepSessionSourcePackage = "com.xiaomi.wearable"
        )

        assertThat(summary).contains("Health Connect 后台读取支持")
        assertThat(summary).doesNotContain("Health Connect 后台读取授权")
    }

    @Test
    fun wearableReadinessSummary_allowsStartWithOnlyRecommendedItemsMissing() {
        val summary = wearableReadinessSummary(
            hasRecordAudioPermission = true,
            hasNotificationPermission = true,
            hasHealthConnectSleepReadPermission = true,
            hasHealthConnectBackgroundReadPermission = true,
            isIgnoringBatteryOptimizations = false,
            hasXiaomiCompanion = true,
            periodicCheckEnabled = false,
            stopOnSleepEndEnabled = true,
            latestWearableSleepSessionSourcePackage = "com.xiaomi.wearable"
        )

        assertThat(summary).contains("可开始睡前前台检测")
        assertThat(summary).contains("建议补充")
        assertThat(summary).contains("电池优化放行")
        assertThat(summary).contains("Health Connect 周期检查")
    }

    @Test
    fun wearableReadinessSummary_requiresXiaomiSleepSyncVerification() {
        val summary = wearableReadinessSummary(
            hasRecordAudioPermission = true,
            hasNotificationPermission = true,
            hasHealthConnectSleepReadPermission = true,
            hasHealthConnectBackgroundReadPermission = true,
            isIgnoringBatteryOptimizations = true,
            hasXiaomiCompanion = true,
            periodicCheckEnabled = true,
            stopOnSleepEndEnabled = true,
            latestWearableSleepSessionSourcePackage = ""
        )

        assertThat(summary).contains("小米睡眠同步仍待验证")
        assertThat(summary).doesNotContain("准备基本完成")
    }

    @Test
    fun wearableReadinessSummary_rejectsNonXiaomiSleepSource() {
        val summary = wearableReadinessSummary(
            hasRecordAudioPermission = true,
            hasNotificationPermission = true,
            hasHealthConnectSleepReadPermission = true,
            hasHealthConnectBackgroundReadPermission = true,
            isIgnoringBatteryOptimizations = true,
            hasXiaomiCompanion = true,
            periodicCheckEnabled = true,
            stopOnSleepEndEnabled = true,
            latestWearableSleepSessionSourcePackage = "com.example.sleep"
        )

        assertThat(summary).contains("最近睡眠不是小米伴侣来源")
        assertThat(summary).doesNotContain("准备基本完成")
    }

    @Test
    fun wearableIntegrationStatusSummary_reportsForegroundDetectionActive() {
        val summary = wearableIntegrationStatusSummary(
            hasXiaomiCompanion = true,
            hasHealthConnectSleepReadPermission = true,
            hasHealthConnectBackgroundReadPermission = true,
            periodicCheckEnabled = true,
            stopOnSleepEndEnabled = true,
            foregroundDetectionActive = true,
            latestWearableSleepSessionSourcePackage = ""
        )

        assertThat(summary).contains("辅助链路运行中")
        assertThat(summary).contains("自动停录")
    }

    @Test
    fun wearableIntegrationStatusSummary_guidesMissingCompanionFirst() {
        val summary = wearableIntegrationStatusSummary(
            hasXiaomiCompanion = false,
            hasHealthConnectSleepReadPermission = false,
            hasHealthConnectBackgroundReadPermission = false,
            periodicCheckEnabled = false,
            stopOnSleepEndEnabled = false,
            foregroundDetectionActive = false,
            latestWearableSleepSessionSourcePackage = ""
        )

        assertThat(summary).contains("请先安装")
        assertThat(summary).contains("Health Connect 睡眠同步")
    }

    @Test
    fun wearableIntegrationStatusSummary_guidesHealthConnectAuthorization() {
        val summary = wearableIntegrationStatusSummary(
            hasXiaomiCompanion = true,
            hasHealthConnectSleepReadPermission = false,
            hasHealthConnectBackgroundReadPermission = false,
            periodicCheckEnabled = false,
            stopOnSleepEndEnabled = false,
            foregroundDetectionActive = false,
            latestWearableSleepSessionSourcePackage = ""
        )

        assertThat(summary).contains("授权本应用读取 Health Connect")
    }

    @Test
    fun wearableIntegrationStatusSummary_guidesBackgroundReadAuthorization() {
        val summary = wearableIntegrationStatusSummary(
            hasXiaomiCompanion = true,
            hasHealthConnectSleepReadPermission = true,
            hasHealthConnectBackgroundReadPermission = false,
            periodicCheckEnabled = true,
            stopOnSleepEndEnabled = true,
            foregroundDetectionActive = false,
            latestWearableSleepSessionSourcePackage = ""
        )

        assertThat(summary).contains("可手动检查最近睡眠")
        assertThat(summary).contains("后台读取授权")
    }

    @Test
    fun wearableIntegrationStatusSummary_reportsUnsupportedBackgroundRead() {
        val summary = wearableIntegrationStatusSummary(
            hasXiaomiCompanion = true,
            hasHealthConnectSleepReadPermission = true,
            hasHealthConnectBackgroundReadPermission = false,
            healthConnectBackgroundReadAvailable = false,
            periodicCheckEnabled = true,
            stopOnSleepEndEnabled = true,
            foregroundDetectionActive = false,
            latestWearableSleepSessionSourcePackage = ""
        )

        assertThat(summary).contains("不支持后台读取")
        assertThat(summary).contains("手动确认")
    }

    @Test
    fun wearableIntegrationStatusSummary_guidesPeriodicCheckBeforeBedtimeStart() {
        val summary = wearableIntegrationStatusSummary(
            hasXiaomiCompanion = true,
            hasHealthConnectSleepReadPermission = true,
            hasHealthConnectBackgroundReadPermission = true,
            periodicCheckEnabled = false,
            stopOnSleepEndEnabled = true,
            foregroundDetectionActive = false,
            latestWearableSleepSessionSourcePackage = ""
        )

        assertThat(summary).contains("开启周期检查")
        assertThat(summary).contains("睡前开启前台检测")
    }

    @Test
    fun wearableIntegrationStatusSummary_reportsConfiguredButNotStarted() {
        val summary = wearableIntegrationStatusSummary(
            hasXiaomiCompanion = true,
            hasHealthConnectSleepReadPermission = true,
            hasHealthConnectBackgroundReadPermission = true,
            periodicCheckEnabled = true,
            stopOnSleepEndEnabled = true,
            foregroundDetectionActive = false,
            latestWearableSleepSessionSourcePackage = "com.xiaomi.wearable"
        )

        assertThat(summary).contains("链路已验证")
        assertThat(summary).contains("等待小米同步睡眠结束记录")
    }

    @Test
    fun wearableIntegrationStatusSummary_reportsPendingXiaomiSyncVerification() {
        val summary = wearableIntegrationStatusSummary(
            hasXiaomiCompanion = true,
            hasHealthConnectSleepReadPermission = true,
            hasHealthConnectBackgroundReadPermission = true,
            periodicCheckEnabled = true,
            stopOnSleepEndEnabled = true,
            foregroundDetectionActive = false,
            latestWearableSleepSessionSourcePackage = ""
        )

        assertThat(summary).contains("同步待验证")
        assertThat(summary).contains("小米来源睡眠")
    }

    @Test
    fun wearableIntegrationStatusSummary_reportsNonXiaomiSourceBeforeConfigured() {
        val summary = wearableIntegrationStatusSummary(
            hasXiaomiCompanion = true,
            hasHealthConnectSleepReadPermission = true,
            hasHealthConnectBackgroundReadPermission = true,
            periodicCheckEnabled = true,
            stopOnSleepEndEnabled = true,
            foregroundDetectionActive = false,
            latestWearableSleepSessionSourcePackage = "com.example.sleep"
        )

        assertThat(summary).contains("最近睡眠来源不是小米伴侣")
        assertThat(summary).contains("Health Connect")
    }

    @Test
    fun wearableIntegrationStatusSummary_reportsAutoStopDisabled() {
        val summary = wearableIntegrationStatusSummary(
            hasXiaomiCompanion = true,
            hasHealthConnectSleepReadPermission = true,
            hasHealthConnectBackgroundReadPermission = true,
            periodicCheckEnabled = true,
            stopOnSleepEndEnabled = false,
            foregroundDetectionActive = false,
            latestWearableSleepSessionSourcePackage = "com.xiaomi.wearable"
        )

        assertThat(summary).contains("自动停录已关闭")
        assertThat(summary).contains("手动停止")
    }

    @Test
    fun wearableIntegrationStatusSummary_reportsForegroundActiveWithAutoStopDisabled() {
        val summary = wearableIntegrationStatusSummary(
            hasXiaomiCompanion = true,
            hasHealthConnectSleepReadPermission = true,
            hasHealthConnectBackgroundReadPermission = true,
            periodicCheckEnabled = true,
            stopOnSleepEndEnabled = false,
            foregroundDetectionActive = true,
            latestWearableSleepSessionSourcePackage = ""
        )

        assertThat(summary).contains("前台鼾声检测已开启")
        assertThat(summary).contains("睡醒后需要手动停止")
    }
}

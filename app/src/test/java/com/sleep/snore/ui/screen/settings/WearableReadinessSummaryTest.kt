package com.sleep.snore.ui.screen.settings

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WearableReadinessSummaryTest {

    @Test
    fun wearableReadinessSummary_reportsReadyWhenAllChecksPass() {
        val summary = wearableReadinessSummary(
            hasRecordAudioPermission = true,
            hasNotificationPermission = true,
            hasHealthConnectPermission = true,
            isIgnoringBatteryOptimizations = true,
            hasXiaomiCompanion = true,
            periodicCheckEnabled = true
        )

        assertThat(summary).contains("睡前检测准备基本完成")
        assertThat(summary).contains("睡前开启前台检测")
    }

    @Test
    fun wearableReadinessSummary_listsMissingItems() {
        val summary = wearableReadinessSummary(
            hasRecordAudioPermission = false,
            hasNotificationPermission = false,
            hasHealthConnectPermission = false,
            isIgnoringBatteryOptimizations = false,
            hasXiaomiCompanion = false,
            periodicCheckEnabled = false
        )

        assertThat(summary).contains("麦克风权限")
        assertThat(summary).contains("通知权限")
        assertThat(summary).contains("Health Connect 睡眠/后台读取授权")
        assertThat(summary).contains("电池优化放行")
        assertThat(summary).contains("小米伴侣 App")
        assertThat(summary).contains("Health Connect 周期检查")
    }

    @Test
    fun wearableIntegrationStatusSummary_reportsForegroundDetectionActive() {
        val summary = wearableIntegrationStatusSummary(
            hasXiaomiCompanion = true,
            hasHealthConnectPermission = true,
            periodicCheckEnabled = true,
            foregroundDetectionActive = true
        )

        assertThat(summary).contains("辅助链路运行中")
        assertThat(summary).contains("自动停录")
    }

    @Test
    fun wearableIntegrationStatusSummary_guidesMissingCompanionFirst() {
        val summary = wearableIntegrationStatusSummary(
            hasXiaomiCompanion = false,
            hasHealthConnectPermission = false,
            periodicCheckEnabled = false,
            foregroundDetectionActive = false
        )

        assertThat(summary).contains("请先安装")
        assertThat(summary).contains("Health Connect 睡眠同步")
    }

    @Test
    fun wearableIntegrationStatusSummary_guidesHealthConnectAuthorization() {
        val summary = wearableIntegrationStatusSummary(
            hasXiaomiCompanion = true,
            hasHealthConnectPermission = false,
            periodicCheckEnabled = false,
            foregroundDetectionActive = false
        )

        assertThat(summary).contains("授权本应用读取 Health Connect")
    }

    @Test
    fun wearableIntegrationStatusSummary_guidesPeriodicCheckBeforeBedtimeStart() {
        val summary = wearableIntegrationStatusSummary(
            hasXiaomiCompanion = true,
            hasHealthConnectPermission = true,
            periodicCheckEnabled = false,
            foregroundDetectionActive = false
        )

        assertThat(summary).contains("开启周期检查")
        assertThat(summary).contains("睡前开启前台检测")
    }

    @Test
    fun wearableIntegrationStatusSummary_reportsConfiguredButNotStarted() {
        val summary = wearableIntegrationStatusSummary(
            hasXiaomiCompanion = true,
            hasHealthConnectPermission = true,
            periodicCheckEnabled = true,
            foregroundDetectionActive = false
        )

        assertThat(summary).contains("链路已配置")
        assertThat(summary).contains("等待小米同步睡眠结束记录")
    }
}

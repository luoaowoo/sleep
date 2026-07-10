package com.sleep.snore.ui.screen.settings

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WearableReadinessSummaryTest {

    @Test
    fun wearableReadinessSummary_reportsReadyWhenAllChecksPass() {
        val summary = wearableReadinessSummary(
            hasRecordAudioPermission = true,
            hasNotificationPermission = true,
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
            isIgnoringBatteryOptimizations = false,
            hasXiaomiCompanion = false,
            periodicCheckEnabled = false
        )

        assertThat(summary).contains("麦克风权限")
        assertThat(summary).contains("通知权限")
        assertThat(summary).contains("电池优化放行")
        assertThat(summary).contains("小米伴侣 App")
        assertThat(summary).contains("Health Connect 周期检查")
    }
}

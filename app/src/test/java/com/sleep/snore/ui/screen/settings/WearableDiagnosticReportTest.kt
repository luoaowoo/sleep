package com.sleep.snore.ui.screen.settings

import androidx.health.connect.client.HealthConnectClient
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WearableDiagnosticReportTest {

    @Test
    fun wearableDiagnosticReport_includesKeyTroubleshootingFields() {
        val report = wearableDiagnosticReport(
            WearableDiagnosticReportInput(
                generatedAtText = "2026-07-11 23:30:00",
                appText = "com.sleep.snore / 1.0 (1)",
                deviceText = "Xiaomi 15 / Android 16",
                healthConnectStatusText = "可用",
                hasRecordAudioPermission = true,
                hasNotificationPermission = false,
                hasHealthConnectSleepReadPermission = true,
                hasHealthConnectBackgroundReadPermission = false,
                isIgnoringBatteryOptimizations = false,
                xiaomiCompanionText = "Mi Fitness (com.xiaomi.wearable)",
                periodicCheckEnabled = true,
                stopOnSleepEndEnabled = true,
                foregroundDetectionActive = true,
                recordingRuntimeText = "运行中，事件数 3",
                recordingActive = true,
                recordingStartTimeMillis = 1_000_500L,
                recordingEventCount = 3,
                activeRecordingTriggerSource = "health_connect_sleep",
                activeRecordingTriggerStartedAtText = "07-11 23:00",
                activeRecordingTriggerStartedAtMillis = 1_000_000L,
                wearableSleepTriggerStatus = "睡前前台检测已开启",
                wearableSleepTriggerLastCheckText = "07-11 23:20",
                latestWearableSleepSessionText = "07-11 01:00 - 07-11 08:00（已读取睡眠结束）",
                workManagerDiagnosticsText = "health_connect_sleep_trigger: ENQUEUED(attempt=0)"
            )
        )

        assertThat(report).contains("SleepSnore 小米手环 / Health Connect 诊断")
        assertThat(report).contains("应用：com.sleep.snore / 1.0 (1)")
        assertThat(report).contains("设备：Xiaomi 15 / Android 16")
        assertThat(report).contains("Health Connect：可用")
        assertThat(report).contains("小米伴侣：Mi Fitness")
        assertThat(report).contains("通知权限：未满足")
        assertThat(report).contains("Health Connect 后台读取：未满足")
        assertThat(report).contains("前台睡前检测：运行中")
        assertThat(report).contains("录音运行态：运行中，事件数 3")
        assertThat(report).contains("录音运行中：已满足")
        assertThat(report).contains("录音开始毫秒：1000500")
        assertThat(report).contains("录音事件数：3")
        assertThat(report).contains("当前录音来源：health_connect_sleep")
        assertThat(report).contains("当前录音触发时间：07-11 23:00")
        assertThat(report).contains("当前录音触发毫秒：1000000")
        assertThat(report).contains("录音开始-触发差值毫秒：500")
        assertThat(report).contains("最近同步睡眠：07-11 01:00 - 07-11 08:00")
        assertThat(report).contains("后台任务：")
        assertThat(report).contains("health_connect_sleep_trigger: ENQUEUED(attempt=0)")
        assertThat(report).contains("后台不能可靠直接开启麦克风")
        assertThat(report).doesNotContain("API Key")
        assertThat(report).doesNotContain("原始音频")
    }

    @Test
    fun healthConnectStatusText_reportsProviderUpdateRequired() {
        assertThat(
            healthConnectStatusText(HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED)
        ).contains("安装或更新")
    }

    @Test
    fun recordingTriggerOffsetMillis_returnsNullWhenTimingMissing() {
        assertThat(recordingTriggerOffsetMillis(0L, 1_000L)).isNull()
        assertThat(recordingTriggerOffsetMillis(1_000L, 0L)).isNull()
    }

    @Test
    fun wearableWorkDiagnosticsText_includesEmptyAndRetryStates() {
        val text = wearableWorkDiagnosticsText(
            listOf(
                WearableWorkDiagnosticItem("health_connect_sleep_trigger", emptyList()),
                WearableWorkDiagnosticItem("active_recording_finalizer", listOf("RUNNING(attempt=2)"))
            )
        )

        assertThat(text).contains("health_connect_sleep_trigger: 无记录")
        assertThat(text).contains("active_recording_finalizer: RUNNING(attempt=2)")
    }
}

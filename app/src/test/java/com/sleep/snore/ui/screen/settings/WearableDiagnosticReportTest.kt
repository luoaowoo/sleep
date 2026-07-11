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
                deviceText = "xiaomi Xiaomi 15 / Android 16 (SDK 35) / build/fingerprint",
                healthConnectStatusText = "可用",
                healthConnectSdkStatusCode = HealthConnectClient.SDK_AVAILABLE,
                healthConnectGrantedPermissionsText = "android.permission.health.READ_SLEEP",
                hasRecordAudioPermission = true,
                hasNotificationPermission = false,
                hasHealthConnectSleepReadPermission = true,
                hasHealthConnectBackgroundReadPermission = false,
                isIgnoringBatteryOptimizations = false,
                xiaomiCompanionText = "Mi Fitness (com.xiaomi.wearable)",
                periodicCheckEnabled = true,
                stopOnSleepEndEnabled = true,
                bedtimeReminderEnabled = true,
                bedtimeReminderTimeText = "22:30",
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
                latestWearableSleepSessionStartMillis = 1_000_000L,
                latestWearableSleepSessionEndMillis = 26_200_000L,
                latestWearableSleepSessionStatus = "已读取睡眠结束",
                latestWearableSleepSessionSourcePackage = "com.xiaomi.wearable",
                workManagerDiagnosticsText = "health_connect_sleep_trigger: ENQUEUED(attempt=0)",
                databaseDiagnosticsText = "activeRecord: id=9, start=1000000, end=1000000, active=true, events=3"
            )
        )

        assertThat(report).contains("SleepSnore 小米手环 / Health Connect 诊断")
        assertThat(report).contains("应用：com.sleep.snore / 1.0 (1)")
        assertThat(report).contains("设备：xiaomi Xiaomi 15 / Android 16 (SDK 35) / build/fingerprint")
        assertThat(report).contains("Health Connect：可用")
        assertThat(report).contains("Health Connect SDK 状态码：")
        assertThat(report).contains("Health Connect 已授权权限：android.permission.health.READ_SLEEP")
        assertThat(report).contains("小米伴侣：Mi Fitness")
        assertThat(report).contains("通知权限：未满足")
        assertThat(report).contains("Health Connect 后台读取：未满足")
        assertThat(report).contains("睡前提醒：已开启（22:30）")
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
        assertThat(report).contains("最近睡眠开始毫秒：1000000")
        assertThat(report).contains("最近睡眠结束毫秒：26200000")
        assertThat(report).contains("最近睡眠状态：已读取睡眠结束")
        assertThat(report).contains("最近睡眠来源：com.xiaomi.wearable")
        assertThat(report).contains("最近睡眠时长分钟：420")
        assertThat(report).contains("最近睡眠与触发重叠分钟：420")
        assertThat(report).contains("最近睡眠自动停录规则判断：满足自动停录时间规则")
        assertThat(report).contains("后台任务：")
        assertThat(report).contains("health_connect_sleep_trigger: ENQUEUED(attempt=0)")
        assertThat(report).contains("数据库：")
        assertThat(report).contains("activeRecord: id=9")
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
    fun healthConnectGrantedPermissionsText_sortsPermissionsAndReportsEmpty() {
        assertThat(
            healthConnectGrantedPermissionsText(setOf("permission.z", "", "permission.a"))
        ).isEqualTo("permission.a, permission.z")
        assertThat(healthConnectGrantedPermissionsText(emptySet())).isEqualTo("无")
    }

    @Test
    fun recordingTriggerOffsetMillis_returnsNullWhenTimingMissing() {
        assertThat(recordingTriggerOffsetMillis(0L, 1_000L)).isNull()
        assertThat(recordingTriggerOffsetMillis(1_000L, 0L)).isNull()
    }

    @Test
    fun sleepOverlapAfterTriggerMinutes_reportsOverlapAfterTrigger() {
        assertThat(
            sleepOverlapAfterTriggerMinutes(
                sleepStartMillis = 1_000L,
                sleepEndMillis = 121_000L,
                triggerStartedAtMillis = 61_000L
            )
        ).isEqualTo(1L)
    }

    @Test
    fun sleepDurationMinutes_returnsNullForInvalidSleep() {
        assertThat(sleepDurationMinutes(0L, 1_000L)).isNull()
        assertThat(sleepDurationMinutes(2_000L, 1_000L)).isNull()
    }

    @Test
    fun sleepAutoStopRuleDiagnostic_reportsInsufficientOverlapBeforeShortSleep() {
        val diagnostic = sleepAutoStopRuleDiagnostic(
            sleepStartMillis = 1_000L,
            sleepEndMillis = 61_000L,
            triggerStartedAtMillis = 60_000L
        )

        assertThat(diagnostic).contains("重叠不足")
    }

    @Test
    fun sleepAutoStopRuleDiagnostic_reportsShortSleepAfterEnoughOverlap() {
        val diagnostic = sleepAutoStopRuleDiagnostic(
            sleepStartMillis = 1_000L,
            sleepEndMillis = 3_601_000L,
            triggerStartedAtMillis = 1_000L
        )

        assertThat(diagnostic).contains("睡眠时长不足")
    }

    @Test
    fun sleepAutoStopRuleDiagnostic_reportsSatisfiedRules() {
        val diagnostic = sleepAutoStopRuleDiagnostic(
            sleepStartMillis = 1_000L,
            sleepEndMillis = 7_201_000L,
            triggerStartedAtMillis = 1_000L
        )

        assertThat(diagnostic).contains("满足自动停录")
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

    @Test
    fun wearableDatabaseDiagnosticsText_includesActiveRecordAndEventKey() {
        val text = wearableDatabaseDiagnosticsText(
            WearableDatabaseDiagnosticSnapshot(
                activeRecordId = 9L,
                activeRecordStartMillis = 1_000L,
                activeRecordEndMillis = 1_000L,
                activeRecordEventCount = 3,
                lastWearableSleepEventKey = "SleepEnded:8000:1000"
            )
        )

        assertThat(text).contains("activeRecord: id=9")
        assertThat(text).contains("active=true")
        assertThat(text).contains("events=3")
        assertThat(text).contains("lastWearableSleepEventKey: SleepEnded:8000:1000")
    }

    @Test
    fun wearableDatabaseDiagnosticsText_reportsMissingActiveRecord() {
        val text = wearableDatabaseDiagnosticsText(
            WearableDatabaseDiagnosticSnapshot(
                activeRecordId = null,
                activeRecordStartMillis = null,
                activeRecordEndMillis = null,
                activeRecordEventCount = null,
                lastWearableSleepEventKey = null
            )
        )

        assertThat(text).contains("activeRecord: 无")
        assertThat(text).contains("lastWearableSleepEventKey: 无")
    }
}

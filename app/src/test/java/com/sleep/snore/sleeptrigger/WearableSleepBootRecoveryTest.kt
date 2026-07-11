package com.sleep.snore.sleeptrigger

import com.google.common.truth.Truth.assertThat
import com.sleep.snore.data.db.entity.SleepRecordEntity
import com.sleep.snore.data.model.Severity
import com.sleep.snore.data.preferences.SettingsPreferences
import org.junit.Test

class WearableSleepBootRecoveryTest {

    @Test
    fun shouldFinalizeWearableRecordingAfterBoot_allowsHealthConnectActiveRecord() {
        val settings = SettingsPreferences(
            activeRecordingTriggerSource = HealthConnectSleepTriggerSource.SOURCE
        )

        assertThat(shouldFinalizeWearableRecordingAfterBoot(settings, activeRecord())).isTrue()
    }

    @Test
    fun shouldFinalizeWearableRecordingAfterBoot_allowsWearableActiveRecordWhenPeriodicCheckDisabled() {
        val settings = SettingsPreferences(
            wearableSleepTriggerEnabled = false,
            activeRecordingTriggerSource = HealthConnectSleepTriggerSource.SOURCE
        )

        assertThat(shouldFinalizeWearableRecordingAfterBoot(settings, activeRecord())).isTrue()
    }

    @Test
    fun shouldFinalizeWearableRecordingAfterBoot_rejectsManualActiveRecord() {
        val settings = SettingsPreferences(activeRecordingTriggerSource = "")

        assertThat(shouldFinalizeWearableRecordingAfterBoot(settings, activeRecord())).isFalse()
    }

    @Test
    fun shouldFinalizeWearableRecordingAfterBoot_rejectsMissingActiveRecord() {
        val settings = SettingsPreferences(
            activeRecordingTriggerSource = HealthConnectSleepTriggerSource.SOURCE
        )

        assertThat(shouldFinalizeWearableRecordingAfterBoot(settings, activeRecord = null)).isFalse()
    }

    @Test
    fun wearableRestartRecoveryPlan_appStartEnqueuesHealthConnectFinalizer() {
        val plan = wearableRestartRecoveryPlan(
            entryPoint = WearableRestartRecoveryEntryPoint.AppStart,
            activeRecordExists = true,
            activeRecordingTriggerSource = HealthConnectSleepTriggerSource.SOURCE
        )

        assertThat(plan?.expectedSource).isEqualTo(HealthConnectSleepTriggerSource.SOURCE)
        assertThat(plan?.statusText).contains("应用启动")
    }

    @Test
    fun wearableRestartRecoveryPlan_bootCompletedUsesRestartStatus() {
        val plan = wearableRestartRecoveryPlan(
            entryPoint = WearableRestartRecoveryEntryPoint.BootCompleted,
            activeRecordExists = true,
            activeRecordingTriggerSource = HealthConnectSleepTriggerSource.SOURCE
        )

        assertThat(plan?.expectedSource).isEqualTo(HealthConnectSleepTriggerSource.SOURCE)
        assertThat(plan?.statusText).contains("重启/更新")
    }

    @Test
    fun wearableRestartRecoveryPlan_rejectsManualOrMissingActiveRecord() {
        assertThat(
            wearableRestartRecoveryPlan(
                entryPoint = WearableRestartRecoveryEntryPoint.AppStart,
                activeRecordExists = true,
                activeRecordingTriggerSource = ""
            )
        ).isNull()
        assertThat(
            wearableRestartRecoveryPlan(
                entryPoint = WearableRestartRecoveryEntryPoint.AppStart,
                activeRecordExists = false,
                activeRecordingTriggerSource = HealthConnectSleepTriggerSource.SOURCE
            )
        ).isNull()
    }

    private fun activeRecord(): SleepRecordEntity {
        return SleepRecordEntity(
            id = 1L,
            startTime = 1_000L,
            endTime = 1_000L,
            sleepDurationMin = 0,
            snoreScore = 0,
            severity = Severity.GOOD.name,
            estAHI = 0f,
            snoreDurationMin = 0,
            snoreRatio = 0f,
            avgDb = 0f,
            maxDb = 0f,
            snoreEventCount = 0,
            apneaEventCount = 0,
            longestApneaSec = 0,
            snoreTypeDistribution = "{}",
            hourlyDistribution = "[]",
            aiSummary = "",
            aiEvaluation = "",
            aiSuggestions = "[]",
            createdAt = 1_000L
        )
    }
}

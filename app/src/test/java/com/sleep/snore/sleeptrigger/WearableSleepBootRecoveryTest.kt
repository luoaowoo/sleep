package com.sleep.snore.sleeptrigger

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.google.common.truth.Truth.assertThat
import com.sleep.snore.data.db.entity.SleepRecordEntity
import com.sleep.snore.data.model.Severity
import com.sleep.snore.data.preferences.SecretTextCipher
import com.sleep.snore.data.preferences.SettingsPreferences
import com.sleep.snore.data.preferences.SettingsPreferencesRepository
import com.sleep.snore.data.repository.SleepRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
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

    @Test
    fun recoverWearableRecordingAfterRestartIfNeeded_enqueuesFinalizerAndWritesStatus() = runTest {
        val settingsRepository = createSettingsRepository()
        settingsRepository.setActiveRecordingTriggerSource(HealthConnectSleepTriggerSource.SOURCE, 1_000L)
        val sleepRepository = mockk<SleepRepository>()
        coEvery { sleepRepository.getActiveRecordingRecord() } returns activeRecord()
        val enqueuedSources = mutableListOf<String>()

        val recovered = recoverWearableRecordingAfterRestartIfNeeded(
            context = mockContext(),
            settingsRepository = settingsRepository,
            sleepRepository = sleepRepository,
            entryPoint = WearableRestartRecoveryEntryPoint.AppStart,
            enqueueFinalizer = { _, expectedSource -> enqueuedSources += expectedSource }
        )

        assertThat(recovered).isTrue()
        assertThat(enqueuedSources).containsExactly(HealthConnectSleepTriggerSource.SOURCE)
        assertThat(settingsRepository.settings.first().wearableSleepTriggerStatus).contains("应用启动")
    }

    @Test
    fun recoverWearableRecordingAfterRestartIfNeeded_skipsManualActiveRecord() = runTest {
        val settingsRepository = createSettingsRepository()
        settingsRepository.setActiveRecordingTriggerSource("manual", 1_000L)
        val sleepRepository = mockk<SleepRepository>()
        coEvery { sleepRepository.getActiveRecordingRecord() } returns activeRecord()
        val enqueuedSources = mutableListOf<String>()

        val recovered = recoverWearableRecordingAfterRestartIfNeeded(
            context = mockContext(),
            settingsRepository = settingsRepository,
            sleepRepository = sleepRepository,
            entryPoint = WearableRestartRecoveryEntryPoint.AppStart,
            enqueueFinalizer = { _, expectedSource -> enqueuedSources += expectedSource }
        )

        assertThat(recovered).isFalse()
        assertThat(enqueuedSources).isEmpty()
        assertThat(settingsRepository.settings.first().wearableSleepTriggerStatus)
            .isEqualTo(SettingsPreferences().wearableSleepTriggerStatus)
    }

    private fun createSettingsRepository(): SettingsPreferencesRepository {
        val dataStoreFile = File.createTempFile("wearable-boot-recovery", ".preferences_pb").apply {
            delete()
            deleteOnExit()
        }
        val dataStore = PreferenceDataStoreFactory.create(
            scope = TestScope(UnconfinedTestDispatcher()),
            produceFile = { dataStoreFile }
        )
        return SettingsPreferencesRepository(dataStore, FakeSecretTextCipher)
    }

    private fun mockContext(): Context {
        val context = mockk<Context>()
        every { context.applicationContext } returns context
        return context
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

    private object FakeSecretTextCipher : SecretTextCipher {
        override fun encrypt(plainText: String): String = "enc:$plainText"
        override fun decrypt(cipherText: String): String? = cipherText.removePrefix("enc:")
    }
}

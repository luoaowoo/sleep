package com.sleep.snore.recording

import com.google.common.truth.Truth.assertThat
import com.sleep.snore.data.db.entity.SleepRecordEntity
import com.sleep.snore.data.db.entity.SnoreEventEntity
import com.sleep.snore.data.model.Severity
import com.sleep.snore.data.preferences.SettingsPreferencesRepository
import com.sleep.snore.data.repository.SleepRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ActiveRecordingFinalizerTest {

    @Test
    fun finalizeIfActive_updatesActiveRecordAndClearsTriggerSource() = runTest {
        val repository = mockk<SleepRepository>()
        val settingsRepository = mockk<SettingsPreferencesRepository>(relaxed = true)
        val finalizer = ActiveRecordingFinalizer(repository, settingsRepository)
        val activeRecord = activeRecord()
        val updatedRecord = slot<SleepRecordEntity>()
        coEvery { repository.getActiveRecordingRecord() } returns activeRecord
        coEvery { settingsRepository.getActiveRecordingTriggerSource() } returns "health_connect_sleep"
        coEvery { repository.getEventsSnapshotByRecordId(activeRecord.id) } returns listOf(
            event(start = 1_000L, duration = 2_000),
            event(start = 15_000L, duration = 3_000)
        )
        coEvery { repository.getAllRecords() } returns flowOf(emptyList())
        coEvery { repository.updateRecord(capture(updatedRecord)) } returns Unit

        val finalized = finalizer.finalizeIfActive(
            expectedTriggerSource = "health_connect_sleep",
            endTimeMillis = 60_000L
        )

        assertThat(finalized).isTrue()
        assertThat(updatedRecord.captured.id).isEqualTo(activeRecord.id)
        assertThat(updatedRecord.captured.endTime).isEqualTo(60_000L)
        assertThat(updatedRecord.captured.snoreEventCount).isEqualTo(2)
        assertThat(updatedRecord.captured.snoreDurationMin).isEqualTo(1)
        assertThat(updatedRecord.captured.sleepDurationMin).isEqualTo(1)
        assertThat(updatedRecord.captured.aiEvaluation.trim().length).isGreaterThan(0)
        coVerify { settingsRepository.clearActiveRecordingTriggerSource() }
    }

    @Test
    fun finalizeIfActive_skipsWhenTriggerSourceDoesNotMatch() = runTest {
        val repository = mockk<SleepRepository>()
        val settingsRepository = mockk<SettingsPreferencesRepository>()
        val finalizer = ActiveRecordingFinalizer(repository, settingsRepository)
        coEvery { repository.getActiveRecordingRecord() } returns activeRecord()
        coEvery { settingsRepository.getActiveRecordingTriggerSource() } returns "manual"

        val finalized = finalizer.finalizeIfActive(
            expectedTriggerSource = "health_connect_sleep",
            endTimeMillis = 60_000L
        )

        assertThat(finalized).isFalse()
        coVerify(exactly = 0) { repository.updateRecord(any()) }
        coVerify(exactly = 0) { settingsRepository.clearActiveRecordingTriggerSource() }
    }

    @Test
    fun finalizeIfActive_clampsEndTimeAfterRecordStart() = runTest {
        val repository = mockk<SleepRepository>()
        val settingsRepository = mockk<SettingsPreferencesRepository>(relaxed = true)
        val finalizer = ActiveRecordingFinalizer(repository, settingsRepository)
        val activeRecord = activeRecord(startTime = 10_000L)
        val updatedRecord = slot<SleepRecordEntity>()
        coEvery { repository.getActiveRecordingRecord() } returns activeRecord
        coEvery { settingsRepository.getActiveRecordingTriggerSource() } returns "health_connect_sleep"
        coEvery { repository.getEventsSnapshotByRecordId(activeRecord.id) } returns emptyList()
        coEvery { repository.getAllRecords() } returns flowOf(emptyList())
        coEvery { repository.updateRecord(capture(updatedRecord)) } returns Unit

        val finalized = finalizer.finalizeIfActive(
            expectedTriggerSource = "health_connect_sleep",
            endTimeMillis = 5_000L
        )

        assertThat(finalized).isTrue()
        assertThat(updatedRecord.captured.endTime).isEqualTo(10_001L)
    }

    private fun activeRecord(startTime: Long = 0L): SleepRecordEntity {
        return SleepRecordEntity(
            id = 42L,
            startTime = startTime,
            endTime = 0L,
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
            aiSummary = "recording",
            aiEvaluation = "",
            aiSuggestions = "[]",
            createdAt = startTime
        )
    }

    private fun event(start: Long, duration: Int): SnoreEventEntity {
        return SnoreEventEntity(
            recordId = 42L,
            startTimestamp = start,
            durationMs = duration,
            peakDb = 55f,
            avgDb = 50f,
            dominantFreq = 180f,
            snoreType = "SOFT_PALATE",
            audioFilePath = "",
            audioFileSizeBytes = 0L,
            aiTypeLabel = "snore"
        )
    }
}

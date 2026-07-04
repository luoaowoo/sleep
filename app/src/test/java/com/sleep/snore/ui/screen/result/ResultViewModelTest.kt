package com.sleep.snore.ui.screen.result

import com.google.common.truth.Truth.assertThat
import com.sleep.snore.data.db.entity.SleepRecordEntity
import com.sleep.snore.data.db.entity.SnoreEventEntity
import com.sleep.snore.data.repository.SleepRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ResultViewModelTest {

    private lateinit var repository: SleepRepository
    private lateinit var viewModel: ResultViewModel

    private val sampleRecord = SleepRecordEntity(
        id = 10L,
        startTime = 1000L,
        endTime = 2000L,
        sleepDurationMin = 10,
        snoreScore = 50,
        severity = "MILD",
        estAHI = 1.0f,
        snoreDurationMin = 1,
        snoreRatio = 0.1f,
        avgDb = 30f,
        maxDb = 40f,
        snoreEventCount = 5,
        apneaEventCount = 0,
        longestApneaSec = 0,
        snoreTypeDistribution = "{}",
        hourlyDistribution = "[]",
        aiSummary = "",
        aiEvaluation = "",
        aiSuggestions = "[]"
    )

    private val sampleEvents = listOf(
        SnoreEventEntity(
            id = 1L,
            recordId = 10L,
            startTimestamp = 1100L,
            durationMs = 500,
            peakDb = 40f,
            avgDb = 30f,
            dominantFreq = 200f,
            snoreType = "SOFT_PALATE",
            audioFilePath = "",
            audioFileSizeBytes = 0L,
            aiTypeLabel = ""
        )
    )

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repository = mockk()
        viewModel = ResultViewModel(repository)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadRecord updates record and events`() = runTest {
        coEvery { repository.getRecordById(10L) } returns sampleRecord
        every { repository.getEventsByRecordId(10L) } returns flowOf(sampleEvents)

        viewModel.loadRecord(10L)

        assertThat(viewModel.record.value).isEqualTo(sampleRecord)
        assertThat(viewModel.events.value).hasSize(1)
        assertThat(viewModel.events.value[0].recordId).isEqualTo(10L)
    }

    @Test
    fun `deleteCurrentRecord sets deleteCompleted true on success`() = runTest {
        coEvery { repository.getRecordById(10L) } returns sampleRecord
        every { repository.getEventsByRecordId(10L) } returns flowOf(sampleEvents)
        coEvery { repository.deleteRecordWithAudio(10L) } returns Unit

        viewModel.loadRecord(10L)
        viewModel.deleteCurrentRecord()

        assertThat(viewModel.deleteCompleted.value).isTrue()
    }
}

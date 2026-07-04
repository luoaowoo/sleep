package com.sleep.snore.ui.screen.home

import com.google.common.truth.Truth.assertThat
import com.sleep.snore.data.db.entity.SleepRecordEntity
import com.sleep.snore.data.preferences.SettingsPreferences
import com.sleep.snore.data.preferences.SettingsPreferencesRepository
import com.sleep.snore.data.remote.DeepSeekClient
import com.sleep.snore.data.repository.SleepRepository
import com.sleep.snore.domain.WeeklyReportGenerator
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private lateinit var repository: SleepRepository
    private lateinit var settingsRepository: SettingsPreferencesRepository
    private lateinit var viewModel: HomeViewModel

    private val sampleRecords = listOf(
        SleepRecordEntity(
            id = 1L,
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
    )

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repository = mockk()
        settingsRepository = mockk()
        every { repository.getRecentRecords(any()) } returns flowOf(sampleRecords)
        every { repository.getLatestRecordFlow() } returns flowOf(null)
        every { settingsRepository.settings } returns flowOf(SettingsPreferences())
        viewModel = HomeViewModel(
            repository = repository,
            settingsRepository = settingsRepository,
            weeklyReportGenerator = WeeklyReportGenerator(),
            deepSeekClient = mockk<DeepSeekClient>()
        )
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads recent records on init`() = runTest {
        val result = viewModel.recentRecords.first { it.isNotEmpty() }

        assertThat(result).hasSize(1)
        assertThat(result[0].id).isEqualTo(1L)
    }

    @Test
    fun `builds weekly report from recent records`() = runTest {
        val report = viewModel.weeklyReportState.first { it.report != null }.report

        assertThat(report).isNotNull()
        assertThat(report?.recordCount).isEqualTo(1)
        assertThat(report?.totalSnoreMinutes).isEqualTo(1)
    }
}

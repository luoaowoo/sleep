package com.sleep.snore.data.repository

import android.content.Context
import android.util.Log
import com.sleep.snore.data.db.dao.FactorLogDao
import com.sleep.snore.data.db.dao.SleepRecordDao
import com.sleep.snore.data.db.dao.SnoreEventDao
import com.sleep.snore.data.db.entity.SnoreEventEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import java.io.File
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class SleepRepositoryTest {

    private lateinit var context: Context
    private lateinit var sleepRecordDao: SleepRecordDao
    private lateinit var snoreEventDao: SnoreEventDao
    private lateinit var factorLogDao: FactorLogDao
    private lateinit var repository: SleepRepository
    private lateinit var filesDir: File

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.w(any(), any<String>()) } returns 0
        filesDir = createTempDir(prefix = "sleep-repository-test")
        context = mockk()
        sleepRecordDao = mockk(relaxed = true)
        snoreEventDao = mockk()
        factorLogDao = mockk(relaxed = true)
        every { context.filesDir } returns filesDir
        repository = SleepRepository(
            context = context,
            sleepRecordDao = sleepRecordDao,
            snoreEventDao = snoreEventDao,
            factorLogDao = factorLogDao
        )
    }

    @After
    fun teardown() {
        filesDir.deleteRecursively()
        unmockkStatic(Log::class)
    }

    @Test
    fun deleteRecordWithAudio_skipsOutsideAudioPathButDeletesRecord() = runTest {
        val outsideFile = File(filesDir.parentFile, "outside.wav")
        every { snoreEventDao.getByRecordId(10L) } returns flowOf(
            listOf(buildEvent(recordId = 10L, audioFilePath = outsideFile.absolutePath))
        )
        coEvery { factorLogDao.deleteByRecordId(10L) } returns Unit
        coEvery { sleepRecordDao.deleteById(10L) } returns Unit

        repository.deleteRecordWithAudio(10L)

        coVerify { factorLogDao.deleteByRecordId(10L) }
        coVerify { sleepRecordDao.deleteById(10L) }
    }

    private fun buildEvent(recordId: Long, audioFilePath: String): SnoreEventEntity = SnoreEventEntity(
        recordId = recordId,
        startTimestamp = 1_000L,
        durationMs = 1_000,
        peakDb = 40f,
        avgDb = 30f,
        dominantFreq = 180f,
        snoreType = "UNKNOWN",
        audioFilePath = audioFilePath,
        audioFileSizeBytes = 0L,
        aiTypeLabel = "未知"
    )
}

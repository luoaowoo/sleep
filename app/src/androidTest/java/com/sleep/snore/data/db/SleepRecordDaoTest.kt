package com.sleep.snore.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sleep.snore.data.db.dao.SleepRecordDao
import com.sleep.snore.data.db.dao.SnoreEventDao
import com.sleep.snore.data.db.entity.SleepRecordEntity
import com.sleep.snore.data.db.entity.SnoreEventEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SleepRecordDaoTest {

    private lateinit var database: SleepDatabase
    private lateinit var sleepRecordDao: SleepRecordDao
    private lateinit var snoreEventDao: SnoreEventDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            SleepDatabase::class.java
        ).allowMainThreadQueries().build()
        sleepRecordDao = database.sleepRecordDao()
        snoreEventDao = database.snoreEventDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun getActiveRecordingRecord_returnsActiveRecord() = runTest {
        val activeRecord = buildRecord(
            startTime = 1_000L,
            endTime = 1_000L,
            aiEvaluation = ""
        )
        val endedRecord = buildRecord(
            startTime = 2_000L,
            endTime = 3_000L,
            aiEvaluation = "evaluated"
        )
        sleepRecordDao.insert(endedRecord)
        val activeId = sleepRecordDao.insert(activeRecord)

        val result = sleepRecordDao.getActiveRecordingRecord()

        assertNotNull(result)
        assertEquals(activeId, result!!.id)
        assertEquals(1_000L, result.startTime)
    }

    @Test
    fun getSnapshotByRecordId_returnsEventsForRecord() = runTest {
        val targetRecordId = sleepRecordDao.insert(buildRecord(startTime = 1_000L))
        val otherRecordId = sleepRecordDao.insert(buildRecord(startTime = 2_000L))

        snoreEventDao.insert(buildEvent(targetRecordId, startTimestamp = 1_100L))
        snoreEventDao.insert(buildEvent(targetRecordId, startTimestamp = 1_200L))
        snoreEventDao.insert(buildEvent(targetRecordId, startTimestamp = 1_300L))
        snoreEventDao.insert(buildEvent(otherRecordId, startTimestamp = 2_100L))

        val events = snoreEventDao.getSnapshotByRecordId(targetRecordId)

        assertEquals(3, events.size)
        events.forEach { event ->
            assertEquals(targetRecordId, event.recordId)
        }
    }

    @Test
    fun deleteOlderThan_deletesOldRecordAndCascadesEvents() = runTest {
        val threshold = 5_000L
        val oldRecordId = sleepRecordDao.insert(
            buildRecord(startTime = 1_000L, createdAt = 1_000L)
        )
        val newRecordId = sleepRecordDao.insert(
            buildRecord(startTime = 2_000L, createdAt = 10_000L)
        )
        snoreEventDao.insert(buildEvent(oldRecordId, startTimestamp = 1_100L))
        snoreEventDao.insert(buildEvent(oldRecordId, startTimestamp = 1_200L))
        snoreEventDao.insert(buildEvent(newRecordId, startTimestamp = 2_100L))

        sleepRecordDao.deleteOlderThan(threshold)

        assertNull(sleepRecordDao.getById(oldRecordId))
        assertNotNull(sleepRecordDao.getById(newRecordId))
        assertEquals(0, snoreEventDao.countByRecordId(oldRecordId))
        assertEquals(1, snoreEventDao.countByRecordId(newRecordId))
    }

    private fun buildRecord(
        startTime: Long,
        endTime: Long = startTime,
        aiEvaluation: String = "",
        createdAt: Long = 0L
    ): SleepRecordEntity = SleepRecordEntity(
        startTime = startTime,
        endTime = endTime,
        sleepDurationMin = 0,
        snoreScore = 0,
        severity = "NONE",
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
        aiEvaluation = aiEvaluation,
        aiSuggestions = "[]",
        isFavorited = false,
        createdAt = createdAt
    )

    private fun buildEvent(
        recordId: Long,
        startTimestamp: Long
    ): SnoreEventEntity = SnoreEventEntity(
        recordId = recordId,
        startTimestamp = startTimestamp,
        durationMs = 0,
        peakDb = 0f,
        avgDb = 0f,
        dominantFreq = 0f,
        snoreType = "NORMAL",
        audioFilePath = "",
        audioFileSizeBytes = 0L,
        aiTypeLabel = "",
        isFavorited = false
    )
}

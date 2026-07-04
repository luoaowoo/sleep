package com.sleep.snore.data.repository

import com.sleep.snore.data.db.dao.FactorLogDao
import com.sleep.snore.data.db.dao.SleepRecordDao
import com.sleep.snore.data.db.dao.SnoreEventDao
import com.sleep.snore.data.db.entity.FactorLogEntity
import com.sleep.snore.data.db.entity.SleepRecordEntity
import com.sleep.snore.data.db.entity.SnoreEventEntity
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SleepRepository @Inject constructor(
    private val sleepRecordDao: SleepRecordDao,
    private val snoreEventDao: SnoreEventDao,
    private val factorLogDao: FactorLogDao
) {
    // ===== SleepRecord =====
    suspend fun insertRecord(record: SleepRecordEntity): Long = sleepRecordDao.insert(record)
    suspend fun updateRecord(record: SleepRecordEntity) = sleepRecordDao.update(record)
    fun getAllRecords(): Flow<List<SleepRecordEntity>> = sleepRecordDao.getAllRecords()
    suspend fun getRecordById(id: Long): SleepRecordEntity? = sleepRecordDao.getById(id)
    suspend fun getActiveRecordingRecord(): SleepRecordEntity? = sleepRecordDao.getActiveRecordingRecord()
    suspend fun getLatestRecord(): SleepRecordEntity? = sleepRecordDao.getLatest()
    fun getLatestRecordFlow(): Flow<SleepRecordEntity?> = sleepRecordDao.getLatestFlow()
    fun getRecentRecords(limit: Int): Flow<List<SleepRecordEntity>> = sleepRecordDao.getRecent(limit)
    fun getRecordsSince(since: Long): Flow<List<SleepRecordEntity>> = sleepRecordDao.getRecordsSince(since)
    fun getRecordsBetween(start: Long, end: Long): Flow<List<SleepRecordEntity>> = sleepRecordDao.getRecordsBetween(start, end)
    suspend fun deleteRecord(id: Long) = sleepRecordDao.deleteById(id)
    suspend fun deleteRecordWithAudio(id: Long) {
        val audioPaths = getEventsByRecordId(id).first().mapNotNull { event ->
            event.audioFilePath.takeIf { it.isNotBlank() }
        }
        factorLogDao.deleteByRecordId(id)
        sleepRecordDao.deleteById(id)
        deleteAudioFiles(audioPaths)
    }
    suspend fun deleteOldRecords(before: Long) = sleepRecordDao.deleteOlderThan(before)
    suspend fun deleteOldRecordsWithAudio(before: Long) {
        val recordIds = sleepRecordDao.getIdsCreatedBefore(before)
        if (recordIds.isEmpty()) return
        val audioPaths = snoreEventDao.getAudioPathsByRecordIds(recordIds)
        factorLogDao.deleteByRecordIds(recordIds)
        sleepRecordDao.deleteOlderThan(before)
        deleteAudioFiles(audioPaths)
    }

    // ===== SnoreEvent =====
    suspend fun insertEvent(event: SnoreEventEntity): Long = snoreEventDao.insert(event)
    suspend fun insertEvents(events: List<SnoreEventEntity>) = snoreEventDao.insertAll(events)
    fun getAllEvents(): Flow<List<SnoreEventEntity>> = snoreEventDao.getAll()
    fun getEventsByRecordId(recordId: Long): Flow<List<SnoreEventEntity>> = snoreEventDao.getByRecordId(recordId)
    suspend fun getEventsSnapshotByRecordId(recordId: Long): List<SnoreEventEntity> =
        snoreEventDao.getSnapshotByRecordId(recordId)
    suspend fun getEventsBefore(before: Long): List<SnoreEventEntity> = snoreEventDao.getBefore(before)
    suspend fun countEvents(recordId: Long): Int = snoreEventDao.countByRecordId(recordId)
    fun getFavoriteEvents(): Flow<List<SnoreEventEntity>> = snoreEventDao.getFavorites()
    suspend fun setEventFavorited(id: Long, favorited: Boolean) = snoreEventDao.setFavorited(id, favorited)

    // ===== FactorLog =====
    suspend fun insertFactorLog(log: FactorLogEntity) = factorLogDao.insert(log)
    suspend fun getFactorLogByDate(date: String): FactorLogEntity? = factorLogDao.getByDate(date)
    fun getAllFactorLogs(): Flow<List<FactorLogEntity>> = factorLogDao.getAll()

    private fun deleteAudioFiles(paths: List<String>) {
        paths.forEach { path ->
            runCatching { File(path).delete() }
        }
    }
}

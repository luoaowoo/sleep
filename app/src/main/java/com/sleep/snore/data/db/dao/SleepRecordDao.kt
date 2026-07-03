package com.sleep.snore.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sleep.snore.data.db.entity.SleepRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SleepRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: SleepRecordEntity): Long

    @Update
    suspend fun update(record: SleepRecordEntity)

    @Query("SELECT * FROM sleep_records ORDER BY start_time DESC")
    fun getAllRecords(): Flow<List<SleepRecordEntity>>

    @Query("SELECT * FROM sleep_records WHERE id = :id")
    suspend fun getById(id: Long): SleepRecordEntity?

    @Query("SELECT * FROM sleep_records WHERE end_time = start_time AND ai_evaluation = '' ORDER BY start_time DESC LIMIT 1")
    suspend fun getActiveRecordingRecord(): SleepRecordEntity?

    @Query("SELECT * FROM sleep_records ORDER BY start_time DESC LIMIT 1")
    suspend fun getLatest(): SleepRecordEntity?

    @Query("SELECT * FROM sleep_records ORDER BY start_time DESC LIMIT 1")
    fun getLatestFlow(): Flow<SleepRecordEntity?>

    @Query("SELECT * FROM sleep_records ORDER BY start_time DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<SleepRecordEntity>>

    @Query("SELECT * FROM sleep_records WHERE start_time >= :since ORDER BY start_time DESC")
    fun getRecordsSince(since: Long): Flow<List<SleepRecordEntity>>

    @Query("SELECT * FROM sleep_records WHERE start_time BETWEEN :start AND :end ORDER BY start_time DESC")
    fun getRecordsBetween(start: Long, end: Long): Flow<List<SleepRecordEntity>>

    @Query("DELETE FROM sleep_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT id FROM sleep_records WHERE created_at < :before")
    suspend fun getIdsCreatedBefore(before: Long): List<Long>

    @Query("DELETE FROM sleep_records WHERE created_at < :before")
    suspend fun deleteOlderThan(before: Long)
}

package com.sleep.snore.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sleep.snore.data.db.entity.SnoreEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SnoreEventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: SnoreEventEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<SnoreEventEntity>)

    @Query("SELECT * FROM snore_events WHERE record_id = :recordId ORDER BY start_timestamp ASC")
    fun getByRecordId(recordId: Long): Flow<List<SnoreEventEntity>>

    @Query("SELECT * FROM snore_events ORDER BY start_timestamp DESC")
    fun getAll(): Flow<List<SnoreEventEntity>>

    @Query("SELECT * FROM snore_events WHERE start_timestamp < :before ORDER BY start_timestamp ASC")
    suspend fun getBefore(before: Long): List<SnoreEventEntity>

    @Query("SELECT audio_file_path FROM snore_events WHERE record_id IN (:recordIds) AND audio_file_path != ''")
    suspend fun getAudioPathsByRecordIds(recordIds: List<Long>): List<String>

    @Query("SELECT COUNT(*) FROM snore_events WHERE record_id = :recordId")
    suspend fun countByRecordId(recordId: Long): Int

    @Query("SELECT * FROM snore_events WHERE is_favorited = 1 ORDER BY start_timestamp DESC")
    fun getFavorites(): Flow<List<SnoreEventEntity>>

    @Query("UPDATE snore_events SET is_favorited = :favorited WHERE id = :id")
    suspend fun setFavorited(id: Long, favorited: Boolean)

    @Query("DELETE FROM snore_events WHERE record_id = :recordId")
    suspend fun deleteByRecordId(recordId: Long)
}

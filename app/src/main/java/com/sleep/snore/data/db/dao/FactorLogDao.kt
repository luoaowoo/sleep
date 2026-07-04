package com.sleep.snore.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sleep.snore.data.db.entity.FactorLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FactorLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: FactorLogEntity)

    @Query("SELECT * FROM factor_logs WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: String): FactorLogEntity?

    @Query("SELECT * FROM factor_logs WHERE record_id = :recordId")
    suspend fun getByRecordId(recordId: Long): FactorLogEntity?

    @Query("SELECT * FROM factor_logs ORDER BY date DESC")
    fun getAll(): Flow<List<FactorLogEntity>>

    @Query("SELECT * FROM factor_logs WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getBetween(startDate: String, endDate: String): Flow<List<FactorLogEntity>>

    @Query("DELETE FROM factor_logs WHERE record_id = :recordId")
    suspend fun deleteByRecordId(recordId: Long)

    @Query("DELETE FROM factor_logs WHERE record_id IN (:recordIds)")
    suspend fun deleteByRecordIds(recordIds: List<Long>)
}

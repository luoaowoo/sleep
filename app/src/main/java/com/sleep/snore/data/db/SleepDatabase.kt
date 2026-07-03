package com.sleep.snore.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.sleep.snore.data.db.dao.FactorLogDao
import com.sleep.snore.data.db.dao.SleepRecordDao
import com.sleep.snore.data.db.dao.SnoreEventDao
import com.sleep.snore.data.db.entity.FactorLogEntity
import com.sleep.snore.data.db.entity.SleepRecordEntity
import com.sleep.snore.data.db.entity.SnoreEventEntity

@Database(
    entities = [
        SleepRecordEntity::class,
        SnoreEventEntity::class,
        FactorLogEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class SleepDatabase : RoomDatabase() {
    abstract fun sleepRecordDao(): SleepRecordDao
    abstract fun snoreEventDao(): SnoreEventDao
    abstract fun factorLogDao(): FactorLogDao
}

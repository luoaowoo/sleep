package com.sleep.snore.data.db

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sleep.snore.data.db.dao.FactorLogDao
import com.sleep.snore.data.db.dao.SleepRecordDao
import com.sleep.snore.data.db.dao.SnoreEventDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SleepDatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SleepDatabase {
        return Room.databaseBuilder(
            context,
            SleepDatabase::class.java,
            "sleep_snore.db"
        )
            .addMigrations(MIGRATION_1_2)
            .build()
    }

    @Provides
    fun provideSleepRecordDao(db: SleepDatabase): SleepRecordDao = db.sleepRecordDao()

    @Provides
    fun provideSnoreEventDao(db: SleepDatabase): SnoreEventDao = db.snoreEventDao()

    @Provides
    fun provideFactorLogDao(db: SleepDatabase): FactorLogDao = db.factorLogDao()

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_sleep_records_start_time` ON `sleep_records` (`start_time`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_sleep_records_created_at` ON `sleep_records` (`created_at`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_snore_events_record_id_start_timestamp` ON `snore_events` (`record_id`, `start_timestamp`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_snore_events_start_timestamp` ON `snore_events` (`start_timestamp`)")
        }
    }
}

package com.sleep.snore.data.db

import android.content.Context
import androidx.room.Room
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
        ).build()
    }

    @Provides
    fun provideSleepRecordDao(db: SleepDatabase): SleepRecordDao = db.sleepRecordDao()

    @Provides
    fun provideSnoreEventDao(db: SleepDatabase): SnoreEventDao = db.snoreEventDao()

    @Provides
    fun provideFactorLogDao(db: SleepDatabase): FactorLogDao = db.factorLogDao()
}

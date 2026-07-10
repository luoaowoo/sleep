package com.sleep.snore.sleeptrigger

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SleepTriggerSourceModule {

    @Binds
    @Singleton
    abstract fun bindSleepTriggerSource(source: HealthConnectSleepTriggerSource): SleepTriggerSource

    @Binds
    @Singleton
    abstract fun bindHealthConnectSleepSessionPoller(
        source: HealthConnectSleepTriggerSource
    ): HealthConnectSleepSessionPoller
}

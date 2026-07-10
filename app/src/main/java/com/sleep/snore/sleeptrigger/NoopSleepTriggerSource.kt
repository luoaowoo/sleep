package com.sleep.snore.sleeptrigger

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

@Singleton
class NoopSleepTriggerSource @Inject constructor() : SleepTriggerSource {
    override val events: Flow<SleepTriggerEvent> = emptyFlow()
}

package com.sleep.snore.sleeptrigger

import kotlinx.coroutines.flow.Flow

interface SleepTriggerSource {
    val events: Flow<SleepTriggerEvent>
}

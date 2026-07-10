package com.sleep.snore.sleeptrigger

sealed interface SleepTriggerEvent {
    val source: String
    val timestamp: Long

    data class SleepStarted(
        override val source: String,
        override val timestamp: Long,
        val confidence: Float
    ) : SleepTriggerEvent

    data class SleepEnded(
        override val source: String,
        override val timestamp: Long
    ) : SleepTriggerEvent
}

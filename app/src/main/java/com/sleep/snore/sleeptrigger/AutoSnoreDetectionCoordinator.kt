package com.sleep.snore.sleeptrigger

import com.sleep.snore.recording.RecordingController
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutoSnoreDetectionCoordinator @Inject constructor(
    private val recordingController: RecordingController
) {

    suspend fun handleEvent(event: SleepTriggerEvent, enabled: Boolean, stopOnSleepEnd: Boolean): Boolean {
        if (!enabled) return false
        return when (event) {
            is SleepTriggerEvent.SleepStarted -> {
                if (event.confidence < MIN_SLEEP_CONFIDENCE) {
                    false
                } else {
                    recordingController.startFromSleepTrigger(event.source)
                }
            }
            is SleepTriggerEvent.SleepEnded -> {
                stopOnSleepEnd && recordingController.stopFromSleepTrigger(event.source)
            }
        }
    }

    private companion object {
        const val MIN_SLEEP_CONFIDENCE = 0.65f
    }
}

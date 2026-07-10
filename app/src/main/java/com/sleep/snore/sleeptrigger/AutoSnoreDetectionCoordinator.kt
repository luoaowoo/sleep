package com.sleep.snore.sleeptrigger

import com.sleep.snore.recording.RecordingController
import com.sleep.snore.recording.RecordingStartResult
import javax.inject.Inject
import javax.inject.Singleton

sealed interface AutoSnoreDetectionResult {
    val handled: Boolean
    val shouldRememberEvent: Boolean
    val statusText: String?

    data class SleepStart(val recordingStartResult: RecordingStartResult) : AutoSnoreDetectionResult {
        override val handled: Boolean = recordingStartResult.confirmed
        override val shouldRememberEvent: Boolean = recordingStartResult.confirmed
        override val statusText: String = recordingStartResult.statusText
    }

    data class SleepEnd(
        override val handled: Boolean,
        private val stopRequested: Boolean = true
    ) : AutoSnoreDetectionResult {
        override val shouldRememberEvent: Boolean = handled
        override val statusText: String = if (handled) {
            "检测到睡眠结束，已请求停止鼾声检测"
        } else if (!stopRequested) {
            "检测到睡眠结束，已按设置保持鼾声检测"
        } else {
            "检测到睡眠结束，但未能停止鼾声检测；将继续重试"
        }
    }

    data object Ignored : AutoSnoreDetectionResult {
        override val handled: Boolean = false
        override val shouldRememberEvent: Boolean = false
        override val statusText: String? = null
    }
}

@Singleton
class AutoSnoreDetectionCoordinator @Inject constructor(
    private val recordingController: RecordingController
) {

    suspend fun handleEvent(
        event: SleepTriggerEvent,
        enabled: Boolean,
        stopOnSleepEnd: Boolean
    ): AutoSnoreDetectionResult {
        if (!enabled) return AutoSnoreDetectionResult.Ignored
        return when (event) {
            is SleepTriggerEvent.SleepStarted -> {
                if (event.confidence < MIN_SLEEP_CONFIDENCE) {
                    AutoSnoreDetectionResult.Ignored
                } else {
                    AutoSnoreDetectionResult.SleepStart(
                        recordingController.startFromSleepTrigger(event.source)
                    )
                }
            }
            is SleepTriggerEvent.SleepEnded -> {
                if (!stopOnSleepEnd) {
                    return AutoSnoreDetectionResult.SleepEnd(handled = false, stopRequested = false)
                }
                AutoSnoreDetectionResult.SleepEnd(
                    recordingController.stopFromSleepTrigger(event.source)
                )
            }
        }
    }

    private companion object {
        const val MIN_SLEEP_CONFIDENCE = 0.65f
    }
}

package com.sleep.snore.sleeptrigger

import com.sleep.snore.data.preferences.SettingsPreferences
import com.sleep.snore.data.preferences.SettingsPreferencesRepository
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

sealed interface RecordingSleepEndFallbackResult {
    data object ContinuePolling : RecordingSleepEndFallbackResult
    data object StopPolling : RecordingSleepEndFallbackResult
    data class StopRecording(
        val statusText: String,
        val eventKey: String,
        val endTimeMillis: Long
    ) : RecordingSleepEndFallbackResult
}

@Singleton
class RecordingSleepEndFallbackPoller @Inject constructor(
    private val settingsRepository: SettingsPreferencesRepository,
    private val sleepSessionPoller: HealthConnectSleepSessionPoller
) {

    suspend fun pollOnce(sessionStartTimeMillis: Long): RecordingSleepEndFallbackResult {
        val settings = settingsRepository.settings.first()
        if (!shouldRunRecordingSleepEndFallback(settings)) {
            return RecordingSleepEndFallbackResult.StopPolling
        }

        val pollResult = runCatching {
            sleepSessionPoller.pollLatestSleepSession(
                now = Instant.now(),
                requireBackgroundRead = true,
                ignoreEventsBefore = sleepEventIgnoreEventsBefore(
                    settings = settings,
                    fallbackMillis = sessionStartTimeMillis
                )
            )
        }.getOrElse {
            settingsRepository.setWearableSleepTriggerStatus("录音服务检查睡眠结束失败，将继续重试")
            return RecordingSleepEndFallbackResult.ContinuePolling
        }

        if (pollResult is HealthConnectSleepTriggerSource.PollResult.DuplicateEvent) {
            val status = SLEEP_END_STOP_STATUS
            settingsRepository.setWearableSleepTriggerStatus(status)
            return RecordingSleepEndFallbackResult.StopRecording(
                statusText = status,
                eventKey = pollResult.eventKey,
                endTimeMillis = pollResult.observedSession.endTime.toEpochMilli()
            )
        }

        val emittedEvent = pollResult as? HealthConnectSleepTriggerSource.PollResult.EventEmitted
        val sleepEnded = emittedEvent?.event as? SleepTriggerEvent.SleepEnded
        if (sleepEnded == null) {
            if (pollResult !is HealthConnectSleepTriggerSource.PollResult.EventEmitted) {
                settingsRepository.setWearableSleepTriggerStatus(
                    "录音服务等待睡眠结束：${pollResult.toWearableSleepStatusText(requireBackgroundRead = false)}"
                )
            }
            return RecordingSleepEndFallbackResult.ContinuePolling
        }

        val status = SLEEP_END_STOP_STATUS
        settingsRepository.setWearableSleepTriggerStatus(status)
        return RecordingSleepEndFallbackResult.StopRecording(
            statusText = status,
            eventKey = emittedEvent.eventKey,
            endTimeMillis = sleepEnded.timestamp
        )
    }

    private companion object {
        const val SLEEP_END_STOP_STATUS = "检测到睡眠结束，录音服务正在停止鼾声检测"
    }
}

internal fun shouldRunRecordingSleepEndFallback(settings: SettingsPreferences): Boolean {
    return settings.wearableSleepTriggerEnabled &&
        settings.wearableStopOnSleepEndEnabled &&
        settings.activeRecordingTriggerSource == HealthConnectSleepTriggerSource.SOURCE
}

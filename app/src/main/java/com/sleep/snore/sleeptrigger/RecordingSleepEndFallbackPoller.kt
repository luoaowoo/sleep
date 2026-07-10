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
    data class StopRecording(val statusText: String) : RecordingSleepEndFallbackResult
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
            return RecordingSleepEndFallbackResult.ContinuePolling
        }

        val emittedEvent = pollResult as? HealthConnectSleepTriggerSource.PollResult.EventEmitted
        if (emittedEvent?.event !is SleepTriggerEvent.SleepEnded) {
            return RecordingSleepEndFallbackResult.ContinuePolling
        }

        settingsRepository.setLastWearableSleepEventKey(emittedEvent.eventKey)
        val status = "检测到睡眠结束，录音服务正在停止鼾声检测"
        settingsRepository.setWearableSleepTriggerStatus(status)
        return RecordingSleepEndFallbackResult.StopRecording(status)
    }
}

internal fun shouldRunRecordingSleepEndFallback(settings: SettingsPreferences): Boolean {
    return settings.wearableSleepTriggerEnabled &&
        settings.wearableStopOnSleepEndEnabled &&
        settings.activeRecordingTriggerSource == HealthConnectSleepTriggerSource.SOURCE
}

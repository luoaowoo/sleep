package com.sleep.snore.recording

import com.sleep.snore.data.db.entity.SleepRecordEntity
import com.sleep.snore.data.preferences.SettingsPreferencesRepository
import com.sleep.snore.sleeptrigger.HealthConnectSleepSessionPoller
import com.sleep.snore.sleeptrigger.HealthConnectSleepTriggerSource
import com.sleep.snore.sleeptrigger.SleepTriggerEvent
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

data class ResolvedWearableSleepEnd(
    val endTimeMillis: Long,
    val eventKey: String
)

@Singleton
class WearableSleepEndTimeResolver @Inject constructor(
    private val settingsRepository: SettingsPreferencesRepository,
    private val sleepSessionPoller: HealthConnectSleepSessionPoller
) {

    suspend fun resolve(activeRecord: SleepRecordEntity): ResolvedWearableSleepEnd? {
        val settings = settingsRepository.settings.first()
        if (settings.activeRecordingTriggerSource != HealthConnectSleepTriggerSource.SOURCE) {
            return null
        }
        val ignoreEventsBeforeMillis = settings.activeRecordingTriggerStartedAtMillis
            .takeIf { it > 0L }
            ?: activeRecord.startTime
        val pollResult = runCatching {
            sleepSessionPoller.pollLatestSleepSession(
                now = Instant.now(),
                requireBackgroundRead = true,
                ignoreEventsBefore = Instant.ofEpochMilli(ignoreEventsBeforeMillis)
            )
        }.getOrNull()
        val emittedEvent = pollResult as? HealthConnectSleepTriggerSource.PollResult.EventEmitted
            ?: return null
        val sleepEnded = emittedEvent.event as? SleepTriggerEvent.SleepEnded ?: return null
        return ResolvedWearableSleepEnd(
            endTimeMillis = sleepEnded.timestamp,
            eventKey = emittedEvent.eventKey
        )
    }
}

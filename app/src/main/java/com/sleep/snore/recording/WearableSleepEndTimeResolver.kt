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

sealed interface WearableSleepEndResolveResult {
    data class Resolved(val sleepEnd: ResolvedWearableSleepEnd) : WearableSleepEndResolveResult
    data object WaitingForSync : WearableSleepEndResolveResult
    data object PermissionMissing : WearableSleepEndResolveResult
    data object BackgroundReadUnavailable : WearableSleepEndResolveResult
    data object ReadFailed : WearableSleepEndResolveResult
    data object NotWearableRecording : WearableSleepEndResolveResult
}

@Singleton
class WearableSleepEndTimeResolver @Inject constructor(
    private val settingsRepository: SettingsPreferencesRepository,
    private val sleepSessionPoller: HealthConnectSleepSessionPoller
) {

    suspend fun resolve(activeRecord: SleepRecordEntity): ResolvedWearableSleepEnd? {
        return (resolveResult(activeRecord) as? WearableSleepEndResolveResult.Resolved)?.sleepEnd
    }

    suspend fun resolveResult(activeRecord: SleepRecordEntity): WearableSleepEndResolveResult {
        val settings = settingsRepository.settings.first()
        if (settings.activeRecordingTriggerSource != HealthConnectSleepTriggerSource.SOURCE) {
            return WearableSleepEndResolveResult.NotWearableRecording
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
        }.getOrElse {
            return WearableSleepEndResolveResult.ReadFailed
        }
        return when (pollResult) {
            HealthConnectSleepTriggerSource.PollResult.PermissionMissing -> {
                WearableSleepEndResolveResult.PermissionMissing
            }
            HealthConnectSleepTriggerSource.PollResult.BackgroundReadUnavailable -> {
                WearableSleepEndResolveResult.BackgroundReadUnavailable
            }
            HealthConnectSleepTriggerSource.PollResult.ReadFailed,
            HealthConnectSleepTriggerSource.PollResult.HealthConnectUnavailable -> {
                WearableSleepEndResolveResult.ReadFailed
            }
            is HealthConnectSleepTriggerSource.PollResult.EventEmitted -> {
                val sleepEnded = pollResult.event as? SleepTriggerEvent.SleepEnded
                    ?: return WearableSleepEndResolveResult.WaitingForSync
                WearableSleepEndResolveResult.Resolved(
                    ResolvedWearableSleepEnd(
                        endTimeMillis = sleepEnded.timestamp,
                        eventKey = pollResult.eventKey
                    )
                )
            }
            is HealthConnectSleepTriggerSource.PollResult.DuplicateEvent -> {
                if (!pollResult.eventKey.startsWith(SLEEP_ENDED_EVENT_KEY_PREFIX)) {
                    return WearableSleepEndResolveResult.WaitingForSync
                }
                WearableSleepEndResolveResult.Resolved(
                    ResolvedWearableSleepEnd(
                        endTimeMillis = pollResult.observedSession.endTime.toEpochMilli(),
                        eventKey = pollResult.eventKey
                    )
                )
            }
            else -> WearableSleepEndResolveResult.WaitingForSync
        }
    }

    private companion object {
        const val SLEEP_ENDED_EVENT_KEY_PREFIX = "SleepEnded:"
    }
}

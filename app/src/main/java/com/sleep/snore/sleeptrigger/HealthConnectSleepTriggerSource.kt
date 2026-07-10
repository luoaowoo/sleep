package com.sleep.snore.sleeptrigger

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.sleep.snore.data.preferences.SettingsPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

@Singleton
class HealthConnectSleepTriggerSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsPreferencesRepository
) : SleepTriggerSource {

    private val mutableEvents = MutableSharedFlow<SleepTriggerEvent>(extraBufferCapacity = 8)
    override val events: Flow<SleepTriggerEvent> = mutableEvents.asSharedFlow()

    suspend fun pollLatestSleepSession(
        now: Instant = Instant.now(),
        requireBackgroundRead: Boolean = true
    ): PollResult {
        if (HealthConnectClient.getSdkStatus(context) != HealthConnectClient.SDK_AVAILABLE) {
            return PollResult.HealthConnectUnavailable
        }
        val client = HealthConnectClient.getOrCreate(context)
        val grantedPermissions = client.permissionController.getGrantedPermissions()
        val requiredPermissions = if (requireBackgroundRead) {
            BACKGROUND_REQUIRED_PERMISSIONS
        } else {
            FOREGROUND_REQUIRED_PERMISSIONS
        }
        if (!grantedPermissions.containsAll(requiredPermissions)) {
            return PollResult.PermissionMissing
        }

        val latestSession = runCatching {
            client.readRecords(
                ReadRecordsRequest(
                    recordType = SleepSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(now.minus(36, ChronoUnit.HOURS), now)
                )
            ).records.maxByOrNull { it.startTime }
        }.getOrElse { throwable ->
            return if (throwable is SecurityException) {
                PollResult.PermissionMissing
            } else {
                PollResult.ReadFailed
            }
        } ?: return PollResult.NoRecentSleep

        val interpretedEvent = HealthConnectSleepEventInterpreter.interpret(
            session = SleepSessionSnapshot(
                startTime = latestSession.startTime,
                endTime = latestSession.endTime
            ),
            now = now
        ) ?: return PollResult.NoRecentSleep
        if (interpretedEvent.eventKey == settingsRepository.getLastWearableSleepEventKey()) {
            return PollResult.DuplicateEvent
        }
        mutableEvents.tryEmit(interpretedEvent.event)
        return PollResult.EventEmitted(interpretedEvent.event, interpretedEvent.eventKey)
    }

    sealed interface PollResult {
        data object HealthConnectUnavailable : PollResult
        data object PermissionMissing : PollResult
        data object NoRecentSleep : PollResult
        data object DuplicateEvent : PollResult
        data object ReadFailed : PollResult
        data class EventEmitted(val event: SleepTriggerEvent, val eventKey: String) : PollResult
    }

    companion object {
        const val SOURCE = "health_connect_sleep"
        const val HEALTH_CONNECT_CONFIDENCE = 0.8f
        val READ_SLEEP_PERMISSION: String = HealthPermission.getReadPermission(SleepSessionRecord::class)
        val BACKGROUND_READ_PERMISSION: String = HealthPermission.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND
        val FOREGROUND_REQUIRED_PERMISSIONS: Set<String> = setOf(READ_SLEEP_PERMISSION)
        val BACKGROUND_REQUIRED_PERMISSIONS: Set<String> = setOf(READ_SLEEP_PERMISSION, BACKGROUND_READ_PERMISSION)
        val REQUIRED_PERMISSIONS: Set<String> = BACKGROUND_REQUIRED_PERMISSIONS
    }
}

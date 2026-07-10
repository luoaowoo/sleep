package com.sleep.snore.sleeptrigger

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
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
    @ApplicationContext private val context: Context
) : SleepTriggerSource {

    private val mutableEvents = MutableSharedFlow<SleepTriggerEvent>(extraBufferCapacity = 8)
    override val events: Flow<SleepTriggerEvent> = mutableEvents.asSharedFlow()
    private var lastEmittedEventKey: String? = null

    suspend fun pollLatestSleepSession(now: Instant = Instant.now()): PollResult {
        if (HealthConnectClient.getSdkStatus(context) != HealthConnectClient.SDK_AVAILABLE) {
            return PollResult.HealthConnectUnavailable
        }
        val client = HealthConnectClient.getOrCreate(context)
        val grantedPermissions = client.permissionController.getGrantedPermissions()
        if (!grantedPermissions.containsAll(REQUIRED_PERMISSIONS)) {
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

        val event = if (latestSession.endTime.isAfter(now)) {
            SleepTriggerEvent.SleepStarted(
                source = SOURCE,
                timestamp = latestSession.startTime.toEpochMilli(),
                confidence = HEALTH_CONNECT_CONFIDENCE
            )
        } else {
            SleepTriggerEvent.SleepEnded(
                source = SOURCE,
                timestamp = latestSession.endTime.toEpochMilli()
            )
        }
        val eventKey = "${event::class.simpleName}:${event.timestamp}:${latestSession.startTime.toEpochMilli()}"
        if (eventKey == lastEmittedEventKey) return PollResult.DuplicateEvent
        lastEmittedEventKey = eventKey
        mutableEvents.tryEmit(event)
        return PollResult.EventEmitted(event)
    }

    sealed interface PollResult {
        data object HealthConnectUnavailable : PollResult
        data object PermissionMissing : PollResult
        data object NoRecentSleep : PollResult
        data object DuplicateEvent : PollResult
        data object ReadFailed : PollResult
        data class EventEmitted(val event: SleepTriggerEvent) : PollResult
    }

    companion object {
        const val SOURCE = "health_connect_sleep"
        const val HEALTH_CONNECT_CONFIDENCE = 0.8f
        val READ_SLEEP_PERMISSION: String = HealthPermission.getReadPermission(SleepSessionRecord::class)
        val BACKGROUND_READ_PERMISSION: String = HealthPermission.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND
        val REQUIRED_PERMISSIONS: Set<String> = setOf(READ_SLEEP_PERMISSION, BACKGROUND_READ_PERMISSION)
    }
}

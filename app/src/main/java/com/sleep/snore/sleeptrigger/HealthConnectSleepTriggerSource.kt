package com.sleep.snore.sleeptrigger

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.sleep.snore.data.preferences.SettingsPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

@Singleton
class HealthConnectSleepTriggerSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsPreferencesRepository
) : SleepTriggerSource, HealthConnectSleepSessionPoller {

    override val events: Flow<SleepTriggerEvent> = emptyFlow()

    override suspend fun pollLatestSleepSession(
        now: Instant,
        requireBackgroundRead: Boolean,
        ignoreEventsBefore: Instant?
    ): PollResult {
        if (HealthConnectClient.getSdkStatus(context) != HealthConnectClient.SDK_AVAILABLE) {
            return PollResult.HealthConnectUnavailable
        }
        val client = HealthConnectClient.getOrCreate(context)
        if (requireBackgroundRead && !client.isBackgroundReadAvailable()) {
            return PollResult.BackgroundReadUnavailable
        }
        val grantedPermissions = client.permissionController.getGrantedPermissions()
        val requiredPermissions = if (requireBackgroundRead) {
            BACKGROUND_REQUIRED_PERMISSIONS
        } else {
            FOREGROUND_REQUIRED_PERMISSIONS
        }
        if (!grantedPermissions.containsAll(requiredPermissions)) {
            return PollResult.PermissionMissing
        }

        val sessions = runCatching {
            val timeRangeFilter = TimeRangeFilter.between(now.minus(36, ChronoUnit.HOURS), now)
            readSleepSessions(
                client = client,
                timeRangeFilter = timeRangeFilter,
                dataOriginFilter = XIAOMI_SLEEP_DATA_ORIGINS
            ).ifEmpty {
                readSleepSessions(
                    client = client,
                    timeRangeFilter = timeRangeFilter,
                    dataOriginFilter = emptySet()
                )
            }
        }.getOrElse { throwable ->
            return if (throwable is SecurityException) {
                PollResult.PermissionMissing
            } else {
                PollResult.ReadFailed
            }
        }

        val actionableSession = selectXiaomiActionableSleepSession(
            sessions = sessions,
            now = now,
            ignoreEventsBefore = ignoreEventsBefore
        ) ?: return PollResult.NoRecentSleep
        if (actionableSession is XiaomiActionableSleepSelection.NoActionable) {
            return PollResult.NoActionableSleep(
                observedSession = actionableSession.observedSession,
                reason = actionableSession.reason
            )
        }
        actionableSession as XiaomiActionableSleepSelection.Actionable
        val interpretedEvent = actionableSession.event
        val interpretedSession = actionableSession.session
        if (interpretedEvent.eventKey == settingsRepository.getLastWearableSleepEventKey()) {
            return PollResult.DuplicateEvent(
                observedSession = interpretedSession,
                eventKey = interpretedEvent.eventKey
            )
        }
        return PollResult.EventEmitted(interpretedEvent.event, interpretedEvent.eventKey, interpretedSession)
    }

    sealed interface PollResult {
        val observedSession: SleepSessionSnapshot?
            get() = null

        data object HealthConnectUnavailable : PollResult
        data object BackgroundReadUnavailable : PollResult
        data object PermissionMissing : PollResult
        data object NoRecentSleep : PollResult
        data class NoActionableSleep(
            override val observedSession: SleepSessionSnapshot,
            val reason: NoActionableSleepReason
        ) : PollResult
        data class DuplicateEvent(
            override val observedSession: SleepSessionSnapshot,
            val eventKey: String
        ) : PollResult
        data object ReadFailed : PollResult
        data class EventEmitted(
            val event: SleepTriggerEvent,
            val eventKey: String,
            override val observedSession: SleepSessionSnapshot? = null
        ) : PollResult

        enum class NoActionableSleepReason {
            ONGOING,
            BEFORE_ACTIVE_RECORDING,
            NON_XIAOMI_SOURCE,
            INSUFFICIENT_ACTIVE_RECORDING_OVERLAP,
            SHORT_SLEEP_SESSION
        }
    }

    companion object {
        const val SOURCE = "health_connect_sleep"
        const val HEALTH_CONNECT_CONFIDENCE = 0.8f
        val XIAOMI_SLEEP_SOURCE_PACKAGE_NAMES: Set<String> = XiaomiSleepCompanionApps.packageNames
        val READ_SLEEP_PERMISSION: String = HealthPermission.getReadPermission(SleepSessionRecord::class)
        val BACKGROUND_READ_PERMISSION: String = HealthPermission.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND
        val FOREGROUND_REQUIRED_PERMISSIONS: Set<String> = setOf(READ_SLEEP_PERMISSION)
        val BACKGROUND_REQUIRED_PERMISSIONS: Set<String> = setOf(READ_SLEEP_PERMISSION, BACKGROUND_READ_PERMISSION)
        val REQUIRED_PERMISSIONS: Set<String> = BACKGROUND_REQUIRED_PERMISSIONS
        private val XIAOMI_SLEEP_DATA_ORIGINS: Set<DataOrigin> = XIAOMI_SLEEP_SOURCE_PACKAGE_NAMES
            .map { DataOrigin(it) }
            .toSet()
    }
}

internal sealed interface XiaomiActionableSleepSelection {
    data class Actionable(
        val session: SleepSessionSnapshot,
        val event: InterpretedSleepEvent
    ) : XiaomiActionableSleepSelection

    data class NoActionable(
        val observedSession: SleepSessionSnapshot,
        val reason: HealthConnectSleepTriggerSource.PollResult.NoActionableSleepReason
    ) : XiaomiActionableSleepSelection
}

internal fun selectXiaomiActionableSleepSession(
    sessions: List<SleepSessionSnapshot>,
    now: Instant,
    ignoreEventsBefore: Instant?
): XiaomiActionableSleepSelection? {
    val latestSession = HealthConnectSleepEventInterpreter.latestValidSession(
        sessions = sessions,
        now = now
    ) ?: return null
    val xiaomiSessions = sessions.filter {
        it.isKnownXiaomiSource && !it.startTime.isAfter(now) && !it.endTime.isBefore(it.startTime)
    }
    if (xiaomiSessions.isEmpty()) {
        return XiaomiActionableSleepSelection.NoActionable(
            observedSession = latestSession,
            reason = HealthConnectSleepTriggerSource.PollResult.NoActionableSleepReason.NON_XIAOMI_SOURCE
        )
    }
    val latestXiaomiSession = HealthConnectSleepEventInterpreter.latestValidSession(
        sessions = xiaomiSessions,
        now = now
    ) ?: latestSession
    val actionableSession = HealthConnectSleepEventInterpreter.latestActionableSession(
        sessions = xiaomiSessions,
        now = now,
        ignoreEventsBefore = ignoreEventsBefore
    ) ?: return XiaomiActionableSleepSelection.NoActionable(
        observedSession = latestXiaomiSession,
        reason = noActionableSleepReason(
            session = latestXiaomiSession,
            now = now,
            ignoreEventsBefore = ignoreEventsBefore
        )
    )
    return XiaomiActionableSleepSelection.Actionable(
        session = actionableSession.first,
        event = actionableSession.second
    )
}

private suspend fun readSleepSessions(
    client: HealthConnectClient,
    timeRangeFilter: TimeRangeFilter,
    dataOriginFilter: Set<DataOrigin>
): List<SleepSessionSnapshot> {
    return client.readRecords(
        ReadRecordsRequest(
            recordType = SleepSessionRecord::class,
            timeRangeFilter = timeRangeFilter,
            dataOriginFilter = dataOriginFilter
        )
    ).records.map { record ->
        SleepSessionSnapshot(
            startTime = record.startTime,
            endTime = record.endTime,
            dataOriginPackageName = record.metadata.dataOrigin.packageName
        )
    }
}

private fun noActionableSleepReason(
    session: SleepSessionSnapshot,
    now: Instant,
    ignoreEventsBefore: Instant?
): HealthConnectSleepTriggerSource.PollResult.NoActionableSleepReason {
    return when {
        session.endTime.isAfter(now) -> HealthConnectSleepTriggerSource.PollResult.NoActionableSleepReason.ONGOING
        ignoreEventsBefore != null && session.endTime.isBefore(ignoreEventsBefore) -> {
            HealthConnectSleepTriggerSource.PollResult.NoActionableSleepReason.BEFORE_ACTIVE_RECORDING
        }
        ignoreEventsBefore != null && HealthConnectSleepEventInterpreter.sessionDurationMillis(session) <
            HealthConnectSleepEventInterpreter.MINIMUM_AUTO_STOP_SLEEP_SESSION_DURATION_MILLIS -> {
            HealthConnectSleepTriggerSource.PollResult.NoActionableSleepReason.SHORT_SLEEP_SESSION
        }
        else -> HealthConnectSleepTriggerSource.PollResult.NoActionableSleepReason.INSUFFICIENT_ACTIVE_RECORDING_OVERLAP
    }
}

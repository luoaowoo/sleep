package com.sleep.snore.recording

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.sleep.snore.data.preferences.SettingsPreferencesRepository
import com.sleep.snore.service.SleepRecordingService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay

sealed interface RecordingStartResult {
    val confirmed: Boolean
    val requestSubmitted: Boolean
    val statusText: String

    data class Confirmed(override val statusText: String) : RecordingStartResult {
        override val confirmed: Boolean = true
        override val requestSubmitted: Boolean = true
    }

    data class Submitted(override val statusText: String) : RecordingStartResult {
        override val confirmed: Boolean = false
        override val requestSubmitted: Boolean = true
    }

    data class Failed(override val statusText: String) : RecordingStartResult {
        override val confirmed: Boolean = false
        override val requestSubmitted: Boolean = false
    }
}

interface RecordingController {
    suspend fun startFromSleepTrigger(source: String): RecordingStartResult
    suspend fun stopFromSleepTrigger(source: String, sleepEndTimeMillis: Long? = null): Boolean
    fun isRecordingActive(): Boolean
}

@Singleton
class AndroidRecordingController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsPreferencesRepository,
    private val recordingFailureNotifier: RecordingFailureNotifier
) : RecordingController {

    override suspend fun startFromSleepTrigger(source: String): RecordingStartResult {
        if (isRecordingActive()) {
            return if (settingsRepository.getActiveRecordingTriggerSource() == source) {
                RecordingStartResult.Confirmed("检测到睡眠，鼾声检测已在运行")
            } else {
                startIssue("检测到睡眠，但当前已有检测在运行；不会接管手动录音")
            }
        }
        val missingPermissionStatus = missingRequiredPermissionStatus()
        if (missingPermissionStatus != null) {
            return startIssue(missingPermissionStatus)
        }
        return runCatching {
            ContextCompat.startForegroundService(context, SleepRecordingService.startIntent(context, source))
            if (waitForConfirmedSource(source)) {
                RecordingStartResult.Confirmed("检测到睡眠，已开启鼾声检测")
            } else if (isRecordingActive()) {
                RecordingStartResult.Submitted("检测到睡眠，已请求开启鼾声检测，正在等待服务确认")
            } else {
                startIssue("检测到睡眠，但后台麦克风未能确认启动；请睡前开启前台检测")
            }
        }.getOrElse {
            val status = "后台启动麦克风服务失败，请睡前开启前台检测"
            startIssue(status)
        }
    }

    override suspend fun stopFromSleepTrigger(source: String, sleepEndTimeMillis: Long?): Boolean {
        if (settingsRepository.getActiveRecordingTriggerSource() != source) return false
        return runCatching {
            context.startService(SleepRecordingService.stopFromTriggerIntent(context, source, sleepEndTimeMillis))
            ActiveRecordingFinalizerWorker.enqueueFallback(context, source, sleepEndTimeMillis)
            true
        }.getOrElse {
            runCatching {
                ActiveRecordingFinalizerWorker.enqueueFallback(context, source, sleepEndTimeMillis)
                true
            }.getOrDefault(false)
        }
    }

    override fun isRecordingActive(): Boolean {
        return SleepRecordingService.recordingState.value.isActive
    }

    private fun missingRequiredPermissionStatus(): String? {
        val hasRecordAudio = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasRecordAudio) return "缺少麦克风权限，请先在睡前检测页授权"

        return null
    }

    private suspend fun startIssue(status: String): RecordingStartResult.Failed {
        settingsRepository.setWearableSleepTriggerStatus(status)
        recordingFailureNotifier.notifyWearableStartIssue(status)
        return RecordingStartResult.Failed(status)
    }

    private suspend fun waitForConfirmedSource(source: String): Boolean {
        repeat(START_CONFIRM_ATTEMPTS) {
            if (settingsRepository.getActiveRecordingTriggerSource() == source) return true
            delay(START_CONFIRM_INTERVAL_MS)
        }
        return settingsRepository.getActiveRecordingTriggerSource() == source
    }

    private companion object {
        const val START_CONFIRM_ATTEMPTS = 25
        const val START_CONFIRM_INTERVAL_MS = 200L
    }
}

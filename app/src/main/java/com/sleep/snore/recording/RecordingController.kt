package com.sleep.snore.recording

import android.content.Context
import androidx.core.content.ContextCompat
import com.sleep.snore.service.SleepRecordingService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface RecordingController {
    fun startFromSleepTrigger(source: String): Boolean
    fun stopFromSleepTrigger(source: String): Boolean
    fun isRecordingActive(): Boolean
}

@Singleton
class AndroidRecordingController @Inject constructor(
    @ApplicationContext private val context: Context
) : RecordingController {

    private var startedFromSleepTrigger = false

    override fun startFromSleepTrigger(source: String): Boolean {
        if (isRecordingActive()) return true
        return runCatching {
            ContextCompat.startForegroundService(context, SleepRecordingService.startIntent(context))
            startedFromSleepTrigger = true
            true
        }.getOrDefault(false)
    }

    override fun stopFromSleepTrigger(source: String): Boolean {
        if (!isRecordingActive() || !startedFromSleepTrigger) return false
        return runCatching {
            context.startService(SleepRecordingService.stopIntent(context))
            startedFromSleepTrigger = false
            true
        }.getOrDefault(false)
    }

    override fun isRecordingActive(): Boolean {
        return SleepRecordingService.recordingState.value.isActive
    }
}

package com.sleep.snore.recording

import android.content.Context
import androidx.core.content.ContextCompat
import com.sleep.snore.data.preferences.SettingsPreferencesRepository
import com.sleep.snore.service.SleepRecordingService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface RecordingController {
    suspend fun startFromSleepTrigger(source: String): Boolean
    suspend fun stopFromSleepTrigger(source: String): Boolean
    fun isRecordingActive(): Boolean
}

@Singleton
class AndroidRecordingController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsPreferencesRepository
) : RecordingController {

    override suspend fun startFromSleepTrigger(source: String): Boolean {
        if (isRecordingActive()) {
            return settingsRepository.getActiveRecordingTriggerSource() == source
        }
        return runCatching {
            ContextCompat.startForegroundService(context, SleepRecordingService.startIntent(context, source))
            true
        }.getOrDefault(false)
    }

    override suspend fun stopFromSleepTrigger(source: String): Boolean {
        if (settingsRepository.getActiveRecordingTriggerSource() != source) return false
        return runCatching {
            context.startService(SleepRecordingService.stopIntent(context))
            true
        }.getOrDefault(false)
    }

    override fun isRecordingActive(): Boolean {
        return SleepRecordingService.recordingState.value.isActive
    }
}

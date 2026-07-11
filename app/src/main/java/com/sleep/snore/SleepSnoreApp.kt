package com.sleep.snore

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import com.sleep.snore.data.preferences.SettingsPreferencesRepository
import com.sleep.snore.data.repository.SleepRepository
import com.sleep.snore.recording.AppVisibilityTracker
import com.sleep.snore.sleeptrigger.WearableRestartRecoveryEntryPoint
import com.sleep.snore.sleeptrigger.applyWearableStartupAction
import com.sleep.snore.sleeptrigger.bedtimeReminderStartupAction
import com.sleep.snore.sleeptrigger.recoverWearableRecordingAfterRestartIfNeeded
import com.sleep.snore.sleeptrigger.wearableSleepCheckStartupActions
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@HiltAndroidApp
class SleepSnoreApp : Application() {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var settingsPreferencesRepository: SettingsPreferencesRepository
    @Inject lateinit var sleepRepository: SleepRepository
    @Inject lateinit var appVisibilityTracker: AppVisibilityTracker

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        WorkManager.initialize(
            this,
            Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .build()
        )
        appVisibilityTracker.register(this)
        appScope.launch {
            recoverWearableRecordingAfterRestartIfNeeded(
                context = this@SleepSnoreApp,
                settingsRepository = settingsPreferencesRepository,
                sleepRepository = sleepRepository,
                entryPoint = WearableRestartRecoveryEntryPoint.AppStart
            )
        }
        appScope.launch {
            settingsPreferencesRepository.settings
                .map { it.wearableSleepTriggerEnabled }
                .distinctUntilChanged()
                .collect { enabled ->
                    wearableSleepCheckStartupActions(enabled).forEach { action ->
                        applyWearableStartupAction(this@SleepSnoreApp, action)
                    }
                }
        }
        appScope.launch {
            settingsPreferencesRepository.settings
                .map { it.bedtimeReminderEnabled to it.bedtimeReminderMinuteOfDay }
                .distinctUntilChanged()
                .collect { (enabled, minuteOfDay) ->
                    applyWearableStartupAction(
                        this@SleepSnoreApp,
                        bedtimeReminderStartupAction(enabled, minuteOfDay)
                    )
                }
        }
    }
}

package com.sleep.snore

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import com.sleep.snore.data.preferences.SettingsPreferencesRepository
import com.sleep.snore.sleeptrigger.HealthConnectSleepTriggerWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class SleepSnoreApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var settingsPreferencesRepository: SettingsPreferencesRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            settingsPreferencesRepository.settings.collect { settings ->
                if (settings.wearableSleepTriggerEnabled) {
                    HealthConnectSleepTriggerWorker.enqueue(this@SleepSnoreApp)
                } else {
                    WorkManager.getInstance(this@SleepSnoreApp)
                        .cancelUniqueWork(HealthConnectSleepTriggerWorker.WORK_NAME)
                }
            }
        }
    }
}

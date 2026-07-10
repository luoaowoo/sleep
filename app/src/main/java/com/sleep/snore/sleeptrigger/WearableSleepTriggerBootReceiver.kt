package com.sleep.snore.sleeptrigger

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sleep.snore.data.preferences.SettingsPreferencesRepository
import com.sleep.snore.data.repository.SleepRepository
import com.sleep.snore.recording.ActiveRecordingFinalizerWorker
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class WearableSleepTriggerBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in SUPPORTED_ACTIONS) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val appContext = context.applicationContext
                val entryPoint = EntryPointAccessors.fromApplication(
                    appContext,
                    WearableSleepTriggerBootEntryPoint::class.java
                )
                val settings = entryPoint.settingsPreferencesRepository().settings.first()
                if (settings.wearableSleepTriggerEnabled) {
                    HealthConnectSleepTriggerWorker.enqueue(appContext)
                    HealthConnectSleepTriggerWorker.enqueueNow(appContext)
                }
                val activeRecord = entryPoint.sleepRepository().getActiveRecordingRecord()
                if (shouldFinalizeWearableRecordingAfterBoot(settings, activeRecord)) {
                    ActiveRecordingFinalizerWorker.enqueueFallback(
                        context = appContext,
                        expectedSource = HealthConnectSleepTriggerSource.SOURCE
                    )
                    entryPoint.settingsPreferencesRepository().setWearableSleepTriggerStatus(
                        "检测到重启/更新后有未完成的手环鼾声记录，已安排兜底结算"
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WearableSleepTriggerBootEntryPoint {
        fun settingsPreferencesRepository(): SettingsPreferencesRepository
        fun sleepRepository(): SleepRepository
    }

    private companion object {
        val SUPPORTED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED
        )
    }
}

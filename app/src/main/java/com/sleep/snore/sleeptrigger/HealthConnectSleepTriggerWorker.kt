package com.sleep.snore.sleeptrigger

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.sleep.snore.data.preferences.SettingsPreferencesRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

@HiltWorker
class HealthConnectSleepTriggerWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val settingsRepository: SettingsPreferencesRepository,
    private val healthConnectSleepTriggerSource: HealthConnectSleepTriggerSource,
    private val coordinator: AutoSnoreDetectionCoordinator
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val settings = settingsRepository.settings.first()
        if (!settings.wearableSleepTriggerEnabled) return Result.success()

        val pollResult = runCatching {
            healthConnectSleepTriggerSource.pollLatestSleepSession()
        }.getOrElse { throwable ->
            settingsRepository.setWearableSleepTriggerStatus(
                "Health Connect 读取失败：${throwable.message.orEmpty()}".trimEnd('：')
            )
            return Result.success()
        }

        val status = if (pollResult is HealthConnectSleepTriggerSource.PollResult.EventEmitted) {
            handleEmittedEvent(pollResult, settings.wearableStopOnSleepEndEnabled)
        } else {
            pollResult.toStatusText()
        }
        settingsRepository.setWearableSleepTriggerStatus(status)
        return Result.success()
    }

    private suspend fun handleEmittedEvent(
        pollResult: HealthConnectSleepTriggerSource.PollResult.EventEmitted,
        stopOnSleepEnd: Boolean
    ): String {
        val handled = coordinator.handleEvent(
            event = pollResult.event,
            enabled = true,
            stopOnSleepEnd = stopOnSleepEnd
        )
        if (handled || pollResult.event is SleepTriggerEvent.SleepEnded) {
            settingsRepository.setLastWearableSleepEventKey(pollResult.eventKey)
        }
        return when (pollResult.event) {
            is SleepTriggerEvent.SleepStarted -> {
                if (handled) {
                    "检测到睡眠，已请求开启鼾声检测"
                } else {
                    "检测到睡眠，但后台麦克风启动失败；请睡前开启前台检测"
                }
            }
            is SleepTriggerEvent.SleepEnded -> {
                if (handled) {
                    "检测到睡眠结束，已请求停止鼾声检测"
                } else {
                    "检测到睡眠结束，无需停止"
                }
            }
        }
    }

    private fun HealthConnectSleepTriggerSource.PollResult.toStatusText(): String {
        return when (this) {
            HealthConnectSleepTriggerSource.PollResult.HealthConnectUnavailable -> "Health Connect 不可用"
            HealthConnectSleepTriggerSource.PollResult.PermissionMissing -> "缺少 Health Connect 睡眠/后台读取权限"
            HealthConnectSleepTriggerSource.PollResult.NoRecentSleep -> "未发现最近睡眠记录"
            HealthConnectSleepTriggerSource.PollResult.DuplicateEvent -> "最近睡眠记录已处理，等待新记录"
            HealthConnectSleepTriggerSource.PollResult.ReadFailed -> "Health Connect 读取失败"
            is HealthConnectSleepTriggerSource.PollResult.EventEmitted -> "已处理睡眠事件"
        }
    }

    companion object {
        const val WORK_NAME = "health_connect_sleep_trigger"

        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<HealthConnectSleepTriggerWorker>(
                repeatInterval = 15,
                repeatIntervalTimeUnit = TimeUnit.MINUTES
            ).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }
}

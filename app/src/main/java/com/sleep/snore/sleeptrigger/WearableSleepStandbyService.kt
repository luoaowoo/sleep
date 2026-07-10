package com.sleep.snore.sleeptrigger

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.sleep.snore.MainActivity
import com.sleep.snore.data.preferences.SettingsPreferencesRepository
import com.sleep.snore.recording.RecordingController
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class WearableSleepStandbyState(
    val isActive: Boolean = false,
    val startedAtMillis: Long = 0L,
    val lastCheckMillis: Long = 0L,
    val statusText: String = "睡前待命未开启"
)

@AndroidEntryPoint
class WearableSleepStandbyService : Service() {

    @Inject lateinit var settingsRepository: SettingsPreferencesRepository
    @Inject lateinit var healthConnectSleepTriggerSource: HealthConnectSleepTriggerSource
    @Inject lateinit var coordinator: AutoSnoreDetectionCoordinator
    @Inject lateinit var recordingController: RecordingController

    private var serviceJob = SupervisorJob()
    private var serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var standbyJob: Job? = null
    private var standbyStartedAtMillis: Long = 0L

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return when (intent?.action) {
            ACTION_STOP -> {
                stopPreStartedRecordingAsync()
                stopStandby("睡前待命已停止，前台鼾声检测也已请求停止")
                START_NOT_STICKY
            }
            ACTION_START -> {
                startStandbyIfNeeded()
                START_STICKY
            }
            null -> {
                startStandbyIfNeeded()
                START_STICKY
            }
            else -> START_NOT_STICKY
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTimeout(startId: Int, fgsType: Int) {
        val status = "睡前待命已达到系统前台服务时长限制，请重新打开应用后再开启"
        Log.w(TAG, "foreground service timed out: startId=$startId type=$fgsType")
        stopStandby(status)
    }

    override fun onDestroy() {
        standbyJob?.cancel()
        serviceScope.cancel()
        _standbyState.value = WearableSleepStandbyState()
        super.onDestroy()
    }

    private fun startStandbyIfNeeded() {
        ensureServiceScope()
        if (standbyJob?.isActive == true) {
            updateNotification(_standbyState.value.statusText)
            return
        }
        val initialStatus = "睡前待命已开启，正在等待手环/Health Connect 睡眠记录"
        standbyStartedAtMillis = System.currentTimeMillis()
        _standbyState.value = WearableSleepStandbyState(
            isActive = true,
            startedAtMillis = standbyStartedAtMillis,
            lastCheckMillis = standbyStartedAtMillis,
            statusText = initialStatus
        )
        if (!startForegroundNotification(initialStatus)) {
            _standbyState.value = WearableSleepStandbyState(statusText = "睡前待命启动失败")
            stopSelf()
            return
        }

        standbyJob = serviceScope.launch {
            settingsRepository.setWearableSleepTriggerEnabled(true)
            settingsRepository.setWearableSleepTriggerStatus(initialStatus)
            while (isActive) {
                if (System.currentTimeMillis() - standbyStartedAtMillis >= MAX_STANDBY_DURATION_MS) {
                    stopStandby("睡前待命已接近 Android 前台服务 6 小时限制，请重新打开应用后再开启")
                    return@launch
                }
                val shouldStop = pollOnce()
                if (shouldStop) {
                    return@launch
                }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private suspend fun pollOnce(): Boolean {
        val settings = settingsRepository.settings.first()
        if (!settings.wearableSleepTriggerEnabled) {
            stopStandby("手环自动检测已关闭，睡前待命停止")
            return true
        }

        val pollResult = runCatching {
            healthConnectSleepTriggerSource.pollLatestSleepSession(
                requireBackgroundRead = true,
                ignoreEventsBefore = sleepEventIgnoreEventsBefore(
                    settings = settings,
                    fallbackMillis = standbyStartedAtMillis
                )
            )
        }.getOrElse { throwable ->
            val status = "睡前待命检查失败：${throwable.message.orEmpty()}".trimEnd('：')
            persistStandbyStatus(status)
            updateStandbyStatus(status)
            return false
        }

        val handleResult = handleWearableSleepPollResult(
            pollResult = pollResult,
            stopOnSleepEnd = settings.wearableStopOnSleepEndEnabled,
            coordinator = coordinator,
            settingsRepository = settingsRepository,
            requireBackgroundRead = true
        )
        val status = handleResult.statusText
        persistStandbyStatus(status)
        updateStandbyStatus(status)
        return if (handleResult.emittedSleepEnd && handleResult.eventHandled) {
            stopStandby("已检测到睡眠结束并请求停止鼾声检测")
            true
        } else {
            false
        }
    }

    private fun stopStandby(status: String) {
        standbyJob?.cancel()
        standbyJob = null
        standbyStartedAtMillis = 0L
        _standbyState.value = WearableSleepStandbyState(statusText = status)
        persistStandbyMessageAsync(status)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private suspend fun persistStandbyStatus(status: String) {
        withContext(NonCancellable) {
            settingsRepository.setWearableSleepTriggerStatus(status)
        }
    }

    private fun persistStandbyMessageAsync(status: String) {
        CoroutineScope(Dispatchers.IO).launch {
            settingsRepository.setWearableSleepTriggerMessage(status)
        }
    }

    private fun stopPreStartedRecordingAsync() {
        CoroutineScope(Dispatchers.IO).launch {
            recordingController.stopFromSleepTrigger(HealthConnectSleepTriggerSource.SOURCE)
        }
    }

    private fun updateStandbyStatus(status: String) {
        val state = WearableSleepStandbyState(
            isActive = true,
            startedAtMillis = standbyStartedAtMillis,
            lastCheckMillis = System.currentTimeMillis(),
            statusText = status
        )
        _standbyState.value = state
        updateNotification(status)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "手环睡眠待命",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "睡前保持前台待命，检测 Health Connect 睡眠记录后启动鼾声检测"
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun startForegroundNotification(status: String): Boolean {
        val notification = buildNotification(status)
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            true
        }.getOrElse { error ->
            Log.e(TAG, "failed to start wearable standby foreground service", error)
            false
        }
    }

    private fun updateNotification(status: String) {
        val notification = buildNotification(status)
        runCatching {
            getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
        }.onFailure {
            Log.w(TAG, "failed to update wearable standby notification", it)
        }
    }

    private fun buildNotification(status: String): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            stopIntent(this),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setContentTitle("手环睡眠待命中")
            .setContentText(status)
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "停止待命", stopIntent)
            .build()
    }

    private fun ensureServiceScope() {
        if (serviceJob.isActive) return
        serviceJob = SupervisorJob()
        serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    }

    companion object {
        const val ACTION_START = "com.sleep.snore.action.START_WEARABLE_SLEEP_STANDBY"
        const val ACTION_STOP = "com.sleep.snore.action.STOP_WEARABLE_SLEEP_STANDBY"
        const val CHANNEL_ID = "wearable_sleep_standby"
        const val NOTIFICATION_ID = 1002
        private const val TAG = "WearableSleepStandby"
        private const val POLL_INTERVAL_MS = 5L * 60L * 1000L
        private const val MAX_STANDBY_DURATION_MS = 5L * 60L * 60L * 1000L + 30L * 60L * 1000L

        private val _standbyState = MutableStateFlow(WearableSleepStandbyState())
        val standbyState: StateFlow<WearableSleepStandbyState> = _standbyState.asStateFlow()

        fun startIntent(context: Context): Intent =
            Intent(context, WearableSleepStandbyService::class.java).setAction(ACTION_START)

        fun stopIntent(context: Context): Intent =
            Intent(context, WearableSleepStandbyService::class.java).setAction(ACTION_STOP)
    }
}

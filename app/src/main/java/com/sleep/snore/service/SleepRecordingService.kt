package com.sleep.snore.service

import android.Manifest
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.sleep.snore.MainActivity
import com.sleep.snore.R
import com.sleep.snore.audio.AudioEncoder
import com.sleep.snore.audio.SnoreDetector
import com.sleep.snore.data.db.entity.SnoreEventEntity
import com.sleep.snore.data.model.SnoreType
import com.sleep.snore.data.repository.SleepRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class SleepRecordingService : Service() {

    @Inject lateinit var repository: SleepRepository

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var wakeLock: PowerManager.WakeLock? = null
    private var snoreDetector: SnoreDetector? = null
    private val audioEncoder = AudioEncoder()

    private var snoreEventCount = 0
    private var currentSnoreType = SnoreType.UNKNOWN

    companion object {
        const val CHANNEL_ID = "sleep_recording"
        const val NOTIFICATION_ID = 1001
        private const val TAG = "SleepRecordingService"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundNotification()
        acquireWakeLock()
        startSnoreDetection()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopSnoreDetection()
        releaseWakeLock()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val channel = android.app.NotificationChannel(
            CHANNEL_ID, "鼾声录制", android.app.NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "睡眠鼾声录制服务通知"
        }
        val manager = getSystemService(android.app.NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun startForegroundNotification() {
        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("正在录制睡眠鼾声")
            .setContentText("鼾声片段: 0 个")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "SleepSnore:WakeLock"
        ).apply {
            acquire(30 * 60 * 1000L)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null
    }

    private fun startSnoreDetection() {
        val outputDir = File(filesDir, "snore_audio").apply { mkdirs() }

        snoreDetector = SnoreDetector(object : SnoreDetector.SnoreCallback {
            override fun onSnoreStarted(timestamp: Long, db: Double) {
                Log.d(TAG, "鼾声开始: $timestamp, ${String.format("%.1f", db)}dB")
            }

            override fun onSnoreEnded(startTimestamp: Long, durationMs: Long, pcmData: ByteArray, peakDb: Double) {
                snoreEventCount++
                updateNotification()

                val fileName = "snore_${startTimestamp}"
                val audioFile = audioEncoder.encodeToOpus(pcmData, outputDir, fileName)

                serviceScope.launch {
                    try {
                        val event = SnoreEventEntity(
                            recordId = 0,
                            startTimestamp = startTimestamp,
                            durationMs = durationMs.toInt(),
                            peakDb = peakDb.toFloat(),
                            avgDb = calculateAvgDb(pcmData).toFloat(),
                            dominantFreq = 0f,
                            snoreType = currentSnoreType.name,
                            audioFilePath = audioFile?.absolutePath ?: "",
                            audioFileSizeBytes = audioFile?.length() ?: 0,
                            aiTypeLabel = "${currentSnoreType.label}鼾声"
                        )
                        pendingEvents.add(event)
                    } catch (e: Exception) {
                        Log.e(TAG, "保存鼾声事件失败", e)
                    }
                }
            }
        })

        snoreDetector?.startListening()
    }

    private val pendingEvents = mutableListOf<SnoreEventEntity>()

    private fun stopSnoreDetection() {
        snoreDetector?.stopListening()
        snoreDetector = null
    }

    private fun updateNotification() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("正在录制睡眠鼾声")
            .setContentText("鼾声片段: $snoreEventCount 个")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()
        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification)
    }

    private fun calculateAvgDb(pcmData: ByteArray): Double {
        val detector = com.sleep.snore.audio.EnergyDetector()
        return detector.calculateDb(pcmData)
    }
}

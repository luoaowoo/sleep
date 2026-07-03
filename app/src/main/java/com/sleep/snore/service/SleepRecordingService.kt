package com.sleep.snore.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.sleep.snore.MainActivity
import com.sleep.snore.audio.AudioEncoder
import com.sleep.snore.audio.SnoreFeatureAnalyzer
import com.sleep.snore.audio.SnoreDetector
import com.sleep.snore.data.db.entity.SleepRecordEntity
import com.sleep.snore.data.db.entity.SnoreEventEntity
import com.sleep.snore.data.model.Severity
import com.sleep.snore.data.model.severityFromScore
import com.sleep.snore.data.preferences.SettingsPreferencesRepository
import com.sleep.snore.data.repository.SleepRepository
import com.sleep.snore.domain.SnoreEvaluator
import com.sleep.snore.domain.SnoreScoreCalculator
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import kotlin.math.max
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

data class RecordingRuntimeState(
    val isActive: Boolean = false,
    val startTime: Long = 0L,
    val eventCount: Int = 0
)

@AndroidEntryPoint
class SleepRecordingService : Service() {

    @Inject lateinit var repository: SleepRepository
    @Inject lateinit var preferencesRepository: SettingsPreferencesRepository

    private var serviceJob = SupervisorJob()
    private var serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private val audioEncoder = AudioEncoder()
    private val pendingEvents = mutableListOf<SnoreEventEntity>()
    private val encodingJobs = mutableListOf<Job>()
    private var startJob: Job? = null

    private var wakeLock: PowerManager.WakeLock? = null
    private var snoreDetector: SnoreDetector? = null
    private var currentRecordId: Long? = null
    private var sessionStartTime: Long = 0L
    private var isSessionActive = false
    private var isFinishingSession = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> finishSessionAndStop()
            else -> startSessionIfNeeded()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTaskRemoved(rootIntent: Intent?) {
        finishSessionAndStop()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        if (isSessionActive && !isFinishingSession) {
            isSessionActive = false
            stopSnoreDetection()
            runBlocking(Dispatchers.IO) {
                finalizeCurrentSession()
            }
        }
        _recordingState.value = RecordingRuntimeState()
        releaseWakeLock()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startSessionIfNeeded() {
        if (isSessionActive) return
        ensureServiceScope()
        isFinishingSession = false
        isSessionActive = true
        sessionStartTime = System.currentTimeMillis()
        _recordingState.value = RecordingRuntimeState(isActive = true, startTime = sessionStartTime)
        synchronized(pendingEvents) { pendingEvents.clear() }
        synchronized(encodingJobs) { encodingJobs.clear() }
        startForegroundNotification(0)
        acquireWakeLock()

        startJob = serviceScope.launch {
            try {
                val settings = preferencesRepository.settings.first()
                if (settings.autoCleanEnabled) cleanOldData()
                val recordId = repository.insertRecord(createEmptyRecord(sessionStartTime))
                currentRecordId = recordId
                if (!isSessionActive) return@launch
                startSnoreDetection(
                    recordId = recordId,
                    silenceThresholdDb = settings.silenceThresholdDb.toDouble()
                )
            } catch (e: Exception) {
                Log.e(TAG, "failed to start recording session", e)
                isSessionActive = false
                _recordingState.value = RecordingRuntimeState()
                releaseWakeLock()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun finishSessionAndStop() {
        if (!isSessionActive) {
            stopSelf()
            return
        }
        isFinishingSession = true
        isSessionActive = false
        stopSnoreDetection()

        serviceScope.launch {
            try {
                finalizeCurrentSession()
            } catch (e: Exception) {
                Log.e(TAG, "failed to finish recording session", e)
            } finally {
                releaseWakeLock()
                _recordingState.value = RecordingRuntimeState()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                serviceScope.cancel()
            }
        }
    }

    private suspend fun finalizeCurrentSession() {
        startJob?.join()
        val jobs = synchronized(encodingJobs) { encodingJobs.toList() }
        jobs.joinAll()
        val recordId = currentRecordId
        if (recordId != null) {
            val endTime = System.currentTimeMillis()
            val events = synchronized(pendingEvents) { pendingEvents.toList() }
            repository.updateRecord(buildFinalRecord(recordId, sessionStartTime, endTime, events))
        }
        val settings = preferencesRepository.settings.first()
        if (settings.autoCleanEnabled) cleanOldData()
    }

    private fun ensureServiceScope() {
        if (serviceJob.isActive) return
        serviceJob = SupervisorJob()
        serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, TEXT_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW).apply {
            description = TEXT_CHANNEL_DESCRIPTION
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun startForegroundNotification(eventCount: Int) {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, SleepRecordingService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(TEXT_RECORDING_TITLE)
            .setContentText(TEXT_SNORE_SEGMENTS.format(eventCount))
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, TEXT_STOP, stopIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    @SuppressLint("MissingPermission")
    private fun updateNotification() {
        val count = synchronized(pendingEvents) { pendingEvents.size }
        _recordingState.value = _recordingState.value.copy(eventCount = count)
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(TEXT_RECORDING_TITLE)
            .setContentText(TEXT_SNORE_SEGMENTS.format(count))
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()
        if (canPostNotifications()) {
            runCatching {
                NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification)
            }
        }
    }

    private fun canPostNotifications(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SleepSnore:RecordingWakeLock").apply {
            acquire(10 * 60 * 60 * 1000L)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    private fun startSnoreDetection(recordId: Long, silenceThresholdDb: Double) {
        val outputDir = File(filesDir, "snore_audio").apply { mkdirs() }
        snoreDetector = SnoreDetector(object : SnoreDetector.SnoreCallback {
            override fun onSnoreStarted(timestamp: Long, db: Double) {
                Log.d(TAG, "snore started: $timestamp, ${String.format(Locale.getDefault(), "%.1f", db)}dB")
            }

            override fun onSnoreEnded(startTimestamp: Long, durationMs: Long, pcmData: ByteArray, peakDb: Double) {
                val job = serviceScope.launch {
                    val features = SnoreFeatureAnalyzer.analyze(pcmData, peakDb, durationMs)
                    val audioFile = audioEncoder.encodeToOpus(pcmData, outputDir, "snore_$startTimestamp")
                    if (audioFile == null || audioFile.length() == 0L) {
                        Log.w(TAG, "skip snore event because audio file was not saved: $startTimestamp")
                        return@launch
                    }
                    val event = SnoreEventEntity(
                        recordId = recordId,
                        startTimestamp = startTimestamp,
                        durationMs = durationMs.toInt(),
                        peakDb = features.peakDb,
                        avgDb = features.avgDb,
                        dominantFreq = features.dominantFreq,
                        snoreType = features.snoreType.name,
                        audioFilePath = audioFile.absolutePath,
                        audioFileSizeBytes = audioFile.length(),
                        aiTypeLabel = features.aiTypeLabel
                    )
                    val savedId = repository.insertEvent(event)
                    synchronized(pendingEvents) { pendingEvents.add(event.copy(id = savedId)) }
                    updateNotification()
                }
                synchronized(encodingJobs) { encodingJobs.add(job) }
            }
        }, silenceThresholdDb = silenceThresholdDb)

        try {
            snoreDetector?.startListening()
        } catch (e: Exception) {
            Log.e(TAG, "failed to start snore detection", e)
            finishSessionAndStop()
        }
    }

    private fun stopSnoreDetection() {
        snoreDetector?.stopListening()
        snoreDetector = null
    }

    private fun createEmptyRecord(startTime: Long): SleepRecordEntity {
        return SleepRecordEntity(
            startTime = startTime,
            endTime = startTime,
            sleepDurationMin = 0,
            snoreScore = 0,
            severity = Severity.GOOD.name,
            estAHI = 0f,
            snoreDurationMin = 0,
            snoreRatio = 0f,
            avgDb = 0f,
            maxDb = 0f,
            snoreEventCount = 0,
            apneaEventCount = 0,
            longestApneaSec = 0,
            snoreTypeDistribution = "{}",
            hourlyDistribution = "[]",
            aiSummary = TEXT_RECORDING_SUMMARY,
            aiEvaluation = "",
            aiSuggestions = "[]",
            createdAt = startTime
        )
    }

    private suspend fun buildFinalRecord(
        recordId: Long,
        startTime: Long,
        endTime: Long,
        events: List<SnoreEventEntity>
    ): SleepRecordEntity {
        val durationMs = max(1L, endTime - startTime)
        val sleepDurationMin = max(1, (durationMs / 60_000L).toInt())
        val snoreDurationMs = events.sumOf { it.durationMs.toLong() }
        val snoreDurationMin = (snoreDurationMs / 60_000L).toInt()
        val snoreRatio = (snoreDurationMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        val avgDb = if (events.isEmpty()) 0f else events.map { it.avgDb }.average().toFloat()
        val maxDb = events.maxOfOrNull { it.peakDb } ?: 0f
        val apneaStats = estimateApneaStats(events, durationMs)

        val base = SleepRecordEntity(
            id = recordId,
            startTime = startTime,
            endTime = endTime,
            sleepDurationMin = sleepDurationMin,
            snoreScore = 0,
            severity = Severity.GOOD.name,
            estAHI = apneaStats.ahi,
            snoreDurationMin = snoreDurationMin,
            snoreRatio = snoreRatio,
            avgDb = avgDb,
            maxDb = maxDb,
            snoreEventCount = events.size,
            apneaEventCount = apneaStats.eventCount,
            longestApneaSec = apneaStats.longestSec,
            snoreTypeDistribution = buildTypeDistribution(events),
            hourlyDistribution = buildHourlyDistribution(events),
            aiSummary = "",
            aiEvaluation = "",
            aiSuggestions = "[]",
            createdAt = startTime
        )
        val score = SnoreScoreCalculator.calculate(base, events)
        val scored = base.copy(snoreScore = score, severity = severityFromScore(score).name)
        val history = repository.getAllRecords().first().filter { it.id != recordId }
        val evaluation = SnoreEvaluator.evaluate(scored, listOf(scored) + history)
        return scored.copy(
            aiSummary = evaluation.summary,
            aiEvaluation = evaluation.evaluation,
            aiSuggestions = evaluation.suggestions.joinToString(prefix = "[", postfix = "]") { "\"${it.replace("\"", "'")}\"" }
        )
    }

    private fun buildTypeDistribution(events: List<SnoreEventEntity>): String {
        if (events.isEmpty()) return "{}"
        return events.groupingBy { it.snoreType }
            .eachCount()
            .entries
            .joinToString(prefix = "{", postfix = "}") { (type, count) ->
                "\"$type\":${count.toFloat() / events.size}"
            }
    }

    private fun buildHourlyDistribution(events: List<SnoreEventEntity>): String {
        val buckets = IntArray(24)
        events.forEach { event ->
            val hour = Calendar.getInstance().apply { timeInMillis = event.startTimestamp }.get(Calendar.HOUR_OF_DAY)
            buckets[hour]++
        }
        return buckets.joinToString(prefix = "[", postfix = "]")
    }

    private fun estimateApneaStats(events: List<SnoreEventEntity>, durationMs: Long): ApneaStats {
        val sortedEvents = events.sortedBy { it.startTimestamp }
        if (sortedEvents.size < 2) return ApneaStats(eventCount = 0, longestSec = 0, ahi = 0f)

        val gapsSec = sortedEvents.zipWithNext().mapNotNull { (previous, next) ->
            val previousEnd = previous.startTimestamp + previous.durationMs
            val gapMs = next.startTimestamp - previousEnd
            if (gapMs >= APNEA_GAP_MS) (gapMs / 1000L).toInt() else null
        }
        val durationHours = (durationMs / 3_600_000f).coerceAtLeast(1f / 60f)
        return ApneaStats(
            eventCount = gapsSec.size,
            longestSec = gapsSec.maxOrNull() ?: 0,
            ahi = (gapsSec.size / durationHours).coerceAtMost(120f)
        )
    }

    private suspend fun cleanOldData() {
        val cutoff = System.currentTimeMillis() - AUTO_CLEAN_RETENTION_MS
        repository.getEventsBefore(cutoff).forEach { event ->
            if (event.audioFilePath.isNotBlank()) {
                runCatching { File(event.audioFilePath).delete() }
            }
        }
        repository.deleteOldRecords(cutoff)
    }

    companion object {
        const val ACTION_START = "com.sleep.snore.action.START_RECORDING"
        const val ACTION_STOP = "com.sleep.snore.action.STOP_RECORDING"
        const val CHANNEL_ID = "sleep_recording"
        const val NOTIFICATION_ID = 1001
        private const val TAG = "SleepRecordingService"

        private val _recordingState = MutableStateFlow(RecordingRuntimeState())
        val recordingState: StateFlow<RecordingRuntimeState> = _recordingState.asStateFlow()

        private const val TEXT_CHANNEL_NAME = "\u9f3e\u58f0\u5f55\u5236"
        private const val TEXT_CHANNEL_DESCRIPTION = "\u7761\u7720\u9f3e\u58f0\u5f55\u5236\u670d\u52a1\u901a\u77e5"
        private const val TEXT_RECORDING_TITLE = "\u6b63\u5728\u76d1\u6d4b\u7761\u7720\u9f3e\u58f0"
        private const val TEXT_SNORE_SEGMENTS = "\u5df2\u4fdd\u5b58\u9f3e\u58f0\u7247\u6bb5\uff1a%d \u4e2a"
        private const val TEXT_STOP = "\u7ed3\u675f"
        private const val TEXT_RECORDING_SUMMARY = "\u6b63\u5728\u8bb0\u5f55\u7761\u7720\u9f3e\u58f0"
        private const val AUTO_CLEAN_RETENTION_MS = 30L * 24L * 60L * 60L * 1000L
        private const val APNEA_GAP_MS = 10_000L

        fun startIntent(context: Context): Intent = Intent(context, SleepRecordingService::class.java).setAction(ACTION_START)
        fun stopIntent(context: Context): Intent = Intent(context, SleepRecordingService::class.java).setAction(ACTION_STOP)
    }
}

private data class ApneaStats(
    val eventCount: Int,
    val longestSec: Int,
    val ahi: Float
)

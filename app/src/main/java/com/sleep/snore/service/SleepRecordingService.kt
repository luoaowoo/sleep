package com.sleep.snore.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.Notification
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
import com.sleep.snore.data.model.Sensitivity
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

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
    private var detectorRestartAttempts = 0
    private var lastSilenceThresholdDb: Double = -40.0
    private var lastMaxSegmentDurationSec: Int = 60
    private var lastSensitivity: Sensitivity = Sensitivity.MEDIUM

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return when (intent?.action) {
            ACTION_STOP -> {
                finishSessionAndStop()
                START_NOT_STICKY
            }
            ACTION_REFRESH_NOTIFICATION -> {
                if (isSessionActive) updateNotification()
                START_NOT_STICKY
            }
            ACTION_START -> {
                startSessionIfNeeded()
                START_STICKY
            }
            else -> {
                if (isSessionActive) START_STICKY else START_NOT_STICKY
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.i(TAG, "task removed while recording; foreground service keeps running")
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        val shouldFinalize = isSessionActive && !isFinishingSession
        if (shouldFinalize) {
            isSessionActive = false
            stopSnoreDetection()
            _recordingState.value = RecordingRuntimeState()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    finalizeCurrentSession()
                } catch (e: Exception) {
                    Log.e(TAG, "failed to finalize session on destroy", e)
                } finally {
                    releaseWakeLock()
                    serviceScope.cancel()
                }
            }
        } else {
            _recordingState.value = RecordingRuntimeState()
            releaseWakeLock()
            serviceScope.cancel()
        }
        super.onDestroy()
    }

    private fun startSessionIfNeeded() {
        if (isSessionActive) return
        ensureServiceScope()
        isFinishingSession = false
        isSessionActive = true
        detectorRestartAttempts = 0
        sessionStartTime = System.currentTimeMillis()
        _recordingState.value = RecordingRuntimeState(isActive = true, startTime = sessionStartTime)
        synchronized(pendingEvents) { pendingEvents.clear() }
        synchronized(encodingJobs) { encodingJobs.clear() }
        if (!startForegroundNotification(0)) {
            isSessionActive = false
            _recordingState.value = RecordingRuntimeState()
            stopSelf()
            return
        }
        acquireWakeLock()

        startJob = serviceScope.launch {
            try {
                val settings = preferencesRepository.settings.first()
                val sensitivity = preferencesRepository.sensitivity.first()
                if (settings.autoCleanEnabled) cleanOldData()
                val activeRecord = recoverActiveRecordingIfAvailable()
                val recordId = if (activeRecord == null) {
                    repository.insertRecord(createEmptyRecord(sessionStartTime))
                } else {
                    activeRecord.id
                }
                currentRecordId = recordId
                if (!isSessionActive) return@launch
                startSnoreDetection(
                    recordId = recordId,
                    silenceThresholdDb = settings.silenceThresholdDb.toDouble(),
                    maxSegmentDurationSec = settings.maxSegmentDurationSec,
                    sensitivity = sensitivity
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
        val completedPendingWork = withTimeoutOrNull(FINALIZE_TIMEOUT_MS) {
            startJob?.join()
            val jobs = synchronized(encodingJobs) { encodingJobs.toList() }
            jobs.joinAll()
            // 清空已完成任务，避免列表无限膨胀
            synchronized(encodingJobs) { encodingJobs.clear() }
            true
        } ?: false
        if (!completedPendingWork) {
            Log.w(TAG, "finalize session timed out while waiting for pending audio work")
        }
        val recordId = currentRecordId
        if (recordId != null) {
            val endTime = System.currentTimeMillis()
            val events = getCurrentSessionEvents(recordId)
            repository.updateRecord(buildFinalRecord(recordId, sessionStartTime, endTime, events))
        }
        val settings = withTimeoutOrNull(SETTINGS_READ_TIMEOUT_MS) {
            preferencesRepository.settings.first()
        }
        if (settings?.autoCleanEnabled == true) cleanOldData()
    }

    private fun ensureServiceScope() {
        if (serviceJob.isActive) return
        serviceJob = SupervisorJob()
        serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    }

    private suspend fun recoverActiveRecordingIfAvailable(): SleepRecordEntity? {
        val activeRecord = repository.getActiveRecordingRecord() ?: return null
        val now = System.currentTimeMillis()
        val ageMs = now - activeRecord.startTime
        val events = repository.getEventsSnapshotByRecordId(activeRecord.id)
        if (ageMs !in 0..MAX_SESSION_RECOVERY_MS) {
            Log.w(TAG, "finalizing stale recording record: ${activeRecord.id}")
            val endTime = events.maxOfOrNull { it.startTimestamp + it.durationMs }
                ?: (activeRecord.startTime + STALE_EMPTY_RECORD_DURATION_MS).coerceAtMost(now)
            repository.updateRecord(buildFinalRecord(activeRecord.id, activeRecord.startTime, endTime, events))
            return null
        }

        sessionStartTime = activeRecord.startTime
        synchronized(pendingEvents) {
            pendingEvents.clear()
            pendingEvents.addAll(events)
        }
        _recordingState.value = RecordingRuntimeState(
            isActive = true,
            startTime = activeRecord.startTime,
            eventCount = events.size
        )
        updateNotification()
        Log.i(TAG, "recovered active recording record: ${activeRecord.id}")
        return activeRecord
    }

    private suspend fun getCurrentSessionEvents(recordId: Long): List<SnoreEventEntity> {
        val savedEvents = repository.getEventsSnapshotByRecordId(recordId)
        val pendingSnapshot = synchronized(pendingEvents) { pendingEvents.toList() }
        return (savedEvents + pendingSnapshot)
            .distinctBy { event -> event.id.takeIf { it != 0L } ?: event.audioFilePath }
            .sortedBy { it.startTimestamp }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, TEXT_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW).apply {
            description = TEXT_CHANNEL_DESCRIPTION
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun startForegroundNotification(eventCount: Int): Boolean {
        val notification = buildRecordingNotification(eventCount)

        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            true
        }.getOrElse { error ->
            Log.e(TAG, "failed to start foreground recording service", error)
            false
        }
    }

    private fun buildRecordingNotification(eventCount: Int): Notification {
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
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(TEXT_RECORDING_TITLE)
            .setContentText(TEXT_SNORE_SEGMENTS.format(eventCount))
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, TEXT_STOP, stopIntent)
            .build()
    }

    @SuppressLint("MissingPermission")
    private fun updateNotification() {
        val count = synchronized(pendingEvents) { pendingEvents.size }
        _recordingState.value = _recordingState.value.copy(eventCount = count)
        val notification = buildRecordingNotification(count)
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
        if (wakeLock?.isHeld == true) return
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SleepSnore:RecordingWakeLock").apply {
            setReferenceCounted(false)
            acquire(MAX_WAKE_LOCK_MS)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    private fun startSnoreDetection(
        recordId: Long,
        silenceThresholdDb: Double,
        maxSegmentDurationSec: Int,
        sensitivity: Sensitivity
    ) {
        lastSilenceThresholdDb = silenceThresholdDb
        lastMaxSegmentDurationSec = maxSegmentDurationSec
        lastSensitivity = sensitivity
        val outputDir = File(filesDir, "snore_audio").apply { mkdirs() }
        snoreDetector = SnoreDetector(
            callback = object : SnoreDetector.SnoreCallback {
                override fun onSnoreStarted(timestamp: Long, db: Double) {
                    Log.d(TAG, "snore started: $timestamp, ${String.format(Locale.getDefault(), "%.1f", db)}dB")
                }

                override fun onSnoreEnded(startTimestamp: Long, durationMs: Long, pcmData: ByteArray, peakDb: Double) {
                    val pendingJobCount = synchronized(encodingJobs) { encodingJobs.size }
                    if (pendingJobCount >= MAX_PENDING_ENCODING_JOBS) {
                        Log.w(TAG, "drop snore event because encoding queue is full: $pendingJobCount")
                        return
                    }
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
                    job.invokeOnCompletion {
                        synchronized(encodingJobs) { encodingJobs.remove(job) }
                    }
                    synchronized(encodingJobs) { encodingJobs.add(job) }
                }

                override fun onRecorderError(errorCode: Int, consecutiveErrors: Int) {
                    Log.w(TAG, "AudioRecord read failed repeatedly: code=$errorCode count=$consecutiveErrors")
                    restartSnoreDetectionAfterRecorderError()
                }
            },
            silenceThresholdDb = silenceThresholdDb,
            maxSegmentDurationSec = maxSegmentDurationSec,
            sensitivity = sensitivity
        )

        try {
            snoreDetector?.startListening()
        } catch (e: Exception) {
            Log.e(TAG, "failed to start snore detection", e)
            finishSessionAndStop()
        }
    }

    private fun stopSnoreDetection() {
        runCatching { snoreDetector?.stopListening() }
            .onFailure { Log.w(TAG, "failed to stop snore detection cleanly", it) }
        snoreDetector = null
    }

    private fun restartSnoreDetectionAfterRecorderError() {
        if (!isSessionActive) return
        val recordId = currentRecordId ?: return
        if (detectorRestartAttempts >= MAX_DETECTOR_RESTART_ATTEMPTS) {
            Log.e(TAG, "max detector restart attempts reached; finishing session")
            finishSessionAndStop()
            return
        }
        detectorRestartAttempts++
        serviceScope.launch {
            stopSnoreDetection()
            delay(DETECTOR_RESTART_DELAY_MS)
            if (!isSessionActive) return@launch
            startSnoreDetection(
                recordId = recordId,
                silenceThresholdDb = lastSilenceThresholdDb,
                maxSegmentDurationSec = lastMaxSegmentDurationSec,
                sensitivity = lastSensitivity
            )
        }
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
        if (sortedEvents.size < 2) return ApneaStats(
            eventCount = 0,
            longestSec = 0,
            ahi = 0f,
            centralApneaCount = 0
        )

        var obstructiveCount = 0
        var centralApneaCount = 0
        var longestSec = 0

        sortedEvents.forEachIndexed { index, current ->
            if (index == 0) return@forEachIndexed
            val previous = sortedEvents[index - 1]
            val previousEnd = previous.startTimestamp + previous.durationMs
            val gapMs = current.startTimestamp - previousEnd
            if (gapMs < APNEA_GAP_MS) return@forEachIndexed

            val gapSec = (gapMs / 1000L).toInt()
            if (gapSec > longestSec) longestSec = gapSec

            val preEvents = sortedEvents.subList(0, index)
            val preAvgDb = if (preEvents.isEmpty()) 0f else preEvents.map { it.avgDb }.average().toFloat()
            val recoveryThreshold = preAvgDb + APNEA_RECOVERY_DB_DELTA
            val gapEnd = current.startTimestamp
            val recoveryWindowEnd = gapEnd + APNEA_RECOVERY_WINDOW_MS
            val hasRecoverySnore = sortedEvents.drop(index).any {
                it.startTimestamp in gapEnd..recoveryWindowEnd && it.avgDb >= recoveryThreshold
            }
            if (hasRecoverySnore) {
                obstructiveCount++
            } else {
                centralApneaCount++
            }
        }

        val durationHours = (durationMs / 3_600_000f).coerceAtLeast(1f / 60f)
        val ahi = (obstructiveCount / durationHours).coerceAtMost(120f)
        return ApneaStats(
            eventCount = obstructiveCount + centralApneaCount,
            longestSec = longestSec,
            ahi = ahi,
            centralApneaCount = centralApneaCount
        )
    }

    private suspend fun cleanOldData() {
        val cutoff = System.currentTimeMillis() - AUTO_CLEAN_RETENTION_MS
        repository.deleteOldRecordsWithAudio(cutoff)
    }

    companion object {
        const val ACTION_START = "com.sleep.snore.action.START_RECORDING"
        const val ACTION_STOP = "com.sleep.snore.action.STOP_RECORDING"
        const val ACTION_REFRESH_NOTIFICATION = "com.sleep.snore.action.REFRESH_NOTIFICATION"
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
        private const val APNEA_RECOVERY_WINDOW_MS = 2_000L
        private const val APNEA_RECOVERY_DB_DELTA = 10f
        private const val FINALIZE_TIMEOUT_MS = 30_000L
        private const val SETTINGS_READ_TIMEOUT_MS = 5_000L
        private const val MAX_SESSION_RECOVERY_MS = 16L * 60L * 60L * 1000L
        private const val MAX_WAKE_LOCK_MS = 16L * 60L * 60L * 1000L
        private const val STALE_EMPTY_RECORD_DURATION_MS = 60_000L
        private const val MAX_DETECTOR_RESTART_ATTEMPTS = 3
        private const val DETECTOR_RESTART_DELAY_MS = 1_000L
        private const val MAX_PENDING_ENCODING_JOBS = 32

        fun startIntent(context: Context): Intent = Intent(context, SleepRecordingService::class.java).setAction(ACTION_START)
        fun stopIntent(context: Context): Intent = Intent(context, SleepRecordingService::class.java).setAction(ACTION_STOP)
        fun refreshNotificationIntent(context: Context): Intent =
            Intent(context, SleepRecordingService::class.java).setAction(ACTION_REFRESH_NOTIFICATION)
    }
}

private data class ApneaStats(
    val eventCount: Int,
    val longestSec: Int,
    val ahi: Float,
    val centralApneaCount: Int
)

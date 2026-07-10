package com.sleep.snore.recording

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.sleep.snore.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordingFailureNotifier @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun notifyWearableStartIssue(message: String) {
        if (!canPostNotifications()) return
        createChannel()
        notify(message)
    }

    private fun buildNotification(message: String) = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_dialog_alert)
        .setContentTitle("手环自动检测需要处理")
        .setContentText(message)
        .setStyle(NotificationCompat.BigTextStyle().bigText(message))
        .setContentIntent(mainActivityIntent())
        .setAutoCancel(true)
        .build()

    @SuppressLint("MissingPermission")
    private fun notify(message: String) {
        runCatching {
            NotificationManagerCompat.from(context).notify(
                NOTIFICATION_ID,
                buildNotification(message)
            )
        }
    }

    private fun mainActivityIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "手环自动检测",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "手环/Health Connect 触发鼾声检测失败时提醒"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun canPostNotifications(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }

    private companion object {
        const val CHANNEL_ID = "wearable_sleep_trigger"
        const val NOTIFICATION_ID = 2002
    }
}

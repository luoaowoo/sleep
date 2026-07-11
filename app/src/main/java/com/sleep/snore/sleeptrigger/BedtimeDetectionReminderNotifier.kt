package com.sleep.snore.sleeptrigger

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
class BedtimeDetectionReminderNotifier @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun notifyBedtimeDetectionReminder() {
        if (!canPostNotifications()) return
        createChannel()
        postNotification()
    }

    @SuppressLint("MissingPermission")
    private fun postNotification() {
        runCatching {
            NotificationManagerCompat.from(context).notify(
                NOTIFICATION_ID,
                buildNotification()
            )
        }
    }

    private fun buildNotification() = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
        .setContentTitle("睡前开启鼾声检测")
        .setContentText("点按打开设置页，手动开启前台检测；应用不会在后台自动开麦。")
        .setStyle(
            NotificationCompat.BigTextStyle()
                .bigText("点按打开设置页，手动开启睡前前台检测。为保护隐私和符合 Android 限制，应用不会在后台自动开启麦克风。")
        )
        .setContentIntent(mainActivityIntent())
        .setAutoCancel(true)
        .build()

    private fun mainActivityIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_START_ROUTE, MainActivity.START_ROUTE_SETTINGS)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            REQUEST_CODE_OPEN_SETTINGS,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "睡前检测提醒",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "每天睡前提醒手动开启前台鼾声检测"
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
        const val CHANNEL_ID = "bedtime_detection_reminder"
        const val NOTIFICATION_ID = 2003
        const val REQUEST_CODE_OPEN_SETTINGS = 3003
    }
}

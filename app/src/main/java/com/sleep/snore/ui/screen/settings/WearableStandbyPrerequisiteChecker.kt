package com.sleep.snore.ui.screen.settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.health.connect.client.HealthConnectClient
import com.sleep.snore.sleeptrigger.HealthConnectSleepTriggerSource
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class WearableStandbyPrerequisiteChecker @Inject constructor(
    @ApplicationContext private val context: Context
) {

    open suspend fun startBlocker(): String? {
        val hasRecordAudio = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasRecordAudio) return "缺少麦克风权限，请先在后台录音区域授权"

        val hasNotificationPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        if (!hasNotificationPermission) return "缺少通知权限，请先在后台录音区域授权"

        if (HealthConnectClient.getSdkStatus(context) != HealthConnectClient.SDK_AVAILABLE) {
            return "Health Connect 不可用，请先安装或启用 Health Connect"
        }
        val grantedPermissions = runCatching {
            HealthConnectClient.getOrCreate(context).permissionController.getGrantedPermissions()
        }.getOrElse {
            return "无法检查 Health Connect 权限，请重新授权"
        }
        if (!grantedPermissions.contains(HealthConnectSleepTriggerSource.READ_SLEEP_PERMISSION)) {
            return "缺少 Health Connect 睡眠读取权限，请先授权 Health Connect"
        }
        if (!grantedPermissions.contains(HealthConnectSleepTriggerSource.BACKGROUND_READ_PERMISSION)) {
            return "缺少 Health Connect 后台读取权限，请先授权 Health Connect；否则只能手动检查最近睡眠"
        }
        return null
    }
}

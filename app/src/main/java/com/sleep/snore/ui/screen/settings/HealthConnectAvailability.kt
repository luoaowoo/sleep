package com.sleep.snore.ui.screen.settings

import androidx.health.connect.client.HealthConnectClient

internal fun healthConnectAvailabilityBlocker(sdkStatus: Int): String? {
    return when (sdkStatus) {
        HealthConnectClient.SDK_AVAILABLE -> null
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
            "Health Connect 需要安装或更新；请点击“打开 Health Connect 设置/安装更新”完成后再授权"
        }
        else -> {
            "Health Connect 在此设备不可用；请确认系统版本、Google Play 服务和 Health Connect 支持状态"
        }
    }
}

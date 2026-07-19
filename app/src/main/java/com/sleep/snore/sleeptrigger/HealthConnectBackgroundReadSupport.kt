package com.sleep.snore.sleeptrigger

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.feature.ExperimentalFeatureAvailabilityApi

@OptIn(ExperimentalFeatureAvailabilityApi::class)
internal fun HealthConnectClient.isBackgroundReadAvailable(): Boolean {
    return runCatching {
        features.getFeatureStatus(HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_IN_BACKGROUND) ==
            HealthConnectFeatures.FEATURE_STATUS_AVAILABLE
    }.getOrDefault(false)
}

internal fun healthConnectBackgroundReadAvailable(context: Context): Boolean {
    if (HealthConnectClient.getSdkStatus(context) != HealthConnectClient.SDK_AVAILABLE) return false
    return runCatching {
        HealthConnectClient.getOrCreate(context).isBackgroundReadAvailable()
    }.getOrDefault(false)
}

internal fun healthConnectPermissionsForRequest(
    includeBackgroundRead: Boolean,
    backgroundReadAvailable: Boolean
): Set<String> {
    return if (includeBackgroundRead && backgroundReadAvailable) {
        HealthConnectSleepTriggerSource.BACKGROUND_REQUIRED_PERMISSIONS
    } else {
        HealthConnectSleepTriggerSource.FOREGROUND_REQUIRED_PERMISSIONS
    }
}

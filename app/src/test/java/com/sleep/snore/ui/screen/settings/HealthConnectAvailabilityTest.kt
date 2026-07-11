package com.sleep.snore.ui.screen.settings

import androidx.health.connect.client.HealthConnectClient
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HealthConnectAvailabilityTest {

    @Test
    fun healthConnectAvailabilityBlocker_allowsAvailableSdk() {
        assertThat(
            healthConnectAvailabilityBlocker(HealthConnectClient.SDK_AVAILABLE)
        ).isNull()
    }

    @Test
    fun healthConnectAvailabilityBlocker_reportsProviderUpdateRequired() {
        assertThat(
            healthConnectAvailabilityBlocker(
                HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED
            )
        ).contains("安装或更新")
    }

    @Test
    fun healthConnectAvailabilityBlocker_reportsUnsupportedDevice() {
        assertThat(
            healthConnectAvailabilityBlocker(HealthConnectClient.SDK_UNAVAILABLE)
        ).contains("设备不可用")
    }
}

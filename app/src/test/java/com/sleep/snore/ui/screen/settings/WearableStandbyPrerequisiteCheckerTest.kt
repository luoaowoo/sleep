package com.sleep.snore.ui.screen.settings

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WearableStandbyPrerequisiteCheckerTest {

    @Test
    fun wearableStandbyStartBlocker_allowsStartWithSleepAndBackgroundReadPermissions() {
        assertThat(
            wearableStandbyStartBlocker(
                hasHealthConnectSleepReadPermission = true,
                hasHealthConnectBackgroundReadPermission = true
            )
        ).isNull()
    }

    @Test
    fun wearableStandbyStartBlocker_blocksMissingSleepReadPermission() {
        assertThat(
            wearableStandbyStartBlocker(
                hasHealthConnectSleepReadPermission = false,
                hasHealthConnectBackgroundReadPermission = true
            )
        ).contains("Health Connect 睡眠读取权限")
    }

    @Test
    fun wearableStandbyStartBlocker_blocksMissingBackgroundReadPermission() {
        assertThat(
            wearableStandbyStartBlocker(
                hasHealthConnectSleepReadPermission = true,
                hasHealthConnectBackgroundReadPermission = false
            )
        ).contains("Health Connect 后台读取权限")
    }
}

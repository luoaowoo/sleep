package com.sleep.snore.ui.screen.settings

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WearableStandbyPrerequisiteCheckerTest {

    @Test
    fun wearableStandbyStartBlocker_allowsStartWithSleepReadPermissionOnly() {
        assertThat(
            wearableStandbyStartBlocker(hasHealthConnectSleepReadPermission = true)
        ).isNull()
    }

    @Test
    fun wearableStandbyStartBlocker_blocksMissingSleepReadPermission() {
        assertThat(
            wearableStandbyStartBlocker(hasHealthConnectSleepReadPermission = false)
        ).contains("Health Connect 睡眠读取权限")
    }
}

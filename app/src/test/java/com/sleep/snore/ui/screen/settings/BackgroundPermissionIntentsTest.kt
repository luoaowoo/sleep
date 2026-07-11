package com.sleep.snore.ui.screen.settings

import android.provider.Settings
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BackgroundPermissionIntentsTest {

    @Test
    fun backgroundPermissionIntents_prioritizesMiuiAutostartAndPowerSettings() {
        val intents = backgroundPermissionIntents(
            packageName = "com.sleep.snore",
            packageLabel = "睡眠鼾声",
            preferMiui = true
        )

        assertThat(intents[0].action).isEqualTo("miui.intent.action.OP_AUTO_START")
        assertThat(intents[1].component?.packageName).isEqualTo("com.miui.securitycenter")
        assertThat(intents[1].component?.className).contains("AutoStartManagementActivity")
        assertThat(intents[2].component?.packageName).isEqualTo("com.miui.powerkeeper")
        assertThat(intents[2].getStringExtra("package_name")).isEqualTo("com.sleep.snore")
        assertThat(intents[2].getStringExtra("package_label")).isEqualTo("睡眠鼾声")
        assertThat(intents[3].action).isEqualTo("miui.intent.action.POWER_HIDE_MODE_APP_LIST")
        assertThat(intents[4].component?.packageName).isEqualTo("com.miui.securitycenter")
        assertThat(intents[4].component?.className).contains("PowerSettings")
    }

    @Test
    fun backgroundPermissionIntents_fallsBackToAppDetailsAndSystemSettings() {
        val intents = backgroundPermissionIntents(
            packageName = "com.sleep.snore",
            packageLabel = "睡眠鼾声",
            preferMiui = true
        )

        assertThat(intents[5].action).isEqualTo(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        assertThat(intents[5].data.toString()).isEqualTo("package:com.sleep.snore")
        assertThat(intents[6].action).isEqualTo(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        assertThat(intents[7].action).isEqualTo(Settings.ACTION_SETTINGS)
    }

    @Test
    fun backgroundPermissionIntents_skipsMiuiPrivatePagesForOtherDevices() {
        val intents = backgroundPermissionIntents(
            packageName = "com.sleep.snore",
            packageLabel = "睡眠鼾声",
            preferMiui = false
        )

        assertThat(intents).hasSize(3)
        assertThat(intents[0].action).isEqualTo(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        assertThat(intents[1].action).isEqualTo(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        assertThat(intents[2].action).isEqualTo(Settings.ACTION_SETTINGS)
    }
}

package com.sleep.snore.ui.screen.settings

import android.content.Intent
import android.provider.Settings
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HealthConnectSettingsIntentsTest {

    @Test
    fun healthConnectSettingsIntents_prioritizesHealthConnectSettings() {
        val intents = healthConnectSettingsIntents()

        assertThat(intents.first().action).isAnyOf(
            "android.health.connect.action.HEALTH_HOME_SETTINGS",
            "androidx.health.ACTION_HEALTH_CONNECT_SETTINGS"
        )
        assertThat(intents.first().`package`).isEqualTo(HEALTH_CONNECT_PROVIDER_PACKAGE_NAME)
    }

    @Test
    fun healthConnectSettingsIntents_fallsBackToAppDetailsStoreAndSystemSettings() {
        val intents = healthConnectSettingsIntents()

        assertThat(intents[1].action).isEqualTo(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        assertThat(intents[1].data.toString()).isEqualTo("package:$HEALTH_CONNECT_PROVIDER_PACKAGE_NAME")
        assertThat(intents[2].action).isEqualTo(Intent.ACTION_VIEW)
        assertThat(intents[2].data.toString()).isEqualTo("market://details?id=$HEALTH_CONNECT_PROVIDER_PACKAGE_NAME")
        assertThat(intents[3].action).isEqualTo(Intent.ACTION_VIEW)
        assertThat(intents[3].data.toString()).contains("play.google.com")
        assertThat(intents[4].action).isEqualTo(Settings.ACTION_SETTINGS)
    }
}

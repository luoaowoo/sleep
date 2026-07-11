package com.sleep.snore.ui.screen.settings

import android.content.Intent
import android.content.pm.PackageInfo
import android.provider.Settings
import com.google.common.truth.Truth.assertThat
import com.sleep.snore.sleeptrigger.XiaomiSleepCompanionApps
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class XiaomiCompanionIntentsTest {

    @Test
    fun findInstalledXiaomiCompanion_detectsInstalledPackageWithoutLaunchIntent() {
        val context = RuntimeEnvironment.getApplication()
        val packageName = "com.mi.health"
        shadowOf(context.packageManager).installPackage(PackageInfo().apply {
            this.packageName = packageName
        })

        val companion = findInstalledXiaomiCompanion(context)

        assertThat(companion?.packageName).isEqualTo(packageName)
    }

    @Test
    fun xiaomiCompanionIntents_installedPackageWithoutLaunchIntentFallsBackToAppDetailsFirst() {
        val context = RuntimeEnvironment.getApplication()
        val app = XiaomiSleepCompanionApps.all.first { it.packageName == "com.mi.health" }

        val intents = xiaomiCompanionIntents(context.packageManager, app)

        assertThat(intents.first().action).isEqualTo(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        assertThat(intents.first().data.toString()).isEqualTo("package:com.mi.health")
        assertThat(intents[1].data.toString()).isEqualTo("market://details?id=com.mi.health")
        assertThat(intents[2].data.toString()).contains("id=com.mi.health")
    }

    @Test
    fun xiaomiCompanionIntents_missingPackageUsesSearchForAllCompanionApps() {
        val context = RuntimeEnvironment.getApplication()

        val intents = xiaomiCompanionIntents(context.packageManager, app = null)

        assertThat(intents).hasSize(2)
        assertThat(intents[0].action).isEqualTo(Intent.ACTION_VIEW)
        assertThat(intents[0].data.toString()).contains("market://search")
        assertThat(intents[0].data.toString()).contains("Mi%20Fitness")
        assertThat(intents[0].data.toString()).contains("Zepp%20Life")
        assertThat(intents[1].data.toString()).contains("play.google.com/store/search")
    }
}

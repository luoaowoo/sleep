package com.sleep.snore.ui.screen.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

internal fun healthConnectSettingsIntents(): List<Intent> {
    val providerPackageName = HEALTH_CONNECT_PROVIDER_PACKAGE_NAME
    return listOf(
        Intent(healthConnectSettingsAction()).setPackage(providerPackageName),
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$providerPackageName")
        },
        Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$providerPackageName")),
        Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$providerPackageName")),
        Intent(Settings.ACTION_SETTINGS)
    )
}

private fun healthConnectSettingsAction(): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        "android.health.connect.action.HEALTH_HOME_SETTINGS"
    } else {
        "androidx.health.ACTION_HEALTH_CONNECT_SETTINGS"
    }
}

internal const val HEALTH_CONNECT_PROVIDER_PACKAGE_NAME = "com.google.android.apps.healthdata"

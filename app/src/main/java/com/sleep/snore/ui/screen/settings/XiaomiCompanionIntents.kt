package com.sleep.snore.ui.screen.settings

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.sleep.snore.sleeptrigger.XiaomiSleepCompanionApp
import com.sleep.snore.sleeptrigger.XiaomiSleepCompanionApps

internal fun findInstalledXiaomiCompanion(context: Context): XiaomiSleepCompanionApp? {
    return XiaomiSleepCompanionApps.all.firstOrNull { app ->
        context.packageManager.isPackageInstalled(app.packageName)
    }
}

internal fun xiaomiCompanionIntents(
    packageManager: PackageManager,
    app: XiaomiSleepCompanionApp?
): List<Intent> {
    val appPackage = app?.packageName
    if (appPackage != null) {
        return listOfNotNull(packageManager.getLaunchIntentForPackage(appPackage)) +
            listOf(
                appDetailsIntent(appPackage),
                marketDetailsIntent(appPackage),
                playDetailsIntent(appPackage)
            )
    }

    val query = Uri.encode(XiaomiSleepCompanionApps.displayNames)
    return listOf(
        Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=$query&c=apps")),
        Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/search?q=$query&c=apps"))
    )
}

internal fun openXiaomiCompanionOrStore(
    context: Context,
    app: XiaomiSleepCompanionApp?
): Boolean {
    val intents = xiaomiCompanionIntents(context.packageManager, app)
    for (intent in intents) {
        val started = runCatching {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.isSuccess
        if (started) return true
    }
    return false
}

private fun PackageManager.isPackageInstalled(packageName: String): Boolean {
    return runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            getPackageInfo(packageName, 0)
        }
    }.isSuccess
}

private fun appDetailsIntent(packageName: String): Intent {
    return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:$packageName")
    }
}

private fun marketDetailsIntent(packageName: String): Intent {
    return Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
}

private fun playDetailsIntent(packageName: String): Intent {
    return Intent(
        Intent.ACTION_VIEW,
        Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
    )
}

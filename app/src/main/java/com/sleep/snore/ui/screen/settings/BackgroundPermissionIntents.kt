package com.sleep.snore.ui.screen.settings

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

internal fun backgroundPermissionIntents(
    packageName: String,
    packageLabel: String,
    preferMiui: Boolean = isLikelyMiuiDevice()
): List<Intent> {
    val systemFallbacks = listOf(
        appDetailsIntent(packageName),
        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
        Intent(Settings.ACTION_SETTINGS)
    )
    if (!preferMiui) return systemFallbacks

    return listOf(
        Intent("miui.intent.action.OP_AUTO_START").addCategory(Intent.CATEGORY_DEFAULT),
        Intent().setComponent(
            ComponentName(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity"
            )
        ),
        Intent().setComponent(
            ComponentName(
                "com.miui.powerkeeper",
                "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"
            )
        ).apply {
            putExtra("package_name", packageName)
            putExtra("package_label", packageLabel)
        },
        Intent("miui.intent.action.POWER_HIDE_MODE_APP_LIST"),
        Intent().setComponent(
            ComponentName(
                "com.miui.securitycenter",
                "com.miui.powercenter.PowerSettings"
            )
        )
    ) + systemFallbacks
}

internal fun openFirstAvailableSettingsIntent(
    context: Context,
    intents: List<Intent>
): Boolean {
    val packageManager = context.packageManager
    for (intent in intents) {
        if (intent.resolveActivity(packageManager) == null) continue
        val started = runCatching {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.isSuccess
        if (started) return true
    }
    return false
}

private fun appDetailsIntent(packageName: String): Intent {
    return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:$packageName")
    }
}

private fun isLikelyMiuiDevice(): Boolean {
    val manufacturer = Build.MANUFACTURER.orEmpty().lowercase()
    val brand = Build.BRAND.orEmpty().lowercase()
    return listOf("xiaomi", "redmi", "poco").any { marker ->
        marker in manufacturer || marker in brand
    }
}

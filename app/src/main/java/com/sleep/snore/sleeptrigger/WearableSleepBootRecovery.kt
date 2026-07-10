package com.sleep.snore.sleeptrigger

import com.sleep.snore.data.db.entity.SleepRecordEntity
import com.sleep.snore.data.preferences.SettingsPreferences

internal fun shouldFinalizeWearableRecordingAfterBoot(
    settings: SettingsPreferences,
    activeRecord: SleepRecordEntity?
): Boolean {
    return activeRecord != null &&
        settings.activeRecordingTriggerSource == HealthConnectSleepTriggerSource.SOURCE
}

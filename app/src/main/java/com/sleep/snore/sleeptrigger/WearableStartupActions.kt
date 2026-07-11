package com.sleep.snore.sleeptrigger

import android.content.Context

internal sealed interface WearableStartupAction {
    data object EnqueuePeriodicSleepCheck : WearableStartupAction
    data object EnqueueImmediateSleepCheck : WearableStartupAction
    data object CancelPeriodicSleepCheck : WearableStartupAction
    data class EnqueueBedtimeReminder(val minuteOfDay: Int) : WearableStartupAction
    data object CancelBedtimeReminder : WearableStartupAction
}

internal fun wearableSleepCheckStartupActions(enabled: Boolean): List<WearableStartupAction> {
    return if (enabled) {
        listOf(
            WearableStartupAction.EnqueuePeriodicSleepCheck,
            WearableStartupAction.EnqueueImmediateSleepCheck
        )
    } else {
        listOf(WearableStartupAction.CancelPeriodicSleepCheck)
    }
}

internal fun bedtimeReminderStartupAction(
    enabled: Boolean,
    minuteOfDay: Int
): WearableStartupAction {
    return if (enabled) {
        WearableStartupAction.EnqueueBedtimeReminder(minuteOfDay)
    } else {
        WearableStartupAction.CancelBedtimeReminder
    }
}

internal fun applyWearableStartupAction(
    context: Context,
    action: WearableStartupAction
) {
    when (action) {
        WearableStartupAction.EnqueuePeriodicSleepCheck -> {
            HealthConnectSleepTriggerWorker.enqueue(context)
        }
        WearableStartupAction.EnqueueImmediateSleepCheck -> {
            HealthConnectSleepTriggerWorker.enqueueNow(context)
        }
        WearableStartupAction.CancelPeriodicSleepCheck -> {
            HealthConnectSleepTriggerWorker.cancel(context)
        }
        is WearableStartupAction.EnqueueBedtimeReminder -> {
            BedtimeDetectionReminderWorker.enqueueNext(context, action.minuteOfDay)
        }
        WearableStartupAction.CancelBedtimeReminder -> {
            BedtimeDetectionReminderWorker.cancel(context)
        }
    }
}

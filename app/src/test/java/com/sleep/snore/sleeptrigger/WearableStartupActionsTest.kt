package com.sleep.snore.sleeptrigger

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WearableStartupActionsTest {

    @Test
    fun wearableSleepCheckStartupActions_enabledOnlySchedulesHealthConnectChecks() {
        val actions = wearableSleepCheckStartupActions(enabled = true)

        assertThat(actions).containsExactly(
            WearableStartupAction.EnqueuePeriodicSleepCheck,
            WearableStartupAction.EnqueueImmediateSleepCheck
        ).inOrder()
    }

    @Test
    fun wearableSleepCheckStartupActions_disabledOnlyCancelsHealthConnectChecks() {
        val actions = wearableSleepCheckStartupActions(enabled = false)

        assertThat(actions).containsExactly(WearableStartupAction.CancelPeriodicSleepCheck)
    }

    @Test
    fun bedtimeReminderStartupAction_enabledOnlySchedulesNotificationReminder() {
        val action = bedtimeReminderStartupAction(enabled = true, minuteOfDay = 22 * 60 + 30)

        assertThat(action).isEqualTo(WearableStartupAction.EnqueueBedtimeReminder(22 * 60 + 30))
    }

    @Test
    fun bedtimeReminderStartupAction_disabledOnlyCancelsNotificationReminder() {
        val action = bedtimeReminderStartupAction(enabled = false, minuteOfDay = 22 * 60 + 30)

        assertThat(action).isEqualTo(WearableStartupAction.CancelBedtimeReminder)
    }

    @Test
    fun startupActions_neverStartMicrophoneRecording() {
        val actions = wearableSleepCheckStartupActions(enabled = true) +
            wearableSleepCheckStartupActions(enabled = false) +
            bedtimeReminderStartupAction(enabled = true, minuteOfDay = 22 * 60 + 30) +
            bedtimeReminderStartupAction(enabled = false, minuteOfDay = 22 * 60 + 30)

        assertThat(actions.map { it.startsMicrophoneRecording() })
            .containsExactly(false, false, false, false, false)
    }
}

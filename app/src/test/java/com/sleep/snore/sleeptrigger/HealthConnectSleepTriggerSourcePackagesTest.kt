package com.sleep.snore.sleeptrigger

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HealthConnectSleepTriggerSourcePackagesTest {

    @Test
    fun xiaomiSleepSourcePackageNames_includeMiFitnessChinaAndZeppLife() {
        assertThat(HealthConnectSleepTriggerSource.XIAOMI_SLEEP_SOURCE_PACKAGE_NAMES)
            .containsAtLeast(
                "com.xiaomi.wearable",
                "com.mi.health",
                "com.xiaomi.hm.health"
            )
        assertThat(HealthConnectSleepTriggerSource.XIAOMI_SLEEP_SOURCE_PACKAGE_NAMES)
            .isEqualTo(XiaomiSleepCompanionApps.packageNames)
    }
}

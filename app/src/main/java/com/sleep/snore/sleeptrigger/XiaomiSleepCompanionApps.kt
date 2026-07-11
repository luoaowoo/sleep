package com.sleep.snore.sleeptrigger

data class XiaomiSleepCompanionApp(
    val label: String,
    val packageName: String
)

object XiaomiSleepCompanionApps {
    val all: List<XiaomiSleepCompanionApp> = listOf(
        XiaomiSleepCompanionApp("Mi Fitness", "com.xiaomi.wearable"),
        XiaomiSleepCompanionApp("小米运动健康", "com.mi.health"),
        XiaomiSleepCompanionApp("Zepp Life", "com.xiaomi.hm.health")
    )

    val packageNames: Set<String> = all.map { it.packageName }.toSet()

    val displayNames: String = "Mi Fitness / 小米运动健康 / Zepp Life"
}

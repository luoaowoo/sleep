package com.sleep.snore.data.model

enum class Severity(val label: String) {
    GOOD("良好"),
    MILD("轻度"),
    MODERATE("中度"),
    SEVERE("重度")
}

fun severityFromScore(score: Int): Severity = when {
    score <= 30 -> Severity.GOOD
    score <= 60 -> Severity.MILD
    score <= 80 -> Severity.MODERATE
    else -> Severity.SEVERE
}

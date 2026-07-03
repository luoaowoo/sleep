package com.sleep.snore.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp

data class UiPreferences(
    val compactModeEnabled: Boolean = false,
    val showTechnicalDetails: Boolean = true
) {
    val pageHorizontalPadding: Dp
        get() = if (compactModeEnabled) Spacing.md else Spacing.lg

    val sectionSpacing: Dp
        get() = if (compactModeEnabled) Spacing.sm else Spacing.md

    val cardPadding: Dp
        get() = if (compactModeEnabled) Spacing.md else Spacing.lg
}

val LocalUiPreferences = staticCompositionLocalOf { UiPreferences() }

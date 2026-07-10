package com.sleep.snore.ui.theme

import androidx.compose.ui.graphics.Color
import com.sleep.snore.data.model.AccentColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class CustomColorSchemeTest {

    @Test
    fun customLightScheme_usesSelectedPrimaryColor() {
        val selected = 0xFFE91E63.toInt()
        val scheme = colorSchemeForCustomColor(selected, darkTheme = false)

        assertEquals(Color(selected), scheme.primary)
    }

    @Test
    fun customDarkScheme_isTonedForDarkSurfaces() {
        val selected = 0xFF2196F3.toInt()
        val scheme = colorSchemeForCustomColor(selected, darkTheme = true)

        assertNotEquals(Color(selected), scheme.primary)
        assertEquals(Color(0xFF121416), scheme.surface)
    }

    @Test
    fun settingsScheme_ignoresStoredCustomColorWhenPresetSelected() {
        val staleCustomColor = 0xFFE91E63.toInt()
        val scheme = colorSchemeForSettings(
            accentColor = AccentColor.GREEN,
            customAccentColorArgb = staleCustomColor,
            darkTheme = false
        )

        assertEquals(GreenLightColorScheme.primary, scheme.primary)
    }

    @Test
    fun settingsScheme_usesStoredCustomColorWhenCustomSelected() {
        val customColor = 0xFFE91E63.toInt()
        val scheme = colorSchemeForSettings(
            accentColor = AccentColor.CUSTOM,
            customAccentColorArgb = customColor,
            darkTheme = false
        )

        assertEquals(Color(customColor), scheme.primary)
    }
}

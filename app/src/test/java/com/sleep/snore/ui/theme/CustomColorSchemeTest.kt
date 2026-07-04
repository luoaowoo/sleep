package com.sleep.snore.ui.theme

import androidx.compose.ui.graphics.Color
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
}

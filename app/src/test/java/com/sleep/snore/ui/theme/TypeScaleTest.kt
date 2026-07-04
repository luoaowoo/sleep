package com.sleep.snore.ui.theme

import com.sleep.snore.data.model.FontScale
import org.junit.Assert.assertTrue
import org.junit.Test

class TypeScaleTest {

    @Test
    fun `LARGE 档所有 TextStyle 的 lineHeight 至少为 fontSize 的 1_2 倍`() {
        val typo = scaledTypography(FontScale.LARGE)
        val allStyles = listOf(
            typo.displayLarge, typo.displayMedium, typo.displaySmall,
            typo.headlineLarge, typo.headlineMedium, typo.headlineSmall,
            typo.titleLarge, typo.titleMedium, typo.titleSmall,
            typo.bodyLarge, typo.bodyMedium, typo.bodySmall,
            typo.labelLarge, typo.labelMedium, typo.labelSmall
        )
        for (style in allStyles) {
            val fs = style.fontSize.value
            val lh = style.lineHeight.value
            assertTrue("Expected lh=$lh >= fs*1.2=${fs * 1.2f}", lh >= fs * 1.2f)
        }
    }

    @Test
    fun `SMALL 档 lineHeight 同步缩放`() {
        val standard = scaledTypography(FontScale.STANDARD)
        val small = scaledTypography(FontScale.SMALL)
        // SMALL 档 displayLarge 的 lineHeight 应小于 STANDARD 档
        assertTrue(small.displayLarge.lineHeight.value < standard.displayLarge.lineHeight.value)
    }

    @Test
    fun `LARGE 档 lineHeight 同步缩放`() {
        val standard = scaledTypography(FontScale.STANDARD)
        val large = scaledTypography(FontScale.LARGE)
        assertTrue(large.displayLarge.lineHeight.value > standard.displayLarge.lineHeight.value)
    }
}

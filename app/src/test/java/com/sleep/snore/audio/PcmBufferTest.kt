package com.sleep.snore.audio

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class PcmBufferTest {

    @Test
    fun concatenate_preservesFrameOrderAndBytes() {
        val output = PcmBuffer.concatenate(
            listOf(
                byteArrayOf(1, 2),
                byteArrayOf(3),
                byteArrayOf(4, 5, 6)
            )
        )

        assertArrayEquals(byteArrayOf(1, 2, 3, 4, 5, 6), output)
    }

    @Test
    fun concatenate_emptyFrames_returnsEmptyArray() {
        val output = PcmBuffer.concatenate(emptyList())

        assertEquals(0, output.size)
    }
}

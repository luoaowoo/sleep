package com.sleep.snore.audio

object PcmBuffer {
    fun concatenate(frames: List<ByteArray>): ByteArray {
        if (frames.isEmpty()) return ByteArray(0)
        val totalSize = frames.sumOf { it.size }
        val output = ByteArray(totalSize)
        var offset = 0
        frames.forEach { frame ->
            frame.copyInto(output, destinationOffset = offset)
            offset += frame.size
        }
        return output
    }
}

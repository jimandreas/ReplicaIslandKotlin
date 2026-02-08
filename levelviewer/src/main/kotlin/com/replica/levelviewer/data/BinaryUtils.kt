package com.replica.levelviewer.data

import java.io.InputStream

object BinaryUtils {
    fun readLEInt(stream: InputStream): Int {
        val b = ByteArray(4)
        val read = stream.read(b, 0, 4)
        if (read != 4) throw IllegalStateException("Expected 4 bytes, got $read")
        return byteArrayToInt(b)
    }

    fun readLEFloat(stream: InputStream): Float {
        return java.lang.Float.intBitsToFloat(readLEInt(stream))
    }

    fun byteArrayToInt(b: ByteArray): Int {
        val b0 = b[0].toInt()
        val b1 = b[1].toInt()
        val b2 = b[2].toInt()
        val b3 = b[3].toInt()
        return ((b3 and 0xff) shl 24) or ((b2 and 0xff) shl 16) or ((b1 and 0xff) shl 8) or (b0 and 0xff)
    }
}

package com.replica.levelviewer

import com.replica.levelviewer.data.BinaryUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BinaryUtilsTest {

    @Test
    fun `byteArrayToInt little-endian conversion`() {
        // 0x01020304 in LE: 04 03 02 01
        val bytes = byteArrayOf(0x04, 0x03, 0x02, 0x01)
        assertEquals(0x01020304, BinaryUtils.byteArrayToInt(bytes))
    }

    @Test
    fun `byteArrayToInt zero`() {
        val bytes = byteArrayOf(0, 0, 0, 0)
        assertEquals(0, BinaryUtils.byteArrayToInt(bytes))
    }

    @Test
    fun `byteArrayToInt small value`() {
        // Value 42 in LE: 42 0 0 0
        val bytes = byteArrayOf(42, 0, 0, 0)
        assertEquals(42, BinaryUtils.byteArrayToInt(bytes))
    }

    @Test
    fun `readLEInt from stream`() {
        val bytes = byteArrayOf(0x04, 0x03, 0x02, 0x01)
        val stream = bytes.inputStream()
        assertEquals(0x01020304, BinaryUtils.readLEInt(stream))
    }

    @Test
    fun `readLEFloat from stream`() {
        // Float 1.0f in LE bytes
        val intBits = java.lang.Float.floatToIntBits(1.0f)
        val bytes = byteArrayOf(
            (intBits and 0xFF).toByte(),
            ((intBits shr 8) and 0xFF).toByte(),
            ((intBits shr 16) and 0xFF).toByte(),
            ((intBits shr 24) and 0xFF).toByte()
        )
        val stream = bytes.inputStream()
        assertEquals(1.0f, BinaryUtils.readLEFloat(stream))
    }
}

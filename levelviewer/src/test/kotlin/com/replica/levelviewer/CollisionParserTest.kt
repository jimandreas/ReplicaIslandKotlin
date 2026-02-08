package com.replica.levelviewer

import com.replica.levelviewer.data.CollisionParser
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIf
import java.io.File
import java.io.FileInputStream

class CollisionParserTest {

    private fun findProjectRoot(): File? {
        var dir = File(".").canonicalFile
        while (dir.parentFile != null) {
            if (File(dir, "app/src/main/res/raw/collision.bin").exists()) return dir
            dir = dir.parentFile
        }
        return null
    }

    fun collisionFileExists(): Boolean = findProjectRoot() != null

    @Test
    @EnabledIf("collisionFileExists")
    fun `parse collision bin from actual game data`() {
        val root = findProjectRoot()!!
        val file = File(root, "app/src/main/res/raw/collision.bin")
        val data = FileInputStream(file).use { CollisionParser.parse(it) }

        assertTrue(data.tiles.isNotEmpty(), "Should have parsed at least one collision tile")
        for ((index, segments) in data.tiles) {
            assertTrue(index in 0..255, "Tile index should be 0-255, got $index")
            assertTrue(segments.isNotEmpty(), "Tile $index should have at least one segment")
            for (seg in segments) {
                assertTrue(seg.startX in 0f..32f, "startX ${seg.startX} out of range for tile $index")
                assertTrue(seg.startY in 0f..32f, "startY ${seg.startY} out of range for tile $index")
                assertTrue(seg.endX in 0f..32f, "endX ${seg.endX} out of range for tile $index")
                assertTrue(seg.endY in 0f..32f, "endY ${seg.endY} out of range for tile $index")
            }
        }
    }

    @Test
    fun `parse collision from synthetic data`() {
        val floatBytes = { f: Float ->
            val bits = java.lang.Float.floatToIntBits(f)
            byteArrayOf(
                (bits and 0xFF).toByte(),
                ((bits shr 8) and 0xFF).toByte(),
                ((bits shr 16) and 0xFF).toByte(),
                ((bits shr 24) and 0xFF).toByte()
            )
        }

        val bytes = mutableListOf<Byte>()
        bytes.add(52) // signature
        bytes.add(1)  // tileCount
        bytes.add(3)  // tileIndex
        bytes.add(1)  // segmentCount
        bytes.addAll(floatBytes(0f).toList())   // startX
        bytes.addAll(floatBytes(0f).toList())   // startY
        bytes.addAll(floatBytes(32f).toList())  // endX
        bytes.addAll(floatBytes(0f).toList())   // endY
        bytes.addAll(floatBytes(0f).toList())   // normalX
        bytes.addAll(floatBytes(1f).toList())   // normalY

        val data = CollisionParser.parse(bytes.toByteArray().inputStream())
        assertEquals(1, data.tiles.size)
        assertNotNull(data.tiles[3])
        val seg = data.tiles[3]!!.first()
        assertEquals(0f, seg.startX)
        assertEquals(32f, seg.endX)
        assertEquals(1f, seg.normalY)
    }
}

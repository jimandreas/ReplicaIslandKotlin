package com.replica.levelviewer

import com.replica.levelviewer.data.LevelFileParser
import com.replica.levelviewer.model.TileLayer
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIf
import java.io.File
import java.io.FileInputStream

class LevelFileParserTest {

    private fun findProjectRoot(): File? {
        var dir = File(".").canonicalFile
        while (dir.parentFile != null) {
            if (File(dir, "app/src/main/res/raw/collision.bin").exists()) return dir
            dir = dir.parentFile
        }
        return null
    }

    fun levelFileExists(): Boolean {
        val root = findProjectRoot() ?: return false
        return File(root, "app/src/main/res/raw/level_0_1_sewer.bin").exists()
    }

    @Test
    @EnabledIf("levelFileExists")
    fun `parse level_0_1_sewer from actual game data`() {
        val root = findProjectRoot()!!
        val file = File(root, "app/src/main/res/raw/level_0_1_sewer.bin")
        val data = FileInputStream(file).use { LevelFileParser.parse(it) }

        assertTrue(data.layers.isNotEmpty(), "Level should have at least one layer")
        assertTrue(data.backgroundIndex in 0..6, "Background index should be 0-6, got ${data.backgroundIndex}")

        val collisionLayer = data.collisionLayer
        assertNotNull(collisionLayer, "Level should have a collision layer")
        assertTrue(collisionLayer!!.width > 0, "Collision width should be > 0")
        assertTrue(collisionLayer.height > 0, "Collision height should be > 0")

        val bgLayers = data.backgroundLayers
        assertTrue(bgLayers.isNotEmpty(), "Level should have at least one background layer")
        for (layer in bgLayers) {
            assertEquals(TileLayer.TYPE_BACKGROUND, layer.type)
            assertTrue(layer.themeIndex in 0..6, "Theme index should be 0-6, got ${layer.themeIndex}")
        }
    }

    @Test
    fun `parse synthetic level data`() {
        val floatBytes = { f: Float ->
            val bits = java.lang.Float.floatToIntBits(f)
            byteArrayOf(
                (bits and 0xFF).toByte(),
                ((bits shr 8) and 0xFF).toByte(),
                ((bits shr 16) and 0xFF).toByte(),
                ((bits shr 24) and 0xFF).toByte()
            )
        }

        val intBytes = { i: Int ->
            byteArrayOf(
                (i and 0xFF).toByte(),
                ((i shr 8) and 0xFF).toByte(),
                ((i shr 16) and 0xFF).toByte(),
                ((i shr 24) and 0xFF).toByte()
            )
        }

        val bytes = mutableListOf<Byte>()
        // Level header
        bytes.add(96)  // signature
        bytes.add(1)   // layerCount
        bytes.add(2)   // backgroundIndex (sewage)

        // Layer 0: collision
        bytes.add(1)   // type = collision
        bytes.add(0)   // themeIndex (unused for collision)
        bytes.addAll(floatBytes(1.0f).toList()) // scrollSpeed

        // TiledWorld: 2x2
        bytes.add(42)  // TiledWorld signature
        bytes.addAll(intBytes(2).toList())  // width
        bytes.addAll(intBytes(2).toList())  // height
        // tiles: y-outer, x-inner
        bytes.add(1)   // x=0 y=0
        bytes.add((-1).toByte())  // x=1 y=0 (empty, will be 255 then sign-extended)
        bytes.add(2)   // x=0 y=1
        bytes.add(3)   // x=1 y=1

        val data = LevelFileParser.parse(bytes.toByteArray().inputStream())
        assertEquals(2, data.backgroundIndex)
        assertEquals(1, data.layers.size)

        val layer = data.layers[0]
        assertEquals(TileLayer.TYPE_COLLISION, layer.type)
        assertEquals(2, layer.width)
        assertEquals(2, layer.height)
        assertEquals(1, layer.tiles[0][0])
        assertTrue(layer.tiles[1][0] < 0)  // -1
        assertEquals(2, layer.tiles[0][1])
        assertEquals(3, layer.tiles[1][1])
    }
}

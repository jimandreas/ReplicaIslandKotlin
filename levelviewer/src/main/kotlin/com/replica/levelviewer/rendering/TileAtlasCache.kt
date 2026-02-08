package com.replica.levelviewer.rendering

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

class TileAtlasCache(private val drawableDir: File) {
    private val atlasCache = mutableMapOf<Int, BufferedImage>()
    private val tileCache = mutableMapOf<Long, BufferedImage?>()

    companion object {
        const val TILE_SIZE = 32

        val THEME_TO_FILE = mapOf(
            0 to "grass.png",
            1 to "island.png",
            2 to "sewage.png",
            3 to "cave.png",
            4 to "lab.png",
            5 to "titletileset.png",
            6 to "tutorial.png"
        )
    }

    fun getAtlas(themeIndex: Int): BufferedImage? {
        return atlasCache.getOrPut(themeIndex) {
            val filename = THEME_TO_FILE[themeIndex] ?: return null
            val file = File(drawableDir, filename)
            if (!file.exists()) return null
            ImageIO.read(file)
        }
    }

    fun getTile(themeIndex: Int, tileIndex: Int): BufferedImage? {
        if (tileIndex < 0) return null
        val key = (themeIndex.toLong() shl 32) or tileIndex.toLong()
        return tileCache.getOrPut(key) {
            val atlas = getAtlas(themeIndex) ?: return null
            val tilesPerRow = atlas.width / TILE_SIZE
            if (tilesPerRow == 0) return null
            val srcX = (tileIndex % tilesPerRow) * TILE_SIZE
            val srcY = (tileIndex / tilesPerRow) * TILE_SIZE
            if (srcX + TILE_SIZE > atlas.width || srcY + TILE_SIZE > atlas.height) return null
            atlas.getSubimage(srcX, srcY, TILE_SIZE, TILE_SIZE)
        }
    }
}

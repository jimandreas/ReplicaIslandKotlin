package com.replica.levelviewer.rendering

import com.replica.levelviewer.model.LevelData
import com.replica.levelviewer.model.TileLayer
import java.awt.AlphaComposite
import java.awt.Color
import java.awt.image.BufferedImage

class LevelRenderer(private val atlasCache: TileAtlasCache) {

    companion object {
        const val TILE_SIZE = 32

        val OBJECT_COLORS = mapOf(
            // Player spawn
            0 to Color(0, 255, 0),      // green - player
            // Common enemy types
            1 to Color(255, 0, 0),       // red - basic enemy
            2 to Color(255, 100, 0),     // orange
            3 to Color(255, 200, 0),     // yellow
            // Coins/collectibles
            4 to Color(255, 255, 0),     // bright yellow
            // Default
            -1 to Color(200, 200, 255)   // light blue
        )
    }

    fun render(level: LevelData, showObjects: Boolean = false): BufferedImage {
        // Find the world dimensions from the collision layer, or the largest background layer
        val collisionLayer = level.collisionLayer
        val refLayer = collisionLayer ?: level.backgroundLayers.maxByOrNull { it.width * it.height }
            ?: throw IllegalArgumentException("Level has no renderable layers")

        val worldWidth = refLayer.width
        val worldHeight = refLayer.height
        val imgWidth = worldWidth * TILE_SIZE
        val imgHeight = worldHeight * TILE_SIZE

        if (imgWidth <= 0 || imgHeight <= 0) {
            return BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
        }

        val image = BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()

        // Fill with dark background
        g.color = Color(32, 32, 48)
        g.fillRect(0, 0, imgWidth, imgHeight)
        g.composite = AlphaComposite.SrcOver

        // Render background tile layers in order
        for (layer in level.backgroundLayers) {
            renderTileLayer(g, layer, worldHeight)
        }

        // Render object spawn locations as colored dots
        if (showObjects) {
            val objectLayer = level.objectLayer
            if (objectLayer != null) {
                renderObjectLayer(g, objectLayer, worldHeight)
            }
        }

        g.dispose()
        return image
    }

    private fun renderTileLayer(g: java.awt.Graphics2D, layer: TileLayer, worldHeight: Int) {
        for (x in 0 until layer.width) {
            for (y in 0 until layer.height) {
                val tileIndex = layer.tiles[x][y]
                if (tileIndex < 0) continue
                val tile = atlasCache.getTile(layer.themeIndex, tileIndex) ?: continue
                val destX = x * TILE_SIZE
                val destY = (worldHeight - 1 - y) * TILE_SIZE
                g.drawImage(tile, destX, destY, null)
            }
        }
    }

    private fun renderObjectLayer(g: java.awt.Graphics2D, layer: TileLayer, worldHeight: Int) {
        for (x in 0 until layer.width) {
            for (y in 0 until layer.height) {
                val tileIndex = layer.tiles[x][y]
                if (tileIndex < 0) continue
                val color = OBJECT_COLORS[tileIndex] ?: OBJECT_COLORS[-1]!!
                g.color = Color(color.red, color.green, color.blue, 180)
                val destX = x * TILE_SIZE + TILE_SIZE / 4
                val destY = (worldHeight - 1 - y) * TILE_SIZE + TILE_SIZE / 4
                g.fillOval(destX, destY, TILE_SIZE / 2, TILE_SIZE / 2)
                g.color = Color(color.red, color.green, color.blue, 255)
                g.drawOval(destX, destY, TILE_SIZE / 2, TILE_SIZE / 2)
            }
        }
    }
}

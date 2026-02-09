package com.replica.levelviewer.rendering

import com.replica.levelviewer.model.CollisionData
import com.replica.levelviewer.model.TileLayer
import java.awt.BasicStroke
import java.awt.Color
import java.awt.RenderingHints
import java.awt.image.BufferedImage

class CollisionRenderer {

    companion object {
        const val TILE_SIZE = 32
        private val COLLISION_COLOR = Color(255, 40, 40, 200)
        private val NORMAL_COLOR = Color(40, 40, 255, 150)
        private const val NORMAL_LENGTH = 6f
    }

    fun render(
        image: BufferedImage,
        collisionLayer: TileLayer,
        collisionData: CollisionData,
        showNormals: Boolean = false
    ) {
        val worldHeight = collisionLayer.height
        val g = image.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.stroke = BasicStroke(2f)

        for (x in 0 until collisionLayer.width) {
            for (y in 0 until collisionLayer.height) {
                val tileIndex = collisionLayer.tiles[x][y]
                if (tileIndex < 0) continue
                val segments = collisionData.tiles[tileIndex] ?: continue

                val baseX = x * TILE_SIZE
                val baseY = y * TILE_SIZE

                for (seg in segments) {
                    val sx = baseX + seg.startX
                    val sy = baseY + (TILE_SIZE - seg.startY)
                    val ex = baseX + seg.endX
                    val ey = baseY + (TILE_SIZE - seg.endY)

                    g.color = COLLISION_COLOR
                    g.drawLine(sx.toInt(), sy.toInt(), ex.toInt(), ey.toInt())

                    if (showNormals) {
                        val midX = (sx + ex) / 2
                        val midY = (sy + ey) / 2
                        val nx = midX + seg.normalX * NORMAL_LENGTH
                        val ny = midY - seg.normalY * NORMAL_LENGTH // Y-flip for normal
                        g.color = NORMAL_COLOR
                        g.drawLine(midX.toInt(), midY.toInt(), nx.toInt(), ny.toInt())
                    }
                }
            }
        }

        g.dispose()
    }
}

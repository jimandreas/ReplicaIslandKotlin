/*
 * Copyright (C) 2010 The Android Open Source Project
 * Copyright (C) 2025 Jim Andreas kotlin conversion
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("SameParameterValue")

package com.replica.replicaisland.rendering

import android.opengl.GLES20
import com.replica.replicaisland.Grid
import com.replica.replicaisland.core.BaseObject
import com.replica.replicaisland.levels.TiledWorld
import kotlin.math.ceil

class TiledVertexGrid(private val mTexture: Texture?, private val mWidth: Int, private val mHeight: Int, private val mTileWidth: Int, private val mTileHeight: Int) : BaseObject() {
    private var mTileMap: Grid? = null
    private var mWorld: TiledWorld? = null
    private var mWorldPixelWidth = 0f
    private var mWorldPixelHeight = 0f
    private var tilesPerRow = 0
    private var tilesPerColumn = 0
    private var generated = false
    override fun reset() {}
    fun setWorld(world: TiledWorld?) {
        mWorld = world
    }

    private fun generateGrid(width: Int, height: Int, startTileX: Int, startTileY: Int): Grid? {
        val tileWidth = mTileWidth
        val tileHeight = mTileHeight
        val tilesAcross = width / tileWidth
        val tilesDown = height / tileHeight
        val texture = mTexture
        val texelWidth = 1.0f / texture!!.width
        val texelHeight = 1.0f / texture.height
        val textureTilesAcross = texture.width / tileWidth
        val textureTilesDown = texture.height / tileHeight
        val tilesPerWorldColumn = mWorld!!.fetchHeight()
        val totalTextureTiles = textureTilesAcross * textureTilesDown
        // Check to see if this entire grid is empty tiles.  If so, we don't need to do anything.
        var entirelyEmpty = true
        var tileY = 0
        var tileX: Int
        while (tileY < tilesDown && entirelyEmpty) {
            tileX = 0
            while (tileX < tilesAcross) {
                val tileIndex = mWorld!!.getTile(startTileX + tileX,
                        tilesPerWorldColumn - 1 - (startTileY + tileY))
                if (tileIndex >= 0) {
                    entirelyEmpty = false
                    break
                }
                tileX++
            }
            tileY++
        }
        var grid: Grid? = null
        if (!entirelyEmpty) {
            grid = Grid(tilesAcross, tilesDown)
            for (tileY in 0 until tilesDown) {
                for (tileX in 0 until tilesAcross) {
                    val offsetX = tileX * tileWidth.toFloat()
                    val offsetY = tileY * tileHeight.toFloat()
                    var tileIndex = mWorld!!.getTile(startTileX + tileX,
                            tilesPerWorldColumn - 1 - (startTileY + tileY))
                    if (tileIndex < 0) {
                        tileIndex = totalTextureTiles - 1 // Assume that the last tile is empty.
                    }
                    var textureOffsetX = tileIndex % textureTilesAcross * tileWidth
                    var textureOffsetY = tileIndex / textureTilesAcross * tileHeight
                    if (textureOffsetX < 0 || textureOffsetX > texture.width - tileWidth || textureOffsetY < 0 || textureOffsetY > texture.height - tileHeight) {
                        textureOffsetX = 0
                        textureOffsetY = 0
                    }
                    val u = (textureOffsetX + GL_MAGIC_OFFSET) * texelWidth
                    val v = (textureOffsetY + GL_MAGIC_OFFSET) * texelHeight
                    val u2 = (textureOffsetX + tileWidth - GL_MAGIC_OFFSET) * texelWidth
                    val v2 = (textureOffsetY + tileHeight - GL_MAGIC_OFFSET) * texelHeight
                    val p0 = floatArrayOf(offsetX, offsetY, 0.0f)
                    val p1 = floatArrayOf(offsetX + tileWidth, offsetY, 0.0f)
                    val p2 = floatArrayOf(offsetX, offsetY + tileHeight, 0.0f)
                    val p3 = floatArrayOf(offsetX + tileWidth, offsetY + tileHeight, 0.0f)
                    val uv0 = floatArrayOf(u, v2)
                    val uv1 = floatArrayOf(u2, v2)
                    val uv2 = floatArrayOf(u, v)
                    val uv3 = floatArrayOf(u2, v)
                    val positions = arrayOf(p0, p1, p2, p3)
                    val uvs = arrayOf(uv0, uv1, uv2, uv3)
                    grid[tileX, tileY, positions] = uvs
                }
            }
        }
        return grid
    }

    fun draw(x: Float, y: Float, scrollOriginX: Float, scrollOriginY: Float) {
        val world = mWorld
        val shader = OpenGLSystem.spriteShader
        val matrix = OpenGLSystem.matrixHelper
        if (shader == null || matrix == null) return

        if (!generated && world != null && mTexture != null) {
            mWorldPixelWidth = mWorld!!.fetchWidth() * mTileWidth.toFloat()
            mWorldPixelHeight = mWorld!!.fetchHeight() * mTileHeight.toFloat()
            tilesPerRow = mWorld!!.fetchWidth()
            tilesPerColumn = mWorld!!.fetchHeight()
            val bufferLibrary = sSystemRegistry.bufferLibrary
            val grid = generateGrid(mWorldPixelWidth.toInt(), mWorldPixelHeight.toInt(), 0, 0)
            mTileMap = grid
            generated = true
            if (grid != null) {
                bufferLibrary!!.add(grid)
                grid.generateHardwareBuffers()
            }
        }

        val tileMap = mTileMap
        if (tileMap != null) {
            val texture = mTexture
            if (texture != null) {
                val originX = (x - scrollOriginX).toInt()
                val originY = (y - scrollOriginY).toInt()
                val worldPixelWidth = mWorldPixelWidth
                val percentageScrollRight = if (scrollOriginX != 0.0f) scrollOriginX / worldPixelWidth else 0.0f
                val tileSpaceX = percentageScrollRight * tilesPerRow
                val leftTile = tileSpaceX.toInt()

                val worldPixelHeight = mWorldPixelHeight
                val percentageScrollUp = if (scrollOriginY != 0.0f) scrollOriginY / worldPixelHeight else 0.0f
                val tileSpaceY = percentageScrollUp * tilesPerColumn
                val bottomTile = tileSpaceY.toInt()

                val horizontalSlop = if ((tileSpaceX - leftTile) * mTileWidth > 0) 1 else 0
                val verticalSlop = if ((tileSpaceY - bottomTile) * mTileHeight > 0) 1 else 0

                OpenGLSystem.bindTexture(GLES20.GL_TEXTURE_2D, texture.name)

                // Set up model matrix for tile offset
                matrix.setIdentityModel()
                matrix.translateModel(originX.toFloat(), originY.toFloat(), 0f)

                GLES20.glUniformMatrix4fv(shader.uMVPMatrixLocation, 1, false, matrix.computeMVP(), 0)
                GLES20.glUniform4f(shader.uColorLocation, 1f, 1f, 1f, 1f)

                tileMap.beginDrawingStrips(shader)

                val horzTileCount = ceil(mWidth.toFloat() / mTileWidth.toDouble()).toInt()
                val vertTileCount = ceil(mHeight.toFloat() / mTileHeight.toDouble()).toInt()
                val endX = leftTile + horizontalSlop + horzTileCount
                val endY = bottomTile + verticalSlop + vertTileCount
                val indexesPerTile = 6
                val indexesPerRow = tilesPerRow * indexesPerTile
                val startOffset = leftTile * indexesPerTile
                val count = (endX - leftTile) * indexesPerTile
                var tileY = bottomTile
                while (tileY < endY && tileY < tilesPerColumn) {
                    val row = tileY * indexesPerRow
                    tileMap.drawStrip(row + startOffset, count)
                    tileY++
                }

                tileMap.endDrawingStrips(shader)
            }
        }
    }

    companion object {
        private const val GL_MAGIC_OFFSET = 0.375f
    }
}

/*
 * Copyright (C) 2010 The Android Open Source Project
 *
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
@file:Suppress("NAME_SHADOWING", "SameParameterValue", "VARIABLE_WITH_REDUNDANT_INITIALIZER")

package com.replica.replicaisland

import com.replica.replicaisland.Grid.Companion.endDrawing
import com.replica.replicaisland.OpenGLSystem.Companion.bindTexture
import com.replica.replicaisland.OpenGLSystem.Companion.gL
import javax.microedition.khronos.opengles.GL10
import kotlin.math.ceil

class TiledVertexGrid(private val mTexture: Texture?, private val mWidth: Int, private val mHeight: Int, private val mTileWidth: Int, private val mTileHeight: Int) : BaseObject() {
    private var mTileMap: Grid? = null
    private var mWorld: TiledWorld? = null
    private var mWorldPixelWidth = 0f
    private var mWorldPixelHeight = 0f
    private var mTilesPerRow = 0
    private var mTilesPerColumn = 0
    private var mGenerated = false
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
        var tileX = 0
        while (tileY < tilesDown && entirelyEmpty) {
            tileX = 0
            while (tileX < tilesAcross && entirelyEmpty) {
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
            grid = Grid(tilesAcross, tilesDown, false)
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
        val gl = gL
        if (!mGenerated && world != null && gl != null && mTexture != null) {
            val tilesAcross = mWorld!!.fetchWidth()
            val tilesDown = mWorld!!.fetchHeight()
            mWorldPixelWidth = mWorld!!.fetchWidth() * mTileWidth.toFloat()
            mWorldPixelHeight = mWorld!!.fetchHeight() * mTileHeight.toFloat()
            mTilesPerRow = tilesAcross
            mTilesPerColumn = tilesDown
            val bufferLibrary = sSystemRegistry.bufferLibrary
            val grid = generateGrid(mWorldPixelWidth.toInt(), mWorldPixelHeight.toInt(), 0, 0)
            mTileMap = grid
            mGenerated = true
            if (grid != null) {
                bufferLibrary!!.add(grid)
                if (sSystemRegistry.contextParameters!!.supportsVBOs) {
                    grid.generateHardwareBuffers(gl)
                }
            }
        }
        val tileMap = mTileMap
        if (tileMap != null) {
            val texture = mTexture
            if (gl != null && texture != null) {
                val originX = (x - scrollOriginX).toInt()
                val originY = (y - scrollOriginY).toInt()
                val worldPixelWidth = mWorldPixelWidth
                val percentageScrollRight = if (scrollOriginX != 0.0f) scrollOriginX / worldPixelWidth else 0.0f
                val tileSpaceX = percentageScrollRight * mTilesPerRow
                val leftTile = tileSpaceX.toInt()

                // calculate the top tile index
                val worldPixelHeight = mWorldPixelHeight
                val percentageScrollUp = if (scrollOriginY != 0.0f) scrollOriginY / worldPixelHeight else 0.0f
                val tileSpaceY = percentageScrollUp * mTilesPerColumn
                val bottomTile = tileSpaceY.toInt()

                // calculate any sub-tile slop that our scroll position may require.
                val horizontalSlop = if ((tileSpaceX - leftTile) * mTileWidth > 0) 1 else 0
                val verticalSlop = if ((tileSpaceY - bottomTile) * mTileHeight > 0) 1 else 0
                bindTexture(GL10.GL_TEXTURE_2D, texture.name)
                tileMap.beginDrawingStrips(gl, true)
                val horzTileCount = ceil(mWidth.toFloat() / mTileWidth.toDouble()).toInt()
                val vertTileCount = ceil(mHeight.toFloat() / mTileHeight.toDouble()).toInt()
                // draw vertex strips
                val endX = leftTile + horizontalSlop + horzTileCount
                val endY = bottomTile + verticalSlop + vertTileCount
                gl.glPushMatrix()
                gl.glLoadIdentity()
                gl.glTranslatef(
                        originX.toFloat(),
                        originY.toFloat(),
                        0.0f)
                val indexesPerTile = 6
                val indexesPerRow = mTilesPerRow * indexesPerTile
                val startOffset = leftTile * indexesPerTile
                val count = (endX - leftTile) * indexesPerTile
                var tileY = bottomTile
                while (tileY < endY && tileY < mTilesPerColumn) {
                    val row = tileY * indexesPerRow
                    tileMap.drawStrip(gl, true, row + startOffset, count)
                    tileY++
                }
                gl.glPopMatrix()
                endDrawing(gl)
            }
        }
    }

    companion object {
        private const val GL_MAGIC_OFFSET = 0.375f
    }
}
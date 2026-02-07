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

package com.replica.replicaisland.rendering

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

/**
 * A shared unit quad (4 vertices, 2 triangles) in a VBO.
 * Replaces glDrawTexfOES. UV coordinates are updated per-draw to handle
 * crop rectangles and sprite flipping. The model matrix positions/scales
 * the quad to the correct screen location.
 *
 * Vertex layout: x, y (2 floats per vertex)
 * TexCoord layout: u, v (2 floats per vertex, updated per draw)
 *
 * Quad covers [0,0] to [1,1] in local space. The model matrix scales
 * it to the desired pixel size and translates to position.
 */
class SpriteQuad {
    private var positionVboId: Int = 0
    private var indexVboId: Int = 0
    private val texCoordBuffer: FloatBuffer
    private var initialized = false

    init {
        texCoordBuffer = ByteBuffer.allocateDirect(4 * 2 * FLOAT_SIZE)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
    }

    fun initialize() {
        if (initialized) return

        // Unit quad positions: [0,0] to [1,1]
        val positions = floatArrayOf(
            0f, 0f,  // bottom-left
            1f, 0f,  // bottom-right
            0f, 1f,  // top-left
            1f, 1f   // top-right
        )

        val indices = shortArrayOf(
            0, 1, 2,
            1, 3, 2
        )

        val posBuffer = ByteBuffer.allocateDirect(positions.size * FLOAT_SIZE)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        posBuffer.put(positions).position(0)

        val idxBuffer = ByteBuffer.allocateDirect(indices.size * SHORT_SIZE)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()
        idxBuffer.put(indices).position(0)

        val buffers = IntArray(2)
        GLES20.glGenBuffers(2, buffers, 0)
        positionVboId = buffers[0]
        indexVboId = buffers[1]

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, positionVboId)
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, positions.size * FLOAT_SIZE, posBuffer, GLES20.GL_STATIC_DRAW)

        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indexVboId)
        GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, indices.size * SHORT_SIZE, idxBuffer, GLES20.GL_STATIC_DRAW)

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)

        initialized = true
    }

    /**
     * Sets UV coordinates for the quad.
     * @param u0 left U
     * @param v0 bottom V
     * @param u1 right U
     * @param v1 top V
     */
    fun setUVs(u0: Float, v0: Float, u1: Float, v1: Float) {
        texCoordBuffer.position(0)
        texCoordBuffer.put(u0).put(v0) // bottom-left
        texCoordBuffer.put(u1).put(v0) // bottom-right
        texCoordBuffer.put(u0).put(v1) // top-left
        texCoordBuffer.put(u1).put(v1) // top-right
        texCoordBuffer.position(0)
    }

    /**
     * Draws the quad using the currently active shader program.
     * @param aPositionLocation attribute location for aPosition
     * @param aTexCoordLocation attribute location for aTexCoord
     */
    fun draw(aPositionLocation: Int, aTexCoordLocation: Int) {
        // Bind position VBO
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, positionVboId)
        GLES20.glEnableVertexAttribArray(aPositionLocation)
        GLES20.glVertexAttribPointer(aPositionLocation, 2, GLES20.GL_FLOAT, false, 0, 0)

        // Tex coords from client buffer (updated per draw)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        GLES20.glEnableVertexAttribArray(aTexCoordLocation)
        GLES20.glVertexAttribPointer(aTexCoordLocation, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer)

        // Draw with index buffer
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indexVboId)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, GLES20.GL_UNSIGNED_SHORT, 0)

        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)
        GLES20.glDisableVertexAttribArray(aPositionLocation)
        GLES20.glDisableVertexAttribArray(aTexCoordLocation)
    }

    fun release() {
        if (initialized) {
            val buffers = intArrayOf(positionVboId, indexVboId)
            GLES20.glDeleteBuffers(2, buffers, 0)
            positionVboId = 0
            indexVboId = 0
            initialized = false
        }
    }

    companion object {
        private const val FLOAT_SIZE = 4
        private const val SHORT_SIZE = 2
    }
}

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
@file:Suppress("unused")

package com.replica.replicaisland

import android.opengl.GLES20
import com.replica.replicaisland.rendering.ShaderProgram
import com.replica.replicaisland.ui.DebugLog
import java.nio.*

/**
 * A 2D rectangular mesh. Can be drawn textured or untextured.
 * Uses OpenGL ES 2.0 with VBOs for hardware-accelerated rendering.
 */
class Grid(quadsAcross: Int, quadsDown: Int) {
    private val floatVertexBuffer: FloatBuffer
    private val floatTexCoordBuffer: FloatBuffer
    private val mVertsAcross: Int
    private val mVertsDown: Int
    private val mIndexCount: Int
    private val shortIndexBuffer: ShortBuffer

    // ES 2.0 VBO handles
    private var vertBufferIndex = 0
    private var texCoordBufferIndex = 0
    private var indexBufferIndex = 0
    private var useHardwareBuffers = false

    operator fun set(quadX: Int, quadY: Int, positions: Array<FloatArray>, uvs: Array<FloatArray>) {
        require(!(quadX < 0 || quadX * 2 >= mVertsAcross)) { "quadX" }
        require(!(quadY < 0 || quadY * 2 >= mVertsDown)) { "quadY" }
        require(positions.size >= 4) { "positions" }
        require(uvs.size >= 4) { "quadY" }
        val i = quadX * 2
        val j = quadY * 2
        setVertex(i, j, positions[0][0], positions[0][1], positions[0][2], uvs[0][0], uvs[0][1])
        setVertex(i + 1, j, positions[1][0], positions[1][1], positions[1][2], uvs[1][0], uvs[1][1])
        setVertex(i, j + 1, positions[2][0], positions[2][1], positions[2][2], uvs[2][0], uvs[2][1])
        setVertex(i + 1, j + 1, positions[3][0], positions[3][1], positions[3][2], uvs[3][0], uvs[3][1])
    }

    private fun setVertex(i: Int, j: Int, x: Float, y: Float, z: Float, u: Float, v: Float) {
        require(i in 0..<mVertsAcross) { "i" }
        require(j in 0..<mVertsDown) { "j" }
        val index = mVertsAcross * j + i
        val posIndex = index * 3
        val texIndex = index * 2
        floatVertexBuffer.put(posIndex, x)
        floatVertexBuffer.put(posIndex + 1, y)
        floatVertexBuffer.put(posIndex + 2, z)
        floatTexCoordBuffer.put(texIndex, u)
        floatTexCoordBuffer.put(texIndex + 1, v)
    }

    fun beginDrawingStrips(shader: ShaderProgram) {
        if (useHardwareBuffers) {
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertBufferIndex)
            GLES20.glEnableVertexAttribArray(shader.aPositionLocation)
            GLES20.glVertexAttribPointer(shader.aPositionLocation, 3, GLES20.GL_FLOAT, false, 0, 0)

            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, texCoordBufferIndex)
            GLES20.glEnableVertexAttribArray(shader.aTexCoordLocation)
            GLES20.glVertexAttribPointer(shader.aTexCoordLocation, 2, GLES20.GL_FLOAT, false, 0, 0)

            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indexBufferIndex)
        } else {
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
            GLES20.glEnableVertexAttribArray(shader.aPositionLocation)
            GLES20.glVertexAttribPointer(shader.aPositionLocation, 3, GLES20.GL_FLOAT, false, 0, floatVertexBuffer)

            GLES20.glEnableVertexAttribArray(shader.aTexCoordLocation)
            GLES20.glVertexAttribPointer(shader.aTexCoordLocation, 2, GLES20.GL_FLOAT, false, 0, floatTexCoordBuffer)

            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)
        }
    }

    fun drawStrip(startIndex: Int, indexCount: Int) {
        var count = indexCount
        if (startIndex + indexCount >= mIndexCount) {
            count = mIndexCount - startIndex
        }
        if (useHardwareBuffers) {
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, count, GLES20.GL_UNSIGNED_SHORT, startIndex * SHORT_SIZE)
        } else {
            shortIndexBuffer.position(startIndex)
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, count, GLES20.GL_UNSIGNED_SHORT, shortIndexBuffer)
        }
    }

    fun endDrawingStrips(shader: ShaderProgram) {
        GLES20.glDisableVertexAttribArray(shader.aPositionLocation)
        GLES20.glDisableVertexAttribArray(shader.aTexCoordLocation)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)
    }

    /**
     * When the OpenGL ES device is lost, GL handles become invalidated.
     * In that case, we just want to "forget" the old handles (without
     * explicitly deleting them) and make new ones.
     */
    fun invalidateHardwareBuffers() {
        vertBufferIndex = 0
        texCoordBufferIndex = 0
        indexBufferIndex = 0
        useHardwareBuffers = false
    }

    fun releaseHardwareBuffers() {
        if (useHardwareBuffers) {
            val buffer = IntArray(1)
            buffer[0] = vertBufferIndex
            GLES20.glDeleteBuffers(1, buffer, 0)
            buffer[0] = texCoordBufferIndex
            GLES20.glDeleteBuffers(1, buffer, 0)
            buffer[0] = indexBufferIndex
            GLES20.glDeleteBuffers(1, buffer, 0)
            invalidateHardwareBuffers()
        }
    }

    fun generateHardwareBuffers() {
        if (!useHardwareBuffers) {
            DebugLog.i("Grid", "Using Hardware Buffers")
            val buffer = IntArray(1)

            // Vertex buffer
            GLES20.glGenBuffers(1, buffer, 0)
            vertBufferIndex = buffer[0]
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertBufferIndex)
            val vertexSize = floatVertexBuffer.capacity() * FLOAT_SIZE
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertexSize, floatVertexBuffer, GLES20.GL_STATIC_DRAW)

            // Texture coordinate buffer
            GLES20.glGenBuffers(1, buffer, 0)
            texCoordBufferIndex = buffer[0]
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, texCoordBufferIndex)
            val texCoordSize = floatTexCoordBuffer.capacity() * FLOAT_SIZE
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, texCoordSize, floatTexCoordBuffer, GLES20.GL_STATIC_DRAW)

            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)

            // Index buffer
            GLES20.glGenBuffers(1, buffer, 0)
            indexBufferIndex = buffer[0]
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indexBufferIndex)
            val indexSize = shortIndexBuffer.capacity() * SHORT_SIZE
            GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, indexSize, shortIndexBuffer, GLES20.GL_STATIC_DRAW)

            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)
            useHardwareBuffers = true
        }
    }

    companion object {
        private const val FLOAT_SIZE = 4
        private const val SHORT_SIZE = 2
    }

    init {
        val vertsAcross = quadsAcross * 2
        val vertsDown = quadsDown * 2
        require(vertsAcross in 0..<65536) { "quadsAcross" }
        require(vertsDown in 0..<65536) { "quadsDown" }
        require(vertsAcross * vertsDown < 65536) { "quadsAcross * quadsDown >= 32768" }
        mVertsAcross = vertsAcross
        mVertsDown = vertsDown
        val size = vertsAcross * vertsDown

        floatVertexBuffer = ByteBuffer.allocateDirect(FLOAT_SIZE * size * 3)
                .order(ByteOrder.nativeOrder()).asFloatBuffer()
        floatTexCoordBuffer = ByteBuffer.allocateDirect(FLOAT_SIZE * size * 2)
                .order(ByteOrder.nativeOrder()).asFloatBuffer()

        val quadCount = quadsAcross * quadsDown
        val indexCount = quadCount * 6
        mIndexCount = indexCount

        shortIndexBuffer = ByteBuffer.allocateDirect(SHORT_SIZE * indexCount)
                .order(ByteOrder.nativeOrder()).asShortBuffer()

        /*
         * Initialize triangle list mesh.
         *
         *     [0]------[1]   [2]------[3] ...
         *      |    /   |     |    /   |
         *      |   /    |     |   /    |
         *      |  /     |     |  /     |
         *     [w]-----[w+1] [w+2]----[w+3]...
         *      |       |
         *
         */

        var i = 0
        for (y in 0 until quadsDown) {
            val indexY = y * 2
            for (x in 0 until quadsAcross) {
                val indexX = x * 2
                val a = (indexY * mVertsAcross + indexX).toShort()
                val b = (indexY * mVertsAcross + indexX + 1).toShort()
                val c = ((indexY + 1) * mVertsAcross + indexX).toShort()
                val d = ((indexY + 1) * mVertsAcross + indexX + 1).toShort()
                shortIndexBuffer.put(i++, a)
                shortIndexBuffer.put(i++, b)
                shortIndexBuffer.put(i++, c)
                shortIndexBuffer.put(i++, b)
                shortIndexBuffer.put(i++, c)
                shortIndexBuffer.put(i++, d)
            }
        }
    }
}

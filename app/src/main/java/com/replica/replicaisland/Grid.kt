/*
 * Copyright (C) 2010 The Android Open Source Project
 * Copyright (C) 2024 Jim Andreas kotlin conversion and updating
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
@file:Suppress("UNUSED_PARAMETER", "unused")

package com.replica.replicaisland

import java.nio.*
import javax.microedition.khronos.opengles.GL10
import javax.microedition.khronos.opengles.GL11

/**
 * A 2D rectangular mesh. Can be drawn textured or untextured.
 * This version is modified from the original Grid.java (found in
 * the SpriteText package in the APIDemos Android sample) to support hardware
 * vertex buffers and to insert edges between grid squares for tiling.
 */
class Grid(quadsAcross: Int, quadsDown: Int, useFixedPoint: Boolean) {
    private var floatVertexBuffer: FloatBuffer? = null
    private var floatTexCoordBuffer: FloatBuffer? = null
    private var fixedVertexBuffer: IntBuffer? = null
    private var fixedTexCoordBuffer: IntBuffer? = null
    private val indexBuffer: CharBuffer
    private var vertexBuffer: Buffer? = null
    private var texCoordBuffer: Buffer? = null
    private var coordinateSize = 0
    private var coordinateType = 0
    private val mVertsAcross: Int
    private val mVertsDown: Int
    private val mIndexCount: Int
    private var useHardwareBuffers: Boolean
    private var vertBufferIndex: Int
    private var indexBufferIndex = 0
    private var textureCoordBufferIndex = 0

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
        require(!(i < 0 || i >= mVertsAcross)) { "i" }
        require(!(j < 0 || j >= mVertsDown)) { "j" }
        val index = mVertsAcross * j + i
        val posIndex = index * 3
        val texIndex = index * 2
        if (coordinateType == GL10.GL_FLOAT) {
            floatVertexBuffer!!.put(posIndex, x)
            floatVertexBuffer!!.put(posIndex + 1, y)
            floatVertexBuffer!!.put(posIndex + 2, z)
            floatTexCoordBuffer!!.put(texIndex, u)
            floatTexCoordBuffer!!.put(texIndex + 1, v)
        } else {
            fixedVertexBuffer!!.put(posIndex, (x * (1 shl 16)).toInt())
            fixedVertexBuffer!!.put(posIndex + 1, (y * (1 shl 16)).toInt())
            fixedVertexBuffer!!.put(posIndex + 2, (z * (1 shl 16)).toInt())
            fixedTexCoordBuffer!!.put(texIndex, (u * (1 shl 16)).toInt())
            fixedTexCoordBuffer!!.put(texIndex + 1, (v * (1 shl 16)).toInt())
        }
    }

    fun beginDrawingStrips(gl: GL10, useTexture: Boolean) {
        beginDrawing(gl, useTexture)
        if (!useHardwareBuffers) {
            gl.glVertexPointer(3, coordinateType, 0, vertexBuffer)
            if (useTexture) {
                gl.glTexCoordPointer(2, coordinateType, 0, texCoordBuffer)
            }
        } else {
            val gl11 = gl as GL11
            // draw using hardware buffers
            gl11.glBindBuffer(GL11.GL_ARRAY_BUFFER, vertBufferIndex)
            gl11.glVertexPointer(3, coordinateType, 0, 0)
            gl11.glBindBuffer(GL11.GL_ARRAY_BUFFER, textureCoordBufferIndex)
            gl11.glTexCoordPointer(2, coordinateType, 0, 0)
            gl11.glBindBuffer(GL11.GL_ELEMENT_ARRAY_BUFFER, indexBufferIndex)
        }
    }

    // Assumes beginDrawingStrips() has been called before this.
    fun drawStrip(gl: GL10, useTexture: Boolean, startIndex: Int, indexCount: Int) {
        var count = indexCount
        if (startIndex + indexCount >= mIndexCount) {
            count = mIndexCount - startIndex
        }
        if (!useHardwareBuffers) {
            gl.glDrawElements(GL10.GL_TRIANGLES, count,
                    GL10.GL_UNSIGNED_SHORT, indexBuffer.position(startIndex))
        } else {
            val gl11 = gl as GL11
            gl11.glDrawElements(GL11.GL_TRIANGLES, count,
                    GL11.GL_UNSIGNED_SHORT, startIndex * CHAR_SIZE)
        }
    }

    fun draw(gl: GL10, useTexture: Boolean) {
        if (!useHardwareBuffers) {
            gl.glVertexPointer(3, coordinateType, 0, vertexBuffer)
            if (useTexture) {
                gl.glTexCoordPointer(2, coordinateType, 0, texCoordBuffer)
            }
            gl.glDrawElements(GL10.GL_TRIANGLES, mIndexCount,
                    GL10.GL_UNSIGNED_SHORT, indexBuffer)
        } else {
            val gl11 = gl as GL11
            // draw using hardware buffers
            gl11.glBindBuffer(GL11.GL_ARRAY_BUFFER, vertBufferIndex)
            gl11.glVertexPointer(3, coordinateType, 0, 0)
            gl11.glBindBuffer(GL11.GL_ARRAY_BUFFER, textureCoordBufferIndex)
            gl11.glTexCoordPointer(2, coordinateType, 0, 0)
            gl11.glBindBuffer(GL11.GL_ELEMENT_ARRAY_BUFFER, indexBufferIndex)
            gl11.glDrawElements(GL11.GL_TRIANGLES, mIndexCount,
                    GL11.GL_UNSIGNED_SHORT, 0)
            gl11.glBindBuffer(GL11.GL_ARRAY_BUFFER, 0)
            gl11.glBindBuffer(GL11.GL_ELEMENT_ARRAY_BUFFER, 0)
        }
    }

    fun usingHardwareBuffers(): Boolean {
        return useHardwareBuffers
    }

    /**
     * When the OpenGL ES device is lost, GL handles become invalidated.
     * In that case, we just want to "forget" the old handles (without
     * explicitly deleting them) and make new ones.
     */
    fun invalidateHardwareBuffers() {
        vertBufferIndex = 0
        indexBufferIndex = 0
        textureCoordBufferIndex = 0
        useHardwareBuffers = false
    }

    /**
     * Deletes the hardware buffers allocated by this object (if any).
     */
    fun releaseHardwareBuffers(gl: GL10?) {
        if (useHardwareBuffers) {
            if (gl is GL11) {
                val buffer = IntArray(1)
                buffer[0] = vertBufferIndex
                gl.glDeleteBuffers(1, buffer, 0)
                buffer[0] = textureCoordBufferIndex
                gl.glDeleteBuffers(1, buffer, 0)
                buffer[0] = indexBufferIndex
                gl.glDeleteBuffers(1, buffer, 0)
            }
            invalidateHardwareBuffers()
        }
    }

    /**
     * Allocates hardware buffers on the graphics card and fills them with
     * data if a buffer has not already been previously allocated.  Note that
     * this function uses the GL_OES_vertex_buffer_object extension, which is
     * not guaranteed to be supported on every device.
     * @param gl  A pointer to the OpenGL ES context.
     */
    fun generateHardwareBuffers(gl: GL10?) {
        if (!useHardwareBuffers) {
            DebugLog.i("Grid", "Using Hardware Buffers")
            if (gl is GL11) {
                val buffer = IntArray(1)

                // Allocate and fill the vertex buffer.
                gl.glGenBuffers(1, buffer, 0)
                vertBufferIndex = buffer[0]
                gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, vertBufferIndex)
                val vertexSize = vertexBuffer!!.capacity() * coordinateSize
                gl.glBufferData(GL11.GL_ARRAY_BUFFER, vertexSize,
                        vertexBuffer, GL11.GL_STATIC_DRAW)

                // Allocate and fill the texture coordinate buffer.
                gl.glGenBuffers(1, buffer, 0)
                textureCoordBufferIndex = buffer[0]
                gl.glBindBuffer(GL11.GL_ARRAY_BUFFER,
                        textureCoordBufferIndex)
                val texCoordSize = texCoordBuffer!!.capacity() * coordinateSize
                gl.glBufferData(GL11.GL_ARRAY_BUFFER, texCoordSize,
                        texCoordBuffer, GL11.GL_STATIC_DRAW)

                // Unbind the array buffer.
                gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, 0)

                // Allocate and fill the index buffer.
                gl.glGenBuffers(1, buffer, 0)
                indexBufferIndex = buffer[0]
                gl.glBindBuffer(GL11.GL_ELEMENT_ARRAY_BUFFER,
                        indexBufferIndex)
                // A char is 2 bytes.
                val indexSize = indexBuffer.capacity() * 2
                gl.glBufferData(GL11.GL_ELEMENT_ARRAY_BUFFER, indexSize, indexBuffer,
                        GL11.GL_STATIC_DRAW)

                // Unbind the element array buffer.
                gl.glBindBuffer(GL11.GL_ELEMENT_ARRAY_BUFFER, 0)
                useHardwareBuffers = true
                // TODO: fix assertions
//                assert(vertBufferIndex != 0)
//                assert(textureCoordBufferIndex != 0)
//                assert(indexBufferIndex != 0)
//                assert(gl11.glGetError() == 0)
            }
        }
    }

    companion object {
        private const val FLOAT_SIZE = 4
        private const val FIXED_SIZE = 4
        private const val CHAR_SIZE = 2
        fun beginDrawing(gl: GL10, useTexture: Boolean) {
            gl.glEnableClientState(GL10.GL_VERTEX_ARRAY)
            if (useTexture) {
                gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY)
                gl.glEnable(GL10.GL_TEXTURE_2D)
            } else {
                gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY)
                gl.glDisable(GL10.GL_TEXTURE_2D)
            }
        }

        @JvmStatic
        fun endDrawing(gl: GL10) {
            gl.glDisableClientState(GL10.GL_VERTEX_ARRAY)
        }
    }

    init {
        val vertsAcross = quadsAcross * 2
        val vertsDown = quadsDown * 2
        require(!(vertsAcross < 0 || vertsAcross >= 65536)) { "quadsAcross" }
        require(!(vertsDown < 0 || vertsDown >= 65536)) { "quadsDown" }
        require(vertsAcross * vertsDown < 65536) { "quadsAcross * quadsDown >= 32768" }
        useHardwareBuffers = false
        mVertsAcross = vertsAcross
        mVertsDown = vertsDown
        val size = vertsAcross * vertsDown
        if (useFixedPoint) {
            fixedVertexBuffer = ByteBuffer.allocateDirect(FIXED_SIZE * size * 3)
                    .order(ByteOrder.nativeOrder()).asIntBuffer()
            fixedTexCoordBuffer = ByteBuffer.allocateDirect(FIXED_SIZE * size * 2)
                    .order(ByteOrder.nativeOrder()).asIntBuffer()
            vertexBuffer = fixedVertexBuffer
            texCoordBuffer = fixedTexCoordBuffer
            coordinateSize = FIXED_SIZE
            coordinateType = GL10.GL_FIXED
        } else {
            floatVertexBuffer = ByteBuffer.allocateDirect(FLOAT_SIZE * size * 3)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer()
            floatTexCoordBuffer = ByteBuffer.allocateDirect(FLOAT_SIZE * size * 2)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer()
            vertexBuffer = floatVertexBuffer
            texCoordBuffer = floatTexCoordBuffer
            coordinateSize = FLOAT_SIZE
            coordinateType = GL10.GL_FLOAT
        }
        val quadCount = quadsAcross * quadsDown
        val indexCount = quadCount * 6
        mIndexCount = indexCount
        indexBuffer = ByteBuffer.allocateDirect(CHAR_SIZE * indexCount)
                .order(ByteOrder.nativeOrder()).asCharBuffer()

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
                val a = (indexY * mVertsAcross + indexX).toChar()
                val b = (indexY * mVertsAcross + indexX + 1).toChar()
                val c = ((indexY + 1) * mVertsAcross + indexX).toChar()
                val d = ((indexY + 1) * mVertsAcross + indexX + 1).toChar()
                indexBuffer.put(i++, a)
                indexBuffer.put(i++, b)
                indexBuffer.put(i++, c)
                indexBuffer.put(i++, b)
                indexBuffer.put(i++, c)
                indexBuffer.put(i++, d)
            }
        }

        vertBufferIndex = 0
    }
}
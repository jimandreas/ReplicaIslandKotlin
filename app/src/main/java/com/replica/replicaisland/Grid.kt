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
    private var mFloatVertexBuffer: FloatBuffer? = null
    private var mFloatTexCoordBuffer: FloatBuffer? = null
    private var mFixedVertexBuffer: IntBuffer? = null
    private var mFixedTexCoordBuffer: IntBuffer? = null
    private val mIndexBuffer: CharBuffer
    private var mVertexBuffer: Buffer? = null
    private var mTexCoordBuffer: Buffer? = null
    private var mCoordinateSize = 0
    private var mCoordinateType = 0
    private val mVertsAcross: Int
    private val mVertsDown: Int
    private val mIndexCount: Int
    private var mUseHardwareBuffers: Boolean
    private var mVertBufferIndex: Int
    private var mIndexBufferIndex = 0
    private var mTextureCoordBufferIndex = 0

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
        if (mCoordinateType == GL10.GL_FLOAT) {
            mFloatVertexBuffer!!.put(posIndex, x)
            mFloatVertexBuffer!!.put(posIndex + 1, y)
            mFloatVertexBuffer!!.put(posIndex + 2, z)
            mFloatTexCoordBuffer!!.put(texIndex, u)
            mFloatTexCoordBuffer!!.put(texIndex + 1, v)
        } else {
            mFixedVertexBuffer!!.put(posIndex, (x * (1 shl 16)).toInt())
            mFixedVertexBuffer!!.put(posIndex + 1, (y * (1 shl 16)).toInt())
            mFixedVertexBuffer!!.put(posIndex + 2, (z * (1 shl 16)).toInt())
            mFixedTexCoordBuffer!!.put(texIndex, (u * (1 shl 16)).toInt())
            mFixedTexCoordBuffer!!.put(texIndex + 1, (v * (1 shl 16)).toInt())
        }
    }

    fun beginDrawingStrips(gl: GL10, useTexture: Boolean) {
        beginDrawing(gl, useTexture)
        if (!mUseHardwareBuffers) {
            gl.glVertexPointer(3, mCoordinateType, 0, mVertexBuffer)
            if (useTexture) {
                gl.glTexCoordPointer(2, mCoordinateType, 0, mTexCoordBuffer)
            }
        } else {
            val gl11 = gl as GL11
            // draw using hardware buffers
            gl11.glBindBuffer(GL11.GL_ARRAY_BUFFER, mVertBufferIndex)
            gl11.glVertexPointer(3, mCoordinateType, 0, 0)
            gl11.glBindBuffer(GL11.GL_ARRAY_BUFFER, mTextureCoordBufferIndex)
            gl11.glTexCoordPointer(2, mCoordinateType, 0, 0)
            gl11.glBindBuffer(GL11.GL_ELEMENT_ARRAY_BUFFER, mIndexBufferIndex)
        }
    }

    // Assumes beginDrawingStrips() has been called before this.
    fun drawStrip(gl: GL10, useTexture: Boolean, startIndex: Int, indexCount: Int) {
        var count = indexCount
        if (startIndex + indexCount >= mIndexCount) {
            count = mIndexCount - startIndex
        }
        if (!mUseHardwareBuffers) {
            gl.glDrawElements(GL10.GL_TRIANGLES, count,
                    GL10.GL_UNSIGNED_SHORT, mIndexBuffer.position(startIndex))
        } else {
            val gl11 = gl as GL11
            gl11.glDrawElements(GL11.GL_TRIANGLES, count,
                    GL11.GL_UNSIGNED_SHORT, startIndex * CHAR_SIZE)
        }
    }

    fun draw(gl: GL10, useTexture: Boolean) {
        if (!mUseHardwareBuffers) {
            gl.glVertexPointer(3, mCoordinateType, 0, mVertexBuffer)
            if (useTexture) {
                gl.glTexCoordPointer(2, mCoordinateType, 0, mTexCoordBuffer)
            }
            gl.glDrawElements(GL10.GL_TRIANGLES, mIndexCount,
                    GL10.GL_UNSIGNED_SHORT, mIndexBuffer)
        } else {
            val gl11 = gl as GL11
            // draw using hardware buffers
            gl11.glBindBuffer(GL11.GL_ARRAY_BUFFER, mVertBufferIndex)
            gl11.glVertexPointer(3, mCoordinateType, 0, 0)
            gl11.glBindBuffer(GL11.GL_ARRAY_BUFFER, mTextureCoordBufferIndex)
            gl11.glTexCoordPointer(2, mCoordinateType, 0, 0)
            gl11.glBindBuffer(GL11.GL_ELEMENT_ARRAY_BUFFER, mIndexBufferIndex)
            gl11.glDrawElements(GL11.GL_TRIANGLES, mIndexCount,
                    GL11.GL_UNSIGNED_SHORT, 0)
            gl11.glBindBuffer(GL11.GL_ARRAY_BUFFER, 0)
            gl11.glBindBuffer(GL11.GL_ELEMENT_ARRAY_BUFFER, 0)
        }
    }

    fun usingHardwareBuffers(): Boolean {
        return mUseHardwareBuffers
    }

    /**
     * When the OpenGL ES device is lost, GL handles become invalidated.
     * In that case, we just want to "forget" the old handles (without
     * explicitly deleting them) and make new ones.
     */
    fun invalidateHardwareBuffers() {
        mVertBufferIndex = 0
        mIndexBufferIndex = 0
        mTextureCoordBufferIndex = 0
        mUseHardwareBuffers = false
    }

    /**
     * Deletes the hardware buffers allocated by this object (if any).
     */
    fun releaseHardwareBuffers(gl: GL10?) {
        if (mUseHardwareBuffers) {
            if (gl is GL11) {
                val buffer = IntArray(1)
                buffer[0] = mVertBufferIndex
                gl.glDeleteBuffers(1, buffer, 0)
                buffer[0] = mTextureCoordBufferIndex
                gl.glDeleteBuffers(1, buffer, 0)
                buffer[0] = mIndexBufferIndex
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
        if (!mUseHardwareBuffers) {
            DebugLog.i("Grid", "Using Hardware Buffers")
            if (gl is GL11) {
                val buffer = IntArray(1)

                // Allocate and fill the vertex buffer.
                gl.glGenBuffers(1, buffer, 0)
                mVertBufferIndex = buffer[0]
                gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, mVertBufferIndex)
                val vertexSize = mVertexBuffer!!.capacity() * mCoordinateSize
                gl.glBufferData(GL11.GL_ARRAY_BUFFER, vertexSize,
                        mVertexBuffer, GL11.GL_STATIC_DRAW)

                // Allocate and fill the texture coordinate buffer.
                gl.glGenBuffers(1, buffer, 0)
                mTextureCoordBufferIndex = buffer[0]
                gl.glBindBuffer(GL11.GL_ARRAY_BUFFER,
                        mTextureCoordBufferIndex)
                val texCoordSize = mTexCoordBuffer!!.capacity() * mCoordinateSize
                gl.glBufferData(GL11.GL_ARRAY_BUFFER, texCoordSize,
                        mTexCoordBuffer, GL11.GL_STATIC_DRAW)

                // Unbind the array buffer.
                gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, 0)

                // Allocate and fill the index buffer.
                gl.glGenBuffers(1, buffer, 0)
                mIndexBufferIndex = buffer[0]
                gl.glBindBuffer(GL11.GL_ELEMENT_ARRAY_BUFFER,
                        mIndexBufferIndex)
                // A char is 2 bytes.
                val indexSize = mIndexBuffer.capacity() * 2
                gl.glBufferData(GL11.GL_ELEMENT_ARRAY_BUFFER, indexSize, mIndexBuffer,
                        GL11.GL_STATIC_DRAW)

                // Unbind the element array buffer.
                gl.glBindBuffer(GL11.GL_ELEMENT_ARRAY_BUFFER, 0)
                mUseHardwareBuffers = true
                // TODO: fix assertions
//                assert(mVertBufferIndex != 0)
//                assert(mTextureCoordBufferIndex != 0)
//                assert(mIndexBufferIndex != 0)
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
        mUseHardwareBuffers = false
        mVertsAcross = vertsAcross
        mVertsDown = vertsDown
        val size = vertsAcross * vertsDown
        if (useFixedPoint) {
            mFixedVertexBuffer = ByteBuffer.allocateDirect(FIXED_SIZE * size * 3)
                    .order(ByteOrder.nativeOrder()).asIntBuffer()
            mFixedTexCoordBuffer = ByteBuffer.allocateDirect(FIXED_SIZE * size * 2)
                    .order(ByteOrder.nativeOrder()).asIntBuffer()
            mVertexBuffer = mFixedVertexBuffer
            mTexCoordBuffer = mFixedTexCoordBuffer
            mCoordinateSize = FIXED_SIZE
            mCoordinateType = GL10.GL_FIXED
        } else {
            mFloatVertexBuffer = ByteBuffer.allocateDirect(FLOAT_SIZE * size * 3)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer()
            mFloatTexCoordBuffer = ByteBuffer.allocateDirect(FLOAT_SIZE * size * 2)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer()
            mVertexBuffer = mFloatVertexBuffer
            mTexCoordBuffer = mFloatTexCoordBuffer
            mCoordinateSize = FLOAT_SIZE
            mCoordinateType = GL10.GL_FLOAT
        }
        val quadCount = quadsAcross * quadsDown
        val indexCount = quadCount * 6
        mIndexCount = indexCount
        mIndexBuffer = ByteBuffer.allocateDirect(CHAR_SIZE * indexCount)
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
                mIndexBuffer.put(i++, a)
                mIndexBuffer.put(i++, b)
                mIndexBuffer.put(i++, c)
                mIndexBuffer.put(i++, b)
                mIndexBuffer.put(i++, c)
                mIndexBuffer.put(i++, d)
            }
        }

        mVertBufferIndex = 0
    }
}
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

@file:Suppress("unused", "SameParameterValue")

package com.replica.replicaisland.rendering

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLUtils
import com.replica.replicaisland.ui.DebugLog
import com.replica.replicaisland.core.BaseObject
import java.io.IOException

/**
 * The Texture Library manages all textures in the game.  Textures are pooled and handed out to
 * requesting parties via allocateTexture().  However, the texture data itself is not immediately
 * loaded at that time; it may have already been loaded or it may be loaded in the future via
 * a call to loadTexture() or loadAllTextures().  This allows Texture objects to be dispersed to
 * various game systems and while the texture data itself is streamed in or loaded as necessary.
 */
class TextureLibrary : BaseObject() {
    private var textureHash: Array<Texture?> = arrayOfNulls(DEFAULT_SIZE)
    private var textureNameWorkspace: IntArray
    override fun reset() {
        removeAll()
    }

    fun allocateTexture(resourceID: Int): Texture? {
        var texture = getTextureByResource(resourceID)
        if (texture == null) {
            texture = addTexture(resourceID, -1, 0, 0)
        }
        return texture
    }

    fun loadTexture(context: Context?, resourceID: Int): Texture {
        var texture = allocateTexture(resourceID)
        texture = loadBitmap(context, texture)
        return texture
    }

    fun loadAll(context: Context?) {
        for (x in textureHash.indices) {
            if (textureHash[x]!!.resource != -1 && !textureHash[x]!!.loaded) {
                loadBitmap(context, textureHash[x])
            }
        }
    }

    fun deleteAll() {
        for (x in textureHash.indices) {
            if (textureHash[x]!!.resource != -1 && textureHash[x]!!.loaded) {
                textureNameWorkspace[0] = textureHash[x]!!.name
                textureHash[x]!!.name = -1
                textureHash[x]!!.loaded = false
                GLES20.glDeleteTextures(1, textureNameWorkspace, 0)
            }
        }
    }

    fun invalidateAll() {
        for (x in textureHash.indices) {
            if (textureHash[x]!!.resource != -1 && textureHash[x]!!.loaded) {
                textureHash[x]!!.name = -1
                textureHash[x]!!.loaded = false
            }
        }
    }

    private fun loadBitmap(context: Context?, texture: Texture?): Texture {
        if (!texture!!.loaded && texture.resource != -1) {
            GLES20.glGenTextures(1, textureNameWorkspace, 0)
            var error = GLES20.glGetError()
            if (error != GLES20.GL_NO_ERROR) {
                DebugLog.d("Texture Load 1", "GLError: $error: ${texture.resource}")
            }
            val textureName = textureNameWorkspace[0]
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureName)
            error = GLES20.glGetError()
            if (error != GLES20.GL_NO_ERROR) {
                DebugLog.d("Texture Load 2", "GLError: $error: ${texture.resource}")
            }
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

            val inputStream = context!!.resources.openRawResource(texture.resource)
            val bitmap: Bitmap = try {
                BitmapFactory.decodeStream(inputStream)
            } finally {
                try {
                    inputStream.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
            error = GLES20.glGetError()
            if (error != GLES20.GL_NO_ERROR) {
                DebugLog.d("Texture Load 3", "GLError: $error: ${texture.resource}")
            }
            texture.name = textureName
            texture.width = bitmap.width
            texture.height = bitmap.height
            bitmap.recycle()
            error = GLES20.glGetError()
            if (error != GLES20.GL_NO_ERROR) {
                DebugLog.d("Texture Load 4", "GLError: $error: ${texture.resource}")
            }
            texture.loaded = true
        }
        return texture
    }

    fun isTextureLoaded(resourceID: Int): Boolean {
        return getTextureByResource(resourceID) != null
    }

    fun getTextureByResource(resourceID: Int): Texture? {
        val index = getHashIndex(resourceID)
        val realIndex = findFirstKey(index, resourceID)
        var texture: Texture? = null
        if (realIndex != -1) {
            texture = textureHash[realIndex]
        }
        return texture
    }

    private fun getHashIndex(id: Int): Int {
        return id % textureHash.size
    }

    private fun findFirstKey(startIndex: Int, key: Int): Int {
        var index = -1
        for (x in textureHash.indices) {
            val actualIndex = (startIndex + x) % textureHash.size
            if (textureHash[actualIndex]!!.resource == key) {
                index = actualIndex
                break
            } else if (textureHash[actualIndex]!!.resource == -1) {
                break
            }
        }
        return index
    }

    private fun addTexture(id: Int, name: Int, width: Int, height: Int): Texture? {
        val index = findFirstKey(getHashIndex(id), -1)
        var texture: Texture? = null
        if (index != -1) {
            textureHash[index]!!.resource = id
            textureHash[index]!!.name = name
            textureHash[index]!!.width = width
            textureHash[index]!!.height = height
            texture = textureHash[index]
        }
        return texture
    }

    fun removeAll() {
        for (x in textureHash.indices) {
            textureHash[x]!!.reset()
        }
    }

    companion object {
        const val DEFAULT_SIZE = 512
        var sBitmapOptions = BitmapFactory.Options()
    }

    init {
        for (x in textureHash.indices) {
            textureHash[x] = Texture()
        }
        textureNameWorkspace = IntArray(1)
        sBitmapOptions.inPreferredConfig = Bitmap.Config.RGB_565
    }
}

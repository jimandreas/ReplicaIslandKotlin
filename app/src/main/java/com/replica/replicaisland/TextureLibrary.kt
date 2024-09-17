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
@file:Suppress("unused", "SimplifyBooleanWithConstants", "SameParameterValue")

package com.replica.replicaisland

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLU
import android.opengl.GLUtils
import java.io.IOException
import javax.microedition.khronos.opengles.GL10
import javax.microedition.khronos.opengles.GL11
import javax.microedition.khronos.opengles.GL11Ext

/**
 * The Texture Library manages all textures in the game.  Textures are pooled and handed out to
 * requesting parties via allocateTexture().  However, the texture data itself is not immediately
 * loaded at that time; it may have already been loaded or it may be loaded in the future via
 * a call to loadTexture() or loadAllTextures().  This allows Texture objects to be dispersed to
 * various game systems and while the texture data itself is streamed in or loaded as necessary.
 */
class TextureLibrary : BaseObject() {
    // Textures are stored in a simple hash.  This class implements its own array-based hash rather
    // than using HashMap for performance.
    private var textureHash: Array<Texture?>
    private var textureNameWorkspace: IntArray
    private var cropWorkspace: IntArray
    override fun reset() {
        removeAll()
    }

    /**
     * Creates a Texture object that is mapped to the passed resource id.  If a texture has already
     * been allocated for this id, the previously allocated Texture object is returned.
     * @param resourceID
     * @return
     */
    fun allocateTexture(resourceID: Int): Texture? {
        var texture = getTextureByResource(resourceID)
        if (texture == null) {
            texture = addTexture(resourceID, -1, 0, 0)
        }
        return texture
    }

    /** Loads a single texture into memory.  Does nothing if the texture is already loaded.  */
    fun loadTexture(context: Context?, gl: GL10?, resourceID: Int): Texture? {
        var texture = allocateTexture(resourceID)
        texture = loadBitmap(context, gl, texture)
        return texture
    }

    /** Loads all unloaded textures into OpenGL memory.  Already-loaded textures are ignored.  */
    fun loadAll(context: Context?, gl: GL10?) {
        for (x in textureHash.indices) {
            if (textureHash[x]!!.resource != -1 && textureHash[x]!!.loaded == false) {
                loadBitmap(context, gl, textureHash[x])
            }
        }
    }

    /** Flushes all textures from OpenGL memory  */
    fun deleteAll(gl: GL10) {
        for (x in textureHash.indices) {
            if (textureHash[x]!!.resource != -1 && textureHash[x]!!.loaded) {
                //TODO: assert(textureHash[x]!!.name != -1)
                textureNameWorkspace[0] = textureHash[x]!!.name
                textureHash[x]!!.name = -1
                textureHash[x]!!.loaded = false
                gl.glDeleteTextures(1, textureNameWorkspace, 0)
                val error = gl.glGetError()
                if (error != GL10.GL_NO_ERROR) {
                    DebugLog.d("Texture Delete", "GLError: " + error + " (" + GLU.gluErrorString(error) + "): " + textureHash[x]!!.resource)
                }
                //TODO: assert(error == GL10.GL_NO_ERROR)
            }
        }
    }

    /** Marks all textures as unloaded  */
    fun invalidateAll() {
        for (x in textureHash.indices) {
            if (textureHash[x]!!.resource != -1 && textureHash[x]!!.loaded) {
                textureHash[x]!!.name = -1
                textureHash[x]!!.loaded = false
            }
        }
    }

    /** Loads a bitmap into OpenGL and sets up the common parameters for 2D texture maps.  */
    private fun loadBitmap(context: Context?, gl: GL10?, texture: Texture?): Texture? {
        //TODO: assert(gl != null)
        //TODO: assert(context != null)
        //TODO: assert(texture != null)
        if (texture!!.loaded == false && texture.resource != -1) {
            gl!!.glGenTextures(1, textureNameWorkspace, 0)
            var error = gl.glGetError()
            if (error != GL10.GL_NO_ERROR) {
                DebugLog.d("Texture Load 1", "GLError: " + error + " (" + GLU.gluErrorString(error) + "): " + texture.resource)
            }
            //TODO: assert(error == GL10.GL_NO_ERROR)
            val textureName = textureNameWorkspace[0]
            gl.glBindTexture(GL10.GL_TEXTURE_2D, textureName)
            error = gl.glGetError()
            if (error != GL10.GL_NO_ERROR) {
                DebugLog.d("Texture Load 2", "GLError: " + error + " (" + GLU.gluErrorString(error) + "): " + texture.resource)
            }
            //TODO: assert(error == GL10.GL_NO_ERROR)
            gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST.toFloat())
            gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR.toFloat())
            gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE.toFloat())
            gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE.toFloat())
            gl.glTexEnvf(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE, GL10.GL_MODULATE.toFloat()) //GL10.GL_REPLACE);
            val `is` = context!!.resources.openRawResource(texture.resource)
            val bitmap: Bitmap
            bitmap = try {
                BitmapFactory.decodeStream(`is`)
            } finally {
                try {
                    `is`.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                    // Ignore.
                }
            }
            GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0)
            error = gl.glGetError()
            if (error != GL10.GL_NO_ERROR) {
                DebugLog.d("Texture Load 3", "GLError: " + error + " (" + GLU.gluErrorString(error) + "): " + texture.resource)
            }
            //TODO: assert(error == GL10.GL_NO_ERROR)
            cropWorkspace[0] = 0
            cropWorkspace[1] = bitmap.height
            cropWorkspace[2] = bitmap.width
            cropWorkspace[3] = -bitmap.height
            (gl as GL11?)!!.glTexParameteriv(GL10.GL_TEXTURE_2D, GL11Ext.GL_TEXTURE_CROP_RECT_OES,
                    cropWorkspace, 0)
            texture.name = textureName
            texture.width = bitmap.width
            texture.height = bitmap.height
            bitmap.recycle()
            error = gl.glGetError()
            if (error != GL10.GL_NO_ERROR) {
                DebugLog.d("Texture Load 4", "GLError: " + error + " (" + GLU.gluErrorString(error) + "): " + texture.resource)
            }
            //TODO: assert(error == GL10.GL_NO_ERROR)
            texture.loaded = true
        }
        return texture
    }

    fun isTextureLoaded(resourceID: Int): Boolean {
        return getTextureByResource(resourceID) != null
    }

    /**
     * Returns the texture associated with the passed Android resource ID.
     * @param resourceID The resource ID of a bitmap defined in R.java.
     * @return An associated Texture object, or null if there is no associated
     * texture in the library.
     */
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

    /**
     * Locates the texture in the hash.  This hash uses a simple linear probe chaining mechanism:
     * if the hash slot is occupied by some other entry, the next empty array index is used.
     * This is O(n) for the worst case (every slot is a cache miss) but the average case is
     * constant time.
     * @param startIndex
     * @param key
     * @return
     */
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

    /** Inserts a texture into the hash  */
    private fun addTexture(id: Int, name: Int, width: Int, height: Int): Texture? {
        val index = findFirstKey(getHashIndex(id), -1)
        var texture: Texture? = null
        //TODO: assert(index != -1)
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
        textureHash = arrayOfNulls(DEFAULT_SIZE)
        for (x in textureHash.indices) {
            textureHash[x] = Texture()
        }
        textureNameWorkspace = IntArray(1)
        cropWorkspace = IntArray(4)
        sBitmapOptions.inPreferredConfig = Bitmap.Config.RGB_565
    }
}
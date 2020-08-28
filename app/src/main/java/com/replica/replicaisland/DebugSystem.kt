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
@file:Suppress("SENSELESS_COMPARISON")

package com.replica.replicaisland

class DebugSystem(library: TextureLibrary?) : BaseObject() {
    private var mRedBoxTexture: Texture? = null
    private var mBlueBoxTexture: Texture? = null
    private var mOutlineBoxTexture: Texture? = null
    private var mRedCircleTexture: Texture? = null
    private var mBlueCircleTexture: Texture? = null
    private var mOutlineCircleTexture: Texture? = null
    private val mWorkVector: Vector2
    override fun reset() {}
    fun drawShape(x: Float, y: Float, width: Float, height: Float, shapeType: Int, colorType: Int) {
        val render = sSystemRegistry.renderSystem
        val factory = sSystemRegistry.drawableFactory
        val camera = sSystemRegistry.cameraSystem
        val params = sSystemRegistry.contextParameters
        mWorkVector[x] = y
        mWorkVector.x = (mWorkVector.x - camera!!.fetchFocusPositionX()
                + params!!.gameWidth / 2)
        mWorkVector.y = (mWorkVector.y - camera.fetchFocusPositionY()
                + params.gameHeight / 2)
        if (mWorkVector.x + width >= 0.0f && mWorkVector.x < params.gameWidth && mWorkVector.y + height >= 0.0f && mWorkVector.y < params.gameHeight) {
            val bitmap = factory!!.allocateDrawableBitmap()
            if (bitmap != null) {
                val texture = getTexture(shapeType, colorType)
                bitmap.resize(texture!!.width, texture.height)
                // TODO: scale stretch hack.  fix!
                bitmap.width = width.toInt()
                bitmap.height = height.toInt()
                bitmap.texture = texture
                mWorkVector[x] = y
                render!!.scheduleForDraw(bitmap, mWorkVector, SortConstants.HUD, true)
            }
        }
    }

    private fun getTexture(shapeType: Int, colorType: Int): Texture? {
        var result: Texture? = null
        if (shapeType == SHAPE_BOX) {
            when (colorType) {
                COLOR_RED -> result = mRedBoxTexture
                COLOR_BLUE -> result = mBlueBoxTexture
                COLOR_OUTLINE -> result = mOutlineBoxTexture
            }
        } else if (shapeType == SHAPE_CIRCLE) {
            when (colorType) {
                COLOR_RED -> result = mRedCircleTexture
                COLOR_BLUE -> result = mBlueCircleTexture
                COLOR_OUTLINE -> result = mOutlineCircleTexture
            }
        }
        return result
    }

    companion object {
        const val COLOR_RED = 0
        const val COLOR_BLUE = 1
        const val COLOR_OUTLINE = 2
        const val SHAPE_BOX = 0
        const val SHAPE_CIRCLE = 1
    }

    init {
        if (library != null) {
            mRedBoxTexture = library.allocateTexture(R.drawable.debug_box_red)
            mBlueBoxTexture = library.allocateTexture(R.drawable.debug_box_blue)
            mOutlineBoxTexture = library.allocateTexture(R.drawable.debug_box_outline)
            mRedCircleTexture = library.allocateTexture(R.drawable.debug_circle_red)
            mBlueCircleTexture = library.allocateTexture(R.drawable.debug_circle_blue)
            mOutlineCircleTexture = library.allocateTexture(R.drawable.debug_circle_outline)
        }
        mWorkVector = Vector2()
    }
}
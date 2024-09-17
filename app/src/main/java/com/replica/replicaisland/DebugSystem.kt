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
@file:Suppress("SENSELESS_COMPARISON")

package com.replica.replicaisland

class DebugSystem(library: TextureLibrary?) : BaseObject() {
    private var redBoxTexture: Texture? = null
    private var blueBoxTexture: Texture? = null
    private var outlineBoxTexture: Texture? = null
    private var redCircleTexture: Texture? = null
    private var blueCircleTexture: Texture? = null
    private var outlineCircleTexture: Texture? = null
    private val workVector: Vector2
    override fun reset() {}
    fun drawShape(x: Float, y: Float, width: Float, height: Float, shapeType: Int, colorType: Int) {
        val render = sSystemRegistry.renderSystem
        val factory = sSystemRegistry.drawableFactory
        val camera = sSystemRegistry.cameraSystem
        val params = sSystemRegistry.contextParameters
        workVector[x] = y
        workVector.x = (workVector.x - camera!!.fetchFocusPositionX()
                + params!!.gameWidth / 2)
        workVector.y = (workVector.y - camera.fetchFocusPositionY()
                + params.gameHeight / 2)
        if (workVector.x + width >= 0.0f && workVector.x < params.gameWidth && workVector.y + height >= 0.0f && workVector.y < params.gameHeight) {
            val bitmap = factory!!.allocateDrawableBitmap()
            if (bitmap != null) {
                val texture = getTexture(shapeType, colorType)
                bitmap.resize(texture!!.width, texture.height)
                // TODO: scale stretch hack.  fix!
                bitmap.width = width.toInt()
                bitmap.height = height.toInt()
                bitmap.texture = texture
                workVector[x] = y
                render!!.scheduleForDraw(bitmap, workVector, SortConstants.HUD, true)
            }
        }
    }

    private fun getTexture(shapeType: Int, colorType: Int): Texture? {
        var result: Texture? = null
        if (shapeType == SHAPE_BOX) {
            when (colorType) {
                COLOR_RED -> result = redBoxTexture
                COLOR_BLUE -> result = blueBoxTexture
                COLOR_OUTLINE -> result = outlineBoxTexture
            }
        } else if (shapeType == SHAPE_CIRCLE) {
            when (colorType) {
                COLOR_RED -> result = redCircleTexture
                COLOR_BLUE -> result = blueCircleTexture
                COLOR_OUTLINE -> result = outlineCircleTexture
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
            redBoxTexture = library.allocateTexture(R.drawable.debug_box_red)
            blueBoxTexture = library.allocateTexture(R.drawable.debug_box_blue)
            outlineBoxTexture = library.allocateTexture(R.drawable.debug_box_outline)
            redCircleTexture = library.allocateTexture(R.drawable.debug_circle_red)
            blueCircleTexture = library.allocateTexture(R.drawable.debug_circle_blue)
            outlineCircleTexture = library.allocateTexture(R.drawable.debug_circle_outline)
        }
        workVector = Vector2()
    }
}
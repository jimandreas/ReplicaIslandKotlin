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
package com.replica.replicaisland

import javax.microedition.khronos.opengles.GL10
import javax.microedition.khronos.opengles.GL11Ext

/**
 * Draws a screen-aligned bitmap to the screen.
 */
open class DrawableBitmap internal constructor(override var texture: Texture?, var width: Int, var height: Int) : DrawableObject() {
    val crop: IntArray = IntArray(4)
    private var mViewWidth: Int
    private var mViewHeight: Int
    private var mOpacity: Float
    open fun reset() {
        texture = null
        mViewWidth = 0
        mViewHeight = 0
        mOpacity = 1.0f
    }

    fun setViewSize(width: Int, height: Int) {
        mViewHeight = height
        mViewWidth = width
    }

    fun setOpacity(opacity: Float) {
        mOpacity = opacity
    }

    /**
     * Draw the bitmap at a given x,y position, expressed in pixels, with the
     * lower-left-hand-corner of the view being (0,0).
     *
     * gl  A pointer to the OpenGL context
     * @param x  The number of pixels to offset this drawable's origin in the x-axis.
     * @param y  The number of pixels to offset this drawable's origin in the y-axis
     * @param scaleX The horizontal scale factor between the bitmap resolution and the display resolution.
     * @param scaleY The vertical scale factor between the bitmap resolution and the display resolution.
     */
    override fun draw(x: Float, y: Float, scaleX: Float, scaleY: Float) {
        val gl = OpenGLSystem.gL
        val texture = texture
        if (gl != null && texture != null) {
            // TODO: assert(texture.loaded)
            val snappedX: Float = x
            val snappedY: Float = y
            val opacity = mOpacity
            val width = width.toFloat()
            val height = height.toFloat()
            val viewWidth = mViewWidth.toFloat()
            val viewHeight = mViewHeight.toFloat()
            var cull = false
            if (viewWidth > 0) {
                if (snappedX + width < 0.0f || snappedX > viewWidth || snappedY + height < 0.0f || snappedY > viewHeight || opacity == 0.0f || !texture.loaded) {
                    cull = true
                }
            }
            if (!cull) {
                OpenGLSystem.bindTexture(GL10.GL_TEXTURE_2D, texture.name)

                // This is necessary because we could be drawing the same texture with different
                // crop (say, flipped horizontally) on the same frame.
                OpenGLSystem.setTextureCrop(crop)
                if (opacity < 1.0f) {
                    gl.glColor4f(opacity, opacity, opacity, opacity)
                }
                (gl as GL11Ext).glDrawTexfOES(snappedX * scaleX, snappedY * scaleY,
                        priority, width * scaleX, height * scaleY)
                if (opacity < 1.0f) {
                    gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f)
                }
            }
        }
    }

    fun resize(width: Int, height: Int) {
        this.width = width
        this.height = height
        setCrop(0, height, width, height)
    }

    /**
     * Changes the crop parameters of this bitmap.  Note that the underlying OpenGL texture's
     * parameters are not changed immediately The crop is updated on the
     * next call to draw().  Note that the image may be flipped by providing a negative width or
     * height.
     *
     */
    fun setCrop(left: Int, bottom: Int, width: Int, height: Int) {
        // Negative width and height values will flip the image.
        crop[0] = left
        crop[1] = bottom
        crop[2] = width
        crop[3] = -height
    }

    override fun visibleAtPosition(position: Vector2?): Boolean {
        var cull = false
        if (mViewWidth > 0) {
            if (position!!.x + width < 0 || position.x > mViewWidth || position.y + height < 0 || position.y > mViewHeight) {
                cull = true
            }
        }
        return !cull
    }

    fun setFlip(horzFlip: Boolean, vertFlip: Boolean) {
        setCrop(if (horzFlip) width else 0,
                if (vertFlip) 0 else height,
                if (horzFlip) -width else width,
                if (vertFlip) -height else height)
    }

    companion object {
        /**
         * Begins drawing bitmaps. Sets the OpenGL state for rapid drawing.
         *
         * @param gl  A pointer to the OpenGL context.
         * @param viewWidth  The width of the screen.
         * @param viewHeight  The height of the screen.
         */
        @JvmStatic
        fun beginDrawing(gl: GL10, viewWidth: Float, viewHeight: Float) {
            gl.glShadeModel(GL10.GL_FLAT)
            gl.glEnable(GL10.GL_BLEND)
            gl.glBlendFunc(GL10.GL_ONE, GL10.GL_ONE_MINUS_SRC_ALPHA)
            gl.glColor4x(0x10000, 0x10000, 0x10000, 0x10000)
            gl.glMatrixMode(GL10.GL_PROJECTION)
            gl.glPushMatrix()
            gl.glLoadIdentity()
            gl.glOrthof(0.0f, viewWidth, 0.0f, viewHeight, 0.0f, 1.0f)
            gl.glMatrixMode(GL10.GL_MODELVIEW)
            gl.glPushMatrix()
            gl.glLoadIdentity()
            gl.glEnable(GL10.GL_TEXTURE_2D)
        }

        /**
         * Ends the drawing and restores the OpenGL state.
         *
         * @param gl  A pointer to the OpenGL context.
         */
        @JvmStatic
        fun endDrawing(gl: GL10) {
            gl.glDisable(GL10.GL_BLEND)
            gl.glMatrixMode(GL10.GL_PROJECTION)
            gl.glPopMatrix()
            gl.glMatrixMode(GL10.GL_MODELVIEW)
            gl.glPopMatrix()
        }
    }

    init {
        mViewWidth = 0
        mViewHeight = 0
        mOpacity = 1.0f
        setCrop(0, height, width, height)
    }
}
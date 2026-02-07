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
import com.replica.replicaisland.utils.Vector2

/**
 * Draws a screen-aligned bitmap to the screen using OpenGL ES 2.0.
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
     * @param x  The number of pixels to offset this drawable's origin in the x-axis.
     * @param y  The number of pixels to offset this drawable's origin in the y-axis
     * @param scaleX The horizontal scale factor between the bitmap resolution and the display resolution.
     * @param scaleY The vertical scale factor between the bitmap resolution and the display resolution.
     */
    override fun draw(x: Float, y: Float, scaleX: Float, scaleY: Float) {
        val texture = texture ?: return
        val shader = OpenGLSystem.spriteShader ?: return
        val quad = OpenGLSystem.spriteQuad ?: return
        val matrix = OpenGLSystem.matrixHelper ?: return

        val opacity = mOpacity
        val w = width.toFloat()
        val h = height.toFloat()
        val viewWidth = mViewWidth.toFloat()
        val viewHeight = mViewHeight.toFloat()

        // Culling
        if (viewWidth > 0) {
            if (x + w < 0.0f || x > viewWidth || y + h < 0.0f || y > viewHeight || opacity == 0.0f || !texture.loaded) {
                return
            }
        }

        OpenGLSystem.bindTexture(GLES20.GL_TEXTURE_2D, texture.name)

        // Convert crop array to UV coordinates.
        // crop = [left, bottom, width, -height] where negative width/height = flip.
        // GLUtils.texImage2D stores bitmaps with V=0 at the image top, so
        // bottom vertices get higher V (image bottom) and top vertices get lower V (image top).
        val texW = texture.width.toFloat()
        val texH = texture.height.toFloat()
        val u0 = crop[0] / texW
        val u1 = (crop[0] + crop[2]) / texW
        val v0 = crop[1] / texH
        val v1 = (crop[1] + crop[3]) / texH
        quad.setUVs(u0, v0, u1, v1)

        // Model matrix: translate to position, scale to pixel size.
        // The orthographic projection is in game coordinates; the viewport
        // handles scaling to physical pixels, so scaleX/scaleY are not applied here.
        matrix.setIdentityModel()
        matrix.translateModel(x, y, 0f)
        matrix.scaleModel(w, h, 1f)

        // Set MVP uniform
        GLES20.glUniformMatrix4fv(shader.uMVPMatrixLocation, 1, false, matrix.computeMVP(), 0)

        // Set color uniform (pre-multiplied alpha)
        GLES20.glUniform4f(shader.uColorLocation, opacity, opacity, opacity, opacity)

        quad.draw(shader.aPositionLocation, shader.aTexCoordLocation)
    }

    fun resize(width: Int, height: Int) {
        this.width = width
        this.height = height
        setCrop(0, height, width, height)
    }

    /**
     * Changes the crop parameters of this bitmap.  Note that the image may be flipped by
     * providing a negative width or height.
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
         * @param viewWidth  The width of the screen.
         * @param viewHeight  The height of the screen.
         */
        @JvmStatic
        fun beginDrawing(viewWidth: Float, viewHeight: Float) {
            val shader = OpenGLSystem.spriteShader ?: return
            val matrix = OpenGLSystem.matrixHelper ?: return

            shader.use()
            GLES20.glEnable(GLES20.GL_BLEND)
            GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA)

            // Set orthographic projection
            matrix.setOrthographicProjection(0f, viewWidth, 0f, viewHeight, -1f, 1f)

            // Bind texture unit 0
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glUniform1i(shader.uTextureLocation, 0)
        }

        /**
         * Ends the drawing and restores the OpenGL state.
         */
        @JvmStatic
        fun endDrawing() {
            GLES20.glDisable(GLES20.GL_BLEND)
        }
    }

    init {
        mViewWidth = 0
        mViewHeight = 0
        mOpacity = 1.0f
        setCrop(0, height, width, height)
    }
}

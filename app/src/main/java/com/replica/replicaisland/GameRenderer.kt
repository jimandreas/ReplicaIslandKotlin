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
@file:Suppress("UNCHECKED_CAST", "LocalVariableName")

package com.replica.replicaisland

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.SystemClock
import com.replica.replicaisland.core.BaseObject
import com.replica.replicaisland.rendering.DrawableBitmap
import com.replica.replicaisland.rendering.MatrixHelper
import com.replica.replicaisland.rendering.OpenGLSystem
import com.replica.replicaisland.rendering.RenderSystem
import com.replica.replicaisland.rendering.ShaderProgram
import com.replica.replicaisland.rendering.SpriteQuad
import com.replica.replicaisland.rendering.TextureLibrary
import com.replica.replicaisland.ui.DebugLog
import com.replica.replicaisland.utils.BufferLibrary
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * GameRenderer the top-level rendering interface for the game engine.  It is called by
 * GLSurfaceView and is responsible for submitting commands to OpenGL ES 2.0.  GameRenderer
 * receives a queue of renderable objects from the thread and uses that to draw the scene every
 * frame.  If no queue is available then no drawing is performed.  If the queue is not changed
 * from frame to frame, the same scene will be redrawn every frame.
 * The GameRenderer also invokes texture loads when it is activated.
 */
class GameRenderer(private var mContext: Context, private val mGame: Game, private val mWidth: Int, private val mHeight: Int) : GLSurfaceView.Renderer {
    private val mHalfWidth: Int = mWidth / 2
    private val mHalfHeight: Int = mHeight / 2
    private var mScaleX: Float
    private var mScaleY: Float
    private var lastTime: Long = 0
    private var profileFrames = 0
    private var profileWaitTime: Long = 0
    private var profileFrameTime: Long = 0
    private var profileSubmitTime: Long = 0
    private var profileObjectCount = 0
    private var drawQueue: ObjectManager? = null
    private var drawQueueChanged: Boolean
    private val drawLock: Any
    private var mCameraX: Float
    private var mCameraY: Float
    private var callbackRequested: Boolean

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // When a new EGL context is created, all previous GL resources are invalidated.
        // Notify the game so it can mark textures/buffers as needing reload.
        mGame.onSurfaceLost()

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1f)
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glDisable(GLES20.GL_DITHER)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        // Compile shaders
        val shader = ShaderProgram()
        if (shader.compile()) {
            OpenGLSystem.spriteShader = shader
            DebugLog.i("Graphics", "ES 2.0 shaders compiled successfully")
        } else {
            DebugLog.e("Graphics", "Failed to compile ES 2.0 shaders!")
        }

        // Initialize sprite quad VBO
        val quad = SpriteQuad()
        quad.initialize()
        OpenGLSystem.spriteQuad = quad

        // Initialize matrix helper
        OpenGLSystem.matrixHelper = MatrixHelper()

        val version = GLES20.glGetString(GLES20.GL_VERSION)
        val renderer = GLES20.glGetString(GLES20.GL_RENDERER)
        DebugLog.i("Graphics Support", "$version ($renderer): ES 2.0 mode")

        mGame.onSurfaceCreated()
    }

    fun loadTextures(library: TextureLibrary?) {
        library!!.loadAll(mContext)
        DebugLog.d("AndouKun", "Textures Loaded.")
    }

    fun flushTextures(library: TextureLibrary?) {
        library!!.deleteAll()
        DebugLog.d("AndouKun", "Textures Unloaded.")
    }

    fun loadBuffers(library: BufferLibrary?) {
        library!!.generateHardwareBuffers()
        DebugLog.d("AndouKun", "Buffers Created.")
    }

    fun flushBuffers(library: BufferLibrary?) {
        library!!.releaseHardwareBuffers()
        DebugLog.d("AndouKun", "Buffers Released.")
    }

    fun requestCallback() {
        callbackRequested = true
    }

    /** Draws the scene.  Note that the draw queue is locked for the duration of this function.  */
    override fun onDrawFrame(gl: GL10?) {
        val time = SystemClock.uptimeMillis()
        val time_delta = time - lastTime
        synchronized(drawLock) {
            if (!drawQueueChanged) {
                while (!drawQueueChanged) {
                    try {
                        drawLock.wait()
                    } catch (_: InterruptedException) {
                    }
                }
            }
            drawQueueChanged = false
        }
        val wait = SystemClock.uptimeMillis()
        if (callbackRequested) {
            mGame.onSurfaceReady()
            callbackRequested = false
        }

        OpenGLSystem.resetBindings()
        DrawableBitmap.beginDrawing(mWidth.toFloat(), mHeight.toFloat())

        synchronized(this) {
            if (drawQueue != null && drawQueue!!.fetchObjects().count > 0) {
                val objects = drawQueue!!.fetchObjects()
                val objectArray: Array<Any?> = objects.array as Array<Any?>
                val count = objects.count
                val scaleX = mScaleX
                val scaleY = mScaleY
                val halfWidth = mHalfWidth.toFloat()
                val halfHeight = mHalfHeight.toFloat()
                profileObjectCount += count
                for (i in 0 until count) {
                    val element = objectArray[i] as RenderSystem.RenderElement?
                    var x = element!!.x
                    var y = element.y
                    if (element.cameraRelative) {
                        x = x - mCameraX + halfWidth
                        y = y - mCameraY + halfHeight
                    }
                    element.mDrawable!!.draw(x, y, scaleX, scaleY)
                }
            }
            // When drawQueue is null (during level transitions), skip clearing
            // so the last rendered frame stays visible instead of flashing black.
        }

        DrawableBitmap.endDrawing()

        val time2 = SystemClock.uptimeMillis()
        lastTime = time2
        profileFrameTime += time_delta
        profileSubmitTime += time2 - time
        profileWaitTime += wait - time
        profileFrames++
        if (profileFrameTime > PROFILE_REPORT_DELAY) {
            val validFrames = profileFrames
            val averageFrameTime = profileFrameTime / validFrames
            val averageSubmitTime = profileSubmitTime / validFrames
            val averageObjectsPerFrame = profileObjectCount.toFloat() / validFrames
            val averageWaitTime = profileWaitTime / validFrames
            DebugLog.d("Render Profile",
                    "Average Submit: " + averageSubmitTime
                            + "  Average Draw: " + averageFrameTime
                            + " Objects/Frame: " + averageObjectsPerFrame
                            + " Wait Time: " + averageWaitTime)
            profileFrameTime = 0
            profileSubmitTime = 0
            profileFrames = 0
            profileObjectCount = 0
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        DebugLog.d("AndouKun", "Surface Size Change: $width, $height")

        val scaleX = width.toFloat() / mWidth
        val scaleY = height.toFloat() / mHeight
        val viewportWidth = (mWidth * scaleX).toInt()
        val viewportHeight = (mHeight * scaleY).toInt()

        GLES20.glViewport(0, 0, viewportWidth, viewportHeight)

        mScaleX = scaleX
        mScaleY = scaleY
        mGame.onSurfaceReady()
    }

    @Synchronized
    fun setDrawQueue(queue: ObjectManager?, cameraX: Float, cameraY: Float) {
        drawQueue = queue
        mCameraX = cameraX
        mCameraY = cameraY
        synchronized(drawLock) {
            drawQueueChanged = true
            drawLock.notify()
        }
    }

    @Synchronized
    fun onPause() {
        synchronized(drawLock) {
            drawQueueChanged = true
            drawLock.notify()
        }
    }

    @Synchronized
    fun waitDrawingComplete() {
    }

    fun setContext(newContext: Context) {
        mContext = newContext
    }

    companion object {
        private const val PROFILE_REPORT_DELAY = 3 * 1000
    }

    init {
        mScaleX = 1.0f
        mScaleY = 1.0f
        drawQueueChanged = false
        drawLock = Any()
        mCameraX = 0.0f
        mCameraY = 0.0f
        callbackRequested = false
    }
}

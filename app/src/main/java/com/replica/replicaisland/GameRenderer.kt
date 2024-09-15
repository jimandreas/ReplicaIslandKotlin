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
@file:Suppress("UNCHECKED_CAST", "LocalVariableName")

package com.replica.replicaisland

import android.content.Context
import android.os.Build
import android.os.SystemClock
import com.replica.replicaisland.DrawableBitmap.Companion.beginDrawing
import com.replica.replicaisland.DrawableBitmap.Companion.endDrawing
import com.replica.replicaisland.OpenGLSystem.Companion.gL
import com.replica.replicaisland.RenderSystem.RenderElement
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

// TODO 2 : apply a kotlin solution to this issue
// https://stackoverflow.com/a/44589962/3853712

// defined in GameThread.kt:
// fun Any.wait() = (this as Object).wait()
// fun Any.notify() = (this as Object).notify()
// fun Any.notifyAll() = (this as Object).notifyAll()

/**
 * GameRenderer the top-level rendering interface for the game engine.  It is called by
 * GLSurfaceView and is responsible for submitting commands to OpenGL.  GameRenderer receives a
 * queue of renderable objects from the thread and uses that to draw the scene every frame.  If
 * no queue is available then no drawing is performed.  If the queue is not changed from frame to
 * frame, the same scene will be redrawn every frame.
 * The GameRenderer also invokes texture loads when it is activated.
 */
class GameRenderer(
    private var mContext: Context,
    private val mGame: Game,
    private val mWidth: Int,
    private val mHeight: Int) : GLSurfaceView.Renderer {

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
        /*
         * Some one-time OpenGL initialization can be made here probably based
         * on features of this particular context
         */
        gl!!.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_FASTEST)
        gl.glClearColor(0.0f, 0.0f, 0.0f, 1f)
        gl.glShadeModel(GL10.GL_FLAT)
        gl.glDisable(GL10.GL_DEPTH_TEST)
        gl.glEnable(GL10.GL_TEXTURE_2D)
        /*
         * By default, OpenGL enables features that improve quality but reduce
         * performance. One might want to tweak that especially on software
         * renderer.
         */gl.glDisable(GL10.GL_DITHER)
        gl.glDisable(GL10.GL_LIGHTING)
        gl.glTexEnvx(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE, GL10.GL_MODULATE)
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT or GL10.GL_DEPTH_BUFFER_BIT)
        val extensions = gl.glGetString(GL10.GL_EXTENSIONS)
        val version = gl.glGetString(GL10.GL_VERSION)
        val renderer = gl.glGetString(GL10.GL_RENDERER)
        val isSoftwareRenderer = renderer.contains("PixelFlinger")
        val isOpenGL10 = version.contains("1.0")
        val supportsDrawTexture = extensions.contains("draw_texture")
        // VBOs are standard in GLES1.1
        // No use using VBOs when software renderering, esp. since older versions of the software renderer
        // had a crash bug related to freeing VBOs.
        val supportsVBOs = !isSoftwareRenderer && (!isOpenGL10 || extensions.contains("vertex_buffer_object"))
        val params = BaseObject.sSystemRegistry.contextParameters
        params!!.supportsDrawTexture = supportsDrawTexture
        params.supportsVBOs = supportsVBOs
        hackBrokenDevices()
        DebugLog.i("Graphics Support", version + " (" + renderer + "): " + (if (supportsDrawTexture) "draw texture," else "") + if (supportsVBOs) "vbos" else "")
        mGame.onSurfaceCreated()
    }

    private fun hackBrokenDevices() {
        // Some devices are broken.  Fix them here.  This is pretty much the only
        // device-specific code in the whole project.  Ugh.
        val params = BaseObject.sSystemRegistry.contextParameters
        if (Build.PRODUCT.contains("morrison")) {
            // This is the Motorola Cliq.  This device LIES and says it supports
            // VBOs, which it actually does not (or, more likely, the extensions string
            // is correct and the GL JNI glue is broken).
            params!!.supportsVBOs = false
            // TODO: if Motorola fixes this, I should switch to using the fingerprint
            // (blur/morrison/morrison/morrison:1.5/CUPCAKE/091007:user/ota-rel-keys,release-keys)
            // instead of the product name so that newer versions use VBOs.
        }
    }

    override fun loadTextures(gl: GL10?, library: TextureLibrary?) {
        if (gl != null) {
            library!!.loadAll(mContext, gl)
            DebugLog.d("AndouKun", "Textures Loaded.")
        }
    }

    override fun flushTextures(gl: GL10?, library: TextureLibrary?) {
        if (gl != null) {
            library!!.deleteAll(gl)
            DebugLog.d("AndouKun", "Textures Unloaded.")
        }
    }

    override fun loadBuffers(gl: GL10?, library: BufferLibrary?) {
        if (gl != null) {
            library!!.generateHardwareBuffers(gl)
            DebugLog.d("AndouKun", "Buffers Created.")
        }
    }

    override fun flushBuffers(gl: GL10?, library: BufferLibrary?) {
        if (gl != null) {
            library!!.releaseHardwareBuffers(gl)
            DebugLog.d("AndouKun", "Buffers Released.")
        }
    }

    override fun onSurfaceLost() {
        mGame.onSurfaceLost()
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
                    } catch (e: InterruptedException) {
                        // No big deal if this wait is interrupted.
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
        beginDrawing(gl!!, mWidth.toFloat(), mHeight.toFloat())
        synchronized(this) {
            if (drawQueue != null && drawQueue!!.fetchObjects().count > 0) {
                gL = gl
                val objects = drawQueue!!.fetchObjects()
                val objectArray: Array<Any?> = objects.array as Array<Any?>
                val count = objects.count
                val scaleX = mScaleX
                val scaleY = mScaleY
                val halfWidth = mHalfWidth.toFloat()
                val halfHeight = mHalfHeight.toFloat()
                profileObjectCount += count
                for (i in 0 until count) {
                    val element = objectArray[i] as RenderElement?
                    var x = element!!.x
                    var y = element.y
                    if (element.cameraRelative) {
                        x = x - mCameraX + halfWidth
                        y = y - mCameraY + halfHeight
                    }
                    element.mDrawable!!.draw(x, y, scaleX, scaleY)
                }
                gL = null
            } else if (drawQueue == null) {
                // If we have no draw queue, clear the screen.  If we have a draw queue that
                // is empty, we'll leave the frame buffer alone.
                gl.glClear(GL10.GL_COLOR_BUFFER_BIT or GL10.GL_DEPTH_BUFFER_BIT)
            }
        }
        endDrawing(gl)
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

        //mWidth = w;0
        //mHeight = h;
        // ensure the same aspect ratio as the game
        val scaleX = width.toFloat() / mWidth
        val scaleY = height.toFloat() / mHeight
        val viewportWidth = (mWidth * scaleX).toInt()
        val viewportHeight = (mHeight * scaleY).toInt()
        gl!!.glViewport(0, 0, viewportWidth, viewportHeight)
        mScaleX = scaleX
        mScaleY = scaleY


        /*
         * Set our projection matrix. This doesn't have to be done each time we
         * draw, but usually a new projection needs to be set when the viewport
         * is resized.
         */
        val ratio = mWidth.toFloat() / mHeight
        gl.glMatrixMode(GL10.GL_PROJECTION)
        gl.glLoadIdentity()
        gl.glFrustumf(-ratio, ratio, -1f, 1f, 1f, 10f)
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
        // Stop waiting to avoid deadlock.
        // TODO: this is a hack.  Probably this renderer
        // should just use GLSurfaceView's non-continuious render
        // mode.
        synchronized(drawLock) {
            drawQueueChanged = true
            drawLock.notify()
        }
    }

    /**
     * This function blocks while drawFrame() is in progress, and may be used by other threads to
     * determine when drawing is occurring.
     */
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
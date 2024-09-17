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
@file:Suppress("unused", "SameParameterValue", "DEPRECATION", "PrivatePropertyName", "LocalVariableName", "ConvertTwoComparisonsToRangeCheck", "UNUSED_VARIABLE")

package com.replica.replicaisland

import android.content.Context
import android.content.pm.ConfigurationInfo
import android.opengl.GLDebugHelper
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.replica.replicaisland.GLSurfaceView.Renderer
import java.io.Writer
import java.util.*
import javax.microedition.khronos.egl.*
import javax.microedition.khronos.opengles.GL
import javax.microedition.khronos.opengles.GL10
import kotlin.math.abs

/**
 * An implementation of SurfaceView that uses the dedicated surface for
 * displaying OpenGL rendering.
 *
 *
 * A GLSurfaceView provides the following features:
 *
 *
 *
 *  * Manages a surface, which is a special piece of memory that can be
 * composited into the Android view system.
 *  * Manages an EGL display, which enables OpenGL to render into a surface.
 *  * Accepts a user-provided Renderer object that does the actual rendering.
 *  * Renders on a dedicated thread to decouple rendering performance from the
 * UI thread.
 *  * Supports both on-demand and continuous rendering.
 *  * Optionally wraps, traces, and/or error-checks the renderer's OpenGL calls.
 *
 *
 * <h3>Using GLSurfaceView</h3>
 *
 *
 * Typically you use GLSurfaceView by subclassing it and overriding one or more of the
 * View system input event methods. If your application does not need to override event
 * methods then GLSurfaceView can be used as-is. For the most part
 * GLSurfaceView behavior is customized by calling "set" methods rather than by subclassing.
 * For example, unlike a regular View, drawing is delegated to a separate Renderer object which
 * is registered with the GLSurfaceView
 * using the [.setRenderer] call.
 *
 *
 * <h3>Initializing GLSurfaceView</h3>
 * All you have to do to initialize a GLSurfaceView is call [.setRenderer].
 * However, if desired, you can modify the default behavior of GLSurfaceView by calling one or
 * more of these methods before calling setRenderer:
 *
 *  * [.setDebugFlags]
 *  * [.setEGLConfigChooser]
 *  * [.setEGLConfigChooser]
 *  * [.setEGLConfigChooser]
 *  * [.setGLWrapper]
 *
 *
 *
 * <h4>Choosing an EGL Configuration</h4>
 * A given Android device may support multiple possible types of drawing surfaces.
 * The available surfaces may differ in how may channels of data are present, as
 * well as how many bits are allocated to each channel. Therefore, the first thing
 * GLSurfaceView has to do when starting to render is choose what type of surface to use.
 *
 *
 * By default GLSurfaceView chooses an available surface that's closest to a 16-bit R5G6B5 surface
 * with a 16-bit depth buffer and no stencil. If you would prefer a different surface (for example,
 * if you do not need a depth buffer) you can override the default behavior by calling one of the
 * setEGLConfigChooser methods.
 *
 *
 * <h4>Debug Behavior</h4>
 * You can optionally modify the behavior of GLSurfaceView by calling
 * one or more of the debugging methods [.setDebugFlags],
 * and [.setGLWrapper]. These methods may be called before and/or after setRenderer, but
 * typically they are called before setRenderer so that they take effect immediately.
 *
 *
 * <h4>Setting a Renderer</h4>
 * Finally, you must call [.setRenderer] to register a [Renderer].
 * The renderer is
 * responsible for doing the actual OpenGL rendering.
 *
 *
 * <h3>Rendering Mode</h3>
 * Once the renderer is set, you can control whether the renderer draws
 * continuously or on-demand by calling
 * [.setRenderMode]. The default is continuous rendering.
 *
 *
 * <h3>Activity Life-cycle</h3>
 * A GLSurfaceView must be notified when the activity is paused and resumed. GLSurfaceView clients
 * are required to call [.onPause] when the activity pauses and
 * [.onResume] when the activity resumes. These calls allow GLSurfaceView to
 * pause and resume the rendering thread, and also allow GLSurfaceView to release and recreate
 * the OpenGL display.
 *
 *
 * <h3>Handling events</h3>
 *
 *
 * To handle an event you will typically subclass GLSurfaceView and override the
 * appropriate method, just as you would with any other View. However, when handling
 * the event, you may need to communicate with the Renderer object
 * that's running in the rendering thread. You can do this using any
 * standard Java cross-thread communication mechanism. In addition,
 * one relatively easy way to communicate with your renderer is
 * to call
 * For example:
 * <pre class="prettyprint">
 * class MyGLSurfaceView extends GLSurfaceView {
 *
 * private MyRenderer myRenderer;
 *
 * public void start() {
 * myRenderer = ...;
 * setRenderer(myRenderer);
 * }
 *
 * public boolean onKeyDown(int keyCode, KeyEvent event) {
 * if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
 * queueEvent(new Runnable() {
 * // This method will be called on the rendering
 * // thread:
 * public void run() {
 * myRenderer.handleDpadCenter();
 * }});
 * return true;
 * }
 * return super.onKeyDown(keyCode, event);
 * }
 * }
</pre> *
 *
 */
class GLSurfaceView : SurfaceView, SurfaceHolder.Callback {
    /**
     * Standard View constructor. In order to render something, you
     * must call [.setRenderer] to register a renderer.
     */
    constructor(context: Context?) : super(context) {
        init()
    }

    /**
     * Standard View constructor. In order to render something, you
     * must call [.setRenderer] to register a renderer.
     */
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    private fun init() {
        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed
        val holder = holder
        holder.addCallback(this)
        holder.setType(SurfaceHolder.SURFACE_TYPE_GPU)
    }

    /**
     * Set the glWrapper. If the glWrapper is not null, its
     * [GLWrapper.wrap] method is called
     * whenever a surface is created. A GLWrapper can be used to wrap
     * the GL object that's passed to the renderer. Wrapping a GL
     * object enables examining and modifying the behavior of the
     * GL calls made by the renderer.
     *
     *
     * Wrapping is typically used for debugging purposes.
     *
     *
     * The default value is null.
     * @param glWrapper the new GLWrapper
     */
    fun setGLWrapper(glWrapper: GLWrapper?) {
        gLWrapper = glWrapper
    }

    /**
     * Set the renderer associated with this view. Also starts the thread that
     * will call the renderer, which in turn causes the rendering to start.
     *
     * This method should be called once and only once in the life-cycle of
     * a GLSurfaceView.
     *
     * The following GLSurfaceView methods can only be called *before*
     * setRenderer is called:
     *
     *  * [.setEGLConfigChooser]
     *  * [.setEGLConfigChooser]
     *  * [.setEGLConfigChooser]
     *
     *
     *
     * The following GLSurfaceView methods can only be called *after*
     * setRenderer is called:
     *
     *  * [.getRenderMode]
     *  * [.onPause]
     *  * [.onResume]
     *  * [.requestRender]
     *  * [.setRenderMode]
     *
     *
     * @param renderer the renderer to use to perform OpenGL drawing.
     */
    fun setRenderer(renderer: Renderer) {
        checkRenderThreadState()
        if (eGLConfigChooser == null) {
            eGLConfigChooser = SimpleEGLConfigChooser(true)
        }
        if (eGLContextFactory == null) {
            eGLContextFactory = DefaultContextFactory()
        }
        if (eGLWindowSurfaceFactory == null) {
            eGLWindowSurfaceFactory = DefaultWindowSurfaceFactory()
        }
        gLThread = GLThread(renderer)
        gLThread!!.start()
    }

    /**
     * Install a custom EGLContextFactory.
     *
     * If this method is
     * called, it must be called before [.setRenderer]
     * is called.
     *
     *
     * If this method is not called, then by default
     * a context will be created with no shared context and
     * with a null attribute list.
     */
    fun setEGLContextFactory(factory: EGLContextFactory?) {
        checkRenderThreadState()
        eGLContextFactory = factory
    }

    /**
     * Install a custom EGLWindowSurfaceFactory.
     *
     * If this method is
     * called, it must be called before [.setRenderer]
     * is called.
     *
     *
     * If this method is not called, then by default
     * a window surface will be created with a null attribute list.
     */
    fun setEGLWindowSurfaceFactory(factory: EGLWindowSurfaceFactory?) {
        checkRenderThreadState()
        eGLWindowSurfaceFactory = factory
    }

    /**
     * Install a custom EGLConfigChooser.
     *
     * If this method is
     * called, it must be called before [.setRenderer]
     * is called.
     *
     *
     * If no setEGLConfigChooser method is called, then by default the
     * view will choose a config as close to 16-bit RGB as possible, with
     * a depth buffer as close to 16 bits as possible.
     * @param configChooser
     */
    fun setEGLConfigChooser(configChooser: EGLConfigChooser?) {
        checkRenderThreadState()
        eGLConfigChooser = configChooser
    }

    /**
     * Install a config chooser which will choose a config
     * as close to 16-bit RGB as possible, with or without an optional depth
     * buffer as close to 16-bits as possible.
     *
     * If this method is
     * called, it must be called before [.setRenderer]
     * is called.
     *
     *
     * If no setEGLConfigChooser method is called, then by default the
     * view will choose a config as close to 16-bit RGB as possible, with
     * a depth buffer as close to 16 bits as possible.
     *
     * @param needDepth
     */
    fun setEGLConfigChooser(needDepth: Boolean) {
        setEGLConfigChooser(SimpleEGLConfigChooser(needDepth))
    }

    /**
     * Install a config chooser which will choose a config
     * with at least the specified component sizes, and as close
     * to the specified component sizes as possible.
     *
     * If this method is
     * called, it must be called before [.setRenderer]
     * is called.
     *
     *
     * If no setEGLConfigChooser method is called, then by default the
     * view will choose a config as close to 16-bit RGB as possible, with
     * a depth buffer as close to 16 bits as possible.
     *
     */
    fun setEGLConfigChooser(redSize: Int, greenSize: Int, blueSize: Int,
                            alphaSize: Int, depthSize: Int, stencilSize: Int) {
        setEGLConfigChooser(ComponentSizeChooser(redSize, greenSize,
                blueSize, alphaSize, depthSize, stencilSize))
    }

    /**
     * Inform the default EGLContextFactory and default EGLConfigChooser
     * which EGLContext client version to pick.
     *
     * Use this method to create an OpenGL ES 2.0-compatible context.
     * Example:
     * <pre class="prettyprint">
     * public MyView(Context context) {
     * super(context);
     * setEGLContextClientVersion(2); // Pick an OpenGL ES 2.0 context.
     * setRenderer(new MyRenderer());
     * }
    </pre> *
     *
     * Note: Activities which require OpenGL ES 2.0 should indicate this by
     * setting @lt;uses-feature android:glEsVersion="0x00020000" /> in the activity's
     * AndroidManifest.xml file.
     *
     * If this method is called, it must be called before [.setRenderer]
     * is called.
     *
     * This method only affects the behavior of the default EGLContexFactory and the
     * default EGLConfigChooser. If
     * [.setEGLContextFactory] has been called, then the supplied
     * EGLContextFactory is responsible for creating an OpenGL ES 2.0-compatible context.
     * If
     * [.setEGLConfigChooser] has been called, then the supplied
     * EGLConfigChooser is responsible for choosing an OpenGL ES 2.0-compatible config.
     * @param version The EGLContext client version to choose. Use 2 for OpenGL ES 2.0
     */
    fun setEGLContextClientVersion(version: Int) {
        checkRenderThreadState()
        eGLContextClientVersion = version
    }
    /**
     * Get the current rendering mode. May be called
     * from any thread. Must not be called before a renderer has been set.
     * @return the current rendering mode.
     * @see .RENDERMODE_CONTINUOUSLY
     *
     * @see .RENDERMODE_WHEN_DIRTY
     */
    /**
     * Set the rendering mode. When renderMode is
     * RENDERMODE_CONTINUOUSLY, the renderer is called
     * repeatedly to re-render the scene. When renderMode
     * is RENDERMODE_WHEN_DIRTY, the renderer only rendered when the surface
     * is created, or when [.requestRender] is called. Defaults to RENDERMODE_CONTINUOUSLY.
     *
     *
     * Using RENDERMODE_WHEN_DIRTY can improve battery life and overall system performance
     * by allowing the GPU and CPU to idle when the view does not need to be updated.
     *
     *
     * This method can only be called after [.setRenderer]
     *
     * renderMode one of the RENDERMODE_X constants
     * @see .RENDERMODE_CONTINUOUSLY
     *
     * @see .RENDERMODE_WHEN_DIRTY
     */
    var renderMode: Int
        get() = gLThread!!.renderMode
        set(renderMode) {
            gLThread!!.renderMode = renderMode
        }

    /**
     * Request that the renderer render a frame.
     * This method is typically used when the render mode has been set to
     * [.RENDERMODE_WHEN_DIRTY], so that frames are only rendered on demand.
     * May be called
     * from any thread. Must not be called before a renderer has been set.
     */
    fun requestRender() {
        gLThread!!.requestRender()
    }

    /**
     * This method is part of the SurfaceHolder.Callback interface, and is
     * not normally called or subclassed by clients of GLSurfaceView.
     */
    override fun surfaceCreated(holder: SurfaceHolder) {
        gLThread!!.surfaceCreated()
    }

    /**
     * This method is part of the SurfaceHolder.Callback interface, and is
     * not normally called or subclassed by clients of GLSurfaceView.
     */
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        // Surface will be destroyed when we return
        gLThread!!.surfaceDestroyed()
    }

    /**
     * This method is part of the SurfaceHolder.Callback interface, and is
     * not normally called or subclassed by clients of GLSurfaceView.
     */
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
        gLThread!!.onWindowResize(w, h)
    }

    /**
     * Inform the view that the activity is paused. The owner of this view must
     * call this method when the activity is paused. Calling this method will
     * pause the rendering thread.
     * Must not be called before a renderer has been set.
     */
    fun onPause() {
        gLThread!!.onPause()
    }

    /**
     * Inform the view that the activity is resumed. The owner of this view must
     * call this method when the activity is resumed. Calling this method will
     * recreate the OpenGL display and resume the rendering
     * thread.
     * Must not be called before a renderer has been set.
     */
    fun onResume() {
        gLThread!!.onResume()
    }

    fun flushTextures(library: TextureLibrary?) {
        gLThread!!.flushTextures(library)
    }

    fun loadTextures(library: TextureLibrary?) {
        gLThread!!.loadTextures(library)
    }

    fun flushBuffers(library: BufferLibrary?) {
        gLThread!!.flushBuffers(library)
    }

    fun loadBuffers(library: BufferLibrary?) {
        gLThread!!.loadBuffers(library)
    }

    fun setSafeMode(safeMode: Boolean) {
        gLThread!!.setSafeMode(safeMode)
    }

    /**
     * Queue a runnable to be run on the GL rendering thread. This can be used
     * to communicate with the Renderer on the rendering thread.
     * Must not be called before a renderer has been set.
     * @param r the runnable to be run on the GL rendering thread.
     */
    fun queueEvent(r: Runnable?) {
        gLThread!!.queueEvent(r)
    }

    /**
     * Inform the view that the window focus has changed.
     */
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        gLThread!!.onWindowFocusChanged(hasFocus)
    }

    /**
     * This method is used as part of the View class and is not normally
     * called or subclassed by clients of GLSurfaceView.
     * Must not be called before a renderer has been set.
     */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        gLThread!!.requestExitAndWait()
    }
    // ----------------------------------------------------------------------
    /**
     * An interface used to wrap a GL interface.
     *
     * Typically
     * used for implementing debugging and tracing on top of the default
     * GL interface. You would typically use this by creating your own class
     * that implemented all the GL methods by delegating to another GL instance.
     * Then you could add your own behavior before or after calling the
     * delegate. All the GLWrapper would do was instantiate and return the
     * wrapper GL instance:
     * <pre class="prettyprint">
     * class MyGLWrapper implements GLWrapper {
     * GL wrap(GL gl) {
     * return new MyGLImplementation(gl);
     * }
     * static class MyGLImplementation implements GL,GL10,GL11,... {
     * ...
     * }
     * }
    </pre> *
     * @see .setGLWrapper
     */
    interface GLWrapper {
        /**
         * Wraps a gl interface in another gl interface.
         * @param gl a GL interface that is to be wrapped.
         * @return either the input argument or another GL object that wraps the input argument.
         */
        fun wrap(gl: GL?): GL?
    }

    /**
     * A generic renderer interface.
     *
     *
     * The renderer is responsible for making OpenGL calls to render a frame.
     *
     *
     * GLSurfaceView clients typically create their own classes that implement
     * this interface, and then call [GLSurfaceView.setRenderer] to
     * register the renderer with the GLSurfaceView.
     *
     *
     * <h3>Threading</h3>
     * The renderer will be called on a separate thread, so that rendering
     * performance is decoupled from the UI thread. Clients typically need to
     * communicate with the renderer from the UI thread, because that's where
     * input events are received. Clients can communicate using any of the
     * standard Java techniques for cross-thread communication, or they can
     * use the queueEvent() convenience method.
     *
     *
     * <h3>EGL Context Lost</h3>
     * There are situations where the EGL rendering context will be lost. This
     * typically happens when device wakes up after going to sleep. When
     * the EGL context is lost, all OpenGL resources (such as textures) that are
     * associated with that context will be automatically deleted. In order to
     * keep rendering correctly, a renderer must recreate any lost resources
     * that it still needs. The [.onSurfaceCreated] method
     * is a convenient place to do this.
     *
     *
     * @see .setRenderer
     */
    interface Renderer {
        /**
         * Called when the surface is created or recreated.
         *
         *
         * Called when the rendering thread
         * starts and whenever the EGL context is lost. The context will typically
         * be lost when the Android device awakes after going to sleep.
         *
         *
         * Since this method is called at the beginning of rendering, as well as
         * every time the EGL context is lost, this method is a convenient place to put
         * code to create resources that need to be created when the rendering
         * starts, and that need to be recreated when the EGL context is lost.
         * Textures are an example of a resource that you might want to create
         * here.
         *
         *
         * Note that when the EGL context is lost, all OpenGL resources associated
         * with that context will be automatically deleted. You do not need to call
         * the corresponding "glDelete" methods such as glDeleteTextures to
         * manually delete these lost resources.
         *
         *
         * @param gl the GL interface. Use `instanceof` to
         * test if the interface supports GL11 or higher interfaces.
         * @param config the EGLConfig of the created surface. Can be used
         * to create matching pbuffers.
         */
        fun onSurfaceCreated(gl: GL10?, config: EGLConfig?)

        /**
         * Called when the surface changed size.
         *
         *
         * Called after the surface is created and whenever
         * the OpenGL ES surface size changes.
         *
         *
         * Typically you will set your viewport here. If your camera
         * is fixed then you could also set your projection matrix here:
         * <pre class="prettyprint">
         * void onSurfaceChanged(GL10 gl, int width, int height) {
         * gl.glViewport(0, 0, width, height);
         * // for a fixed camera, set the projection too
         * float ratio = (float) width / height;
         * gl.glMatrixMode(GL10.GL_PROJECTION);
         * gl.glLoadIdentity();
         * gl.glFrustumf(-ratio, ratio, -1, 1, 1, 10);
         * }
        </pre> *
         * @param gl the GL interface. Use `instanceof` to
         * test if the interface supports GL11 or higher interfaces.
         * @param width
         * @param height
         */
        fun onSurfaceChanged(gl: GL10?, width: Int, height: Int)

        /**
         * Called when the OpenGL context has been lost is about
         * to be recreated.  onSurfaceCreated() will be called after
         * onSurfaceLost().
         */
        fun onSurfaceLost()

        /**
         * Called to draw the current frame.
         *
         *
         * This method is responsible for drawing the current frame.
         *
         *
         * The implementation of this method typically looks like this:
         * <pre class="prettyprint">
         * void onDrawFrame(GL10 gl) {
         * gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
         * //... other gl calls to render the scene ...
         * }
        </pre> *
         * @param gl the GL interface. Use `instanceof` to
         * test if the interface supports GL11 or higher interfaces.
         */
        fun onDrawFrame(gl: GL10?)
        fun loadTextures(gl: GL10?, library: TextureLibrary?)
        fun flushTextures(gl: GL10?, library: TextureLibrary?)
        fun loadBuffers(gl: GL10?, library: BufferLibrary?)
        fun flushBuffers(gl: GL10?, library: BufferLibrary?)
    }

    /**
     * An interface for customizing the eglCreateContext and eglDestroyContext calls.
     *
     *
     * This interface must be implemented by clients wishing to call
     * [GLSurfaceView.setEGLContextFactory]
     */
    interface EGLContextFactory {
        fun createContext(egl: EGL10?, display: EGLDisplay?, eglConfig: EGLConfig?): EGLContext?
        fun destroyContext(egl: EGL10?, display: EGLDisplay?, context: EGLContext?)
    }

    private inner class DefaultContextFactory : EGLContextFactory {
        private val EGL_CONTEXT_CLIENT_VERSION = 0x3098
        override fun createContext(egl: EGL10?, display: EGLDisplay?, eglConfig: EGLConfig?): EGLContext? {
            val attrib_list = intArrayOf(EGL_CONTEXT_CLIENT_VERSION, eGLContextClientVersion,
                    EGL10.EGL_NONE)
            return egl!!.eglCreateContext(display, eglConfig, EGL10.EGL_NO_CONTEXT,
                    if (eGLContextClientVersion != 0) attrib_list else null)
        }

        override fun destroyContext(egl: EGL10?, display: EGLDisplay?,
                                    context: EGLContext?) {
            egl!!.eglDestroyContext(display, context)
        }
    }

    /**
     * An interface for customizing the eglCreateWindowSurface and eglDestroySurface calls.
     *
     *
     * This interface must be implemented by clients wishing to call
     * [GLSurfaceView.setEGLWindowSurfaceFactory]
     */
    interface EGLWindowSurfaceFactory {
        fun createWindowSurface(egl: EGL10?, display: EGLDisplay?, config: EGLConfig?,
                                nativeWindow: Any?): EGLSurface?

        fun destroySurface(egl: EGL10?, display: EGLDisplay?, surface: EGLSurface?)
    }

    private class DefaultWindowSurfaceFactory : EGLWindowSurfaceFactory {
        override fun createWindowSurface(egl: EGL10?, display: EGLDisplay?,
                                         config: EGLConfig?, nativeWindow: Any?): EGLSurface? {
            return egl!!.eglCreateWindowSurface(display, config, nativeWindow, null)
        }

        override fun destroySurface(egl: EGL10?, display: EGLDisplay?,
                                    surface: EGLSurface?) {
            egl!!.eglDestroySurface(display, surface)
        }
    }

    /**
     * An interface for choosing an EGLConfig configuration from a list of
     * potential configurations.
     *
     *
     * This interface must be implemented by clients wishing to call
     * [GLSurfaceView.setEGLConfigChooser]
     */
    interface EGLConfigChooser {
        /**
         * Choose a configuration from the list. Implementors typically
         * implement this method by calling
         * [EGL10.eglChooseConfig] and iterating through the results. Please consult the
         * EGL specification available from The Khronos Group to learn how to call eglChooseConfig.
         * @param egl the EGL10 for the current display.
         * @param display the current display.
         * @return the chosen configuration.
         */
        fun chooseConfig(egl: EGL10?, display: EGLDisplay?): EGLConfig
    }

    private abstract inner class BaseConfigChooser(configSpec: IntArray) : EGLConfigChooser {
        override fun chooseConfig(egl: EGL10?, display: EGLDisplay?): EGLConfig {
            val num_config = IntArray(1)
            require(egl!!.eglChooseConfig(display, mConfigSpec, null, 0,
                    num_config)) { "eglChooseConfig failed" }
            val numConfigs = num_config[0]
            require(numConfigs > 0) { "No configs match configSpec" }
            val configs = arrayOfNulls<EGLConfig>(numConfigs)
            require(egl.eglChooseConfig(display, mConfigSpec, configs, numConfigs,
                    num_config)) { "eglChooseConfig#2 failed" }
            return chooseConfig(egl, display, configs)
                    ?: throw IllegalArgumentException("No config chosen")
        }

        abstract fun chooseConfig(egl: EGL10?, display: EGLDisplay?,
                                  configs: Array<EGLConfig?>): EGLConfig?

        protected var mConfigSpec: IntArray
        private fun filterConfigSpec(configSpec: IntArray): IntArray {
            if (eGLContextClientVersion != 2) {
                return configSpec
            }
            /* We know none of the subclasses define EGL_RENDERABLE_TYPE.
             * And we know the configSpec is well formed.
             */
            val len = configSpec.size
            val newConfigSpec = IntArray(len + 2)
            System.arraycopy(configSpec, 0, newConfigSpec, 0, len - 1)
            newConfigSpec[len - 1] = EGL10.EGL_RENDERABLE_TYPE
            newConfigSpec[len] = 4 /* EGL_OPENGL_ES2_BIT */
            newConfigSpec[len + 1] = EGL10.EGL_NONE
            return newConfigSpec
        }

        init {
            mConfigSpec = filterConfigSpec(configSpec)
        }
    }

    private open inner class ComponentSizeChooser(redSize: Int, greenSize: Int, blueSize: Int,
                                                  alphaSize: Int, depthSize: Int, stencilSize: Int) : BaseConfigChooser(intArrayOf(
            EGL10.EGL_RED_SIZE, redSize,
            EGL10.EGL_GREEN_SIZE, greenSize,
            EGL10.EGL_BLUE_SIZE, blueSize,
            EGL10.EGL_ALPHA_SIZE, alphaSize,
            EGL10.EGL_DEPTH_SIZE, depthSize,
            EGL10.EGL_STENCIL_SIZE, stencilSize,
            EGL10.EGL_NONE)) {
        override fun chooseConfig(egl: EGL10?, display: EGLDisplay?,
                                  configs: Array<EGLConfig?>): EGLConfig? {
            var closestConfig: EGLConfig? = null
            var closestDistance = 1000
            for (config in configs) {
                val d = findConfigAttrib(egl, display, config,
                        EGL10.EGL_DEPTH_SIZE, 0)
                val s = findConfigAttrib(egl, display, config,
                        EGL10.EGL_STENCIL_SIZE, 0)
                if (d >= mDepthSize && s >= mStencilSize) {
                    val r = findConfigAttrib(egl, display, config,
                            EGL10.EGL_RED_SIZE, 0)
                    val g = findConfigAttrib(egl, display, config,
                            EGL10.EGL_GREEN_SIZE, 0)
                    val b = findConfigAttrib(egl, display, config,
                            EGL10.EGL_BLUE_SIZE, 0)
                    val a = findConfigAttrib(egl, display, config,
                            EGL10.EGL_ALPHA_SIZE, 0)
                    val distance = (
                            abs(r - mRedSize)
                            + abs(g - mGreenSize)
                            + abs(b - mBlueSize)
                            + abs(a - mAlphaSize))
                    if (distance < closestDistance) {
                        closestDistance = distance
                        closestConfig = config
                    }
                }
            }
            return closestConfig
        }

        private fun findConfigAttrib(egl: EGL10?, display: EGLDisplay?,
                                     config: EGLConfig?, attribute: Int, defaultValue: Int): Int {
            return if (egl!!.eglGetConfigAttrib(display, config, attribute, mValue)) {
                mValue[0]
            } else defaultValue
        }

        private val mValue: IntArray = IntArray(1)

        // Subclasses can adjust these values:
        protected var mRedSize: Int = redSize
        protected var mGreenSize: Int = greenSize
        protected var mBlueSize: Int = blueSize
        protected var mAlphaSize: Int = alphaSize
        protected var mDepthSize: Int = depthSize
        protected var mStencilSize: Int = stencilSize

    }

    /**
     * This class will choose a supported surface as close to
     * RGB565 as possible, with or without a depth buffer.
     *
     */
    private inner class SimpleEGLConfigChooser(withDepthBuffer: Boolean) : ComponentSizeChooser(4, 4, 4, 0, if (withDepthBuffer) 16 else 0, 0) {
        init {
            // Adjust target values. This way we'll accept a 4444 or
            // 555 buffer if there's no 565 buffer available.
            mRedSize = 5
            mGreenSize = 6
            mBlueSize = 5
        }
    }

    /**
     * An EGL helper class.
     */
    private inner class EglHelper {
        /**
         * Initialize EGL for a given configuration spec.
         */
        fun start() {
            /*
             * Get an EGL instance
             */
            mEgl = EGLContext.getEGL() as EGL10

            /*
             * Get to the default display.
             */eglDisplay = mEgl!!.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)
            if (eglDisplay === EGL10.EGL_NO_DISPLAY) {
                throw RuntimeException("eglGetDisplay failed")
            }

            /*
             * We can now initialize EGL for that display
             */
            val version = IntArray(2)
            if (!mEgl!!.eglInitialize(eglDisplay, version)) {
                throw RuntimeException("eglInitialize failed")
            }
            mEglConfig = eGLConfigChooser!!.chooseConfig(mEgl, eglDisplay)

            /*
            * Create an OpenGL ES context. This must be done only once, an
            * OpenGL context is a somewhat heavy object.
            */eglContext = eGLContextFactory!!.createContext(mEgl, eglDisplay, mEglConfig)
            if (eglContext == null || eglContext === EGL10.EGL_NO_CONTEXT) {
                throwEglException("createContext")
            }
            eglSurface = null
        }

        /*
         * React to the creation of a new surface by creating and returning an
         * OpenGL interface that renders to that surface.
         */
        fun createSurface(holder: SurfaceHolder?): GL? {
            /*
             *  The window size has changed, so we need to create a new
             *  surface.
             */
            if (eglSurface != null && eglSurface !== EGL10.EGL_NO_SURFACE) {

                /*
                 * Unbind and destroy the old EGL surface, if
                 * there is one.
                 */
                mEgl!!.eglMakeCurrent(eglDisplay, EGL10.EGL_NO_SURFACE,
                        EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT)
                eGLWindowSurfaceFactory!!.destroySurface(mEgl, eglDisplay, eglSurface)
            }

            /*
             * Create an EGL surface we can render into.
             */eglSurface = eGLWindowSurfaceFactory!!.createWindowSurface(mEgl,
                    eglDisplay, mEglConfig, holder)
            if (eglSurface == null || eglSurface === EGL10.EGL_NO_SURFACE) {
                throwEglException("createWindowSurface")
            }

            /*
             * Before we can issue GL commands, we need to make sure
             * the context is current and bound to a surface.
             */if (!mEgl!!.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
                throwEglException("eglMakeCurrent")
            }
            var gl = eglContext!!.gl
            if (gLWrapper != null) {
                gl = gLWrapper!!.wrap(gl)
            }
            if (debugFlags and (DEBUG_CHECK_GL_ERROR or DEBUG_LOG_GL_CALLS) != 0) {
                var configFlags = 0
                var log: Writer? = null
                if (debugFlags and DEBUG_CHECK_GL_ERROR != 0) {
                    configFlags = configFlags or GLDebugHelper.CONFIG_CHECK_GL_ERROR
                }
                if (debugFlags and DEBUG_LOG_GL_CALLS != 0) {
                    log = LogWriter()
                }
                gl = GLDebugHelper.wrap(gl, configFlags, log)
            }
            return gl
        }

        /**
         * Display the current render surface.
         * @return false if the context has been lost.
         */
        fun swap(): Boolean {
            mEgl!!.eglSwapBuffers(eglDisplay, eglSurface)

            /*
             * Always check for EGL_CONTEXT_LOST, which means the context
             * and all associated data were lost (For instance because
             * the device went to sleep). We need to sleep until we
             * get a new surface.
             */return mEgl!!.eglGetError() != EGL11.EGL_CONTEXT_LOST
        }

        fun destroySurface() {
            if (eglSurface != null && eglSurface !== EGL10.EGL_NO_SURFACE) {
                mEgl!!.eglMakeCurrent(eglDisplay, EGL10.EGL_NO_SURFACE,
                        EGL10.EGL_NO_SURFACE,
                        EGL10.EGL_NO_CONTEXT)
                eGLWindowSurfaceFactory!!.destroySurface(mEgl, eglDisplay, eglSurface)
                eglSurface = null
            }
        }

        fun finish() {
            if (eglContext != null) {
                eGLContextFactory!!.destroyContext(mEgl, eglDisplay, eglContext)
                eglContext = null
            }
            if (eglDisplay != null) {
                mEgl!!.eglTerminate(eglDisplay)
                eglDisplay = null
            }
        }

        private fun throwEglException(function: String) {
            throw RuntimeException(function + " failed: " + mEgl!!.eglGetError())
        }

        /** Checks to see if the current context is valid.   */
        fun verifyContext(): Boolean {
            val currentContext = mEgl!!.eglGetCurrentContext()
            return currentContext !== EGL10.EGL_NO_CONTEXT && mEgl!!.eglGetError() != EGL11.EGL_CONTEXT_LOST
        }

        var mEgl: EGL10? = null
        var eglDisplay: EGLDisplay? = null
        var eglSurface: EGLSurface? = null
        var mEglConfig: EGLConfig? = null
        var eglContext: EGLContext? = null
    }

    /**
     * A generic GL Thread. Takes care of initializing EGL and GL. Delegates
     * to a Renderer instance to do the actual drawing. Can be configured to
     * render continuously or on request.
     *
     * All potentially blocking synchronization is done through the
     * sGLThreadManager object. This avoids multiple-lock ordering issues.
     *
     */
    private inner class GLThread(renderer: Renderer) : Thread() {
        override fun run() {
            name = "GLThread $id"
            if (LOG_THREADS) {
                DebugLog.i("GLThread", "starting tid=$id")
            }
            try {
                guardedRun()
            } catch (e: InterruptedException) {
                // fall thru and exit normally
            } finally {
                sGLThreadManager.threadExiting(this)
            }
        }

        /*
         * This private method should only be called inside a
         * synchronized(sGLThreadManager) block.
         */
        private fun stopEglLocked() {
            if (haveEglSurface) {
                haveEglSurface = false
                eglHelper!!.destroySurface()
                sGLThreadManager.releaseEglSurfaceLocked(this)
            }
        }

        @Throws(InterruptedException::class)
        private fun guardedRun() {
            eglHelper = EglHelper()
            haveEglContext = false
            haveEglSurface = false
            try {
                var gl: GL10? = null
                var createEglSurface = false
                var sizeChanged = false
                var wantRenderNotification = false
                var doRenderNotification = false
                var w = 0
                var h = 0
                var event: Runnable? = null
                var framesSinceResetHack = 0
                while (true) {
                    synchronized(sGLThreadManager) {
                        while (true) {
                            if (shouldExit) {
                                return
                            }
                            if (eventQueue.isNotEmpty()) {
                                event = eventQueue.removeAt(0)
                                break
                            }

                            // Do we need to release the EGL surface?
                            if (haveEglSurface && mPaused) {
                                if (LOG_SURFACE) {
                                    DebugLog.i("GLThread", "releasing EGL surface because paused tid=$id")
                                }
                                stopEglLocked()
                            }

                            // Have we lost the surface view surface?
                            if (!hasSurface && !waitingForSurface) {
                                if (LOG_SURFACE) {
                                    DebugLog.i("GLThread", "noticed surfaceView surface lost tid=$id")
                                }
                                if (haveEglSurface) {
                                    stopEglLocked()
                                }
                                waitingForSurface = true
                                sGLThreadManager.notifyAll()
                            }

                            // Have we acquired the surface view surface?
                            if (hasSurface && waitingForSurface) {
                                if (LOG_SURFACE) {
                                    DebugLog.i("GLThread", "noticed surfaceView surface acquired tid=$id")
                                }
                                waitingForSurface = false
                                sGLThreadManager.notifyAll()
                            }
                            if (doRenderNotification) {
                                wantRenderNotification = false
                                doRenderNotification = false
                                renderComplete = true
                                sGLThreadManager.notifyAll()
                            }

                            // Ready to draw?
                            if (!mPaused && hasSurface
                                    && mWidth > 0 && mHeight > 0
                                    && (mRequestRender || mRenderMode == RENDERMODE_CONTINUOUSLY)) {
                                if (haveEglContext && !haveEglSurface) {
                                    // Let's make sure the context hasn't been lost.
                                    if (!eglHelper!!.verifyContext()) {
                                        eglHelper!!.finish()
                                        mRenderer.onSurfaceLost()
                                        haveEglContext = false
                                    }
                                }
                                // If we don't have an egl surface, try to acquire one.
                                if (!haveEglContext && sGLThreadManager.tryAcquireEglSurfaceLocked(this)) {
                                    haveEglContext = true
                                    eglHelper!!.start()
                                    sGLThreadManager.notifyAll()
                                }
                                if (haveEglContext && !haveEglSurface) {
                                    haveEglSurface = true
                                    createEglSurface = true
                                    sizeChanged = true
                                }
                                if (haveEglSurface) {
                                    if (mSizeChanged) {
                                        sizeChanged = true
                                        w = mWidth
                                        h = mHeight
                                        wantRenderNotification = true
                                        if (DRAW_TWICE_AFTER_SIZE_CHANGED) {
                                            // We keep mRequestRender true so that we draw twice after the size changes.
                                            // (Once because of mSizeChanged, the second time because of mRequestRender.)
                                            // This forces the updated graphics onto the screen.
                                        } else {
                                            mRequestRender = false
                                        }
                                        mSizeChanged = false
                                    } else {
                                        mRequestRender = false
                                    }
                                    sGLThreadManager.notifyAll()
                                    break
                                }
                            }

                            // By design, this is the only place in a GLThread thread where we wait().
                            if (LOG_THREADS) {
                                DebugLog.i("GLThread", "waiting tid=$id")
                            }
                            sGLThreadManager.wait()
                        }
                    } // end of synchronized(sGLThreadManager)
                    if (event != null) {
                        event!!.run()
                        event = null
                        continue
                    }
                    if (mHasFocus) {
                        if (createEglSurface) {
                            gl = eglHelper!!.createSurface(holder) as GL10?
                            sGLThreadManager.checkGLDriver(gl)
                            if (LOG_RENDERER) {
                                DebugLog.w("GLThread", "onSurfaceCreated")
                            }
                            mGL = gl
                            mRenderer.onSurfaceCreated(gl, eglHelper!!.mEglConfig)
                            createEglSurface = false
                            framesSinceResetHack = 0
                        }
                        if (sizeChanged) {
                            if (LOG_RENDERER) {
                                DebugLog.w("GLThread", "onSurfaceChanged($w, $h)")
                            }
                            mRenderer.onSurfaceChanged(gl, w, h)
                            sizeChanged = false
                        }
                        if (LOG_RENDERER) {
                            DebugLog.w("GLThread", "onDrawFrame")
                        }

                        // Some phones (Motorola Cliq, Backflip; also the
                        // Huawei Pulse, and maybe the Samsung Behold II), use a
                        // broken graphics driver from Qualcomm.  It fails in a
                        // very specific case: when the EGL context is lost due to
                        // resource constraints, and then recreated, if GL commands
                        // are sent within two frames of the surface being created
                        // then eglSwapBuffers() will hang.  Normally, applications using
                        // the stock GLSurfaceView never run into this problem because it
                        // discards the EGL context explicitly on every pause.  But
                        // I've modified this class to not do that--I only want to reload
                        // textures when the context is actually lost--so this bug
                        // revealed itself as black screens on devices like the Cliq.
                        // Thus, in "safe mode," I force two swaps to occur before
                        // issuing any GL commands.  Don't ask me how long it took
                        // to figure this out.
                        if (framesSinceResetHack > 1 || !mSafeMode) {
                            mRenderer.onDrawFrame(gl)
                        } else {
                            DebugLog.w("GLThread", "Safe Mode Wait...")
                        }
                        framesSinceResetHack++
                        if (!eglHelper!!.swap()) {
                            if (LOG_SURFACE) {
                                DebugLog.i("GLThread", "egl surface lost tid=$id")
                            }
                            stopEglLocked()
                        }
                    }
                    if (wantRenderNotification) {
                        doRenderNotification = true
                    }
                }
            } finally {
                mGL = null
                /*
                 * clean-up everything...
                 */synchronized(sGLThreadManager) {
                    stopEglLocked()
                    eglHelper!!.finish()
                }
            }
        }

        var renderMode: Int
            get() {
                synchronized(sGLThreadManager) { return mRenderMode }
            }
            set(renderMode) {
                require(RENDERMODE_WHEN_DIRTY <= renderMode
                        && renderMode <= RENDERMODE_CONTINUOUSLY) { "renderMode" }
                synchronized(sGLThreadManager) {
                    mRenderMode = renderMode
                    sGLThreadManager.notifyAll()
                }
            }

        fun requestRender() {
            synchronized(sGLThreadManager) {
                mRequestRender = true
                sGLThreadManager.notifyAll()
            }
        }

        fun surfaceCreated() {
            synchronized(sGLThreadManager) {
                if (LOG_THREADS) {
                    DebugLog.i("GLThread", "surfaceCreated tid=$id")
                }
                hasSurface = true
                sGLThreadManager.notifyAll()
            }
        }

        fun surfaceDestroyed() {
            synchronized(sGLThreadManager) {
                if (LOG_THREADS) {
                    DebugLog.i("GLThread", "surfaceDestroyed tid=$id")
                }
                hasSurface = false
                sGLThreadManager.notifyAll()
                while (!waitingForSurface && !exited) {
                    try {
                        sGLThreadManager.wait()
                    } catch (e: InterruptedException) {
                        currentThread().interrupt()
                    }
                }
            }
        }

        fun onPause() {
            synchronized(sGLThreadManager) {
                mPaused = true
                sGLThreadManager.notifyAll()
            }
        }

        fun onResume() {
            synchronized(sGLThreadManager) {
                mPaused = false
                mRequestRender = true
                sGLThreadManager.notifyAll()
            }
        }

        fun onWindowResize(w: Int, h: Int) {
            synchronized(sGLThreadManager) {
                mWidth = w
                mHeight = h
                mSizeChanged = true
                mRequestRender = true
                renderComplete = false
                sGLThreadManager.notifyAll()

                // Wait for thread to react to resize and render a frame
                while (!exited && !mPaused && !renderComplete) {
                    if (LOG_SURFACE) {
                        DebugLog.i("Main thread", "onWindowResize waiting for render complete.")
                    }
                    try {
                        sGLThreadManager.wait()
                    } catch (ex: InterruptedException) {
                        currentThread().interrupt()
                    }
                }
            }
        }

        fun loadTextures(library: TextureLibrary?) {
            synchronized(this) {
                //TODO 2 fix (mGL != null)
                if (mGL != null && hasSurface) {
                    mRenderer.loadTextures(mGL, library)
                }
            }
        }

        fun flushTextures(library: TextureLibrary?) {
            synchronized(this) {
                //TODO 2 fix (mGL != null)
                if (mGL != null) {
                    mRenderer.flushTextures(mGL, library)
                }
            }
        }

        fun loadBuffers(library: BufferLibrary?) {
            synchronized(this) {
                //TODO 2 fix (mGL != null)
                if (mGL != null) {
                    mRenderer.loadBuffers(mGL, library)
                }
            }
        }

        fun flushBuffers(library: BufferLibrary?) {
            synchronized(this) {
                //TODO 2 fix (mGL != null)
                if (mGL != null) {
                    mRenderer.flushBuffers(mGL, library)
                }
            }
        }

        // On some Qualcomm devices (such as the HTC Magic running Android 1.6),
        // there's a bug in the graphics driver that will cause glViewport() to
        // do the wrong thing in a very specific situation.  When the screen is
        // rotated, if a surface is created in one layout (say, portrait view)
        // and then rotated to another, subsequent calls to glViewport are clipped.
        // So, if the window is, say, 320x480 when the surface is created, and
        // then the rotation occurs and glViewport() is called with the new
        // size of 480x320, devices with the buggy driver will clip the viewport
        // to the old width (which means 320x320...ugh!).  This is fixed in
        // Android 2.1 Qualcomm devices (like Nexus One) and doesn't affect
        // non-Qualcomm devices (like the Motorola DROID).
        //
        // Unfortunately, under Android 1.6 this exact case occurs when the
        // screen is put to sleep and then wakes up again.  The lock screen
        // comes up in portrait mode, but at the same time the window surface
        // is also created in the backgrounded game.  When the lock screen is closed
        // and the game comes forward, the window is fixed to the correct size
        // which causes the bug to occur.
        // The solution used here is to simply never render when the window surface
        // does not have the focus.  When the lock screen (or menu) is up, rendering
        // will stop.  This resolves the driver bug (as the egl surface won't be created
        // until after the screen size has been fixed), and is generally good practice
        // since you don't want to be doing a lot of CPU intensive work when the lock
        // screen is up (to preserve battery life).
        fun onWindowFocusChanged(hasFocus: Boolean) {
            synchronized(sGLThreadManager) {
                mHasFocus = hasFocus
                sGLThreadManager.notifyAll()
            }
            if (LOG_SURFACE) {
                DebugLog.i("Main thread", "Focus " + if (mHasFocus) "gained" else "lost")
            }
        }

        fun requestExitAndWait() {
            // don't call this from GLThread thread or it is a guaranteed
            // deadlock!
            synchronized(sGLThreadManager) {
                shouldExit = true
                sGLThreadManager.notifyAll()
                while (!exited) {
                    try {
                        sGLThreadManager.wait()
                    } catch (ex: InterruptedException) {
                        currentThread().interrupt()
                    }
                }
            }
        }

        /**
         * Queue an "event" to be run on the GL rendering thread.
         * @param r the runnable to be run on the GL rendering thread.
         */
        fun queueEvent(r: Runnable?) {
            requireNotNull(r) { "r must not be null" }
            synchronized(sGLThreadManager) {
                eventQueue.add(r)
                sGLThreadManager.notifyAll()
            }
        }

        fun setSafeMode(on: Boolean) {
            mSafeMode = on
        }

        // Once the thread is started, all accesses to the following member
        // variables are protected by the sGLThreadManager monitor
        private var shouldExit = false
        var exited = false
        private var mPaused = false
        private var hasSurface = false
        private var waitingForSurface = false
        private var haveEglContext = false
        private var haveEglSurface = false
        private var mWidth = 0
        private var mHeight = 0
        private var mRenderMode: Int
        private var mRequestRender = true
        private var renderComplete = false
        private val eventQueue = ArrayList<Runnable>()
        private var mGL: GL10? = null
        private var mHasFocus = false
        private var mSafeMode = false

        // End of member variables protected by the sGLThreadManager monitor.
        private val mRenderer: Renderer
        private var eglHelper: EglHelper? = null

        init {
            mRenderMode = RENDERMODE_CONTINUOUSLY
            mRenderer = renderer
        }
    }

    internal class LogWriter : Writer() {
        override fun close() {
            flushBuilder()
        }

        override fun flush() {
            flushBuilder()
        }

        override fun write(buf: CharArray, offset: Int, count: Int) {
            for (i in 0 until count) {
                val c = buf[offset + i]
                if (c == '\n') {
                    flushBuilder()
                } else {
                    builder.append(c)
                }
            }
        }

        private fun flushBuilder() {
            if (builder.isNotEmpty()) {
                DebugLog.v("GLSurfaceView", builder.toString())
                builder.delete(0, builder.length)
            }
        }

        private val builder = StringBuilder()
    }

    private fun checkRenderThreadState() {
        check(gLThread == null) { "setRenderer has already been called for this instance." }
    }

    private class GLThreadManager {
        @Synchronized
        fun threadExiting(thread: GLThread) {
            if (LOG_THREADS) {
                DebugLog.i("GLThread", "exiting tid=" + thread.id)
            }
            thread.exited = true
            if (eglOwner === thread) {
                eglOwner = null
            }
            notifyAll()
        }

        /*
         * Tries once to acquire the right to use an EGL
         * surface. Does not block. Requires that we are already
         * in the sGLThreadManager monitor when this is called.
         *
         * @return true if the right to use an EGL surface was acquired.
         */
        fun tryAcquireEglSurfaceLocked(thread: GLThread): Boolean {
            if (eglOwner === thread || eglOwner == null) {
                eglOwner = thread
                notifyAll()
                return true
            }
            checkGLESVersion()
            return multipleGLESContextsAllowed
        }

        /*
         * Releases the EGL surface. Requires that we are already in the
         * sGLThreadManager monitor when this is called.
         */
        fun releaseEglSurfaceLocked(thread: GLThread) {
            if (eglOwner === thread) {
                eglOwner = null
            }
            notifyAll()
        }

        @Synchronized
        fun checkGLDriver(gl: GL10?) {
            if (!gLESDriverCheckComplete) {
                checkGLESVersion()
                if (gLESVersion < kGLES_20) {
                    val renderer = gl!!.glGetString(GL10.GL_RENDERER)
                    multipleGLESContextsAllowed = false
                    notifyAll()
                }
                gLESDriverCheckComplete = true
            }
        }

        private fun checkGLESVersion() {
            if (!gLESVersionCheckComplete) {
                gLESVersion = ConfigurationInfo.GL_ES_VERSION_UNDEFINED
                if (gLESVersion >= kGLES_20) {
                    multipleGLESContextsAllowed = true
                }
                gLESVersionCheckComplete = true
            }
        }

        private var gLESVersionCheckComplete = false
        private var gLESVersion = 0
        private var gLESDriverCheckComplete = false
        private var multipleGLESContextsAllowed = false
        private val gLContextCount = 0
        private var eglOwner: GLThread? = null

        companion object {
            private const val kGLES_20 = 0x20000
        }
    }

    private var mSizeChanged = true
    private var gLThread: GLThread? = null
    private var eGLConfigChooser: EGLConfigChooser? = null
    private var eGLContextFactory: EGLContextFactory? = null
    private var eGLWindowSurfaceFactory: EGLWindowSurfaceFactory? = null
    private var gLWrapper: GLWrapper? = null
    /**
     * Get the current value of the debug flags.
     * @return the current value of the debug flags.
     */
    /**
     * Set the debug flags to a new value. The value is
     * constructed by OR-together zero or more
     * of the DEBUG_CHECK_* constants. The debug flags take effect
     * whenever a surface is created. The default value is zero.
     * debugFlags the new debug flags
     * @see .DEBUG_CHECK_GL_ERROR
     *
     * @see .DEBUG_LOG_GL_CALLS
     */
    var debugFlags = 0
    private var eGLContextClientVersion = 0

    companion object {
        private const val LOG_THREADS = false
        private const val LOG_SURFACE = true
        private const val LOG_RENDERER = false

        // Work-around for bug 2263168
        private const val DRAW_TWICE_AFTER_SIZE_CHANGED = true

        /**
         * The renderer only renders
         * when the surface is created, or when [.requestRender] is called.
         *
         * @see .getRenderMode
         * @see .setRenderMode
         */
        const val RENDERMODE_WHEN_DIRTY = 0

        /**
         * The renderer is called
         * continuously to re-render the scene.
         *
         * @see .getRenderMode
         * @see .setRenderMode
         * @see .requestRender
         */
        const val RENDERMODE_CONTINUOUSLY = 1

        /**
         * Check glError() after every GL call and throw an exception if glError indicates
         * that an error has occurred. This can be used to help track down which OpenGL ES call
         * is causing an error.
         *
         * @see .getDebugFlags
         *
         * @see .setDebugFlags
         */
        const val DEBUG_CHECK_GL_ERROR = 1

        /**
         * Log GL calls to the system log at "verbose" level with tag "GLSurfaceView".
         *
         * @see .getDebugFlags
         *
         * @see .setDebugFlags
         */
        const val DEBUG_LOG_GL_CALLS = 2
        private val sGLThreadManager = GLThreadManager()
    }
}
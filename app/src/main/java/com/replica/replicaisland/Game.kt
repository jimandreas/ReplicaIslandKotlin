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
@file:Suppress("CascadeIf")

package com.replica.replicaisland

import android.content.Context
import android.os.Build
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.Toast

/**
 * High-level setup object for the AndouKun game engine.
 * This class sets up the core game engine objects and threads.  It also passes events to the
 * game thread from the main UI thread.
 */
class Game : AllocationGuard() {
    private var gameThread: GameThread? = null
    private var mGame: Thread? = null
    private var mGameRoot: ObjectManager? = null
    var renderer: GameRenderer? = null
        private set
    private var surfaceView: GLSurfaceView? = null
    private var mRunning = false
    private var bootstrapComplete = false
    private var pendingLevel: LevelTree.Level? = null
    private var currentLevel: LevelTree.Level? = null
    private var lastLevel: LevelTree.Level? = null
    private var gLDataLoaded = false
    private val mContextParameters: ContextParameters = ContextParameters()
    private var touchFilter: TouchFilter? = null

    /**
     * Creates core game objects and constructs the game engine object graph.  Note that the
     * game does not actually begin running after this function is called (see start() below).
     * Also note that textures are not loaded from the resource pack by this function, as OpenGl
     * isn't yet available.
     */
    fun bootstrap(
        context: Context,
        viewWidth: Int,
        viewHeight: Int,
        gameWidth: Int,
        gameHeight: Int,
        difficulty: Int) {

        if (!bootstrapComplete) {
            renderer = GameRenderer(context, this, gameWidth, gameHeight)

            // Create core systems
            BaseObject.sSystemRegistry.openGLSystem = OpenGLSystem(null)
            BaseObject.sSystemRegistry.customToastSystem = CustomToastSystem(context)
            val params = mContextParameters
            params.viewWidth = viewWidth
            params.viewHeight = viewHeight
            params.gameWidth = gameWidth
            params.gameHeight = gameHeight
            params.viewScaleX = viewWidth.toFloat() / gameWidth
            params.viewScaleY = viewHeight.toFloat() / gameHeight
            params.context = context
            params.difficulty = difficulty
            BaseObject.sSystemRegistry.contextParameters = params
            val sdkVersion = Build.VERSION.SDK_INT
            touchFilter = if (sdkVersion < Build.VERSION_CODES.ECLAIR) {
                SingleTouchFilter()
            } else {
                MultiTouchFilter()
            }

            // Short-term textures are cleared between levels.
            val shortTermTextureLibrary = TextureLibrary()
            BaseObject.sSystemRegistry.shortTermTextureLibrary = shortTermTextureLibrary

            // Long-term textures persist between levels.
            val longTermTextureLibrary = TextureLibrary()
            BaseObject.sSystemRegistry.longTermTextureLibrary = longTermTextureLibrary

            // The buffer library manages hardware VBOs.
            BaseObject.sSystemRegistry.bufferLibrary = BufferLibrary()
            BaseObject.sSystemRegistry.soundSystem = SoundSystem()

            // The root of the game graph.
            val gameRoot = MainLoop()
            val input = InputSystem()
            BaseObject.sSystemRegistry.inputSystem = input
            BaseObject.sSystemRegistry.registerForReset(input)
            val windowMgr = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val rotationIndex = windowMgr.defaultDisplay.rotation
            input.setTheScreenRotation(rotationIndex)
            val inputInterface = InputGameInterface()
            gameRoot.add(inputInterface)
            BaseObject.sSystemRegistry.inputGameInterface = inputInterface
            val level = LevelSystem()
            BaseObject.sSystemRegistry.levelSystem = level
            val collision = CollisionSystem()
            BaseObject.sSystemRegistry.collisionSystem = collision
            BaseObject.sSystemRegistry.hitPointPool = HitPointPool()
            val gameManager = GameObjectManager((params.viewWidth * 2).toFloat())
            BaseObject.sSystemRegistry.gameObjectManager = gameManager
            val objectFactory = GameObjectFactory()
            BaseObject.sSystemRegistry.gameObjectFactory = objectFactory
            BaseObject.sSystemRegistry.hotSpotSystem = HotSpotSystem()
            BaseObject.sSystemRegistry.levelBuilder = LevelBuilder()
            BaseObject.sSystemRegistry.channelSystem = ChannelSystem()
            BaseObject.sSystemRegistry.registerForReset(BaseObject.sSystemRegistry.channelSystem!!)
            val camera = CameraSystem()
            BaseObject.sSystemRegistry.cameraSystem = camera
            BaseObject.sSystemRegistry.registerForReset(camera)
            collision.loadCollisionTiles(context.resources.openRawResource(R.raw.collision))
            gameRoot.add(gameManager)

            // Camera must come after the game manager so that the camera target moves before the camera
            // centers.
            gameRoot.add(camera)


            // More basic systems.
            val dynamicCollision = GameObjectCollisionSystem()
            gameRoot.add(dynamicCollision)
            BaseObject.sSystemRegistry.gameObjectCollisionSystem = dynamicCollision
            val renderer = RenderSystem()
            BaseObject.sSystemRegistry.renderSystem = renderer
            BaseObject.sSystemRegistry.vectorPool = VectorPool()
            BaseObject.sSystemRegistry.drawableFactory = DrawableFactory()
            val hud = HudSystem()
            hud.setFuelDrawable(
                    DrawableBitmap(longTermTextureLibrary.allocateTexture(
                            R.drawable.ui_bar), 0, 0),
                    DrawableBitmap(longTermTextureLibrary.allocateTexture(
                            R.drawable.ui_bar_bg), 0, 0))
            hud.setFadeTexture(longTermTextureLibrary.allocateTexture(R.drawable.black))
            hud.setButtonDrawables(
                    DrawableBitmap(longTermTextureLibrary.allocateTexture(
                            R.drawable.ui_button_fly_disabled), 0, 0),
                    DrawableBitmap(longTermTextureLibrary.allocateTexture(
                            R.drawable.ui_button_fly_off), 0, 0),
                    DrawableBitmap(longTermTextureLibrary.allocateTexture(
                            R.drawable.ui_button_fly_on), 0, 0),
                    DrawableBitmap(longTermTextureLibrary.allocateTexture(
                            R.drawable.ui_button_stomp_off), 0, 0),
                    DrawableBitmap(longTermTextureLibrary.allocateTexture(
                            R.drawable.ui_button_stomp_on), 0, 0),
                    DrawableBitmap(longTermTextureLibrary.allocateTexture(
                            R.drawable.ui_movement_slider_base), 0, 0),
                    DrawableBitmap(longTermTextureLibrary.allocateTexture(
                            R.drawable.ui_movement_slider_button_off), 0, 0),
                    DrawableBitmap(longTermTextureLibrary.allocateTexture(
                            R.drawable.ui_movement_slider_button_on), 0, 0))
            val digitTextures = arrayOf(
                    longTermTextureLibrary.allocateTexture(R.drawable.ui_0),
                    longTermTextureLibrary.allocateTexture(R.drawable.ui_1),
                    longTermTextureLibrary.allocateTexture(R.drawable.ui_2),
                    longTermTextureLibrary.allocateTexture(R.drawable.ui_3),
                    longTermTextureLibrary.allocateTexture(R.drawable.ui_4),
                    longTermTextureLibrary.allocateTexture(R.drawable.ui_5),
                    longTermTextureLibrary.allocateTexture(R.drawable.ui_6),
                    longTermTextureLibrary.allocateTexture(R.drawable.ui_7),
                    longTermTextureLibrary.allocateTexture(R.drawable.ui_8),
                    longTermTextureLibrary.allocateTexture(R.drawable.ui_9)
            )
            val digits = arrayOf(
                    DrawableBitmap(digitTextures[0], 0, 0),
                    DrawableBitmap(digitTextures[1], 0, 0),
                    DrawableBitmap(digitTextures[2], 0, 0),
                    DrawableBitmap(digitTextures[3], 0, 0),
                    DrawableBitmap(digitTextures[4], 0, 0),
                    DrawableBitmap(digitTextures[5], 0, 0),
                    DrawableBitmap(digitTextures[6], 0, 0),
                    DrawableBitmap(digitTextures[7], 0, 0),
                    DrawableBitmap(digitTextures[8], 0, 0),
                    DrawableBitmap(digitTextures[9], 0, 0)
            )
            val xDrawable = DrawableBitmap(
                    longTermTextureLibrary.allocateTexture(R.drawable.ui_x), 0, 0)
            hud.setDigitDrawables(digits, xDrawable)
            hud.setCollectableDrawables(
                    DrawableBitmap(
                            longTermTextureLibrary.allocateTexture(R.drawable.ui_pearl), 0, 0),
                    DrawableBitmap(
                            longTermTextureLibrary.allocateTexture(R.drawable.ui_gem), 0, 0))
            BaseObject.sSystemRegistry.hudSystem = hud
            if (AndouKun.VERSION < 0) {
                hud.setShowFPS(true)
            }
            gameRoot.add(hud)
            BaseObject.sSystemRegistry.vibrationSystem = VibrationSystem()
            val eventRecorder = EventRecorder()
            BaseObject.sSystemRegistry.eventRecorder = eventRecorder
            BaseObject.sSystemRegistry.registerForReset(eventRecorder)
            gameRoot.add(collision)

            // debug systems
            //BaseObject.sSystemRegistry.debugSystem = new DebugSystem(longTermTextureLibrary);
            //dynamicCollision.setDebugPrefs(false, true);
            objectFactory.preloadEffects()
            mGameRoot = gameRoot
            gameThread = GameThread(this.renderer!!)
            gameThread!!.setGameRoot(mGameRoot)
            currentLevel = null
            bootstrapComplete = true
        }
    }

    @Synchronized
    private fun stopLevel() {
        stop()
        val manager = BaseObject.sSystemRegistry.gameObjectManager
        manager!!.destroyAll()
        manager.commitUpdates()

        //TODO: it's not strictly necessary to clear the static data here, but if I don't do it
        // then two things happen: first, the static data will refer to junk Texture objects, and
        // second, memory that may not be needed for the next level will hang around.  One solution
        // would be to break up the texture library into static and non-static things, and
        // then selectively clear static game components based on their usefulness next level,
        // but this is way simpler.
        val factory = BaseObject.sSystemRegistry.gameObjectFactory
        factory!!.clearStaticData()
        factory.sanityCheckPools()

        // Reset the level
        BaseObject.sSystemRegistry.levelSystem!!.reset()

        // Ensure sounds have stopped.
        BaseObject.sSystemRegistry.soundSystem!!.stopAll()

        // Reset systems that need it.
        BaseObject.sSystemRegistry.reset()

        // Dump the short-term texture objects only.
        surfaceView!!.flushTextures(BaseObject.sSystemRegistry.shortTermTextureLibrary)
        BaseObject.sSystemRegistry.shortTermTextureLibrary!!.removeAll()
        surfaceView!!.flushBuffers(BaseObject.sSystemRegistry.bufferLibrary)
        BaseObject.sSystemRegistry.bufferLibrary!!.removeAll()
    }

    @Synchronized
    fun requestNewLevel() {
        // tell the Renderer to call us back when the
        // render thread is ready to manage some texture memory.
        renderer!!.requestCallback()
    }

    @Synchronized
    fun restartLevel() {
        DebugLog.d("AndouKun", "Restarting...")
        val level = currentLevel
        stop()

        // Destroy all game objects and respawn them.  No need to destroy other systems.
        val manager = BaseObject.sSystemRegistry.gameObjectManager
        manager!!.destroyAll()
        manager.commitUpdates()

        // Ensure sounds have stopped.
        BaseObject.sSystemRegistry.soundSystem!!.stopAll()

        // Reset systems that need it.
        BaseObject.sSystemRegistry.reset()
        val levelSystem = BaseObject.sSystemRegistry.levelSystem
        levelSystem!!.incrementAttemptsCount()
        levelSystem.spawnObjects()
        BaseObject.sSystemRegistry.hudSystem!!.startFade(true, 0.2f)
        currentLevel = level
        pendingLevel = null
        start()
    }

    @Synchronized
    private fun goToLevel(level: LevelTree.Level) {
        val params = BaseObject.sSystemRegistry.contextParameters
        BaseObject.sSystemRegistry.levelSystem!!.loadLevel(level,
                params!!.context!!.resources.openRawResource(level.resource), mGameRoot!!)
        val context = params.context
        renderer!!.setContext(context!!)
        surfaceView!!.loadTextures(BaseObject.sSystemRegistry.longTermTextureLibrary)
        surfaceView!!.loadTextures(BaseObject.sSystemRegistry.shortTermTextureLibrary)
        surfaceView!!.loadBuffers(BaseObject.sSystemRegistry.bufferLibrary)
        gLDataLoaded = true
        currentLevel = level
        pendingLevel = null
        val time = BaseObject.sSystemRegistry.timeSystem
        time!!.reset()
        val hud = BaseObject.sSystemRegistry.hudSystem
        hud?.startFade(true, 1.0f)
        val toast = BaseObject.sSystemRegistry.customToastSystem
        if (toast != null) {
            if (level.inThePast) {
                toast.toast(context.getString(R.string.memory_playback_start), Toast.LENGTH_LONG)
            } else {
                if (lastLevel != null && lastLevel!!.inThePast) {
                    toast.toast(context.getString(R.string.memory_playback_complete), Toast.LENGTH_LONG)
                }
            }
        }
        lastLevel = level
        start()
    }

    /** Starts the game running.  */
    fun start() {
        if (!mRunning) {
            //TODO 2 - fix assert(mGame == null)
            // Now's a good time to run the GC.
            val r = Runtime.getRuntime()
            r.gc()
            DebugLog.d("AndouKun", "Start!")
            mGame = Thread(gameThread)
            mGame!!.name = "Game"
            mGame!!.start()
            mRunning = true
            guardActive = false
        } else {
            gameThread!!.resumeGame()
        }
    }

    fun stop() {
        if (mRunning) {
            DebugLog.d("AndouKun", "Stop!")
            if (gameThread!!.paused) {
                gameThread!!.resumeGame()
            }
            gameThread!!.stopGame()
            try {
                mGame!!.join()
            } catch (e: InterruptedException) {
                mGame!!.interrupt()
            }
            mGame = null
            mRunning = false
            currentLevel = null
            guardActive = false
        }
    }

    fun onTrackballEvent(event: MotionEvent): Boolean {
        if (mRunning) {
            if (event.action == MotionEvent.ACTION_MOVE) {
                BaseObject.sSystemRegistry.inputSystem!!.roll(event.rawX, event.rawY)
            } else if (event.action == MotionEvent.ACTION_DOWN) {
                onKeyDownEvent(KeyEvent.KEYCODE_DPAD_CENTER)
            } else if (event.action == MotionEvent.ACTION_UP) {
                onKeyUpEvent(KeyEvent.KEYCODE_DPAD_CENTER)
            }
        }
        return true
    }

    fun onOrientationEvent(x: Float, y: Float, z: Float): Boolean {
        if (mRunning) {
            BaseObject.sSystemRegistry.inputSystem!!.setOrientation(x, y, z)
        }
        return true
    }

    fun onTouchEvent(event: MotionEvent?): Boolean {
        if (mRunning) {
            touchFilter!!.updateTouch(event)
        }
        return true
    }

    fun onKeyDownEvent(keyCode: Int): Boolean {
        val result = false
        if (mRunning) {
            BaseObject.sSystemRegistry.inputSystem!!.keyDown(keyCode)
        }
        return result
    }

    fun onKeyUpEvent(keyCode: Int): Boolean {
        val result = false
        if (mRunning) {
            BaseObject.sSystemRegistry.inputSystem!!.keyUp(keyCode)
        }
        return result
    }

    fun onPause() {
        if (mRunning) {
            gameThread!!.pauseGame()
        }
    }

    fun onResume(context: Context?, force: Boolean) {
        if (force && mRunning) {
            gameThread!!.resumeGame()
        } else {
            renderer!!.setContext(context!!)
            // Don't explicitly resume the game here.  We'll do that in
            // the SurfaceReady() callback, which will prevent the game
            // starting before the render thread is ready to go.
            BaseObject.sSystemRegistry.contextParameters!!.context = context
        }
    }

    fun onSurfaceReady() {
        DebugLog.d("AndouKun", "Surface Ready")
        if (pendingLevel != null && pendingLevel !== currentLevel) {
            if (mRunning) {
                stopLevel()
            }
            goToLevel(pendingLevel!!)
        } else if (gameThread!!.paused && mRunning) {
            gameThread!!.resumeGame()
        }
    }

    fun setSurfaceView(view: GLSurfaceView?) {
        surfaceView = view
    }

    fun onSurfaceLost() {
        DebugLog.d("AndouKun", "Surface Lost")
        BaseObject.sSystemRegistry.shortTermTextureLibrary!!.invalidateAll()
        BaseObject.sSystemRegistry.longTermTextureLibrary!!.invalidateAll()
        BaseObject.sSystemRegistry.bufferLibrary!!.invalidateHardwareBuffers()
        gLDataLoaded = false
    }

    fun onSurfaceCreated() {
        DebugLog.d("AndouKun", "Surface Created")

        // TODO: this is dumb.  SurfaceView doesn't need to control everything here.
        // GL should just be passed to this function and then set up directly.
        if (!gLDataLoaded && gameThread!!.paused && mRunning && pendingLevel == null) {
            surfaceView!!.loadTextures(BaseObject.sSystemRegistry.longTermTextureLibrary)
            surfaceView!!.loadTextures(BaseObject.sSystemRegistry.shortTermTextureLibrary)
            surfaceView!!.loadBuffers(BaseObject.sSystemRegistry.bufferLibrary)
            gLDataLoaded = true
        }
    }

    fun setPendingLevel(level: LevelTree.Level?) {
        pendingLevel = level
    }

    fun setSoundEnabled(soundEnabled: Boolean) {
        BaseObject.sSystemRegistry.soundSystem!!.soundEnabled = soundEnabled
    }

    fun setControlOptions(clickAttack: Boolean,
                          tiltControls: Boolean, tiltSensitivity: Int, movementSensitivity: Int, onScreenControls: Boolean) {
        BaseObject.sSystemRegistry.inputGameInterface!!.setUseClickForAttack(clickAttack)
        BaseObject.sSystemRegistry.inputGameInterface!!.setUseOrientationForMovement(tiltControls)
        BaseObject.sSystemRegistry.inputGameInterface!!.setOrientationMovementSensitivity(tiltSensitivity / 100.0f)
        BaseObject.sSystemRegistry.inputGameInterface!!.setMovementSensitivity(movementSensitivity / 100.0f)
        BaseObject.sSystemRegistry.inputGameInterface!!.setUseOnScreenControls(onScreenControls)
        BaseObject.sSystemRegistry.hudSystem!!.setMovementSliderMode(onScreenControls)
    }

    fun setSafeMode(safe: Boolean) {
        surfaceView!!.setSafeMode(safe)
    }

    val gameTime: Float
        get() = BaseObject.sSystemRegistry.timeSystem!!.gameTime
    val lastDeathPosition: Vector2?
        get() = BaseObject.sSystemRegistry.eventRecorder!!.lastDeathPosition
    var lastEnding: Int
        get() = BaseObject.sSystemRegistry.eventRecorder!!.lastEnding
        set(ending) {
            BaseObject.sSystemRegistry.eventRecorder!!.lastEnding = ending
        }
    val isPaused: Boolean
        get() = mRunning && gameThread != null && gameThread!!.paused

    fun setKeyConfig(leftKey: Int, rightKey: Int, jumpKey: Int,
                     attackKey: Int) {
        BaseObject.sSystemRegistry.inputGameInterface!!.setKeys(leftKey, rightKey, jumpKey, attackKey)
    }

    val robotsDestroyed: Int
        get() = BaseObject.sSystemRegistry.eventRecorder!!.robotsDestroyed
    val pearlsCollected: Int
        get() = BaseObject.sSystemRegistry.eventRecorder!!.pearlsCollected
    val pearlsTotal: Int
        get() = BaseObject.sSystemRegistry.eventRecorder!!.pearlsTotal

}
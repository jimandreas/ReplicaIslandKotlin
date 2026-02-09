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
@file:Suppress("CascadeIf",
    "DEPRECATION",
    "KotlinConstantConditions",
    "SimplifyBooleanWithConstants"
)

package com.replica.replicaisland

import android.content.Context
import android.os.Build
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.Toast
import com.replica.replicaisland.core.BaseObject
import com.replica.replicaisland.core.GameObjectFactory
import com.replica.replicaisland.core.GameObjectManager
import com.replica.replicaisland.entities.HitPointPool
import com.replica.replicaisland.input.InputGameInterface
import com.replica.replicaisland.input.InputSystem
import com.replica.replicaisland.input.MultiTouchFilter
import com.replica.replicaisland.input.SingleTouchFilter
import com.replica.replicaisland.input.TouchFilter
import com.replica.replicaisland.levels.LevelBuilder
import com.replica.replicaisland.levels.LevelTree
import com.replica.replicaisland.mechanics.ChannelSystem
import com.replica.replicaisland.mechanics.CollisionSystem
import com.replica.replicaisland.mechanics.EventRecorder
import com.replica.replicaisland.mechanics.HotSpotSystem
import com.replica.replicaisland.rendering.CameraSystem
import com.replica.replicaisland.rendering.DrawableBitmap
import com.replica.replicaisland.rendering.DrawableFactory
import com.replica.replicaisland.rendering.OpenGLSystem
import com.replica.replicaisland.rendering.RenderSystem
import com.replica.replicaisland.rendering.TextureLibrary
import com.replica.replicaisland.sound.SoundSystem
import com.replica.replicaisland.ui.CustomToastSystem
import com.replica.replicaisland.ui.DebugLog
import com.replica.replicaisland.ui.HudSystem
import com.replica.replicaisland.utils.AllocationGuard
import com.replica.replicaisland.utils.BufferLibrary
import com.replica.replicaisland.utils.ContextParameters
import com.replica.replicaisland.utils.Vector2
import com.replica.replicaisland.utils.VectorPool

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
    fun bootstrap(context: Context, viewWidth: Int, viewHeight: Int, gameWidth: Int, gameHeight: Int, difficulty: Int) {
        if (!bootstrapComplete) {
            renderer = GameRenderer(context, this, gameWidth, gameHeight)

            // Create core systems
            BaseObject.sSystemRegistry.openGLSystem = OpenGLSystem()
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
            BaseObject.sSystemRegistry.soundSystem!!.preloadSounds()

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
                DrawableBitmap(
                    longTermTextureLibrary.allocateTexture(
                        R.drawable.ui_bar
                    ), 0, 0
                ),
                DrawableBitmap(
                    longTermTextureLibrary.allocateTexture(
                        R.drawable.ui_bar_bg
                    ), 0, 0
                )
            )
            hud.setFadeTexture(longTermTextureLibrary.allocateTexture(R.drawable.black))
            hud.setButtonDrawables(
                DrawableBitmap(
                    longTermTextureLibrary.allocateTexture(
                        R.drawable.ui_button_fly_disabled
                    ), 0, 0
                ),
                DrawableBitmap(
                    longTermTextureLibrary.allocateTexture(
                        R.drawable.ui_button_fly_off
                    ), 0, 0
                ),
                DrawableBitmap(
                    longTermTextureLibrary.allocateTexture(
                        R.drawable.ui_button_fly_on
                    ), 0, 0
                ),
                DrawableBitmap(
                    longTermTextureLibrary.allocateTexture(
                        R.drawable.ui_button_stomp_off
                    ), 0, 0
                ),
                DrawableBitmap(
                    longTermTextureLibrary.allocateTexture(
                        R.drawable.ui_button_stomp_on
                    ), 0, 0
                ),
                DrawableBitmap(
                    longTermTextureLibrary.allocateTexture(
                        R.drawable.ui_movement_slider_base
                    ), 0, 0
                ),
                DrawableBitmap(
                    longTermTextureLibrary.allocateTexture(
                        R.drawable.ui_movement_slider_button_off
                    ), 0, 0
                ),
                DrawableBitmap(
                    longTermTextureLibrary.allocateTexture(
                        R.drawable.ui_movement_slider_button_on
                    ), 0, 0
                )
            )
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
                longTermTextureLibrary.allocateTexture(R.drawable.ui_x), 0, 0
            )
            hud.setDigitDrawables(digits, xDrawable)
            hud.setCollectableDrawables(
                DrawableBitmap(
                    longTermTextureLibrary.allocateTexture(R.drawable.ui_pearl), 0, 0
                ),
                DrawableBitmap(
                    longTermTextureLibrary.allocateTexture(R.drawable.ui_gem), 0, 0
                )
            )
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
        renderer!!.flushTextures(BaseObject.sSystemRegistry.shortTermTextureLibrary)
        BaseObject.sSystemRegistry.shortTermTextureLibrary!!.removeAll()
        renderer!!.flushBuffers(BaseObject.sSystemRegistry.bufferLibrary)
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
        renderer!!.loadTextures(BaseObject.sSystemRegistry.longTermTextureLibrary)
        renderer!!.loadTextures(BaseObject.sSystemRegistry.shortTermTextureLibrary)
        renderer!!.loadBuffers(BaseObject.sSystemRegistry.bufferLibrary)
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
            sGuardActive = false
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
            } catch (_: InterruptedException) {
                mGame!!.interrupt()
            }
            mGame = null
            mRunning = false
            currentLevel = null
            sGuardActive = false
        }
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

    fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (mRunning) {
            val axisX = event.getAxisValue(MotionEvent.AXIS_X)
            val axisY = event.getAxisValue(MotionEvent.AXIS_Y)
            val rightTrigger = event.getAxisValue(MotionEvent.AXIS_RTRIGGER)
            BaseObject.sSystemRegistry.inputSystem?.gamepadAxis(axisX, axisY)
            BaseObject.sSystemRegistry.inputSystem?.setRightTrigger(rightTrigger)
            BaseObject.sSystemRegistry.inputSystem?.setGamepadConnected(true)
        }
        return true
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

    fun onSurfaceLost() {
        DebugLog.d("AndouKun", "Surface Lost")
        BaseObject.sSystemRegistry.shortTermTextureLibrary!!.invalidateAll()
        BaseObject.sSystemRegistry.longTermTextureLibrary!!.invalidateAll()
        BaseObject.sSystemRegistry.bufferLibrary!!.invalidateHardwareBuffers()
        gLDataLoaded = false
    }

    fun onSurfaceCreated() {
        DebugLog.d("AndouKun", "Surface Created")

        if (!gLDataLoaded && gameThread!!.paused && mRunning && pendingLevel == null) {
            renderer!!.loadTextures(BaseObject.sSystemRegistry.longTermTextureLibrary)
            renderer!!.loadTextures(BaseObject.sSystemRegistry.shortTermTextureLibrary)
            renderer!!.loadBuffers(BaseObject.sSystemRegistry.bufferLibrary)
            gLDataLoaded = true
        }
    }

    fun setPendingLevel(level: LevelTree.Level?) {
        pendingLevel = level
    }

    fun setSoundEnabled(soundEnabled: Boolean) {
        BaseObject.sSystemRegistry.soundSystem!!.soundEnabled = soundEnabled
    }

    fun setControlOptions(clickAttack: Boolean, movementSensitivity: Int, onScreenControls: Boolean) {
        BaseObject.sSystemRegistry.inputGameInterface!!.setUseClickForAttack(clickAttack)
        BaseObject.sSystemRegistry.inputGameInterface!!.setMovementSensitivity(movementSensitivity / 100.0f)
        BaseObject.sSystemRegistry.inputGameInterface!!.setUseOnScreenControls(onScreenControls)
        BaseObject.sSystemRegistry.hudSystem!!.setMovementSliderMode(onScreenControls)
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
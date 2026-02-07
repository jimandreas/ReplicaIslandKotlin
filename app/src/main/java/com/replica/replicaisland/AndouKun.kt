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

@file:Suppress("unused",
    "UNUSED_ANONYMOUS_PARAMETER",
    "DEPRECATION",
    "SimplifyBooleanWithConstants",
    "KotlinConstantConditions"
)

package com.replica.replicaisland

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.ActivityInfo
import android.opengl.GLSurfaceView
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.os.Bundle
import android.os.Debug
import android.util.DisplayMetrics
import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.replica.replicaisland.data.PreferencesManager
import com.replica.replicaisland.levels.LevelTree
import com.replica.replicaisland.mechanics.GameFlowEvent
import com.replica.replicaisland.ui.AnimationPlayerActivity
import com.replica.replicaisland.ui.ConversationDialogFragment
import com.replica.replicaisland.ui.DebugLog
import com.replica.replicaisland.ui.DiaryDialogFragment
import com.replica.replicaisland.ui.GameOverActivity
import com.replica.replicaisland.ui.LevelSelectDialogFragment
import com.replica.replicaisland.ui.LevelSelectResult
import com.replica.replicaisland.ui.UIConstants
import java.lang.reflect.InvocationTargetException

/**
 * Core activity for the game.  Sets up a surface view for OpenGL, bootstraps
 * the game engine, and manages UI events.  Also manages game progression,
 * transitioning to other activites, save game, and input events.
 */
class AndouKun : AppCompatActivity(), SensorEventListener {
    private var gLSurfaceView: GLSurfaceView? = null
    private var mGame: Game? = null
    private var methodTracing = false
    private var levelRow = 0
    private var levelIndex = 0
    private var totalGameTime = 0f
    private var robotsDestroyed = 0
    private var pearlsCollected = 0
    private var pearlsTotal = 0
    private var mLastEnding = -1
    private var mLinearMode = 0
    private var difficulty = 1
    private var extrasUnlocked = false
    private var sensorManager: SensorManager? = null
    private lateinit var prefsManager: PreferencesManager
    private var lastTouchTime = 0L
    private var lastRollTime = 0L
    private var pauseMessage: View? = null
    private var waitMessage: View? = null
    private var levelNameBox: View? = null
    private var levelName: TextView? = null
    private var waitFadeAnimation: Animation? = null
    private var eventReporter: EventReporter? = null
    private var eventReporterThread: Thread? = null
    private var sessionId = 0L

    /** Called when the activity is first created.  */
    @SuppressLint("ApplySharedPref", "CommitPrefEdits")
    override fun onCreate(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            super.onCreate(savedInstanceState)
        } else {
            super.onCreate(null)
        }

        // New method of landscape orientation.
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemBars()
        // end of new method of landscape orientation

        prefsManager = PreferencesManager.getInstance(this)
        val debugLogs = prefsManager.getDebugEnabled()
        if (VERSION < 0 || debugLogs) {
            DebugLog.setDebugLogging(true)
        } else {
            DebugLog.setDebugLogging(false)
        }
        DebugLog.d("AndouKun", "onCreate")
        setContentView(R.layout.main)

        // Register fragment result listener for level selection
        supportFragmentManager.setFragmentResultListener(
            LevelSelectResult.REQUEST_KEY,
            this
        ) { _, bundle ->
            handleLevelSelectResult(bundle)
        }

        gLSurfaceView = findViewById<View>(R.id.glsurfaceview) as GLSurfaceView
        pauseMessage = findViewById(R.id.pausedMessage)
        waitMessage = findViewById(R.id.pleaseWaitMessage)
        levelNameBox = findViewById(R.id.levelNameBox)
        levelName = findViewById<View>(R.id.levelName) as TextView
        waitFadeAnimation = AnimationUtils.loadAnimation(this, R.anim.wait_message_fade)


        gLSurfaceView!!.setEGLContextClientVersion(2)
        gLSurfaceView!!.setEGLConfigChooser(false) // 16 bit, no z-buffer
        //gLSurfaceView.setDebugFlags(GLSurfaceView.DEBUG_CHECK_GL_ERROR | GLSurfaceView.DEBUG_LOG_GL_CALLS);
        mGame = Game()
        val dm = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(dm)
        var defaultWidth = 480
        val defaultHeight = 320
        if (dm.widthPixels != defaultWidth) {
            val ratio = dm.widthPixels.toFloat() / dm.heightPixels
            defaultWidth = (defaultHeight * ratio).toInt()
        }
        levelRow = 0
        levelIndex = 0
        // Make sure that old game information is cleared when we start a new game.
        if (intent.getBooleanExtra("newGame", false)) {
            prefsManager.clearGameProgress()
        }
        levelRow = prefsManager.getLevelRow()
        // hackjra
        //levelRow = 10 // hackjra
        levelIndex = prefsManager.getLevelIndex()
        var completed = prefsManager.getLevelCompleted()
        totalGameTime = prefsManager.getTotalGameTime()
        robotsDestroyed = prefsManager.getRobotsDestroyed()
        pearlsCollected = prefsManager.getPearlsCollected()
        pearlsTotal = prefsManager.getPearlsTotal()
        mLinearMode = if (intent.getBooleanExtra("linearMode", false)) 1 else prefsManager.getLinearMode()
        extrasUnlocked = prefsManager.getExtrasUnlocked()
        difficulty = intent.getIntExtra("difficulty", prefsManager.getDifficulty())
        mGame!!.bootstrap(this, dm.widthPixels, dm.heightPixels, defaultWidth, defaultHeight, difficulty)
        gLSurfaceView!!.setRenderer(mGame!!.renderer)
        var levelTreeResource = R.xml.level_tree
        if (mLinearMode != 0) {
            levelTreeResource = R.xml.linear_level_tree
        }


        // Android activity lifecycle rules make it possible for this activity to be created
        // and come to the foreground without the MainMenu Activity ever running, so in that
        // case we need to make sure that this static data is valid.
        if (!LevelTree.isLoaded(levelTreeResource)) {
            LevelTree.loadLevelTree(levelTreeResource, this)
            LevelTree.loadAllDialog(this)
        }
        if (intent.getBooleanExtra("startAtLevelSelect", false)) {
            LevelSelectDialogFragment.newInstance(unlockAll = true)
                .show(supportFragmentManager, LevelSelectDialogFragment.TAG)
        } else {
            if (!LevelTree.levelIsValid(levelRow, levelIndex)) {
                // bad data?  Let's try to recover.

                // is the row valid?
                if (LevelTree.rowIsValid(levelRow)) {
                    // In that case, just start the row over.
                    levelIndex = 0
                    completed = 0
                } else if (LevelTree.rowIsValid(levelRow - 1)) {
                    // If not, try to back up a row.
                    levelRow--
                    levelIndex = 0
                    completed = 0
                }
                if (!LevelTree.levelIsValid(levelRow, levelIndex)) {
                    // if all else fails, start the game over.
                    levelRow = 0
                    levelIndex = 0
                    completed = 0
                }
            }
            LevelTree.updateCompletedState(levelRow, completed)
            mGame!!.setPendingLevel(LevelTree.fetch(levelRow, levelIndex))
            if (LevelTree.fetch(levelRow, levelIndex).showWaitMessage) {
                showWaitMessage()
            } else {
                hideWaitMessage()
            }
        }
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        // This activity uses the media stream.
        volumeControlStream = AudioManager.STREAM_MUSIC
        sessionId = prefsManager.getSessionId().takeIf { it != 0L } ?: System.currentTimeMillis()
        eventReporter = null
        eventReporterThread = null
        val statsEnabled = prefsManager.getStatsEnabled()
        if (statsEnabled) {
            eventReporter = EventReporter()
            eventReporterThread = Thread(eventReporter)
            eventReporterThread!!.name = "EventReporter"
            eventReporterThread!!.start()
        }
    }

    override fun onDestroy() {
        DebugLog.d("AndouKun", "onDestroy()")
        mGame!!.stop()
        if (eventReporterThread != null) {
            eventReporter!!.stop()
            try {
                eventReporterThread!!.join()
            } catch (e: InterruptedException) {
                eventReporterThread!!.interrupt()
            }
        }
        super.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        DebugLog.d("AndouKun", "onPause")
        hidePauseMessage()
        mGame!!.onPause()
        gLSurfaceView!!.onPause()
        mGame!!.renderer!!.onPause() // hack!
        if (methodTracing) {
            Debug.stopMethodTracing()
            methodTracing = false
        }
        if (sensorManager != null) {
            sensorManager!!.unregisterListener(this)
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()

        // Preferences may have changed while we were paused.
        val debugLogs = prefsManager.getDebugEnabled()
        if (VERSION < 0 || debugLogs) {
            DebugLog.setDebugLogging(true)
        } else {
            DebugLog.setDebugLogging(false)
        }
        DebugLog.d("AndouKun", "onResume")
        gLSurfaceView!!.onResume()
        mGame!!.onResume(this, false)
        val soundEnabled = prefsManager.getSoundEnabled()
        val safeMode = prefsManager.getSafeMode()
        val clickAttack = prefsManager.getClickAttack()
        val movementSensitivity = prefsManager.getMovementSensitivity()
        val onScreenControls = prefsManager.getScreenControls()
        val leftKey = prefsManager.getLeftKey()
        val rightKey = prefsManager.getRightKey()
        val jumpKey = prefsManager.getJumpKey()
        val attackKey = prefsManager.getAttackKey()
        mGame!!.setSoundEnabled(soundEnabled)
        mGame!!.setControlOptions(clickAttack, movementSensitivity, onScreenControls)
        mGame!!.setKeyConfig(leftKey, rightKey, jumpKey, attackKey)
        if (sensorManager != null) {
            val orientation = sensorManager!!.getDefaultSensor(Sensor.TYPE_ORIENTATION)
            if (orientation != null) {
                sensorManager!!.registerListener(this,
                        orientation,
                        SensorManager.SENSOR_DELAY_GAME,
                        0)
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
    }

    private fun hideSystemBars() {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!mGame!!.isPaused) {
            mGame!!.onTouchEvent(event)
            val time = System.currentTimeMillis()
            if (event.action == MotionEvent.ACTION_MOVE && time - lastTouchTime < 32) {
                // Sleep so that the main thread doesn't get flooded with UI events.
                try {
                    Thread.sleep(32)
                } catch (e: InterruptedException) {
                    // No big deal if this sleep is interrupted.
                }
                mGame!!.renderer!!.waitDrawingComplete()
            }
            lastTouchTime = time
        }
        return true
    }

    @SuppressLint("GestureBackNavigation")
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        Log.d("claudeopus", "onKeyDown keyCode=$keyCode (${KeyEvent.keyCodeToString(keyCode)})")
        var result = true
        // Check if this is a gamepad BACK button (B button on many controllers)
        val isGamepadSource = (event.source and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
                              (event.source and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
        if (keyCode == KeyEvent.KEYCODE_BACK && isGamepadSource) {
            // Treat gamepad B button as attack, not back/exit
            result = mGame!!.onKeyDownEvent(keyCode)
        } else if (keyCode == KeyEvent.KEYCODE_BACK) {
            val time = System.currentTimeMillis()
            if (time - lastRollTime > ROLL_TO_FACE_BUTTON_DELAY &&
                    time - lastTouchTime > ROLL_TO_FACE_BUTTON_DELAY) {
                showDialog(QUIT_GAME_DIALOG)
                result = true
            }
        } else if (keyCode == KeyEvent.KEYCODE_MENU) {
            result = true
            if (mGame!!.isPaused) {
                hidePauseMessage()
                mGame!!.onResume(this, true)
            } else {
                val time = System.currentTimeMillis()
                if (time - lastRollTime > ROLL_TO_FACE_BUTTON_DELAY &&
                        time - lastTouchTime > ROLL_TO_FACE_BUTTON_DELAY) {
                    showPauseMessage()
                    mGame!!.onPause()
                }
                if (VERSION < 0) {
                    result = false // Allow the debug menu to come up in debug mode.
                }
            }
        } else {
            result = mGame!!.onKeyDownEvent(keyCode)
            // Sleep so that the main thread doesn't get flooded with UI events.
            try {
                Thread.sleep(4)
            } catch (e: InterruptedException) {
                // No big deal if this sleep is interrupted.
            }
        }
        return result
    }

    @SuppressLint("GestureBackNavigation")
    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        var result = false
        // Check if this is a gamepad BACK button (B button on many controllers)
        val isGamepadSource = (event.source and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
                              (event.source and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
        if (keyCode == KeyEvent.KEYCODE_BACK && isGamepadSource) {
            // Treat gamepad B button as attack, not back/exit
            result = mGame!!.onKeyUpEvent(keyCode)
        } else if (keyCode == KeyEvent.KEYCODE_BACK) {
            result = true
        } else if (keyCode == KeyEvent.KEYCODE_MENU) {
            if (VERSION < 0) {
                result = false // Allow the debug menu to come up in debug mode.
            }
        } else {
            result = mGame!!.onKeyUpEvent(keyCode)
            // Sleep so that the main thread doesn't get flooded with UI events.
            try {
                Thread.sleep(4)
            } catch (e: InterruptedException) {
                // No big deal if this sleep is interrupted.
            }
        }
        return result
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        Log.d("claudeopus", "onGenericMotionEvent source=${event.source}")
        if (event.source and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK ||
            event.source and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD) {
            val axisX = event.getAxisValue(MotionEvent.AXIS_X)
            val axisY = event.getAxisValue(MotionEvent.AXIS_Y)
            Log.d("claudeopus", "Gamepad joystick: X=$axisX Y=$axisY")
            return mGame?.onGenericMotionEvent(event) ?: false
        }
        return super.onGenericMotionEvent(event)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        var handled = false
        // Only allow the debug menu in development versions.
        if (VERSION < 0) {
            menu.add(0, CHANGE_LEVEL_ID, 0, R.string.change_level)
            menu.add(0, TEST_ANIMATION_ID, 0, R.string.test_animation)
            menu.add(0, TEST_DIARY_ID, 0, R.string.test_diary)
            menu.add(0, METHOD_TRACING_ID, 0, R.string.method_tracing)
            handled = true
        }
        return handled
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val i: Intent
        when (item.itemId) {
            CHANGE_LEVEL_ID -> {
                LevelSelectDialogFragment.newInstance(unlockAll = false)
                    .show(supportFragmentManager, LevelSelectDialogFragment.TAG)
                return true
            }
            TEST_ANIMATION_ID -> {
                i = Intent(this, AnimationPlayerActivity::class.java)
                i.putExtra("animation", AnimationPlayerActivity.ROKUDOU_ENDING)
                startActivity(i)
                return true
            }
            TEST_DIARY_ID -> {
                DiaryDialogFragment.newInstance(R.string.Diary10)
                    .show(supportFragmentManager, DiaryDialogFragment.TAG)
                return true
            }
            METHOD_TRACING_ID -> {
                if (methodTracing) {
                    Debug.stopMethodTracing()
                } else {
                    Debug.startMethodTracing("andou")
                }
                methodTracing = !methodTracing
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (requestCode == ACTIVITY_ANIMATION_PLAYER && intent != null) {
            val lastAnimation = intent.getIntExtra("animation", -1)
            // record ending events.
            if (lastAnimation > -1) {
                mGame!!.lastEnding = lastAnimation
            }
            // on finishing animation playback, force a level change.
            onGameFlowEvent(GameFlowEvent.EVENT_GO_TO_NEXT_LEVEL, 0)
        }
    }

    /**
     * Handles the result from LevelSelectDialogFragment via Fragment Result API.
     */
    private fun handleLevelSelectResult(bundle: Bundle) {
        levelRow = bundle.getInt(LevelSelectResult.RESULT_ROW)
        levelIndex = bundle.getInt(LevelSelectResult.RESULT_INDEX)
        LevelTree.updateCompletedState(levelRow, 0)

        saveGame()
        mGame!!.setPendingLevel(LevelTree.fetch(levelRow, levelIndex))
        if (LevelTree.fetch(levelRow, levelIndex).showWaitMessage) {
            showWaitMessage()
        } else {
            hideWaitMessage()
        }
    }

    /*
     *  When the game thread needs to stop its own execution (to go to a new level, or restart the
     *  current level), it registers a runnable on the main thread which orders the action via this
     *  function.
     */
    fun onGameFlowEvent(eventCode: Int, index: Int) {
        when (eventCode) {
            GameFlowEvent.EVENT_END_GAME -> {
                mGame!!.stop()
                finish()
            }
            GameFlowEvent.EVENT_RESTART_LEVEL -> {
                if (LevelTree.fetch(levelRow, levelIndex).restartable) {
                    if (eventReporter != null) {
                        eventReporter!!.addEvent(EventReporter.EVENT_DEATH,
                                mGame!!.lastDeathPosition!!.x,
                                mGame!!.lastDeathPosition!!.y,
                                mGame!!.gameTime,
                                LevelTree.fetch(levelRow, levelIndex).name,
                                VERSION,
                                sessionId)
                    }
                    mGame!!.restartLevel()
                    return
                }
                LevelTree.fetch(levelRow, levelIndex).completed = true
                val currentGroup = LevelTree.levels[levelRow]
                val count = currentGroup.levels.size
                var groupCompleted = true
                if (eventReporter != null) {
                    eventReporter!!.addEvent(EventReporter.EVENT_BEAT_LEVEL, 0f, 0f,
                            mGame!!.gameTime,
                            LevelTree.fetch(levelRow, levelIndex).name,
                            VERSION,
                            sessionId)
                }
                var x = 0
                while (x < count) {
                    if (currentGroup.levels[x].completed == false) {
                        // We haven't completed the group yet.
                        levelIndex = x
                        groupCompleted = false
                        break
                    }
                    x++
                }
                if (groupCompleted) {
                    levelIndex = 0
                    levelRow++
                }
                totalGameTime += mGame!!.gameTime
                robotsDestroyed += mGame!!.robotsDestroyed
                pearlsCollected += mGame!!.pearlsCollected
                pearlsTotal += mGame!!.pearlsTotal
                if (levelRow < LevelTree.levels.size) {
                    val currentLevel = LevelTree.fetch(levelRow, levelIndex)
                    if (currentLevel.inThePast || LevelTree.levels[levelRow].levels.size > 1) {
                        // go to the level select.
                        LevelSelectDialogFragment.newInstance(unlockAll = false)
                            .show(supportFragmentManager, LevelSelectDialogFragment.TAG)
                    } else {
                        // go directly to the next level
                        mGame!!.setPendingLevel(currentLevel)
                        if (currentLevel.showWaitMessage) {
                            showWaitMessage()
                        } else {
                            hideWaitMessage()
                        }
                        mGame!!.requestNewLevel()
                    }
                    saveGame()
                } else {
                    if (eventReporter != null) {
                        eventReporter!!.addEvent(EventReporter.EVENT_BEAT_GAME, 0f, 0f,
                                mGame!!.gameTime,
                                "end",
                                VERSION,
                                sessionId)
                    }
                    // We beat the game!
                    levelRow = 0
                    levelIndex = 0
                    mLastEnding = mGame!!.lastEnding
                    extrasUnlocked = true
                    saveGame()
                    mGame!!.stop()
                    val i = Intent(this, GameOverActivity::class.java)
                    startActivity(i)
                    if (UIConstants.mOverridePendingTransition != null) {
                        try {
                            UIConstants.mOverridePendingTransition!!.invoke(this@AndouKun, R.anim.activity_fade_in, R.anim.activity_fade_out)
                        } catch (ite: InvocationTargetException) {
                            DebugLog.d("Activity Transition", "Invocation Target Exception")
                        } catch (ie: IllegalAccessException) {
                            DebugLog.d("Activity Transition", "Illegal Access Exception")
                        }
                    }
                    finish()
                }
            }
            GameFlowEvent.EVENT_GO_TO_NEXT_LEVEL -> {
                LevelTree.fetch(levelRow, levelIndex).completed = true
                val currentGroup = LevelTree.levels[levelRow]
                val count = currentGroup.levels.size
                var groupCompleted = true
                if (eventReporter != null) {
                    eventReporter!!.addEvent(EventReporter.EVENT_BEAT_LEVEL, 0f, 0f,
                            mGame!!.gameTime,
                            LevelTree.fetch(levelRow, levelIndex).name,
                            VERSION,
                            sessionId)
                }
                var x = 0
                while (x < count) {
                    if (currentGroup.levels[x].completed == false) {
                        levelIndex = x
                        groupCompleted = false
                        break
                    }
                    x++
                }
                if (groupCompleted) {
                    levelIndex = 0
                    levelRow++
                }
                totalGameTime += mGame!!.gameTime
                robotsDestroyed += mGame!!.robotsDestroyed
                pearlsCollected += mGame!!.pearlsCollected
                pearlsTotal += mGame!!.pearlsTotal
                if (levelRow < LevelTree.levels.size) {
                    val currentLevel = LevelTree.fetch(levelRow, levelIndex)
                    if (currentLevel.inThePast || LevelTree.levels[levelRow].levels.size > 1) {
                        LevelSelectDialogFragment.newInstance(unlockAll = false)
                            .show(supportFragmentManager, LevelSelectDialogFragment.TAG)
                    } else {
                        mGame!!.setPendingLevel(currentLevel)
                        if (currentLevel.showWaitMessage) {
                            showWaitMessage()
                        } else {
                            hideWaitMessage()
                        }
                        mGame!!.requestNewLevel()
                    }
                    saveGame()
                } else {
                    if (eventReporter != null) {
                        eventReporter!!.addEvent(EventReporter.EVENT_BEAT_GAME, 0f, 0f,
                                mGame!!.gameTime,
                                "end",
                                VERSION,
                                sessionId)
                    }
                    levelRow = 0
                    levelIndex = 0
                    mLastEnding = mGame!!.lastEnding
                    extrasUnlocked = true
                    saveGame()
                    mGame!!.stop()
                    val i = Intent(this, GameOverActivity::class.java)
                    startActivity(i)
                    if (UIConstants.mOverridePendingTransition != null) {
                        try {
                            UIConstants.mOverridePendingTransition!!.invoke(this@AndouKun, R.anim.activity_fade_in, R.anim.activity_fade_out)
                        } catch (ite: InvocationTargetException) {
                            DebugLog.d("Activity Transition", "Invocation Target Exception")
                        } catch (ie: IllegalAccessException) {
                            DebugLog.d("Activity Transition", "Illegal Access Exception")
                        }
                    }
                    finish()
                }
            }
            GameFlowEvent.EVENT_SHOW_DIARY -> {
                val level = LevelTree.fetch(levelRow, levelIndex)
                level.diaryCollected = true
                DiaryDialogFragment.newInstance(level.dialogResources!!.diaryEntry)
                    .show(supportFragmentManager, DiaryDialogFragment.TAG)
            }
            GameFlowEvent.EVENT_SHOW_DIALOG_CHARACTER1 -> {
                ConversationDialogFragment.newInstance(levelRow, levelIndex, index, 1)
                    .show(supportFragmentManager, ConversationDialogFragment.TAG)
            }
            GameFlowEvent.EVENT_SHOW_DIALOG_CHARACTER2 -> {
                ConversationDialogFragment.newInstance(levelRow, levelIndex, index, 2)
                    .show(supportFragmentManager, ConversationDialogFragment.TAG)
            }
            GameFlowEvent.EVENT_SHOW_ANIMATION -> {
                val i = Intent(this, AnimationPlayerActivity::class.java)
                i.putExtra("animation", index)
                startActivityForResult(i, ACTIVITY_ANIMATION_PLAYER)
                if (UIConstants.mOverridePendingTransition != null) {
                    try {
                        UIConstants.mOverridePendingTransition!!.invoke(this@AndouKun, R.anim.activity_fade_in, R.anim.activity_fade_out)
                    } catch (ite: InvocationTargetException) {
                        DebugLog.d("Activity Transition", "Invocation Target Exception")
                    } catch (ie: IllegalAccessException) {
                        DebugLog.d("Activity Transition", "Illegal Access Exception")
                    }
                }
            }
        }
    }

    private fun saveGame() {
        val completed = LevelTree.packCompletedLevels(levelRow)
        prefsManager.saveLevelProgress(levelRow, levelIndex, completed)
        prefsManager.setSessionId(sessionId)
        prefsManager.setTotalGameTime(totalGameTime)
        prefsManager.setLastEnding(mLastEnding)
        prefsManager.saveGameStats(totalGameTime, pearlsCollected, pearlsTotal, robotsDestroyed)
        prefsManager.setLinearMode(mLinearMode)
        prefsManager.setExtrasUnlocked(extrasUnlocked)
        prefsManager.setDifficulty(difficulty)
    }

    private fun showPauseMessage() {
        if (pauseMessage != null) {
            pauseMessage!!.visibility = View.VISIBLE
        }
        if (levelNameBox != null && levelName != null) {
            levelName!!.text = LevelTree.fetch(levelRow, levelIndex).name
            levelNameBox!!.visibility = View.VISIBLE
        }
    }

    private fun hidePauseMessage() {
        if (pauseMessage != null) {
            pauseMessage!!.visibility = View.GONE
        }
        if (levelNameBox != null) {
            levelNameBox!!.visibility = View.GONE
        }
    }

    private fun showWaitMessage() {
        if (waitMessage != null) {
            waitMessage!!.visibility = View.VISIBLE
            waitMessage!!.startAnimation(waitFadeAnimation)
        }
    }

    private fun hideWaitMessage() {
        if (waitMessage != null) {
            waitMessage!!.visibility = View.GONE
            waitMessage!!.clearAnimation()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // TODO Auto-generated method stub
    }

    override fun onSensorChanged(event: SensorEvent) {
        synchronized(this) {
            if (event.sensor.type == Sensor.TYPE_ORIENTATION) {
                val x = event.values[1]
                val y = event.values[2]
                val z = event.values[0]
                mGame!!.onOrientationEvent(x, y, z)
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateDialog(id: Int): Dialog {
        var dialog: Dialog? = null
        if (id == QUIT_GAME_DIALOG) {
            dialog = AlertDialog.Builder(this)
                    .setTitle(R.string.quit_game_dialog_title)
                    .setPositiveButton(R.string.quit_game_dialog_ok) { dialogIn, whichButton ->
                        finish()
                        if (UIConstants.mOverridePendingTransition != null) {
                            try {
                                UIConstants.mOverridePendingTransition!!.invoke(this@AndouKun, R.anim.activity_fade_in, R.anim.activity_fade_out)
                            } catch (ite: InvocationTargetException) {
                                DebugLog.d("Activity Transition", "Invocation Target Exception")
                            } catch (ie: IllegalAccessException) {
                                DebugLog.d("Activity Transition", "Illegal Access Exception")
                            }
                        }
                    }
                    .setNegativeButton(R.string.quit_game_dialog_cancel, null)
                    .setMessage(R.string.quit_game_dialog_message)
                    .create()
        }
        return dialog!!
    }

    companion object {
        private const val ACTIVITY_CHANGE_LEVELS = 0
        private const val ACTIVITY_CONVERSATION = 1
        private const val ACTIVITY_DIARY = 2
        private const val ACTIVITY_ANIMATION_PLAYER = 3
        private const val CHANGE_LEVEL_ID = Menu.FIRST
        private const val TEST_ANIMATION_ID = CHANGE_LEVEL_ID + 1
        private const val TEST_DIARY_ID = CHANGE_LEVEL_ID + 2
        private const val METHOD_TRACING_ID = CHANGE_LEVEL_ID + 3
        private const val ROLL_TO_FACE_BUTTON_DELAY = 400
        const val QUIT_GAME_DIALOG = 0

        // If the version is a negative number, debug features (logging and a debug menu)
        // are enabled.
        // hackjra
        //const val VERSION = -1
       const val VERSION = 14

    }
}
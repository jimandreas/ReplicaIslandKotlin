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
@file:Suppress("unused", "UNUSED_ANONYMOUS_PARAMETER", "DEPRECATION", "RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS", "SimplifyBooleanWithConstants")

package com.replica.replicaisland

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.SharedPreferences.Editor
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.os.Bundle
import android.os.Debug
import android.util.DisplayMetrics
import android.view.*
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.TextView
import java.lang.reflect.InvocationTargetException

/**
 * Core activity for the game.  Sets up a surface view for OpenGL, bootstraps
 * the game engine, and manages UI events.  Also manages game progression,
 * transitioning to other activites, save game, and input events.
 */
class AndouKun : Activity(), SensorEventListener {
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
    private var prefsEditor: Editor? = null
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
        val prefs = getSharedPreferences(PreferenceConstants.PREFERENCE_NAME, MODE_PRIVATE)
        val debugLogs = prefs.getBoolean(PreferenceConstants.PREFERENCE_ENABLE_DEBUG, false)
        if (VERSION < 0 || debugLogs) {
            DebugLog.setDebugLogging(true)
        } else {
            DebugLog.setDebugLogging(false)
        }
        DebugLog.d("AndouKun", "onCreate")
        setContentView(R.layout.main)
        gLSurfaceView = findViewById<View>(R.id.glsurfaceview) as GLSurfaceView
        pauseMessage = findViewById(R.id.pausedMessage)
        waitMessage = findViewById(R.id.pleaseWaitMessage)
        levelNameBox = findViewById(R.id.levelNameBox)
        levelName = findViewById<View>(R.id.levelName) as TextView
        waitFadeAnimation = AnimationUtils.loadAnimation(this, R.anim.wait_message_fade)


        //gLSurfaceView.setGLWrapper(new GLErrorLogger());
        gLSurfaceView!!.setEGLConfigChooser(false) // 16 bit, no z-buffer
        //gLSurfaceView.setDebugFlags(GLSurfaceView.DEBUG_CHECK_GL_ERROR | GLSurfaceView.DEBUG_LOG_GL_CALLS);
        mGame = Game()
        mGame!!.setSurfaceView(gLSurfaceView)
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
        prefsEditor = prefs.edit()
        // Make sure that old game information is cleared when we start a new game.
        if (intent.getBooleanExtra("newGame", false)) {
            prefsEditor!!.remove(PreferenceConstants.PREFERENCE_LEVEL_ROW)
            prefsEditor!!.remove(PreferenceConstants.PREFERENCE_LEVEL_INDEX)
            prefsEditor!!.remove(PreferenceConstants.PREFERENCE_LEVEL_COMPLETED)
            prefsEditor!!.remove(PreferenceConstants.PREFERENCE_LINEAR_MODE)
            prefsEditor!!.remove(PreferenceConstants.PREFERENCE_TOTAL_GAME_TIME)
            prefsEditor!!.remove(PreferenceConstants.PREFERENCE_PEARLS_COLLECTED)
            prefsEditor!!.remove(PreferenceConstants.PREFERENCE_PEARLS_TOTAL)
            prefsEditor!!.remove(PreferenceConstants.PREFERENCE_ROBOTS_DESTROYED)
            prefsEditor!!.remove(PreferenceConstants.PREFERENCE_DIFFICULTY)
            prefsEditor!!.commit()
        }
        levelRow = prefs.getInt(PreferenceConstants.PREFERENCE_LEVEL_ROW, 0)
        levelIndex = prefs.getInt(PreferenceConstants.PREFERENCE_LEVEL_INDEX, 0)
        var completed = prefs.getInt(PreferenceConstants.PREFERENCE_LEVEL_COMPLETED, 0)
        totalGameTime = prefs.getFloat(PreferenceConstants.PREFERENCE_TOTAL_GAME_TIME, 0.0f)
        robotsDestroyed = prefs.getInt(PreferenceConstants.PREFERENCE_ROBOTS_DESTROYED, 0)
        pearlsCollected = prefs.getInt(PreferenceConstants.PREFERENCE_PEARLS_COLLECTED, 0)
        pearlsTotal = prefs.getInt(PreferenceConstants.PREFERENCE_PEARLS_TOTAL, 0)
        mLinearMode = prefs.getInt(PreferenceConstants.PREFERENCE_LINEAR_MODE,
                if (intent.getBooleanExtra("linearMode", false)) 1 else 0)
        extrasUnlocked = prefs.getBoolean(PreferenceConstants.PREFERENCE_EXTRAS_UNLOCKED, false)
        difficulty = prefs.getInt(PreferenceConstants.PREFERENCE_DIFFICULTY, intent.getIntExtra("difficulty", 1))
        mGame!!.bootstrap(this, dm.widthPixels, dm.heightPixels, defaultWidth, defaultHeight, difficulty)
        gLSurfaceView!!.setRenderer(mGame!!.renderer as GLSurfaceView.Renderer)
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
            val i = Intent(this, LevelSelectActivity::class.java)
            i.putExtra("unlockAll", true)
            startActivityForResult(i, ACTIVITY_CHANGE_LEVELS)
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
        sessionId = prefs.getLong(PreferenceConstants.PREFERENCE_SESSION_ID, System.currentTimeMillis())
        eventReporter = null
        eventReporterThread = null
        val statsEnabled = prefs.getBoolean(PreferenceConstants.PREFERENCE_STATS_ENABLED, true)
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

        // Preferences may have changed while we were paused.
        val prefs = getSharedPreferences(PreferenceConstants.PREFERENCE_NAME, MODE_PRIVATE)
        val debugLogs = prefs.getBoolean(PreferenceConstants.PREFERENCE_ENABLE_DEBUG, false)
        if (VERSION < 0 || debugLogs) {
            DebugLog.setDebugLogging(true)
        } else {
            DebugLog.setDebugLogging(false)
        }
        DebugLog.d("AndouKun", "onResume")
        gLSurfaceView!!.onResume()
        mGame!!.onResume(this, false)
        val soundEnabled = prefs.getBoolean(PreferenceConstants.PREFERENCE_SOUND_ENABLED, true)
        val safeMode = prefs.getBoolean(PreferenceConstants.PREFERENCE_SAFE_MODE, false)
        val clickAttack = prefs.getBoolean(PreferenceConstants.PREFERENCE_CLICK_ATTACK, true)
        val tiltControls = prefs.getBoolean(PreferenceConstants.PREFERENCE_TILT_CONTROLS, false)
        val tiltSensitivity = prefs.getInt(PreferenceConstants.PREFERENCE_TILT_SENSITIVITY, 50)
        val movementSensitivity = prefs.getInt(PreferenceConstants.PREFERENCE_MOVEMENT_SENSITIVITY, 100)
        val onScreenControls = prefs.getBoolean(PreferenceConstants.PREFERENCE_SCREEN_CONTROLS, false)
        val leftKey = prefs.getInt(PreferenceConstants.PREFERENCE_LEFT_KEY, KeyEvent.KEYCODE_DPAD_LEFT)
        val rightKey = prefs.getInt(PreferenceConstants.PREFERENCE_RIGHT_KEY, KeyEvent.KEYCODE_DPAD_RIGHT)
        val jumpKey = prefs.getInt(PreferenceConstants.PREFERENCE_JUMP_KEY, KeyEvent.KEYCODE_SPACE)
        val attackKey = prefs.getInt(PreferenceConstants.PREFERENCE_ATTACK_KEY, KeyEvent.KEYCODE_SHIFT_LEFT)
        mGame!!.setSoundEnabled(soundEnabled)
        mGame!!.setControlOptions(clickAttack, tiltControls, tiltSensitivity, movementSensitivity, onScreenControls)
        mGame!!.setKeyConfig(leftKey, rightKey, jumpKey, attackKey)
        mGame!!.setSafeMode(safeMode)
        if (sensorManager != null) {
            val orientation = sensorManager!!.getDefaultSensor(Sensor.TYPE_ORIENTATION)
            if (orientation != null) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                    sensorManager!!.registerListener(this,
                            orientation,
                            SensorManager.SENSOR_DELAY_GAME,
                            0)
                }
            }
        }
    }

    override fun onTrackballEvent(event: MotionEvent): Boolean {
        if (!mGame!!.isPaused) {
            mGame!!.onTrackballEvent(event)
            val time = System.currentTimeMillis()
            lastRollTime = time
        }
        return true
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

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        var result = true
        if (keyCode == KeyEvent.KEYCODE_BACK) {
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

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        var result = false
        if (keyCode == KeyEvent.KEYCODE_BACK) {
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

    override fun onMenuItemSelected(featureId: Int, item: MenuItem): Boolean {
        val i: Intent
        when (item.itemId) {
            CHANGE_LEVEL_ID -> {
                i = Intent(this, LevelSelectActivity::class.java)
                startActivityForResult(i, ACTIVITY_CHANGE_LEVELS)
                return true
            }
            TEST_ANIMATION_ID -> {
                i = Intent(this, AnimationPlayerActivity::class.java)
                i.putExtra("animation", AnimationPlayerActivity.ROKUDOU_ENDING)
                startActivity(i)
                return true
            }
            TEST_DIARY_ID -> {
                i = Intent(this, DiaryActivity::class.java)
                i.putExtra("text", R.string.Diary10)
                startActivity(i)
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
        return super.onMenuItemSelected(featureId, item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (requestCode == ACTIVITY_CHANGE_LEVELS) {
            if (resultCode == RESULT_OK) {

                levelRow = intent.extras!!.getInt("row")
                levelIndex = intent.extras!!.getInt("index")
                LevelTree.updateCompletedState(levelRow, 0)

                saveGame()
                mGame!!.setPendingLevel(LevelTree.fetch(levelRow, levelIndex))
                if (LevelTree.fetch(levelRow, levelIndex).showWaitMessage) {
                    showWaitMessage()
                } else {
                    hideWaitMessage()
                }
            }
        } else if (requestCode == ACTIVITY_ANIMATION_PLAYER) {
            val lastAnimation = intent.getIntExtra("animation", -1)
            // record ending events.
            if (lastAnimation > -1) {
                mGame!!.lastEnding = lastAnimation
            }
            // on finishing animation playback, force a level change.
            onGameFlowEvent(GameFlowEvent.EVENT_GO_TO_NEXT_LEVEL, 0)
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
                        val i = Intent(this, LevelSelectActivity::class.java)
                        startActivityForResult(i, ACTIVITY_CHANGE_LEVELS)
                        if (UIConstants.mOverridePendingTransition != null) {
                            try {
                                UIConstants.mOverridePendingTransition!!.invoke(this@AndouKun, R.anim.activity_fade_in, R.anim.activity_fade_out)
                            } catch (ite: InvocationTargetException) {
                                DebugLog.d("Activity Transition", "Invocation Target Exception")
                            } catch (ie: IllegalAccessException) {
                                DebugLog.d("Activity Transition", "Illegal Access Exception")
                            }
                        }
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
                        val i = Intent(this, LevelSelectActivity::class.java)
                        startActivityForResult(i, ACTIVITY_CHANGE_LEVELS)
                        if (UIConstants.mOverridePendingTransition != null) {
                            try {
                                UIConstants.mOverridePendingTransition!!.invoke(this@AndouKun, R.anim.activity_fade_in, R.anim.activity_fade_out)
                            } catch (ite: InvocationTargetException) {
                                DebugLog.d("Activity Transition", "Invocation Target Exception")
                            } catch (ie: IllegalAccessException) {
                                DebugLog.d("Activity Transition", "Illegal Access Exception")
                            }
                        }
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
                val i = Intent(this, DiaryActivity::class.java)
                val level = LevelTree.fetch(levelRow, levelIndex)
                level.diaryCollected = true
                i.putExtra("text", level.dialogResources!!.diaryEntry)
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
            }
            GameFlowEvent.EVENT_SHOW_DIALOG_CHARACTER1 -> {
                val i = Intent(this, ConversationDialogActivity::class.java)
                i.putExtra("levelRow", levelRow)
                i.putExtra("levelIndex", levelIndex)
                i.putExtra("index", index)
                i.putExtra("character", 1)
                startActivity(i)
            }
            GameFlowEvent.EVENT_SHOW_DIALOG_CHARACTER2 -> {
                val i = Intent(this, ConversationDialogActivity::class.java)
                i.putExtra("levelRow", levelRow)
                i.putExtra("levelIndex", levelIndex)
                i.putExtra("index", index)
                i.putExtra("character", 2)
                startActivity(i)
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
        if (prefsEditor != null) {
            val completed = LevelTree.packCompletedLevels(levelRow)
            prefsEditor!!.putInt(PreferenceConstants.PREFERENCE_LEVEL_ROW, levelRow)
            prefsEditor!!.putInt(PreferenceConstants.PREFERENCE_LEVEL_INDEX, levelIndex)
            prefsEditor!!.putInt(PreferenceConstants.PREFERENCE_LEVEL_COMPLETED, completed)
            prefsEditor!!.putLong(PreferenceConstants.PREFERENCE_SESSION_ID, sessionId)
            prefsEditor!!.putFloat(PreferenceConstants.PREFERENCE_TOTAL_GAME_TIME, totalGameTime)
            prefsEditor!!.putInt(PreferenceConstants.PREFERENCE_LAST_ENDING, mLastEnding)
            prefsEditor!!.putInt(PreferenceConstants.PREFERENCE_ROBOTS_DESTROYED, robotsDestroyed)
            prefsEditor!!.putInt(PreferenceConstants.PREFERENCE_PEARLS_COLLECTED, pearlsCollected)
            prefsEditor!!.putInt(PreferenceConstants.PREFERENCE_PEARLS_TOTAL, pearlsTotal)
            prefsEditor!!.putInt(PreferenceConstants.PREFERENCE_LINEAR_MODE, mLinearMode)
            prefsEditor!!.putBoolean(PreferenceConstants.PREFERENCE_EXTRAS_UNLOCKED, extrasUnlocked)
            prefsEditor!!.putInt(PreferenceConstants.PREFERENCE_DIFFICULTY, difficulty)
            prefsEditor!!.commit()
        }
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
        const val VERSION = 14
    }
}
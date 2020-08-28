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
    private var mGLSurfaceView: GLSurfaceView? = null
    private var mGame: Game? = null
    private var mMethodTracing = false
    private var mLevelRow = 0
    private var mLevelIndex = 0
    private var mTotalGameTime = 0f
    private var mRobotsDestroyed = 0
    private var mPearlsCollected = 0
    private var mPearlsTotal = 0
    private var mLastEnding = -1
    private var mLinearMode = 0
    private var mDifficulty = 1
    private var mExtrasUnlocked = false
    private var mSensorManager: SensorManager? = null
    private var mPrefsEditor: Editor? = null
    private var mLastTouchTime = 0L
    private var mLastRollTime = 0L
    private var mPauseMessage: View? = null
    private var mWaitMessage: View? = null
    private var mLevelNameBox: View? = null
    private var mLevelName: TextView? = null
    private var mWaitFadeAnimation: Animation? = null
    private var mEventReporter: EventReporter? = null
    private var mEventReporterThread: Thread? = null
    private var mSessionId = 0L

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
        mGLSurfaceView = findViewById<View>(R.id.glsurfaceview) as GLSurfaceView
        mPauseMessage = findViewById(R.id.pausedMessage)
        mWaitMessage = findViewById(R.id.pleaseWaitMessage)
        mLevelNameBox = findViewById(R.id.levelNameBox)
        mLevelName = findViewById<View>(R.id.levelName) as TextView
        mWaitFadeAnimation = AnimationUtils.loadAnimation(this, R.anim.wait_message_fade)


        //mGLSurfaceView.setGLWrapper(new GLErrorLogger());
        mGLSurfaceView!!.setEGLConfigChooser(false) // 16 bit, no z-buffer
        //mGLSurfaceView.setDebugFlags(GLSurfaceView.DEBUG_CHECK_GL_ERROR | GLSurfaceView.DEBUG_LOG_GL_CALLS);
        mGame = Game()
        mGame!!.setSurfaceView(mGLSurfaceView)
        val dm = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(dm)
        var defaultWidth = 480
        val defaultHeight = 320
        if (dm.widthPixels != defaultWidth) {
            val ratio = dm.widthPixels.toFloat() / dm.heightPixels
            defaultWidth = (defaultHeight * ratio).toInt()
        }
        mLevelRow = 0
        mLevelIndex = 0
        mPrefsEditor = prefs.edit()
        // Make sure that old game information is cleared when we start a new game.
        if (intent.getBooleanExtra("newGame", false)) {
            mPrefsEditor!!.remove(PreferenceConstants.PREFERENCE_LEVEL_ROW)
            mPrefsEditor!!.remove(PreferenceConstants.PREFERENCE_LEVEL_INDEX)
            mPrefsEditor!!.remove(PreferenceConstants.PREFERENCE_LEVEL_COMPLETED)
            mPrefsEditor!!.remove(PreferenceConstants.PREFERENCE_LINEAR_MODE)
            mPrefsEditor!!.remove(PreferenceConstants.PREFERENCE_TOTAL_GAME_TIME)
            mPrefsEditor!!.remove(PreferenceConstants.PREFERENCE_PEARLS_COLLECTED)
            mPrefsEditor!!.remove(PreferenceConstants.PREFERENCE_PEARLS_TOTAL)
            mPrefsEditor!!.remove(PreferenceConstants.PREFERENCE_ROBOTS_DESTROYED)
            mPrefsEditor!!.remove(PreferenceConstants.PREFERENCE_DIFFICULTY)
            mPrefsEditor!!.commit()
        }
        mLevelRow = prefs.getInt(PreferenceConstants.PREFERENCE_LEVEL_ROW, 0)
        mLevelIndex = prefs.getInt(PreferenceConstants.PREFERENCE_LEVEL_INDEX, 0)
        var completed = prefs.getInt(PreferenceConstants.PREFERENCE_LEVEL_COMPLETED, 0)
        mTotalGameTime = prefs.getFloat(PreferenceConstants.PREFERENCE_TOTAL_GAME_TIME, 0.0f)
        mRobotsDestroyed = prefs.getInt(PreferenceConstants.PREFERENCE_ROBOTS_DESTROYED, 0)
        mPearlsCollected = prefs.getInt(PreferenceConstants.PREFERENCE_PEARLS_COLLECTED, 0)
        mPearlsTotal = prefs.getInt(PreferenceConstants.PREFERENCE_PEARLS_TOTAL, 0)
        mLinearMode = prefs.getInt(PreferenceConstants.PREFERENCE_LINEAR_MODE,
                if (intent.getBooleanExtra("linearMode", false)) 1 else 0)
        mExtrasUnlocked = prefs.getBoolean(PreferenceConstants.PREFERENCE_EXTRAS_UNLOCKED, false)
        mDifficulty = prefs.getInt(PreferenceConstants.PREFERENCE_DIFFICULTY, intent.getIntExtra("difficulty", 1))
        mGame!!.bootstrap(this, dm.widthPixels, dm.heightPixels, defaultWidth, defaultHeight, mDifficulty)
        mGLSurfaceView!!.setRenderer(mGame!!.renderer as GLSurfaceView.Renderer)
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
            if (!LevelTree.levelIsValid(mLevelRow, mLevelIndex)) {
                // bad data?  Let's try to recover.

                // is the row valid?
                if (LevelTree.rowIsValid(mLevelRow)) {
                    // In that case, just start the row over.
                    mLevelIndex = 0
                    completed = 0
                } else if (LevelTree.rowIsValid(mLevelRow - 1)) {
                    // If not, try to back up a row.
                    mLevelRow--
                    mLevelIndex = 0
                    completed = 0
                }
                if (!LevelTree.levelIsValid(mLevelRow, mLevelIndex)) {
                    // if all else fails, start the game over.
                    mLevelRow = 0
                    mLevelIndex = 0
                    completed = 0
                }
            }
            LevelTree.updateCompletedState(mLevelRow, completed)
            mGame!!.setPendingLevel(LevelTree.fetch(mLevelRow, mLevelIndex))
            if (LevelTree.fetch(mLevelRow, mLevelIndex).showWaitMessage) {
                showWaitMessage()
            } else {
                hideWaitMessage()
            }
        }
        mSensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        // This activity uses the media stream.
        volumeControlStream = AudioManager.STREAM_MUSIC
        mSessionId = prefs.getLong(PreferenceConstants.PREFERENCE_SESSION_ID, System.currentTimeMillis())
        mEventReporter = null
        mEventReporterThread = null
        val statsEnabled = prefs.getBoolean(PreferenceConstants.PREFERENCE_STATS_ENABLED, true)
        if (statsEnabled) {
            mEventReporter = EventReporter()
            mEventReporterThread = Thread(mEventReporter)
            mEventReporterThread!!.name = "EventReporter"
            mEventReporterThread!!.start()
        }
    }

    override fun onDestroy() {
        DebugLog.d("AndouKun", "onDestroy()")
        mGame!!.stop()
        if (mEventReporterThread != null) {
            mEventReporter!!.stop()
            try {
                mEventReporterThread!!.join()
            } catch (e: InterruptedException) {
                mEventReporterThread!!.interrupt()
            }
        }
        super.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        DebugLog.d("AndouKun", "onPause")
        hidePauseMessage()
        mGame!!.onPause()
        mGLSurfaceView!!.onPause()
        mGame!!.renderer!!.onPause() // hack!
        if (mMethodTracing) {
            Debug.stopMethodTracing()
            mMethodTracing = false
        }
        if (mSensorManager != null) {
            mSensorManager!!.unregisterListener(this)
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
        mGLSurfaceView!!.onResume()
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
        if (mSensorManager != null) {
            val orientation = mSensorManager!!.getDefaultSensor(Sensor.TYPE_ORIENTATION)
            if (orientation != null) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                    mSensorManager!!.registerListener(this,
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
            mLastRollTime = time
        }
        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!mGame!!.isPaused) {
            mGame!!.onTouchEvent(event)
            val time = System.currentTimeMillis()
            if (event.action == MotionEvent.ACTION_MOVE && time - mLastTouchTime < 32) {
                // Sleep so that the main thread doesn't get flooded with UI events.
                try {
                    Thread.sleep(32)
                } catch (e: InterruptedException) {
                    // No big deal if this sleep is interrupted.
                }
                mGame!!.renderer!!.waitDrawingComplete()
            }
            mLastTouchTime = time
        }
        return true
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        var result = true
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            val time = System.currentTimeMillis()
            if (time - mLastRollTime > ROLL_TO_FACE_BUTTON_DELAY &&
                    time - mLastTouchTime > ROLL_TO_FACE_BUTTON_DELAY) {
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
                if (time - mLastRollTime > ROLL_TO_FACE_BUTTON_DELAY &&
                        time - mLastTouchTime > ROLL_TO_FACE_BUTTON_DELAY) {
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
                if (mMethodTracing) {
                    Debug.stopMethodTracing()
                } else {
                    Debug.startMethodTracing("andou")
                }
                mMethodTracing = !mMethodTracing
                return true
            }
        }
        return super.onMenuItemSelected(featureId, item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (requestCode == ACTIVITY_CHANGE_LEVELS) {
            if (resultCode == RESULT_OK) {
                mLevelRow = intent.extras.getInt("row")
                mLevelIndex = intent.extras.getInt("index")
                LevelTree.updateCompletedState(mLevelRow, 0)
                saveGame()
                mGame!!.setPendingLevel(LevelTree.fetch(mLevelRow, mLevelIndex))
                if (LevelTree.fetch(mLevelRow, mLevelIndex).showWaitMessage) {
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
                if (LevelTree.fetch(mLevelRow, mLevelIndex).restartable) {
                    if (mEventReporter != null) {
                        mEventReporter!!.addEvent(EventReporter.EVENT_DEATH,
                                mGame!!.lastDeathPosition!!.x,
                                mGame!!.lastDeathPosition!!.y,
                                mGame!!.gameTime,
                                LevelTree.fetch(mLevelRow, mLevelIndex).name,
                                VERSION,
                                mSessionId)
                    }
                    mGame!!.restartLevel()
                    return
                }
                LevelTree.fetch(mLevelRow, mLevelIndex).completed = true
                val currentGroup = LevelTree.levels[mLevelRow]
                val count = currentGroup.levels.size
                var groupCompleted = true
                if (mEventReporter != null) {
                    mEventReporter!!.addEvent(EventReporter.EVENT_BEAT_LEVEL, 0f, 0f,
                            mGame!!.gameTime,
                            LevelTree.fetch(mLevelRow, mLevelIndex).name,
                            VERSION,
                            mSessionId)
                }
                var x = 0
                while (x < count) {
                    if (currentGroup.levels[x].completed == false) {
                        // We haven't completed the group yet.
                        mLevelIndex = x
                        groupCompleted = false
                        break
                    }
                    x++
                }
                if (groupCompleted) {
                    mLevelIndex = 0
                    mLevelRow++
                }
                mTotalGameTime += mGame!!.gameTime
                mRobotsDestroyed += mGame!!.robotsDestroyed
                mPearlsCollected += mGame!!.pearlsCollected
                mPearlsTotal += mGame!!.pearlsTotal
                if (mLevelRow < LevelTree.levels.size) {
                    val currentLevel = LevelTree.fetch(mLevelRow, mLevelIndex)
                    if (currentLevel.inThePast || LevelTree.levels[mLevelRow].levels.size > 1) {
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
                    if (mEventReporter != null) {
                        mEventReporter!!.addEvent(EventReporter.EVENT_BEAT_GAME, 0f, 0f,
                                mGame!!.gameTime,
                                "end",
                                VERSION,
                                mSessionId)
                    }
                    // We beat the game!
                    mLevelRow = 0
                    mLevelIndex = 0
                    mLastEnding = mGame!!.lastEnding
                    mExtrasUnlocked = true
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
                LevelTree.fetch(mLevelRow, mLevelIndex).completed = true
                val currentGroup = LevelTree.levels[mLevelRow]
                val count = currentGroup.levels.size
                var groupCompleted = true
                if (mEventReporter != null) {
                    mEventReporter!!.addEvent(EventReporter.EVENT_BEAT_LEVEL, 0f, 0f,
                            mGame!!.gameTime,
                            LevelTree.fetch(mLevelRow, mLevelIndex).name,
                            VERSION,
                            mSessionId)
                }
                var x = 0
                while (x < count) {
                    if (currentGroup.levels[x].completed == false) {
                        mLevelIndex = x
                        groupCompleted = false
                        break
                    }
                    x++
                }
                if (groupCompleted) {
                    mLevelIndex = 0
                    mLevelRow++
                }
                mTotalGameTime += mGame!!.gameTime
                mRobotsDestroyed += mGame!!.robotsDestroyed
                mPearlsCollected += mGame!!.pearlsCollected
                mPearlsTotal += mGame!!.pearlsTotal
                if (mLevelRow < LevelTree.levels.size) {
                    val currentLevel = LevelTree.fetch(mLevelRow, mLevelIndex)
                    if (currentLevel.inThePast || LevelTree.levels[mLevelRow].levels.size > 1) {
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
                    if (mEventReporter != null) {
                        mEventReporter!!.addEvent(EventReporter.EVENT_BEAT_GAME, 0f, 0f,
                                mGame!!.gameTime,
                                "end",
                                VERSION,
                                mSessionId)
                    }
                    mLevelRow = 0
                    mLevelIndex = 0
                    mLastEnding = mGame!!.lastEnding
                    mExtrasUnlocked = true
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
                val level = LevelTree.fetch(mLevelRow, mLevelIndex)
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
                i.putExtra("levelRow", mLevelRow)
                i.putExtra("levelIndex", mLevelIndex)
                i.putExtra("index", index)
                i.putExtra("character", 1)
                startActivity(i)
            }
            GameFlowEvent.EVENT_SHOW_DIALOG_CHARACTER2 -> {
                val i = Intent(this, ConversationDialogActivity::class.java)
                i.putExtra("levelRow", mLevelRow)
                i.putExtra("levelIndex", mLevelIndex)
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
        if (mPrefsEditor != null) {
            val completed = LevelTree.packCompletedLevels(mLevelRow)
            mPrefsEditor!!.putInt(PreferenceConstants.PREFERENCE_LEVEL_ROW, mLevelRow)
            mPrefsEditor!!.putInt(PreferenceConstants.PREFERENCE_LEVEL_INDEX, mLevelIndex)
            mPrefsEditor!!.putInt(PreferenceConstants.PREFERENCE_LEVEL_COMPLETED, completed)
            mPrefsEditor!!.putLong(PreferenceConstants.PREFERENCE_SESSION_ID, mSessionId)
            mPrefsEditor!!.putFloat(PreferenceConstants.PREFERENCE_TOTAL_GAME_TIME, mTotalGameTime)
            mPrefsEditor!!.putInt(PreferenceConstants.PREFERENCE_LAST_ENDING, mLastEnding)
            mPrefsEditor!!.putInt(PreferenceConstants.PREFERENCE_ROBOTS_DESTROYED, mRobotsDestroyed)
            mPrefsEditor!!.putInt(PreferenceConstants.PREFERENCE_PEARLS_COLLECTED, mPearlsCollected)
            mPrefsEditor!!.putInt(PreferenceConstants.PREFERENCE_PEARLS_TOTAL, mPearlsTotal)
            mPrefsEditor!!.putInt(PreferenceConstants.PREFERENCE_LINEAR_MODE, mLinearMode)
            mPrefsEditor!!.putBoolean(PreferenceConstants.PREFERENCE_EXTRAS_UNLOCKED, mExtrasUnlocked)
            mPrefsEditor!!.putInt(PreferenceConstants.PREFERENCE_DIFFICULTY, mDifficulty)
            mPrefsEditor!!.commit()
        }
    }

    private fun showPauseMessage() {
        if (mPauseMessage != null) {
            mPauseMessage!!.visibility = View.VISIBLE
        }
        if (mLevelNameBox != null && mLevelName != null) {
            mLevelName!!.text = LevelTree.fetch(mLevelRow, mLevelIndex).name
            mLevelNameBox!!.visibility = View.VISIBLE
        }
    }

    private fun hidePauseMessage() {
        if (mPauseMessage != null) {
            mPauseMessage!!.visibility = View.GONE
        }
        if (mLevelNameBox != null) {
            mLevelNameBox!!.visibility = View.GONE
        }
    }

    private fun showWaitMessage() {
        if (mWaitMessage != null) {
            mWaitMessage!!.visibility = View.VISIBLE
            mWaitMessage!!.startAnimation(mWaitFadeAnimation)
        }
    }

    private fun hideWaitMessage() {
        if (mWaitMessage != null) {
            mWaitMessage!!.visibility = View.GONE
            mWaitMessage!!.clearAnimation()
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
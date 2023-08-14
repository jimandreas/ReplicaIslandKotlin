@file:Suppress("DEPRECATION", "UNUSED_ANONYMOUS_PARAMETER")

package com.replica.replicaisland

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.view.animation.AnimationUtils
import java.lang.reflect.InvocationTargetException

class ExtrasMenuActivity : Activity() {
    private var mLinearModeButton: View? = null
    private var mLevelSelectButton: View? = null
    private var mControlsButton: View? = null
    private var background: View? = null
    private var mLevelSelectLocked: View? = null
    private var mLinearModeLocked: View? = null
    private var buttonFlickerAnimation: Animation? = null
    private var fadeOutAnimation: Animation? = null
    private var alternateFadeOutAnimation: Animation? = null
    private var lockedAnimation: Animation? = null
    private var pendingGameStart = 0
    private val sLinearModeButtonListener = View.OnClickListener {
        val prefs = getSharedPreferences(PreferenceConstants.PREFERENCE_NAME, MODE_PRIVATE)
        val row = prefs.getInt(PreferenceConstants.PREFERENCE_LEVEL_ROW, 0)
        val index = prefs.getInt(PreferenceConstants.PREFERENCE_LEVEL_INDEX, 0)
        if (row != 0 || index != 0) {
            pendingGameStart = START_LINEAR_MODE
            showDialog(NEW_GAME_DIALOG)
        } else {
            startGame(START_LINEAR_MODE)
        }
    }
    private val sLevelSelectButtonListener = View.OnClickListener {
        val prefs = getSharedPreferences(PreferenceConstants.PREFERENCE_NAME, MODE_PRIVATE)
        val row = prefs.getInt(PreferenceConstants.PREFERENCE_LEVEL_ROW, 0)
        val index = prefs.getInt(PreferenceConstants.PREFERENCE_LEVEL_INDEX, 0)
        if (row != 0 || index != 0) {
            pendingGameStart = START_LEVEL_SELECT
            showDialog(NEW_GAME_DIALOG)
        } else {
            startGame(START_LEVEL_SELECT)
        }
    }
    private val sLockedSelectButtonListener = View.OnClickListener { showDialog(EXTRAS_LOCKED_DIALOG) }
    private val sControlsButtonListener = View.OnClickListener { v ->
        val i = Intent(baseContext, SetPreferencesActivity::class.java)
        i.putExtra("controlConfig", true)
        v.startAnimation(buttonFlickerAnimation)
        fadeOutAnimation!!.setAnimationListener(StartActivityAfterAnimation(i))
        background!!.startAnimation(fadeOutAnimation)
        mLinearModeButton!!.startAnimation(alternateFadeOutAnimation)
        mLevelSelectButton!!.startAnimation(alternateFadeOutAnimation)
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            super.onCreate(savedInstanceState)
        } else {
            super.onCreate(null)
        }
        setContentView(R.layout.extras_menu)
        val prefs = getSharedPreferences(PreferenceConstants.PREFERENCE_NAME, MODE_PRIVATE)
        val extrasUnlocked = prefs.getBoolean(PreferenceConstants.PREFERENCE_EXTRAS_UNLOCKED, false)
        mLinearModeButton = findViewById(R.id.linearModeButton)
        mLevelSelectButton = findViewById(R.id.levelSelectButton)
        mControlsButton = findViewById(R.id.controlsButton)
        mLinearModeLocked = findViewById(R.id.linearModeLocked)
        mLevelSelectLocked = findViewById(R.id.levelSelectLocked)
        background = findViewById(R.id.mainMenuBackground)
        buttonFlickerAnimation = AnimationUtils.loadAnimation(this, R.anim.button_flicker)
        fadeOutAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_out)
        alternateFadeOutAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_out)
        if (extrasUnlocked) {
            mLinearModeButton!!.setOnClickListener(sLinearModeButtonListener)
            mLevelSelectButton!!.setOnClickListener(sLevelSelectButtonListener)
            mLinearModeLocked!!.visibility = View.GONE
            mLevelSelectLocked!!.visibility = View.GONE
        } else {
            lockedAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in_out)
            mLinearModeButton!!.setOnClickListener(sLockedSelectButtonListener)
            mLevelSelectButton!!.setOnClickListener(sLockedSelectButtonListener)
            mLinearModeLocked!!.startAnimation(lockedAnimation)
            mLevelSelectLocked!!.startAnimation(lockedAnimation)
        }
        mControlsButton!!.setOnClickListener(sControlsButtonListener)


        // Keep the volume control type consistent across all activities.
        volumeControlStream = AudioManager.STREAM_MUSIC
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        var result = true
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish()
            if (UIConstants.mOverridePendingTransition != null) {
                try {
                    UIConstants.mOverridePendingTransition!!.invoke(this@ExtrasMenuActivity, R.anim.activity_fade_in, R.anim.activity_fade_out)
                } catch (ite: InvocationTargetException) {
                    DebugLog.d("Activity Transition", "Invocation Target Exception")
                } catch (ie: IllegalAccessException) {
                    DebugLog.d("Activity Transition", "Illegal Access Exception")
                }
            }
        } else {
            result = super.onKeyDown(keyCode, event)
        }
        return result
    }

    override fun onCreateDialog(id: Int): Dialog {
        var dialog: Dialog? = null
        if (id == NEW_GAME_DIALOG) {
            dialog = AlertDialog.Builder(this)
                    .setTitle(R.string.new_game_dialog_title)
                    .setPositiveButton(R.string.new_game_dialog_ok) { _, whichButton -> startGame(pendingGameStart) }
                    .setNegativeButton(R.string.new_game_dialog_cancel, null)
                    .setMessage(R.string.new_game_dialog_message)
                    .create()
        } else if (id == EXTRAS_LOCKED_DIALOG) {
            dialog = AlertDialog.Builder(this)
                    .setTitle(R.string.extras_locked_dialog_title)
                    .setPositiveButton(R.string.extras_locked_dialog_ok, null)
                    .setMessage(R.string.extras_locked_dialog_message)
                    .create()
        }
        return dialog!!
    }

    private fun startGame(type: Int) {
        if (type == START_LINEAR_MODE) {
            val i = Intent(baseContext, DifficultyMenuActivity::class.java)
            i.putExtra("linearMode", true)
            i.putExtra("newGame", true)
            mLinearModeButton!!.startAnimation(buttonFlickerAnimation)
            buttonFlickerAnimation!!.setAnimationListener(StartActivityAfterAnimation(i))
        } else if (type == START_LEVEL_SELECT) {
            val i = Intent(baseContext, DifficultyMenuActivity::class.java)
            i.putExtra("startAtLevelSelect", true)
            i.putExtra("newGame", true)
            mLevelSelectButton!!.startAnimation(buttonFlickerAnimation)
            buttonFlickerAnimation!!.setAnimationListener(StartActivityAfterAnimation(i))
        }
    }

    private inner class StartActivityAfterAnimation(private val intent: Intent) : AnimationListener {
        override fun onAnimationEnd(animation: Animation) {
            mLinearModeButton!!.visibility = View.INVISIBLE
            mLinearModeButton!!.clearAnimation()
            mLevelSelectButton!!.visibility = View.INVISIBLE
            mLevelSelectButton!!.clearAnimation()
            mControlsButton!!.visibility = View.INVISIBLE
            mControlsButton!!.clearAnimation()
            startActivity(intent)
            finish()
            if (UIConstants.mOverridePendingTransition != null) {
                try {
                    UIConstants.mOverridePendingTransition!!.invoke(this@ExtrasMenuActivity, R.anim.activity_fade_in, R.anim.activity_fade_out)
                } catch (ite: InvocationTargetException) {
                    DebugLog.d("Activity Transition", "Invocation Target Exception")
                } catch (ie: IllegalAccessException) {
                    DebugLog.d("Activity Transition", "Illegal Access Exception")
                }
            }
        }

        override fun onAnimationRepeat(animation: Animation) {
            // TODO Auto-generated method stub
        }

        override fun onAnimationStart(animation: Animation) {
            // TODO Auto-generated method stub
        }
    }

    companion object {
        const val NEW_GAME_DIALOG = 0
        const val EXTRAS_LOCKED_DIALOG = 1
        private const val START_LINEAR_MODE = 0
        private const val START_LEVEL_SELECT = 1
    }
}
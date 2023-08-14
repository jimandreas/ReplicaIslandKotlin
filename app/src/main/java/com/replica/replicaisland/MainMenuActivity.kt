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
@file:Suppress("DEPRECATION", "SENSELESS_COMPARISON", "UNUSED_ANONYMOUS_PARAMETER", "ConvertTwoComparisonsToRangeCheck", "CascadeIf")

package com.replica.replicaisland

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.View
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.view.animation.AnimationUtils
import android.widget.ImageView
import com.replica.replicaisland.LevelTree.isLoaded
import com.replica.replicaisland.LevelTree.loadAllDialog
import com.replica.replicaisland.LevelTree.loadLevelTree
import java.lang.reflect.InvocationTargetException
import kotlin.math.abs

class MainMenuActivity : Activity() {
    private var paused = false
    private var mStartButton: View? = null
    private var optionsButton: View? = null
    private var mExtrasButton: View? = null
    private var background: View? = null
    private var mTicker: View? = null
    private var buttonFlickerAnimation: Animation? = null
    private var fadeOutAnimation: Animation? = null
    private var alternateFadeOutAnimation: Animation? = null
    private var fadeInAnimation: Animation? = null
    private var justCreated = false
    private var selectedControlsString: String? = null

    // Create an anonymous implementation of OnClickListener
    private val sContinueButtonListener = View.OnClickListener { v ->
        if (!paused) {
            val i = Intent(baseContext, AndouKun::class.java)
            v.startAnimation(buttonFlickerAnimation)
            fadeOutAnimation!!.setAnimationListener(StartActivityAfterAnimation(i))
            background!!.startAnimation(fadeOutAnimation)
            optionsButton!!.startAnimation(alternateFadeOutAnimation)
            mExtrasButton!!.startAnimation(alternateFadeOutAnimation)
            mTicker!!.startAnimation(alternateFadeOutAnimation)
            paused = true
        }
    }
    private val sOptionButtonListener = View.OnClickListener { v ->
        if (!paused) {
            val i = Intent(baseContext, SetPreferencesActivity::class.java)
            v.startAnimation(buttonFlickerAnimation)
            fadeOutAnimation!!.setAnimationListener(StartActivityAfterAnimation(i))
            background!!.startAnimation(fadeOutAnimation)
            mStartButton!!.startAnimation(alternateFadeOutAnimation)
            mExtrasButton!!.startAnimation(alternateFadeOutAnimation)
            mTicker!!.startAnimation(alternateFadeOutAnimation)
            paused = true
        }
    }
    private val sExtrasButtonListener = View.OnClickListener { v ->
        if (!paused) {
            val i = Intent(baseContext, ExtrasMenuActivity::class.java)
            v.startAnimation(buttonFlickerAnimation)
            buttonFlickerAnimation!!.setAnimationListener(StartActivityAfterAnimation(i))
            paused = true
        }
    }
    private val sStartButtonListener = View.OnClickListener { v ->
        if (!paused) {
            val i = Intent(baseContext, DifficultyMenuActivity::class.java)
            i.putExtra("newGame", true)
            v.startAnimation(buttonFlickerAnimation)
            buttonFlickerAnimation!!.setAnimationListener(StartActivityAfterAnimation(i))
            paused = true
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            super.onCreate(savedInstanceState)
        } else {
            super.onCreate(null)
        }
        setContentView(R.layout.mainmenu)
        paused = true
        mStartButton = findViewById(R.id.startButton)
        optionsButton = findViewById(R.id.optionButton)
        background = findViewById(R.id.mainMenuBackground)
        if (optionsButton != null) {
            optionsButton!!.setOnClickListener(sOptionButtonListener)
        }
        mExtrasButton = findViewById(R.id.extrasButton)
        mExtrasButton!!.setOnClickListener(sExtrasButtonListener)
        buttonFlickerAnimation = AnimationUtils.loadAnimation(this, R.anim.button_flicker)
        fadeOutAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_out)
        alternateFadeOutAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_out)
        fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        val prefs = getSharedPreferences(PreferenceConstants.PREFERENCE_NAME, MODE_PRIVATE)
        val row = prefs.getInt(PreferenceConstants.PREFERENCE_LEVEL_ROW, 0)
        val index = prefs.getInt(PreferenceConstants.PREFERENCE_LEVEL_INDEX, 0)
        var levelTreeResource = R.xml.level_tree
        if (row != 0 || index != 0) {
            val linear = prefs.getInt(PreferenceConstants.PREFERENCE_LINEAR_MODE, 0)
            if (linear != 0) {
                levelTreeResource = R.xml.linear_level_tree
            }
        }
        if (!isLoaded(levelTreeResource)) {
            loadLevelTree(levelTreeResource, this)
            loadAllDialog(this)
        }
        mTicker = findViewById(R.id.ticker)
        if (mTicker != null) {
            mTicker!!.isFocusable = true
            mTicker!!.requestFocus()
            mTicker!!.isSelected = true
        }
        justCreated = true

        // Keep the volume control type consistent across all activities.
        volumeControlStream = AudioManager.STREAM_MUSIC

        //MediaPlayer mp = MediaPlayer.create(this, R.raw.bwv_115);
        //mp.start();
    }

    override fun onPause() {
        super.onPause()
        paused = true
    }

    @SuppressLint("ApplySharedPref")
    override fun onResume() {
        super.onResume()
        paused = false
        buttonFlickerAnimation!!.setAnimationListener(null)
        if (mStartButton != null) {

            // Change "start" to "continue" if there's a saved game.
            val prefs = getSharedPreferences(PreferenceConstants.PREFERENCE_NAME, MODE_PRIVATE)
            val row = prefs.getInt(PreferenceConstants.PREFERENCE_LEVEL_ROW, 0)
            val index = prefs.getInt(PreferenceConstants.PREFERENCE_LEVEL_INDEX, 0)
            if (row != 0 || index != 0) {
                (mStartButton as ImageView).setImageDrawable(resources.getDrawable(R.drawable.ui_button_continue))
                mStartButton!!.setOnClickListener(sContinueButtonListener)
            } else {
                (mStartButton as ImageView).setImageDrawable(resources.getDrawable(R.drawable.ui_button_start))
                mStartButton!!.setOnClickListener(sStartButtonListener)
            }
            val touch: TouchFilter
            val sdkVersion = Build.VERSION.SDK.toInt()
            touch = if (sdkVersion < Build.VERSION_CODES.ECLAIR) {
                SingleTouchFilter()
            } else {
                MultiTouchFilter()
            }
            val lastVersion = prefs.getInt(PreferenceConstants.PREFERENCE_LAST_VERSION, 0)
            if (lastVersion == 0) {
                // This is the first time the game has been run.
                // Pre-configure the control options to match the device.
                // The resource system can tell us what this device has.
                // TODO: is there a better way to do this?  Seems like a kind of neat
                // way to do custom device profiles.
                val navType = getString(R.string.nav_type)
                selectedControlsString = getString(R.string.control_setup_dialog_trackball)
                if (navType != null) {
                    if (navType.equals("DPad", ignoreCase = true)) {
                        // Turn off the click-to-attack pref on devices that have a dpad.
                        val editor = prefs.edit()
                        editor.putBoolean(PreferenceConstants.PREFERENCE_CLICK_ATTACK, false)
                        editor.commit()
                        selectedControlsString = getString(R.string.control_setup_dialog_dpad)
                    } else if (navType.equals("None", ignoreCase = true)) {
                        val editor = prefs.edit()

                        // This test relies on the PackageManager if api version >= 5.
                        selectedControlsString = if (touch.supportsMultitouch(this)) {
                            // Default to screen controls.
                            editor.putBoolean(PreferenceConstants.PREFERENCE_SCREEN_CONTROLS, true)
                            getString(R.string.control_setup_dialog_screen)
                        } else {
                            // Turn on tilt controls if there's nothing else.
                            editor.putBoolean(PreferenceConstants.PREFERENCE_TILT_CONTROLS, true)
                            getString(R.string.control_setup_dialog_tilt)
                        }
                        editor.commit()
                    }
                }
            }
            if (abs(lastVersion) < abs(AndouKun.VERSION)) {
                // This is a new install or an upgrade.

                // Check the safe mode option.
                // Useful reference: http://en.wikipedia.org/wiki/List_of_Android_devices
                if (Build.PRODUCT.contains("morrison") ||  // Motorola Cliq/Dext
                        Build.MODEL.contains("Pulse") ||  // Huawei Pulse
                        Build.MODEL.contains("U8220") ||  // Huawei Pulse
                        Build.MODEL.contains("U8230") ||  // Huawei U8230
                        Build.MODEL.contains("MB300") ||  // Motorola Backflip
                        Build.MODEL.contains("MB501") ||  // Motorola Quench / Cliq XT
                        Build.MODEL.contains("Behold+II")) {    // Samsung Behold II
                    // These are all models that users have complained about.  They likely use
                    // the same buggy QTC graphics driver.  Turn on Safe Mode by default
                    // for these devices.
                    val editor = prefs.edit()
                    editor.putBoolean(PreferenceConstants.PREFERENCE_SAFE_MODE, true)
                    editor.commit()
                }
                val editor = prefs.edit()
                if (lastVersion > 0 && lastVersion < 14) {
                    // if the user has beat the game once, go ahead and unlock stuff for them.
                    if (prefs.getInt(PreferenceConstants.PREFERENCE_LAST_ENDING, -1) != -1) {
                        editor.putBoolean(PreferenceConstants.PREFERENCE_EXTRAS_UNLOCKED, true)
                    }
                }

                // show what's new message
                editor.putInt(PreferenceConstants.PREFERENCE_LAST_VERSION, AndouKun.VERSION)
                editor.commit()
                showDialog(WHATS_NEW_DIALOG)

                // screen controls were added in version 14
                if (lastVersion > 0 && lastVersion < 14 &&
                        prefs.getBoolean(PreferenceConstants.PREFERENCE_TILT_CONTROLS, false)) {
                    if (touch.supportsMultitouch(this)) {
                        // show message about switching from tilt to screen controls
                        showDialog(TILT_TO_SCREEN_CONTROLS_DIALOG)
                    }
                } else if (lastVersion == 0) {
                    // show message about auto-selected control schemes.
                    showDialog(CONTROL_SETUP_DIALOG)
                }
            }
        }
        if (background != null) {
            background!!.clearAnimation()
        }
        if (mTicker != null) {
            mTicker!!.clearAnimation()
            mTicker!!.animation = fadeInAnimation
        }
        if (justCreated) {
            if (mStartButton != null) {
                mStartButton!!.startAnimation(AnimationUtils.loadAnimation(this, R.anim.button_slide))
            }
            if (mExtrasButton != null) {
                val anim = AnimationUtils.loadAnimation(this, R.anim.button_slide)
                anim.startOffset = 500L
                mExtrasButton!!.startAnimation(anim)
            }
            if (optionsButton != null) {
                val anim = AnimationUtils.loadAnimation(this, R.anim.button_slide)
                anim.startOffset = 1000L
                optionsButton!!.startAnimation(anim)
            }
            justCreated = false
        } else {
            mStartButton!!.clearAnimation()
            optionsButton!!.clearAnimation()
            mExtrasButton!!.clearAnimation()
        }
    }

    @SuppressLint("ApplySharedPref")
    override fun onCreateDialog(id: Int): Dialog {
        val dialog: Dialog
        dialog = if (id == WHATS_NEW_DIALOG) {
            AlertDialog.Builder(this)
                    .setTitle(R.string.whats_new_dialog_title)
                    .setPositiveButton(R.string.whats_new_dialog_ok, null)
                    .setMessage(R.string.whats_new_dialog_message)
                    .create()
        } else if (id == TILT_TO_SCREEN_CONTROLS_DIALOG) {
            AlertDialog.Builder(this)
                    .setTitle(R.string.onscreen_tilt_dialog_title)
                    .setPositiveButton(R.string.onscreen_tilt_dialog_ok) { thisDialog, whichButton ->
                        val prefs = getSharedPreferences(PreferenceConstants.PREFERENCE_NAME, MODE_PRIVATE)
                        val editor = prefs.edit()
                        editor.putBoolean(PreferenceConstants.PREFERENCE_SCREEN_CONTROLS, true)
                        editor.commit()
                    }
                    .setNegativeButton(R.string.onscreen_tilt_dialog_cancel, null)
                    .setMessage(R.string.onscreen_tilt_dialog_message)
                    .create()
        } else if (id == CONTROL_SETUP_DIALOG) {
            val messageFormat = resources.getString(R.string.control_setup_dialog_message)
            val message = String.format(messageFormat, selectedControlsString)
            val sytledMessage: CharSequence = Html.fromHtml(message) // lame.
            AlertDialog.Builder(this)
                    .setTitle(R.string.control_setup_dialog_title)
                    .setPositiveButton(R.string.control_setup_dialog_ok, null)
                    .setNegativeButton(R.string.control_setup_dialog_change) { thisDialog, whichButton ->
                        val i = Intent(baseContext, SetPreferencesActivity::class.java)
                        i.putExtra("controlConfig", true)
                        startActivity(i)
                    }
                    .setMessage(sytledMessage)
                    .create()
        } else {
            super.onCreateDialog(id)
        }
        return dialog
    }

    private inner class StartActivityAfterAnimation constructor(private val intent: Intent) : AnimationListener {
        override fun onAnimationEnd(animation: Animation) {
            startActivity(intent)
            if (UIConstants.mOverridePendingTransition != null) {
                try {
                    UIConstants.mOverridePendingTransition!!.invoke(this@MainMenuActivity, R.anim.activity_fade_in, R.anim.activity_fade_out)
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
        private const val WHATS_NEW_DIALOG = 0
        private const val TILT_TO_SCREEN_CONTROLS_DIALOG = 1
        private const val CONTROL_SETUP_DIALOG = 2
    }
}
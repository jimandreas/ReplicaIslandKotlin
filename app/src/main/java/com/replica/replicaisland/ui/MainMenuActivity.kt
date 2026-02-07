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

@file:Suppress("unused", "CascadeIf")

package com.replica.replicaisland.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.replica.replicaisland.AndouKun
import com.replica.replicaisland.R
import com.replica.replicaisland.data.PreferencesManager
import com.replica.replicaisland.input.MultiTouchFilter
import com.replica.replicaisland.input.SingleTouchFilter
import com.replica.replicaisland.input.TouchFilter
import com.replica.replicaisland.levels.LevelTree
import java.lang.reflect.InvocationTargetException
import kotlin.math.abs

class MainMenuActivity : AppCompatActivity() {
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

        // New method of landscape orientation.
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
        // end of new method of landscape orientation

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
        val prefsManager = PreferencesManager.getInstance(this)
        val row = prefsManager.getLevelRow()
        val index = prefsManager.getLevelIndex()
        var levelTreeResource = R.xml.level_tree
        if (row != 0 || index != 0) {
            val linear = prefsManager.getLinearMode()
            if (linear != 0) {
                levelTreeResource = R.xml.linear_level_tree
            }
        }
        if (!LevelTree.isLoaded(levelTreeResource)) {
            LevelTree.loadLevelTree(levelTreeResource, this)
            LevelTree.loadAllDialog(this)
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

        // Set up fragment result listeners for dialog callbacks
        supportFragmentManager.setFragmentResultListener(
            ControlSetupDialogFragment.REQUEST_KEY, this
        ) { _, bundle ->
            if (bundle.getBoolean(ControlSetupDialogFragment.RESULT_OPEN_SETTINGS)) {
                val i = Intent(baseContext, SetPreferencesActivity::class.java)
                i.putExtra("controlConfig", true)
                startActivity(i)
            }
        }

        //MediaPlayer mp = MediaPlayer.create(this, R.raw.bwv_115);
        //mp.start();
    }

    override fun onPause() {
        super.onPause()
        paused = true
    }

    @SuppressLint("ApplySharedPref",
        "UseCompatLoadingForDrawables", "UseKtx")
    override fun onResume() {
        super.onResume()
        paused = false
        buttonFlickerAnimation!!.setAnimationListener(null)
        if (mStartButton != null) {

            // Change "start" to "continue" if there's a saved game.
            val prefsManager = PreferencesManager.getInstance(this)
            val row = prefsManager.getLevelRow()
            val index = prefsManager.getLevelIndex()
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
            val lastVersion = prefsManager.getLastVersion()
            if (lastVersion == 0) {
                // This is the first time the game has been run.
                // Pre-configure the control options to match the device.
                // The resource system can tell us what this device has.
                // TODO: is there a better way to do this?  Seems like a kind of neat
                // way to do custom device profiles.
                val navType = getString(R.string.nav_type)
                selectedControlsString = getString(R.string.control_setup_dialog_dpad)
                if (navType.equals("DPad", ignoreCase = true)) {
                    // Turn off the click-to-attack pref on devices that have a dpad.
                    prefsManager.setClickAttack(false)
                    selectedControlsString = getString(R.string.control_setup_dialog_dpad)
                } else if (navType.equals("None", ignoreCase = true)) {
                    // This test relies on the PackageManager if api version >= 5.
                    if (touch.supportsMultitouch(this)) {
                        // Default to screen controls.
                        prefsManager.setScreenControls(true)
                        selectedControlsString = getString(R.string.control_setup_dialog_screen)
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
                    prefsManager.setSafeMode(true)
                }
                if (lastVersion in 1..<14) {
                    // if the user has beat the game once, go ahead and unlock stuff for them.
                    if (prefsManager.getLastEnding() != -1) {
                        prefsManager.setExtrasUnlocked(true)
                    }
                }

                // show what's new message
                prefsManager.setLastVersion(AndouKun.VERSION)
                WhatsNewDialogFragment.newInstance()
                    .show(supportFragmentManager, WhatsNewDialogFragment.TAG)

                if (lastVersion == 0) {
                    // show message about auto-selected control schemes.
                    ControlSetupDialogFragment.newInstance(selectedControlsString ?: "")
                        .show(supportFragmentManager, ControlSetupDialogFragment.TAG)
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

    private inner class StartActivityAfterAnimation(private val intent: Intent) :
        Animation.AnimationListener {
        override fun onAnimationEnd(animation: Animation) {
            startActivity(intent)
            if (UIConstants.mOverridePendingTransition != null) {
                try {
                    UIConstants.mOverridePendingTransition!!.invoke(this@MainMenuActivity, R.anim.activity_fade_in, R.anim.activity_fade_out)
                } catch (_: InvocationTargetException) {
                    DebugLog.d("Activity Transition", "Invocation Target Exception")
                } catch (_: IllegalAccessException) {
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
}
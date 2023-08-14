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
@file:Suppress("HandlerLeak")
package com.replica.replicaisland

import android.app.Activity
import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.DisplayMetrics
import android.view.MotionEvent
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.TranslateAnimation
import android.widget.ImageView
import java.lang.reflect.InvocationTargetException

class AnimationPlayerActivity : Activity() {
    private var mAnimation: AnimationDrawable? = null
    private var animationType = 0
    private var animationEndTime: Long = 0
    private val killActivityHandler = KillActivityHandler()

    internal inner class KillActivityHandler : Handler() {
        override fun handleMessage(msg: Message) {
            finish()
            if (UIConstants.mOverridePendingTransition != null) {
                try {
                    UIConstants.mOverridePendingTransition!!.invoke(
                            this@AnimationPlayerActivity,
                            R.anim.activity_fade_in,
                            R.anim.activity_fade_out)
                } catch (ite: InvocationTargetException) {
                    DebugLog.d("Activity Transition", "Invocation Target Exception")
                } catch (ie: IllegalAccessException) {
                    DebugLog.d("Activity Transition", "Illegal Access Exception")
                }
            }
        }

        fun sleep(delayMillis: Long) {
            this.removeMessages(0)
            sendMessageDelayed(obtainMessage(0), delayMillis)
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            super.onCreate(savedInstanceState)
        } else {
            super.onCreate(null)
        }
        val callingIntent = intent
        animationType = callingIntent.getIntExtra("animation", KYLE_DEATH)
        if (animationType == KYLE_DEATH) {
            setContentView(R.layout.animation_player)
            val canvasImage = findViewById<View>(R.id.animation_canvas) as ImageView
            canvasImage.setImageResource(R.drawable.kyle_fall)
            mAnimation = canvasImage.drawable as AnimationDrawable
        } else {
            if (animationType == WANDA_ENDING || animationType == KABOCHA_ENDING) {

                val metrics = DisplayMetrics()
                windowManager.defaultDisplay.getMetrics(metrics)
                val startX = if (animationType == WANDA_ENDING) {
                    setContentView(R.layout.good_ending_animation)
                    200 * metrics.density
                } else {
                    setContentView(R.layout.kabocha_ending_animation)
                    -200 * metrics.density
                }

                // HACK
                // the TranslateAnimation system doesn't support device independent pixels.
                // So for the Wanda ending and Kabocha endings, in which the game over text
                // scrolls in horizontally, compute the size based on the actual density of
                // the display and just generate the anim in code.  The Rokudou animation
                // can be safely loaded from a file.
                val gameOverAnim: Animation = TranslateAnimation(startX, 0f, 0f, 0f)
                gameOverAnim.duration = 6000
                gameOverAnim.fillAfter = true
                gameOverAnim.isFillEnabled = true
                gameOverAnim.startOffset = 8000
                val background = findViewById<View>(R.id.animation_background)
                val foreground = findViewById<View>(R.id.animation_foreground)
                val gameOver = findViewById<View>(R.id.game_over)
                val foregroundAnim = AnimationUtils.loadAnimation(this, R.anim.horizontal_layer2_slide)
                val backgroundAnim = AnimationUtils.loadAnimation(this, R.anim.horizontal_layer1_slide)
                background.startAnimation(backgroundAnim)
                foreground.startAnimation(foregroundAnim)
                gameOver.startAnimation(gameOverAnim)
                animationEndTime = gameOverAnim.duration + System.currentTimeMillis()
            } else if (animationType == ROKUDOU_ENDING) {
                setContentView(R.layout.rokudou_ending_animation)
                val background = findViewById<View>(R.id.animation_background)
                val sphere = findViewById<View>(R.id.animation_sphere)
                val cliffs = findViewById<View>(R.id.animation_cliffs)
                val rokudou = findViewById<View>(R.id.animation_rokudou)
                val gameOver = findViewById<View>(R.id.game_over)
                val backgroundAnim = AnimationUtils.loadAnimation(this, R.anim.rokudou_slide_bg)
                val sphereAnim = AnimationUtils.loadAnimation(this, R.anim.rokudou_slide_sphere)
                val cliffsAnim = AnimationUtils.loadAnimation(this, R.anim.rokudou_slide_cliffs)
                val rokudouAnim = AnimationUtils.loadAnimation(this, R.anim.rokudou_slide_rokudou)
                val gameOverAnim = AnimationUtils.loadAnimation(this, R.anim.rokudou_game_over)
                background.startAnimation(backgroundAnim)
                sphere.startAnimation(sphereAnim)
                cliffs.startAnimation(cliffsAnim)
                rokudou.startAnimation(rokudouAnim)
                gameOver.startAnimation(gameOverAnim)
                animationEndTime = gameOverAnim.duration + System.currentTimeMillis()
            }
            // TODO: handle assert
//            else {
//                assert(false)
//            }
        }

        // Pass the calling intent back so that we can figure out which animation just played.
        setResult(RESULT_OK, callingIntent)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val time = System.currentTimeMillis()
        if (time > animationEndTime) {
            finish()
        } else {
            try {
                Thread.sleep(32)
            } catch (e: InterruptedException) {
                // Safe to ignore.
            }
        }
        return true
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        if (hasFocus && mAnimation != null) {
            mAnimation!!.start()
            killActivityHandler.sleep(mAnimation!!.getDuration(0) * mAnimation!!.numberOfFrames.toLong())
        }
    }

    companion object {
        const val KYLE_DEATH = 0
        const val WANDA_ENDING = 1
        const val KABOCHA_ENDING = 2
        const val ROKUDOU_ENDING = 3
    }
}
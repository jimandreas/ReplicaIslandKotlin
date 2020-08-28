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
@file:Suppress("unused", "RemoveEmptySecondaryConstructorBody", "CascadeIf")

package com.replica.replicaisland

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.os.Bundle
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import android.widget.TextView
import java.lang.reflect.InvocationTargetException
import kotlin.math.floor
import kotlin.math.min

class GameOverActivity : Activity() {
    private val mPearlPercent = 100.0f
    private val mEnemiesDestroyedPercent = 100.0f
    private val mPlayTime = 0.0f
    private val mEnding = AnimationPlayerActivity.KABOCHA_ENDING
    private var mPearlView: IncrementingTextView? = null
    private var mEnemiesDestroyedView: IncrementingTextView? = null
    private var mPlayTimeView: IncrementingTextView? = null
    private var mEndingView: TextView? = null

    class IncrementingTextView : TextView {
        private var mTargetValue = 0f
        private var mIncrement = 1.0f
        private var mCurrentValue = 0.0f
        private val mLastTime: Long = 0
        private var mMode = MODE_NONE

        constructor(context: Context?) : super(context) {}
        constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {}
        constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {}

        fun setTargetValue(target: Float) {
            mTargetValue = target
            postInvalidate()
        }

        fun setMode(mode: Int) {
            mMode = mode
        }

        fun setIncrement(increment: Float) {
            mIncrement = increment
        }

        public override fun onDraw(canvas: Canvas) {
            val time = SystemClock.uptimeMillis()
            val delta = time - mLastTime
            if (delta > INCREMENT_DELAY_MS) {
                if (mCurrentValue < mTargetValue) {
                    mCurrentValue += mIncrement
                    mCurrentValue = min(mCurrentValue, mTargetValue)
                    val value: String
                    value = if (mMode == MODE_PERCENT) {
                        "$mCurrentValue%"
                    } else if (mMode == MODE_TIME) {
                        val seconds = mCurrentValue
                        val minutes = seconds / 60.0f
                        val hours = minutes / 60.0f
                        val totalHours = floor(hours.toDouble()).toInt()
                        val totalHourMinutes = totalHours * 60.0f
                        val totalMinutes = (minutes - totalHourMinutes).toInt()
                        val totalMinuteSeconds = totalMinutes * 60.0f
                        val totalHourSeconds = totalHourMinutes * 60.0f
                        val totalSeconds = (seconds - (totalMinuteSeconds + totalHourSeconds)).toInt()
                        "$totalHours:$totalMinutes:$totalSeconds"
                    } else {
                        mCurrentValue.toString() + ""
                    }
                    text = value
                    postInvalidateDelayed(INCREMENT_DELAY_MS.toLong())
                }
            }
            super.onDraw(canvas)
        }

        companion object {
            private const val INCREMENT_DELAY_MS = 2 * 1000
            private const val MODE_NONE = 0
            const val MODE_PERCENT = 1
            const val MODE_TIME = 2
        }
    }

    private val sOKClickListener = View.OnClickListener {
        finish()
        if (UIConstants.mOverridePendingTransition != null) {
            try {
                UIConstants.mOverridePendingTransition!!.invoke(this@GameOverActivity, R.anim.activity_fade_in, R.anim.activity_fade_out)
            } catch (ite: InvocationTargetException) {
                DebugLog.d("Activity Transition", "Invocation Target Exception")
            } catch (ie: IllegalAccessException) {
                DebugLog.d("Activity Transition", "Illegal Access Exception")
            }
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            super.onCreate(savedInstanceState)
        } else {
            super.onCreate(null)
        }
        setContentView(R.layout.game_over)
        mPearlView = findViewById<View>(R.id.pearl_percent) as IncrementingTextView
        mEnemiesDestroyedView = findViewById<View>(R.id.enemy_percent) as IncrementingTextView
        mPlayTimeView = findViewById<View>(R.id.total_play_time) as IncrementingTextView
        mEndingView = findViewById<View>(R.id.ending) as TextView
        val prefs = getSharedPreferences(PreferenceConstants.PREFERENCE_NAME, MODE_PRIVATE)
        val playTime = prefs.getFloat(PreferenceConstants.PREFERENCE_TOTAL_GAME_TIME, 0.0f)
        val ending = prefs.getInt(PreferenceConstants.PREFERENCE_LAST_ENDING, -1)
        val pearlsCollected = prefs.getInt(PreferenceConstants.PREFERENCE_PEARLS_COLLECTED, 0)
        val pearlsTotal = prefs.getInt(PreferenceConstants.PREFERENCE_PEARLS_TOTAL, 0)
        val enemies = prefs.getInt(PreferenceConstants.PREFERENCE_ROBOTS_DESTROYED, 0)
        if (pearlsCollected > 0 && pearlsTotal > 0) {
            mPearlView!!.setTargetValue((pearlsCollected / pearlsTotal.toFloat() * 100.0f))
        } else {
            mPearlView!!.text = "--"
        }
        mPearlView!!.setMode(IncrementingTextView.MODE_PERCENT)
        mEnemiesDestroyedView!!.setTargetValue(enemies.toFloat())
        mPlayTimeView!!.setTargetValue(playTime)
        mPlayTimeView!!.setIncrement(90.0f)
        mPlayTimeView!!.setMode(IncrementingTextView.MODE_TIME)
        if (ending == AnimationPlayerActivity.KABOCHA_ENDING) {
            mEndingView!!.setText(R.string.game_results_kabocha_ending)
        } else if (ending == AnimationPlayerActivity.ROKUDOU_ENDING) {
            mEndingView!!.setText(R.string.game_results_rokudou_ending)
        } else {
            mEndingView!!.setText(R.string.game_results_wanda_ending)
        }
        val okButton = findViewById<View>(R.id.ok) as Button
        okButton.setOnClickListener(sOKClickListener)
    }
}
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

package com.replica.replicaisland.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.replica.replicaisland.R
import com.replica.replicaisland.core.BaseObject
import java.lang.reflect.InvocationTargetException

/**
 * @deprecated Use [DiaryDialogFragment] instead for displaying diary entries.
 * This activity is kept for rollback purposes.
 */
@Deprecated("Use DiaryDialogFragment instead", ReplaceWith("DiaryDialogFragment"))
class DiaryActivity : Activity() {
    private val killDiaryListener = View.OnClickListener {
        finish()
        if (UIConstants.mOverridePendingTransition != null) {
            try {
                UIConstants.mOverridePendingTransition!!.invoke(this@DiaryActivity, R.anim.activity_fade_in, R.anim.activity_fade_out)
            } catch (_: InvocationTargetException) {
                DebugLog.d("Activity Transition", "Invocation Target Exception")
            } catch (_: IllegalAccessException) {
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

        // New method of landscape orientation.
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
        // end of new method of landscape orientation

        setContentView(R.layout.diary)
        val text = findViewById<View>(R.id.diarytext) as TextView
        val image = findViewById<View>(R.id.diarybackground) as ImageView
        image.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade))
        val callingIntent = intent
        val textResource = callingIntent.getIntExtra("text", -1)
        if (textResource != -1) {
            text.setText(textResource)
        }
        val okArrow = findViewById<View>(R.id.ok) as ImageView
        okArrow.setOnClickListener(killDiaryListener)
        okArrow.setBackgroundResource(R.drawable.ui_button)
        val anim = okArrow.background as AnimationDrawable
        anim.start()
        BaseObject.sSystemRegistry.customToastSystem!!.toast(getString(R.string.diary_found), Toast.LENGTH_SHORT)
    }
}
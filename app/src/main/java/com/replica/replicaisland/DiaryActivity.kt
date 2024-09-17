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
package com.replica.replicaisland

import android.app.Activity
import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import java.lang.reflect.InvocationTargetException

class DiaryActivity : Activity() {
    private val killDiaryListener = View.OnClickListener {
        finish()
        if (UIConstants.mOverridePendingTransition != null) {
            try {
                UIConstants.mOverridePendingTransition!!.invoke(this@DiaryActivity, R.anim.activity_fade_in, R.anim.activity_fade_out)
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
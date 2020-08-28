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
@file:Suppress("unused")

package com.replica.replicaisland

import android.content.Context
import android.content.res.TypedArray
import android.preference.Preference
import android.util.AttributeSet
import android.view.View
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView

class SliderPreference : Preference, OnSeekBarChangeListener {
    private var mValue = INITIAL_VALUE
    private var mMinText: String? = null
    private var mMaxText: String? = null

    constructor(context: Context?) : super(context) {
        widgetLayoutResource = R.layout.slider_preference
    }

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, android.R.attr.preferenceStyle) {
        widgetLayoutResource = R.layout.slider_preference
    }

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {
        val a = context.obtainStyledAttributes(attrs,
                R.styleable.SliderPreference, defStyle, 0)
        mMinText = a.getString(R.styleable.SliderPreference_minText)
        mMaxText = a.getString(R.styleable.SliderPreference_maxText)
        a.recycle()
        widgetLayoutResource = R.layout.slider_preference
    }

    override fun onBindView(view: View) {
        super.onBindView(view)
        if (mMinText != null) {
            val minText = view.findViewById<View>(R.id.min) as TextView
            minText.text = mMinText
        }
        if (mMaxText != null) {
            val maxText = view.findViewById<View>(R.id.max) as TextView
            maxText.text = mMaxText
        }
        val bar = view.findViewById<View>(R.id.slider) as SeekBar
        bar.max = MAX_SLIDER_VALUE
        bar.progress = mValue
        bar.setOnSeekBarChangeListener(this)
    }

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        if (fromUser) {
            mValue = progress
            persistInt(mValue)
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {}
    override fun onStopTrackingTouch(seekBar: SeekBar) {}
    override fun onGetDefaultValue(ta: TypedArray, index: Int): Any {
        return Utils.clamp(ta.getInt(index, INITIAL_VALUE), 0, MAX_SLIDER_VALUE)
    }

    override fun onSetInitialValue(restoreValue: Boolean, defaultValue: Any?) {
        mValue = if (defaultValue != null) defaultValue as Int else INITIAL_VALUE
        if (!restoreValue) {
            persistInt(mValue)
        } else {
            mValue = getPersistedInt(mValue)
        }
    }

    companion object {
        private const val MAX_SLIDER_VALUE = 100
        private const val INITIAL_VALUE = 50
    }
}
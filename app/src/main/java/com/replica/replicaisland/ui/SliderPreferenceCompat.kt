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

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import androidx.preference.PreferenceViewHolder
import androidx.preference.SeekBarPreference
import com.replica.replicaisland.R
import androidx.core.content.withStyledAttributes

class SliderPreferenceCompat @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.preference.R.attr.seekBarPreferenceStyle,
    defStyleRes: Int = 0
) : SeekBarPreference(context, attrs, defStyleAttr, defStyleRes) {

    private var minText: String? = null
    private var maxText: String? = null

    init {
        context.withStyledAttributes(
            attrs,
            R.styleable.SliderPreference,
            defStyleAttr,
            defStyleRes
        ) {
            minText = getString(R.styleable.SliderPreference_minText)
            maxText = getString(R.styleable.SliderPreference_maxText)
        }

        layoutResource = R.layout.slider_preference_compat
        max = MAX_SLIDER_VALUE
        showSeekBarValue = false
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        holder.findViewById(R.id.min)?.let { view ->
            (view as TextView).text = minText
        }
        holder.findViewById(R.id.max)?.let { view ->
            (view as TextView).text = maxText
        }
    }

    companion object {
        private const val MAX_SLIDER_VALUE = 100
    }
}

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
import androidx.preference.DialogPreference
import com.replica.replicaisland.R
import com.replica.replicaisland.data.PreferencesManager

class KeyboardConfigDialogPreferenceCompat @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.preference.R.attr.dialogPreferenceStyle,
    defStyleRes: Int = 0
) : DialogPreference(context, attrs, defStyleAttr, defStyleRes) {

    val leftPrefKey: String?
    val rightPrefKey: String?
    val jumpPrefKey: String?
    val attackPrefKey: String?

    private var prefsManager: PreferencesManager? = null

    init {
        val a = context.obtainStyledAttributes(
            attrs,
            R.styleable.KeyConfigPreference,
            defStyleAttr,
            defStyleRes
        )
        leftPrefKey = a.getString(R.styleable.KeyConfigPreference_leftKey)
        rightPrefKey = a.getString(R.styleable.KeyConfigPreference_rightKey)
        jumpPrefKey = a.getString(R.styleable.KeyConfigPreference_jumpKey)
        attackPrefKey = a.getString(R.styleable.KeyConfigPreference_attackKey)
        a.recycle()

        dialogLayoutResource = R.layout.key_config
    }

    fun setPreferencesManager(manager: PreferencesManager) {
        prefsManager = manager
    }

    fun getPreferencesManager(): PreferencesManager? = prefsManager
}

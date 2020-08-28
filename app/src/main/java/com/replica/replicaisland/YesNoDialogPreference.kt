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
package com.replica.replicaisland

import android.R
import android.content.Context
import android.preference.DialogPreference
import android.util.AttributeSet

class YesNoDialogPreference @JvmOverloads constructor(context: Context?, attrs: AttributeSet? = null,
                                                      defStyle: Int = R.attr.yesNoPreferenceStyle) : DialogPreference(context, attrs, defStyle) {
    private var mListener: YesNoDialogListener? = null

    interface YesNoDialogListener {
        fun onDialogClosed(positiveResult: Boolean)
    }

    fun setListener(listener: YesNoDialogListener?) {
        mListener = listener
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (mListener != null) {
            mListener!!.onDialogClosed(positiveResult)
        }
    }
}
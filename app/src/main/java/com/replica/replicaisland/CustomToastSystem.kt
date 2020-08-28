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

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast

class CustomToastSystem(context: Context) : BaseObject() {
    private val mView: View
    private val mText: TextView
    private val mToast: Toast
    override fun reset() {
        // TODO Auto-generated method stub
    }

    fun toast(text: String?, length: Int) {
        mText.text = text
        mToast.setGravity(Gravity.CENTER, 0, 0)
        mToast.duration = length
        mToast.show()
    }

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        // TODO: fix this null passing
        mView = inflater.inflate(R.layout.custom_toast, null)
        mText = mView.findViewById<View>(R.id.text) as TextView
        mToast = Toast(context)
        mToast.view = mView
    }
}
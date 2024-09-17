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

import android.content.Context
import android.view.MotionEvent

class MultiTouchFilter : SingleTouchFilter() {
    private var checkedForMultitouch = false
    private var mSupportsMultitouch = false
    override fun updateTouch(event: MotionEvent?) {
        val params = sSystemRegistry.contextParameters
        val pointerCount = event!!.pointerCount
        for (x in 0 until pointerCount) {
            val action = event.action
            val actualEvent = action and MotionEvent.ACTION_MASK
            val id = event.getPointerId(x)
            if (actualEvent == MotionEvent.ACTION_POINTER_UP || actualEvent == MotionEvent.ACTION_UP || actualEvent == MotionEvent.ACTION_CANCEL) {
                sSystemRegistry.inputSystem!!.touchUp(id,
                        event.getX(x) * (1.0f / params!!.viewScaleX),
                        event.getY(x) * (1.0f / params.viewScaleY))
            } else {
                sSystemRegistry.inputSystem!!.touchDown(id,
                        event.getX(x) * (1.0f / params!!.viewScaleX),
                        event.getY(x) * (1.0f / params.viewScaleY))
            }
        }
    }

    override fun supportsMultitouch(context: Context?): Boolean {
        if (!checkedForMultitouch) {
            val packageManager = context!!.packageManager
            mSupportsMultitouch = packageManager.hasSystemFeature("android.hardware.touchscreen.multitouch")
            checkedForMultitouch = true
        }
        return mSupportsMultitouch
    }
}
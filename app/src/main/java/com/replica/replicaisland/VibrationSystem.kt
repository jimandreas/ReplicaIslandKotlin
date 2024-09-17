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
@file:Suppress("DEPRECATION")

package com.replica.replicaisland

import android.content.Context
import android.os.Vibrator

/** A system for accessing the Android vibrator.  Note that this system requires the app's
 * AndroidManifest.xml to contain permissions for the Vibrator service.
 */
class VibrationSystem : BaseObject() {
    override fun reset() {}
    fun vibrate(seconds: Float) {
        val params = sSystemRegistry.contextParameters
        if (params?.context != null) {
            val vibrator = params.context!!.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate((seconds * 1000).toLong())
        }
    }
}
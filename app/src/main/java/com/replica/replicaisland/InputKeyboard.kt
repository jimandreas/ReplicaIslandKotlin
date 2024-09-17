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

import android.view.KeyEvent

class InputKeyboard {
    val keys: Array<InputButton?>
    fun press(currentTime: Float, keycode: Int) {
        // TODO: assert(keycode >= 0 && keycode < keys.size)
        if (keycode >= 0 && keycode < keys.size) {
            keys[keycode]!!.press(currentTime, 1.0f)
        }
    }

    fun release(keycode: Int) {
        // TODO: assert(keycode >= 0 && keycode < keys.size)
        if (keycode >= 0 && keycode < keys.size) {
            keys[keycode]!!.release()
        }
    }

    fun releaseAll() {
        val count = keys.size
        for (x in 0 until count) {
            keys[x]!!.release()
        }
    }

    fun resetAll() {
        val count = keys.size
        for (x in 0 until count) {
            keys[x]!!.reset()
        }
    }

    init {
        val count = KeyEvent.getMaxKeyCode()
        keys = arrayOfNulls(count)
        for (x in 0 until count) {
            keys[x] = InputButton()
        }
    }
}
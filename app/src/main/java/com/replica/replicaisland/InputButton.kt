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
@file:Suppress("unused")

package com.replica.replicaisland

class InputButton {
    var pressed = false
        private set
    var lastPressedTime = 0f
        private set
    private var downTime = 0f
    var magnitude = 0f
    fun press(currentTime: Float, magnitude: Float) {
        if (!pressed) {
            pressed = true
            downTime = currentTime
        }
        this.magnitude = magnitude
        lastPressedTime = currentTime
    }

    fun release() {
        pressed = false
    }

    fun getTriggered(currentTime: Float): Boolean {
        return pressed && currentTime - downTime <= BaseObject.sSystemRegistry.timeSystem!!.frameDelta * 2.0f
    }

    fun getPressedDuration(currentTime: Float): Float {
        return currentTime - downTime
    }

    var y: Float
        get() {
            var magnitude = 0.0f
            if (pressed) {
                magnitude = this.magnitude
            }
            return magnitude
        }
        set(magnitude) {
            this.magnitude = magnitude
        }

    fun reset() {
        pressed = false
        magnitude = 0.0f
        lastPressedTime = 0.0f
        downTime = 0.0f
    }
}
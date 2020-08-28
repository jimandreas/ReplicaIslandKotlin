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
// NOTE: fix the "GetX" and "GetY" before converting
@file:Suppress("unused")

package com.replica.replicaisland

import kotlin.math.max

class InputXY {
    private var mXAxis: InputButton
    private var mYAxis: InputButton

    constructor() {
        mXAxis = InputButton()
        mYAxis = InputButton()
    }

    constructor(xAxis: InputButton, yAxis: InputButton) {
        mXAxis = xAxis
        mYAxis = yAxis
    }

    fun press(currentTime: Float, x: Float, y: Float) {
        mXAxis.press(currentTime, x)
        mYAxis.press(currentTime, y)
    }

    fun release() {
        mXAxis.release()
        mYAxis.release()
    }

    fun getTriggered(time: Float): Boolean {
        return mXAxis.getTriggered(time) || mYAxis.getTriggered(time)
    }

    val pressed: Boolean
        get() = mXAxis.pressed || mYAxis.pressed

    fun setVector(vector: Vector2) {
        vector.x = mXAxis.magnitude
        vector.y = mYAxis.magnitude
    }

    fun retreiveXaxisMagnitude(): Float {
        return mXAxis.magnitude
    }

    fun retreiveYaxisMagnitude(): Float {
        return mYAxis.magnitude
    }

    val lastPressedTime: Float
        get() = max(mXAxis.lastPressedTime, mYAxis.lastPressedTime)

    fun releaseX() {
        mXAxis.release()
    }

    fun releaseY() {
        mYAxis.release()
    }

    fun setMagnitude(x: Float, y: Float) {
        mXAxis.magnitude = x
        mYAxis.magnitude = y
    }

    fun reset() {
        mXAxis.reset()
        mYAxis.reset()
    }

    fun clone(other: InputXY) {
        if (other.pressed) {
            press(other.lastPressedTime, other.retreiveXaxisMagnitude(), other.retreiveYaxisMagnitude())
        } else {
            release()
        }
    }
}
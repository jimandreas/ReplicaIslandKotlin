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

class InputTouchScreen : BaseObject() {
    private val touchPoints: Array<InputXY?>
    override fun reset() {
        for (x in 0 until MAX_TOUCH_POINTS) {
            touchPoints[x]!!.reset()
        }
    }

    fun press(index: Int, currentTime: Float, x: Float, y: Float) {
        // TODO: assert(index >= 0 && index < MAX_TOUCH_POINTS)
        if (index < MAX_TOUCH_POINTS) {
            touchPoints[index]!!.press(currentTime, x, y)
        }
    }

    fun release(index: Int) {
        if (index < MAX_TOUCH_POINTS) {
            touchPoints[index]!!.release()
        }
    }

    fun resetAll() {
        for (x in 0 until MAX_TOUCH_POINTS) {
            touchPoints[x]!!.reset()
        }
    }

    fun getTriggered(index: Int, time: Float): Boolean {
        var triggered = false
        if (index < MAX_TOUCH_POINTS) {
            triggered = touchPoints[index]!!.getTriggered(time)
        }
        return triggered
    }

    fun getPressed(index: Int): Boolean {
        var pressed = false
        if (index < MAX_TOUCH_POINTS) {
            pressed = touchPoints[index]!!.pressed
        }
        return pressed
    }

    fun setVector(index: Int, vector: Vector2?) {
        if (index < MAX_TOUCH_POINTS) {
            touchPoints[index]!!.setVector(vector!!)
        }
    }

    fun getX(index: Int): Float {
        var magnitude = 0.0f
        if (index < MAX_TOUCH_POINTS) {
            magnitude = touchPoints[index]!!.retreiveXaxisMagnitude()
        }
        return magnitude
    }

    fun getY(index: Int): Float {
        var magnitude = 0.0f
        if (index < MAX_TOUCH_POINTS) {
            magnitude = touchPoints[index]!!.retreiveYaxisMagnitude()
        }
        return magnitude
    }

    fun getLastPressedTime(index: Int): Float {
        var time = 0.0f
        if (index < MAX_TOUCH_POINTS) {
            time = touchPoints[index]!!.lastPressedTime
        }
        return time
    }

    fun findPointerInRegion(regionX: Float, regionY: Float, regionWidth: Float, regionHeight: Float): InputXY? {
        var touch: InputXY? = null
        for (x in 0 until MAX_TOUCH_POINTS) {
            val pointer = touchPoints[x]
            if (pointer!!.pressed &&
                    getTouchedWithinRegion(pointer.retreiveXaxisMagnitude(), pointer.retreiveYaxisMagnitude(), regionX, regionY, regionWidth, regionHeight)) {
                touch = pointer
                break
            }
        }
        return touch
    }

    private fun getTouchedWithinRegion(x: Float, y: Float, regionX: Float, regionY: Float, regionWidth: Float, regionHeight: Float): Boolean {
        return x >= regionX && y >= regionY && x <= regionX + regionWidth && y <= regionY + regionHeight
    }

    fun getTriggered(gameTime: Float): Boolean {
        var triggered = false
        var x = 0
        while (x < MAX_TOUCH_POINTS && !triggered) {
            triggered = touchPoints[x]!!.getTriggered(gameTime)
            x++
        }
        return triggered
    }

    init {
        touchPoints = arrayOfNulls(MAX_TOUCH_POINTS)
        for (x in 0 until MAX_TOUCH_POINTS) {
            touchPoints[x] = InputXY()
        }
    }

    companion object {
        private const val MAX_TOUCH_POINTS = 5
    }
}
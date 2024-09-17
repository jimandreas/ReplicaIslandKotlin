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
@file:Suppress("UNUSED_PARAMETER")

package com.replica.replicaisland

/**
 * Manages input from a roller wheel and touch screen.  Reduces frequent UI messages to
 * an average direction over a short period of time.
 */
class InputSystem : BaseObject() {
    private val touchScreen = InputTouchScreen()
    private val orientationSensor = InputXY()
    private val trackball = InputXY()
    private val keyboard = InputKeyboard()
    private var screenRotation = 0
    private val orientationInput = FloatArray(3)
    private val orientationOutput = FloatArray(3)
    override fun reset() {
        trackball.reset()
        touchScreen.reset()
        keyboard.resetAll()
        orientationSensor.reset()
    }

    fun roll(x: Float, y: Float) {
        val time = sSystemRegistry.timeSystem
        trackball.press(time!!.gameTime, trackball.retreiveXaxisMagnitude() + x, trackball.retreiveYaxisMagnitude() + y)
    }

    fun touchDown(index: Int, x: Float, y: Float) {
        val params = sSystemRegistry.contextParameters
        val time = sSystemRegistry.timeSystem
        // Change the origin of the touch location from the top-left to the bottom-left to match
        // OpenGL space.
        // TODO: UNIFY THIS SHIT
        touchScreen.press(index, time!!.gameTime, x, params!!.gameHeight - y)
    }

    fun touchUp(index: Int, x: Float, y: Float) {
        // TODO: record up location?
        touchScreen.release(index)
    }

    fun setOrientation(x: Float, y: Float, z: Float) {
        // The order of orientation axes changes depending on the rotation of the screen.
        // Some devices call landscape "ROTAION_90" (e.g. phones), while others call it
        // "ROTATION_0" (e.g. tablets).  So we need to adjust the axes from canonical
        // space into screen space depending on the rotation of the screen from
        // whatever this device calls "default."
        orientationInput[0] = x
        orientationInput[1] = y
        orientationInput[2] = z
        canonicalOrientationToScreenOrientation(screenRotation, orientationInput, orientationOutput)

        // Now we have screen space rotations around xyz.
        val horizontalMotion = orientationOutput[1] / 90.0f
        val verticalMotion = orientationOutput[0] / 90.0f
        val time = sSystemRegistry.timeSystem
        orientationSensor.press(time!!.gameTime, horizontalMotion, verticalMotion)
    }

    fun keyDown(keycode: Int) {
        val time = sSystemRegistry.timeSystem
        val gameTime = time!!.gameTime
        keyboard.press(gameTime, keycode)
    }

    fun keyUp(keycode: Int) {
        keyboard.release(keycode)
    }

    fun releaseAllKeys() {
        trackball.releaseX()
        trackball.releaseY()
        touchScreen.resetAll()
        keyboard.releaseAll()
        orientationSensor.release()
    }

    fun fetchTouchScreen(): InputTouchScreen {
        return touchScreen
    }

    fun fetchOrientationSensor(): InputXY {
        return orientationSensor
    }

    fun fetchTrackball(): InputXY {
        return trackball
    }

    fun fetchKeyboard(): InputKeyboard {
        return keyboard
    }

    fun setTheScreenRotation(rotation: Int) {
        screenRotation = rotation
    }

    companion object {
        // Thanks to NVIDIA for this useful canonical-to-screen orientation function.
        // More here: http://developer.download.nvidia.com/tegra/docs/tegra_android_accelerometer_v5f.pdf
        fun canonicalOrientationToScreenOrientation(
                displayRotation: Int, canVec: FloatArray, screenVec: FloatArray) {
            val axisSwap = arrayOf(intArrayOf(1, -1, 0, 1), intArrayOf(-1, -1, 1, 0), intArrayOf(-1, 1, 0, 1), intArrayOf(1, 1, 1, 0)) // ROTATION_270
            val `as` = axisSwap[displayRotation]
            screenVec[0] = `as`[0].toFloat() * canVec[`as`[2]]
            screenVec[1] = `as`[1].toFloat() * canVec[`as`[3]]
            screenVec[2] = canVec[2]
        }
    }

    init {
        reset()
    }
}
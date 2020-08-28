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
@file:Suppress("UNUSED_PARAMETER")

package com.replica.replicaisland

/**
 * Manages input from a roller wheel and touch screen.  Reduces frequent UI messages to
 * an average direction over a short period of time.
 */
class InputSystem : BaseObject() {
    private val mTouchScreen = InputTouchScreen()
    private val mOrientationSensor = InputXY()
    private val mTrackball = InputXY()
    private val mKeyboard = InputKeyboard()
    private var mScreenRotation = 0
    private val mOrientationInput = FloatArray(3)
    private val mOrientationOutput = FloatArray(3)
    override fun reset() {
        mTrackball.reset()
        mTouchScreen.reset()
        mKeyboard.resetAll()
        mOrientationSensor.reset()
    }

    fun roll(x: Float, y: Float) {
        val time = sSystemRegistry.timeSystem
        mTrackball.press(time!!.gameTime, mTrackball.retreiveXaxisMagnitude() + x, mTrackball.retreiveYaxisMagnitude() + y)
    }

    fun touchDown(index: Int, x: Float, y: Float) {
        val params = sSystemRegistry.contextParameters
        val time = sSystemRegistry.timeSystem
        // Change the origin of the touch location from the top-left to the bottom-left to match
        // OpenGL space.
        // TODO: UNIFY THIS SHIT
        mTouchScreen.press(index, time!!.gameTime, x, params!!.gameHeight - y)
    }

    fun touchUp(index: Int, x: Float, y: Float) {
        // TODO: record up location?
        mTouchScreen.release(index)
    }

    fun setOrientation(x: Float, y: Float, z: Float) {
        // The order of orientation axes changes depending on the rotation of the screen.
        // Some devices call landscape "ROTAION_90" (e.g. phones), while others call it
        // "ROTATION_0" (e.g. tablets).  So we need to adjust the axes from canonical
        // space into screen space depending on the rotation of the screen from
        // whatever this device calls "default."
        mOrientationInput[0] = x
        mOrientationInput[1] = y
        mOrientationInput[2] = z
        canonicalOrientationToScreenOrientation(mScreenRotation, mOrientationInput, mOrientationOutput)

        // Now we have screen space rotations around xyz.
        val horizontalMotion = mOrientationOutput[1] / 90.0f
        val verticalMotion = mOrientationOutput[0] / 90.0f
        val time = sSystemRegistry.timeSystem
        mOrientationSensor.press(time!!.gameTime, horizontalMotion, verticalMotion)
    }

    fun keyDown(keycode: Int) {
        val time = sSystemRegistry.timeSystem
        val gameTime = time!!.gameTime
        mKeyboard.press(gameTime, keycode)
    }

    fun keyUp(keycode: Int) {
        mKeyboard.release(keycode)
    }

    fun releaseAllKeys() {
        mTrackball.releaseX()
        mTrackball.releaseY()
        mTouchScreen.resetAll()
        mKeyboard.releaseAll()
        mOrientationSensor.release()
    }

    fun fetchTouchScreen(): InputTouchScreen {
        return mTouchScreen
    }

    fun fetchOrientationSensor(): InputXY {
        return mOrientationSensor
    }

    fun fetchTrackball(): InputXY {
        return mTrackball
    }

    fun fetchKeyboard(): InputKeyboard {
        return mKeyboard
    }

    fun setTheScreenRotation(rotation: Int) {
        mScreenRotation = rotation
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
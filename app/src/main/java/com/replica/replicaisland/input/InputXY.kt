package com.replica.replicaisland.input

import com.replica.replicaisland.Vector2
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
@file:Suppress("unused")

package com.replica.replicaisland

class InputTouchScreen : BaseObject() {
    private val mTouchPoints: Array<InputXY?>
    override fun reset() {
        for (x in 0 until MAX_TOUCH_POINTS) {
            mTouchPoints[x]!!.reset()
        }
    }

    fun press(index: Int, currentTime: Float, x: Float, y: Float) {
        // TODO: assert(index >= 0 && index < MAX_TOUCH_POINTS)
        if (index < MAX_TOUCH_POINTS) {
            mTouchPoints[index]!!.press(currentTime, x, y)
        }
    }

    fun release(index: Int) {
        if (index < MAX_TOUCH_POINTS) {
            mTouchPoints[index]!!.release()
        }
    }

    fun resetAll() {
        for (x in 0 until MAX_TOUCH_POINTS) {
            mTouchPoints[x]!!.reset()
        }
    }

    fun getTriggered(index: Int, time: Float): Boolean {
        var triggered = false
        if (index < MAX_TOUCH_POINTS) {
            triggered = mTouchPoints[index]!!.getTriggered(time)
        }
        return triggered
    }

    fun getPressed(index: Int): Boolean {
        var pressed = false
        if (index < MAX_TOUCH_POINTS) {
            pressed = mTouchPoints[index]!!.pressed
        }
        return pressed
    }

    fun setVector(index: Int, vector: Vector2?) {
        if (index < MAX_TOUCH_POINTS) {
            mTouchPoints[index]!!.setVector(vector!!)
        }
    }

    fun getX(index: Int): Float {
        var magnitude = 0.0f
        if (index < MAX_TOUCH_POINTS) {
            magnitude = mTouchPoints[index]!!.retreiveXaxisMagnitude()
        }
        return magnitude
    }

    fun getY(index: Int): Float {
        var magnitude = 0.0f
        if (index < MAX_TOUCH_POINTS) {
            magnitude = mTouchPoints[index]!!.retreiveYaxisMagnitude()
        }
        return magnitude
    }

    fun getLastPressedTime(index: Int): Float {
        var time = 0.0f
        if (index < MAX_TOUCH_POINTS) {
            time = mTouchPoints[index]!!.lastPressedTime
        }
        return time
    }

    fun findPointerInRegion(regionX: Float, regionY: Float, regionWidth: Float, regionHeight: Float): InputXY? {
        var touch: InputXY? = null
        for (x in 0 until MAX_TOUCH_POINTS) {
            val pointer = mTouchPoints[x]
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
            triggered = mTouchPoints[x]!!.getTriggered(gameTime)
            x++
        }
        return triggered
    }

    init {
        mTouchPoints = arrayOfNulls(MAX_TOUCH_POINTS)
        for (x in 0 until MAX_TOUCH_POINTS) {
            mTouchPoints[x] = InputXY()
        }
    }

    companion object {
        private const val MAX_TOUCH_POINTS = 5
    }
}
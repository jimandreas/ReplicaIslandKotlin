package com.replica.replicaisland.input

import com.replica.replicaisland.core.BaseObject

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
        return pressed && currentTime - downTime <= BaseObject.Companion.sSystemRegistry.timeSystem!!.frameDelta * 2.0f
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
package com.replica.replicaisland.input

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
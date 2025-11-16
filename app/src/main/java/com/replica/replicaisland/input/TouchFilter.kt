package com.replica.replicaisland.input

import android.content.Context
import android.view.MotionEvent
import com.replica.replicaisland.core.BaseObject

abstract class TouchFilter : BaseObject() {
    abstract fun updateTouch(event: MotionEvent?)
    open fun supportsMultitouch(context: Context?): Boolean {
        return false
    }

    override fun reset() {}
}
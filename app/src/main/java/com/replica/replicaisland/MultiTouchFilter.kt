package com.replica.replicaisland

import android.content.Context
import android.view.MotionEvent

class MultiTouchFilter : SingleTouchFilter() {
    private var mCheckedForMultitouch = false
    private var mSupportsMultitouch = false
    override fun updateTouch(event: MotionEvent?) {
        val params = sSystemRegistry.contextParameters
        val pointerCount = event!!.pointerCount
        for (x in 0 until pointerCount) {
            val action = event.action
            val actualEvent = action and MotionEvent.ACTION_MASK
            val id = event.getPointerId(x)
            if (actualEvent == MotionEvent.ACTION_POINTER_UP || actualEvent == MotionEvent.ACTION_UP || actualEvent == MotionEvent.ACTION_CANCEL) {
                sSystemRegistry.inputSystem!!.touchUp(id,
                        event.getX(x) * (1.0f / params!!.viewScaleX),
                        event.getY(x) * (1.0f / params.viewScaleY))
            } else {
                sSystemRegistry.inputSystem!!.touchDown(id,
                        event.getX(x) * (1.0f / params!!.viewScaleX),
                        event.getY(x) * (1.0f / params.viewScaleY))
            }
        }
    }

    override fun supportsMultitouch(context: Context?): Boolean {
        if (!mCheckedForMultitouch) {
            val packageManager = context!!.packageManager
            mSupportsMultitouch = packageManager.hasSystemFeature("android.hardware.touchscreen.multitouch")
            mCheckedForMultitouch = true
        }
        return mSupportsMultitouch
    }
}
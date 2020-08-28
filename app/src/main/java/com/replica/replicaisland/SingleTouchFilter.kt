package com.replica.replicaisland

import android.view.MotionEvent

open class SingleTouchFilter : TouchFilter() {
    override fun updateTouch(event: MotionEvent?) {
        val params = sSystemRegistry.contextParameters
        if (event!!.action == MotionEvent.ACTION_UP) {
            sSystemRegistry.inputSystem!!.touchUp(0, event.rawX * (1.0f / params!!.viewScaleX),
                    event.rawY * (1.0f / params.viewScaleY))
        } else {
            sSystemRegistry.inputSystem!!.touchDown(0, event.rawX * (1.0f / params!!.viewScaleX),
                    event.rawY * (1.0f / params.viewScaleY))
        }
    }

    override fun reset() {}
}
package com.replica.replicaisland.ui

import com.replica.replicaisland.GameComponent
import com.replica.replicaisland.core.BaseObject
import com.replica.replicaisland.rendering.DrawableObject
import com.replica.replicaisland.rendering.RenderComponent

class FrameRateWatcherComponent : GameComponent() {
    private var renderComponent: RenderComponent? = null
    private var mDrawable: DrawableObject? = null
    private val maxFrameTime = 1.0f / 30.0f
    override fun reset() {
        renderComponent = null
        mDrawable = null
    }

    override fun update(timeDelta: Float, parent: BaseObject?) {
        if (renderComponent != null && mDrawable != null) {
            if (timeDelta > maxFrameTime) {
                renderComponent!!.drawable = mDrawable
            } else {
                renderComponent!!.drawable = null
            }
        }
    }

    fun setup(render: RenderComponent?, drawable: DrawableObject?) {
        renderComponent = render
        mDrawable = drawable
    }

    init {
        setPhaseToThis(ComponentPhases.THINK.ordinal)
    }
}
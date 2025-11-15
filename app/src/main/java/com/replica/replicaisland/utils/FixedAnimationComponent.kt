package com.replica.replicaisland.utils

import com.replica.replicaisland.GameComponent
import com.replica.replicaisland.core.BaseObject
import com.replica.replicaisland.core.GameObject
import com.replica.replicaisland.rendering.SpriteComponent

class FixedAnimationComponent : GameComponent() {
    private var animationIndex = 0
    override fun reset() {
        animationIndex = 0
    }

    override fun update(timeDelta: Float, parent: BaseObject?) {
        // We look up the sprite component each frame so that this component can be shared.
        val parentObject = parent as GameObject
        val sprite = parentObject.findByClass(SpriteComponent::class.java)
        if (sprite != null) {
            sprite.playAnimation(animationIndex)
        }
    }

    fun setAnimation(index: Int) {
        animationIndex = index
    }

    init {
        setPhaseToThis(ComponentPhases.ANIMATION.ordinal)
        reset()
    }
}
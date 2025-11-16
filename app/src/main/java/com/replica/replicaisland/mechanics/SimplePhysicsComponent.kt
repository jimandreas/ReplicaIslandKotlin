package com.replica.replicaisland.mechanics

import com.replica.replicaisland.GameComponent
import com.replica.replicaisland.utils.Utils
import com.replica.replicaisland.core.BaseObject
import com.replica.replicaisland.core.GameObject

/** A light-weight physics implementation for use with non-complex characters (enemies, etc).  */
class SimplePhysicsComponent : GameComponent() {
    private var mBounciness = 0f
    override fun reset() {
        mBounciness = DEFAULT_BOUNCINESS
    }

    fun setBounciness(bounciness: Float) {
        mBounciness = bounciness
    }

    override fun update(timeDelta: Float, parent: BaseObject?) {
        val parentObject = parent as GameObject
        val impulse = parentObject.impulse
        var velocityX = parentObject.velocity.x + impulse.x
        var velocityY = parentObject.velocity.y + impulse.y
        if (parentObject.touchingCeiling() && velocityY > 0.0f
                || parentObject.touchingGround() && velocityY < 0.0f) {
            velocityY = -velocityY * mBounciness
            if (Utils.close(velocityY, 0.0f)) {
                velocityY = 0.0f
            }
        }
        if (parentObject.touchingRightWall() && velocityX > 0.0f
                || parentObject.touchingLeftWall() && velocityX < 0.0f) {
            velocityX = -velocityX * mBounciness
            if (Utils.close(velocityX, 0.0f)) {
                velocityX = 0.0f
            }
        }
        parentObject.velocity[velocityX] = velocityY
        impulse.zero()
    }

    companion object {
        private const val DEFAULT_BOUNCINESS = 0.1f
    }

    init {
        setPhaseToThis(ComponentPhases.POST_PHYSICS.ordinal)
        reset()
    }
}
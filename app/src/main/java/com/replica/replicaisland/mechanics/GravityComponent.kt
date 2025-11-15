package com.replica.replicaisland.mechanics

import com.replica.replicaisland.GameComponent
import com.replica.replicaisland.utils.Vector2
import com.replica.replicaisland.core.BaseObject
import com.replica.replicaisland.core.GameObject

/**
 * A game component that implements gravity.  Adding this component to a game object will cause
 * it to be pulled down towards the ground.
 */
class GravityComponent : GameComponent() {
    val gravity: Vector2
    private val scaledGravity: Vector2
    override fun reset() {
        gravity.set(sDefaultGravity)
    }

    override fun update(timeDelta: Float, parent: BaseObject?) {
        scaledGravity.set(gravity)
        scaledGravity.multiply(timeDelta)
        (parent as GameObject).velocity.add(scaledGravity)
    }

    fun setGravityMultiplier(multiplier: Float) {
        gravity.set(sDefaultGravity)
        gravity.multiply(multiplier)
    }

    companion object {
        private val sDefaultGravity = Vector2(0.0f, -400.0f)
    }

    init {
        gravity = Vector2(sDefaultGravity)
        scaledGravity = Vector2()
        setPhaseToThis(ComponentPhases.PHYSICS.ordinal)
    }
}
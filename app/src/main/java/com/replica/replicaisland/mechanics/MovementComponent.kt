package com.replica.replicaisland.mechanics

import com.replica.replicaisland.GameComponent
import com.replica.replicaisland.utils.Interpolator
import com.replica.replicaisland.core.BaseObject
import com.replica.replicaisland.core.GameObject

/**
 * A game component that implements velocity-based movement.
 */
class MovementComponent : GameComponent() {
    override fun reset() {}
    override fun update(timeDelta: Float, parent: BaseObject?) {
        val `object` = parent as GameObject
        sInterpolator.setAll(`object`.velocity.x, `object`.targetVelocity.x, `object`.acceleration.x)
        val offsetX = sInterpolator.interpolate(timeDelta)
        val newX = `object`.position.x + offsetX
        val newVelocityX = sInterpolator.fetchCurrent()
        sInterpolator.setAll(`object`.velocity.y, `object`.targetVelocity.y, `object`.acceleration.y)
        val offsetY = sInterpolator.interpolate(timeDelta)
        val newY = `object`.position.y + offsetY
        val newVelocityY = sInterpolator.fetchCurrent()
        if (!`object`.positionLocked) {
            `object`.position[newX] = newY
        }
        `object`.velocity[newVelocityX] = newVelocityY
    }

    companion object {
        // If multiple game components were ever running in different threads, this would need
        // to be non-static.
        private val sInterpolator = Interpolator()
    }

    init {
        setPhaseToThis(ComponentPhases.MOVEMENT.ordinal)
    }
}
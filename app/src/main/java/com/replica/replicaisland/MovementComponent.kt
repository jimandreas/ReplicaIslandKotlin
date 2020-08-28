/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.replica.replicaisland

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
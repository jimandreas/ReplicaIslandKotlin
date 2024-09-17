/*
 * Copyright (C) 2010 The Android Open Source Project
 * Copyright (C) 2024 Jim Andreas kotlin conversion and updating
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

//import com.replica.replicaisland.GravityComponent
import kotlin.math.sqrt

class OrbitalMagnetComponent : GameComponent() {
    private var strength = 0f
    private val mCenter: Vector2 = Vector2()
    private val delta: Vector2 = Vector2()
    private val mRim: Vector2 = Vector2()
    private val mVelocity: Vector2 = Vector2()
    private var magnetRadius = 0f
    private var mAreaRadius = 0f
    override fun reset() {
        mCenter.zero()
        delta.zero()
        mRim.zero()
        mVelocity.zero()
        strength = DEFAULT_STRENGTH
        mAreaRadius = 0.0f
        magnetRadius = 0.0f
    }

    override fun update(timeDelta: Float, parent: BaseObject?) {
        val manager = sSystemRegistry.gameObjectManager
        if (manager != null) {
            val player = manager.player
            if (player != null) {
                val parentObject = parent as GameObject
                applyMagnetism(player,
                        parentObject.centeredPositionX,
                        parentObject.centeredPositionY,
                        timeDelta)
            }
        }
    }

    private fun applyMagnetism(target: GameObject, centerX: Float, centerY: Float, timeDelta: Float) {
        mCenter[centerX] = centerY
        val targetX = target.centeredPositionX
        val targetY = target.centeredPositionY
        delta[targetX] = targetY
        delta.subtract(mCenter)
        val distanceFromCenter2 = delta.length2()
        val area2 = mAreaRadius * mAreaRadius
        if (distanceFromCenter2 < area2) {
            mRim.set(delta)
            mRim.normalize()
            mRim.multiply(magnetRadius)
            mRim.add(mCenter)
            // rim is now the closest point on the magnet circle

            // remove gravity
            val targetVelocity = target.velocity
            val gravity = target.findByClass(GravityComponent::class.java)
            //val gravityVector = gravity.gravity
            val gravityVector = gravity!!.gravity
            mVelocity.set(gravityVector)
            mVelocity.multiply(timeDelta)
            targetVelocity.subtract(mVelocity)
            delta.add(targetVelocity)
            delta.normalize()
            delta.multiply(magnetRadius)
            delta.add(mCenter)

            // delta is now the next point on the magnet circle in the direction of
            // movement.
            delta.subtract(mRim)
            delta.normalize()
            // Now delta is the tangent to the magnet circle, pointing in the direction
            // of movement.
            mVelocity.set(delta)
            mVelocity.normalize()

            // mVelocity is now the direction to push the player
            mVelocity.multiply(strength)
            if (distanceFromCenter2 > magnetRadius * magnetRadius) {
                val distance = sqrt(distanceFromCenter2.toDouble()).toFloat()
                var weight = (distance - magnetRadius) / (mAreaRadius - magnetRadius)
                weight = 1.0f - weight
                mVelocity.multiply(weight)
            }
            val speed = targetVelocity.length()
            targetVelocity.add(mVelocity)
            if (targetVelocity.length2() > speed * speed) {
                targetVelocity.normalize()
                targetVelocity.multiply(speed)
            }
        }
    }

    fun setup(areaRadius: Float, orbitRadius: Float) {
        mAreaRadius = areaRadius
        magnetRadius = orbitRadius
    }

    companion object {
        private const val DEFAULT_STRENGTH = 15.0f
    }

    init {
        reset()
        setPhaseToThis(ComponentPhases.COLLISION_DETECTION.ordinal)
    }
}
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
@file:Suppress("unused")

package com.replica.replicaisland

//import com.replica.replicaisland.GravityComponent
import kotlin.math.abs
import kotlin.math.min

/**
 * Component that adds physics to its parent game object.  This component implements force
 * calculation based on mass, impulses, friction, and collisions.
 */
class PhysicsComponent internal constructor() : GameComponent() {
    var mass = 0f
    private var bounciness // 1.0 = super bouncy, 0.0 = zero bounce
            = 0f
    private var inertia = 0f
    var staticFrictionCoeffecient = 0f
    var dynamicFrictionCoeffecient = 0f
    override fun reset() {
        // TODO: no reason to call accessors here locally.
        mass = DEFAULT_MASS
        bounciness = DEFAULT_BOUNCINESS
        inertia = DEFAULT_INERTIA
        staticFrictionCoeffecient = DEFAULT_STATIC_FRICTION_COEFFECIENT
        dynamicFrictionCoeffecient = DEFAULT_DYNAMIC_FRICTION_COEFFECIENT
    }

    override fun update(timeDelta: Float, parent: BaseObject?) {
        val parentObject = parent as GameObject

        // we look to user data so that other code can provide impulses
        val impulseVector = parentObject.impulse
        val currentVelocity = parentObject.velocity
        val surfaceNormal = parentObject.backgroundCollisionNormal
        if (surfaceNormal.length2() > 0.0f) {
            resolveCollision(currentVelocity, impulseVector, surfaceNormal, impulseVector)
        }
        val vectorPool = sSystemRegistry.vectorPool

        // if our speed is below inertia, we need to overcome inertia before we can move.
        var physicsCausesMovement = true
        val inertiaSquared = inertia * inertia
        val newVelocity = vectorPool!!.allocate(currentVelocity)
        newVelocity.add(impulseVector)
        if (newVelocity.length2() < inertiaSquared) {
            physicsCausesMovement = false
        }
        val touchingFloor = parentObject.touchingGround()
        val gravity = parentObject.findByClass(GravityComponent::class.java)
        if (touchingFloor && currentVelocity.y <= 0.0f && abs(newVelocity.x) > 0.0f && gravity != null) {
            val gravityVector = gravity.gravity

            // if we were moving last frame, we'll use dynamic friction. Else
            // static.
            var frictionCoeffecient = if (abs(currentVelocity.x) > 0.0f) dynamicFrictionCoeffecient else staticFrictionCoeffecient
            frictionCoeffecient *= timeDelta

            // Friction = cofN, where cof = friction coefficient and N = force
            // perpendicular to the ground.
            val maxFriction = (abs(gravityVector.y) * mass
                    * frictionCoeffecient)
            if (maxFriction > abs(newVelocity.x)) {
                newVelocity.x = 0.0f
            } else {
                newVelocity.x = (newVelocity.x
                        - maxFriction * Utils.sign(newVelocity.x))
            }
        }
        if (abs(newVelocity.x) < 0.01f) {
            newVelocity.x = 0.0f
        }
        if (abs(newVelocity.y) < 0.01f) {
            newVelocity.y = 0.0f
        }

        // physics-based movements means constant acceleration, always. set the target to the
        // velocity.
        if (physicsCausesMovement) {
            parentObject.velocity = newVelocity
            parentObject.targetVelocity = newVelocity
            parentObject.acceleration = Vector2.ZERO
            parentObject.impulse = Vector2.ZERO
        }
        vectorPool.release(newVelocity)
    }

    private fun resolveCollision(velocity: Vector2?, impulse: Vector2?, opposingNormal: Vector2?,
                                   outputImpulse: Vector2) {
        val vectorPool = sSystemRegistry.vectorPool
        outputImpulse.set(impulse!!)
        val collisionNormal = vectorPool!!.allocate(opposingNormal)
        collisionNormal.normalize()
        val relativeVelocity = vectorPool.allocate(velocity)
        relativeVelocity.add(impulse)
        val dotRelativeAndNormal = relativeVelocity.dot(collisionNormal)

        // make sure the motion of the entity requires resolution
        if (dotRelativeAndNormal < 0.0f) {
            val coefficientOfRestitution = bounciness // 0 = perfectly inelastic,
            // 1 = perfectly elastic

            // calculate an impulse to apply to the entity
            var j = -(1 + coefficientOfRestitution) * dotRelativeAndNormal
            j /= collisionNormal.dot(collisionNormal) * (1 / mass)
            val entity1Adjust = vectorPool.allocate(collisionNormal)
            entity1Adjust.set(collisionNormal)
            entity1Adjust.multiply(j)
            entity1Adjust.divide(mass)
            entity1Adjust.add(impulse)
            outputImpulse.set(entity1Adjust)
            vectorPool.release(entity1Adjust)
        }
        vectorPool.release(collisionNormal)
        vectorPool.release(relativeVelocity)
    }

    private fun resolveCollision(velocity: Vector2?, impulse: Vector2?, opposingNormal: Vector2?,
                                   otherMass: Float, otherVelocity: Vector2?, otherImpulse: Vector2?,
                                   otherBounciness: Float, outputImpulse: Vector2) {
        val vectorPool = sSystemRegistry.vectorPool
        val collisionNormal = vectorPool!!.allocate(opposingNormal)
        collisionNormal.normalize()
        val entity1Velocity = vectorPool.allocate(velocity)
        entity1Velocity.add(impulse!!)
        val entity2Velocity = vectorPool.allocate(otherVelocity)
        entity2Velocity.add(otherImpulse!!)
        val relativeVelocity = vectorPool.allocate(entity1Velocity)
        relativeVelocity.subtract(entity2Velocity)
        val dotRelativeAndNormal = relativeVelocity.dot(collisionNormal)

        // make sure the entities' motion requires resolution
        if (dotRelativeAndNormal < 0.0f) {
            val bounciness = min(bounciness + otherBounciness, 1.0f)
            val coefficientOfRestitution = bounciness // 0 = perfectly inelastic,
            // 1 = perfectly elastic

            // calculate an impulse to apply to both entities
            var j = -(1 + coefficientOfRestitution) * dotRelativeAndNormal
            j /= collisionNormal.dot(collisionNormal) * (1 / mass + 1 / otherMass)
            val entity1Adjust = vectorPool.allocate(collisionNormal)
            entity1Adjust.multiply(j)
            entity1Adjust.divide(mass)
            entity1Adjust.add(impulse)
            outputImpulse.set(entity1Adjust)

            // TODO: Deal impulses both ways.
            /*
             * Vector3 entity2Adjust = (collisionNormal j);
             * entity2Adjust[0] /= otherMass;
             * entity2Adjust[1] /= otherMass;
             * entity2Adjust[2] /= otherMass;
             *
             * const Vector3 newEntity2Impulse = otherImpulse + entity2Adjust;
             */vectorPool.release(entity1Adjust)
        }
        vectorPool.release(collisionNormal)
        vectorPool.release(entity1Velocity)
        vectorPool.release(entity2Velocity)
        vectorPool.release(relativeVelocity)
    }

    companion object {
        private const val DEFAULT_MASS = 1.0f
        private const val DEFAULT_BOUNCINESS = 0.1f
        private const val DEFAULT_INERTIA = 0.01f
        private const val DEFAULT_STATIC_FRICTION_COEFFECIENT = 0.05f
        private const val DEFAULT_DYNAMIC_FRICTION_COEFFECIENT = 0.02f
    }

    init {
        reset()
        setPhaseToThis(ComponentPhases.POST_PHYSICS.ordinal)
    }
}
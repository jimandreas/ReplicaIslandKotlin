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

import kotlin.math.abs

/**
 * Helper class for interpolating velocity over time given a target velocity and acceleration.
 * The current velocity will be accelerated towards the target until the target is reached.
 * Note that acceleration is effectively an absolute value--it always points in the direction of
 * the target velocity.
 */
class Interpolator : AllocationGuard() {
    private var mCurrent = 0f
    private var mTarget = 0f
    private var mAcceleration = 0f

    // Rather than simply interpolating acceleration and velocity for each time step
    // (as in, position += (velocity * time); velocity += (acceleration * time);),
    // we actually perform the work needed to calculate the integral of velocity with respect to
    // time.
    //
    // The integral of velocity is:
    //
    // integral[(v + aT)dT]
    //
    // Simplified to:
    //
    // vT + 1/2 * aT^2
    //
    // Thus:
    // change in position = velocity * time + (0.5 * acceleration * (time^2))
    // change in velocity = acceleration * time
    fun setAll(current: Float, target: Float, acceleration: Float) {
        mCurrent = current
        mTarget = target
        mAcceleration = acceleration
    }

    // While this function writes directly to velocity, it doesn't affect
    // position.  Instead, the position offset is returned so that it can be blended.
    fun interpolate(secondsDelta: Float): Float {
        val oldVelocity = mCurrent

        // point the acceleration at the target, or zero it if we are already
        // there
        val directionalAcceleration = calculateAcceleration(oldVelocity, mAcceleration, mTarget)

        // calculate scaled acceleration (0.5 * acceleration * (time^2))
        val scaledAcceleration: Float
        scaledAcceleration = scaleAcceleration(directionalAcceleration, secondsDelta)

        // calculate the change in position
        val positionOffset = oldVelocity * secondsDelta + scaledAcceleration

        // change in velocity = v + aT
        var newVelocity = oldVelocity + directionalAcceleration * secondsDelta

        // check to see if we've passed our target velocity since the last time
        // step.  If so, clamp to the target
        if (passedTarget(oldVelocity, newVelocity, mTarget)) {
            newVelocity = mTarget
        }
        mCurrent = newVelocity
        return positionOffset
    }

    fun fetchCurrent(): Float {
        return mCurrent
    }

    private fun passedTarget(oldVelocity: Float, newVelocity: Float, targetVelocity: Float): Boolean {
        var result = false
        if (oldVelocity < targetVelocity && newVelocity > targetVelocity) {
            result = true
        } else if (oldVelocity > targetVelocity && newVelocity < targetVelocity) {
            result = true
        }
        return result
    }

    // find the magnitude and direction of acceleration.
    // in this system, acceleration always points toward target velocity
    private fun calculateAcceleration(velocity: Float, accelerationIn: Float, target: Float): Float {
        var acceleration = accelerationIn
        if (abs(velocity - target) < 0.0001f) {
            // no accel needed
            acceleration = 0.0f
        } else if (velocity > target) {
            // accel must be negative
            acceleration *= -1.0f
        }
        return acceleration
    }

    // calculates 1/2 aT^2
    private fun scaleAcceleration(acceleration: Float, secondsDelta: Float): Float {
        val timeSquared = secondsDelta * secondsDelta
        var scaledAccel = acceleration * timeSquared
        scaledAccel *= 0.5f
        return scaledAccel
    }
}
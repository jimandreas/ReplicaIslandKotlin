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
@file:Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER", "UnnecessaryVariable", "unused")

package com.replica.replicaisland

import kotlin.math.max
import kotlin.math.min

/** A sphere collision volume.  */
class SphereCollisionVolume : CollisionVolume {
    var radius: Float
    private var mCenter: Vector2
    private var workspaceVector: Vector2
    private var workspaceVector2: Vector2

    constructor(radius: Float, centerX: Float, centerY: Float) : super() {
        this.radius = radius
        mCenter = Vector2(centerX, centerY)
        workspaceVector = Vector2()
        workspaceVector2 = Vector2()
    }

    constructor(radius: Int, centerX: Int, centerY: Int) : super() {
        this.radius = radius.toFloat()
        mCenter = Vector2(centerX, centerY)
        workspaceVector = Vector2()
        workspaceVector2 = Vector2()
    }
    
    constructor(radius: Float, centerX: Float, centerY: Float, hit: Int) : super(hit) {
        this.radius = radius
        mCenter = Vector2(centerX, centerY)
        workspaceVector = Vector2()
        workspaceVector2 = Vector2()
    }

    constructor(radius: Int, centerX: Int, centerY: Int, hit: Int) : super(hit) {
        this.radius = radius.toFloat()
        mCenter = Vector2(centerX, centerY)
        workspaceVector = Vector2()
        workspaceVector2 = Vector2()
    }

    override fun fetchMaxX(): Float {
        return mCenter.x + radius
    }

    override fun fetchMinX(): Float {
        return mCenter.x - radius
    }

    override fun fetchMaxY(): Float {
        return mCenter.y + radius
    }

    override fun fetchMinY(): Float {
        return mCenter.y - radius
    }

    var center: Vector2?
        get() = mCenter
        set(center) {
            mCenter.set(center!!)
        }

    fun reset() {
        mCenter.zero()
        radius = 0f
    }

    override fun intersects(position: Vector2?, flip: FlipInfo?, other: CollisionVolume?,
                            otherPosition: Vector2?, otherFlip: FlipInfo?): Boolean {
        var result = false
        if (other is AABoxCollisionVolume) {
            // It's more accurate to do a sphere-as-box test than a box-as-sphere test.
            result = other.intersects(otherPosition, otherFlip, this, position, flip)
        } else {
            workspaceVector.set(position!!)
            offsetByCenter(workspaceVector, mCenter, flip)
            var otherRadius = 0f
            if (other is SphereCollisionVolume) {
                val sphereOther = other
                workspaceVector2.set(otherPosition!!)
                offsetByCenter(workspaceVector2, sphereOther.center!!, otherFlip)
                workspaceVector.subtract(workspaceVector2)
                otherRadius = sphereOther.radius
            } else {
                // Whatever this volume is, pretend it's a sphere.
                val deltaX = (other!!.maxXPosition(otherFlip)
                        - other.minXPosition(otherFlip))
                val deltaY = (other.maxYPosition(otherFlip)
                        - other.minYPosition(otherFlip))
                val centerX = deltaX / 2.0f
                val centerY = deltaY / 2.0f
                workspaceVector2.set(otherPosition!!)
                workspaceVector2.x += centerX
                workspaceVector2.y += centerY
                otherRadius = max(deltaX, deltaY)
            }
            val maxDistance = radius + otherRadius
            val distance2 = workspaceVector.length2()
            val maxDistance2 = maxDistance * maxDistance
            if (distance2 < maxDistance2) {
                result = true
            }
        }
        return result
    }

    fun growBy(other: CollisionVolume) {
        val maxX: Float
        val minX: Float
        val maxY: Float
        val minY: Float
        if (radius > 0) {
            maxX = max(fetchMaxX(), other.fetchMaxX())
            minX = min(fetchMinX(), other.fetchMinX())
            maxY = max(fetchMaxY(), other.fetchMaxY())
            minY = min(fetchMinY(), other.fetchMinY())
        } else {
            maxX = other.fetchMaxX()
            minX = other.fetchMinX()
            maxY = other.fetchMaxY()
            minY = other.fetchMinY()
        }
        val horizontalDelta = maxX - minX
        val verticalDelta = maxY - minY
        val diameter = max(horizontalDelta, verticalDelta)
        val newCenterX = minX + horizontalDelta / 2.0f
        val newCenterY = minY + verticalDelta / 2.0f
        val newRadius = diameter / 2.0f
        mCenter[newCenterX] = newCenterY
        radius = newRadius
    }

    companion object {
        private fun offsetByCenter(position: Vector2, center: Vector2, flip: FlipInfo?) {
            if (flip != null && (flip.flipX || flip.flipY)) {
                if (flip.flipX) {
                    position.x += flip.parentWidth - center.x
                } else {
                    position.x += center.x
                }
                if (flip.flipY) {
                    position.y += flip.parentHeight - center.y
                } else {
                    position.y += center.y
                }
            } else {
                position.add(center)
            }
        }
    }
}
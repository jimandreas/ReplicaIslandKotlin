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

// Simple collision detection component for objects not requiring complex collision (projectiles, etc)
class SimpleCollisionComponent : GameComponent() {
    private val previousPosition: Vector2
    private val currentPosition: Vector2
    private val movementDirection: Vector2
    private val hitPoint: Vector2
    private val hitNormal: Vector2
    override fun reset() {
        previousPosition.zero()
        currentPosition.zero()
        movementDirection.zero()
        hitPoint.zero()
        hitNormal.zero()
    }

    override fun update(timeDelta: Float, parent: BaseObject?) {
        val parentObject = parent as GameObject
        if (previousPosition.length2() > 0.0f) {
            currentPosition[parentObject.centeredPositionX] = parentObject.centeredPositionY
            movementDirection.set(currentPosition)
            movementDirection.subtract(previousPosition)
            if (movementDirection.length2() > 0.0f) {
                val collision = sSystemRegistry.collisionSystem
                if (collision != null) {
                    val hit = collision.castRay(previousPosition, currentPosition,
                            movementDirection, hitPoint, hitNormal, parentObject)
                    if (hit) {
                        // snap
                        val halfWidth = parentObject.width / 2.0f
                        val halfHeight = parentObject.height / 2.0f
                        if (!Utils.close(hitNormal.x, 0.0f)) {
                            parentObject.position.x = hitPoint.x - halfWidth
                        }
                        if (!Utils.close(hitNormal.y, 0.0f)) {
                            parentObject.position.y = hitPoint.y - halfHeight
                        }
                        val timeSystem = sSystemRegistry.timeSystem
                        if (timeSystem != null) {
                            val time = timeSystem.gameTime
                            if (hitNormal.x > 0.0f) {
                                parentObject.lastTouchedLeftWallTime = time
                            } else if (hitNormal.x < 0.0) {
                                parentObject.lastTouchedRightWallTime = time
                            }
                            if (hitNormal.y > 0.0f) {
                                parentObject.lastTouchedFloorTime = time
                            } else if (hitNormal.y < 0.0f) {
                                parentObject.lastTouchedCeilingTime = time
                            }
                        }
                        parentObject.backgroundCollisionNormal = hitNormal
                    }
                }
            }
        }
        previousPosition[parentObject.centeredPositionX] = parentObject.centeredPositionY
    }

    init {
        setPhaseToThis(ComponentPhases.COLLISION_DETECTION.ordinal)
        previousPosition = Vector2()
        currentPosition = Vector2()
        movementDirection = Vector2()
        hitPoint = Vector2()
        hitNormal = Vector2()
    }
}
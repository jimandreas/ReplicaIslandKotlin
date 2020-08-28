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
    private val mPreviousPosition: Vector2
    private val mCurrentPosition: Vector2
    private val mMovementDirection: Vector2
    private val mHitPoint: Vector2
    private val mHitNormal: Vector2
    override fun reset() {
        mPreviousPosition.zero()
        mCurrentPosition.zero()
        mMovementDirection.zero()
        mHitPoint.zero()
        mHitNormal.zero()
    }

    override fun update(timeDelta: Float, parent: BaseObject?) {
        val parentObject = parent as GameObject
        if (mPreviousPosition.length2() > 0.0f) {
            mCurrentPosition[parentObject.centeredPositionX] = parentObject.centeredPositionY
            mMovementDirection.set(mCurrentPosition)
            mMovementDirection.subtract(mPreviousPosition)
            if (mMovementDirection.length2() > 0.0f) {
                val collision = sSystemRegistry.collisionSystem
                if (collision != null) {
                    val hit = collision.castRay(mPreviousPosition, mCurrentPosition,
                            mMovementDirection, mHitPoint, mHitNormal, parentObject)
                    if (hit) {
                        // snap
                        val halfWidth = parentObject.width / 2.0f
                        val halfHeight = parentObject.height / 2.0f
                        if (!Utils.close(mHitNormal.x, 0.0f)) {
                            parentObject.position.x = mHitPoint.x - halfWidth
                        }
                        if (!Utils.close(mHitNormal.y, 0.0f)) {
                            parentObject.position.y = mHitPoint.y - halfHeight
                        }
                        val timeSystem = sSystemRegistry.timeSystem
                        if (timeSystem != null) {
                            val time = timeSystem.gameTime
                            if (mHitNormal.x > 0.0f) {
                                parentObject.lastTouchedLeftWallTime = time
                            } else if (mHitNormal.x < 0.0) {
                                parentObject.lastTouchedRightWallTime = time
                            }
                            if (mHitNormal.y > 0.0f) {
                                parentObject.lastTouchedFloorTime = time
                            } else if (mHitNormal.y < 0.0f) {
                                parentObject.lastTouchedCeilingTime = time
                            }
                        }
                        parentObject.backgroundCollisionNormal = mHitNormal
                    }
                }
            }
        }
        mPreviousPosition[parentObject.centeredPositionX] = parentObject.centeredPositionY
    }

    init {
        setPhaseToThis(ComponentPhases.COLLISION_DETECTION.ordinal)
        mPreviousPosition = Vector2()
        mCurrentPosition = Vector2()
        mMovementDirection = Vector2()
        mHitPoint = Vector2()
        mHitNormal = Vector2()
    }
}
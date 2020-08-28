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
@file:Suppress("unused", "VARIABLE_WITH_REDUNDANT_INITIALIZER", "ConstantConditionIf")

package com.replica.replicaisland

import java.util.*
import kotlin.math.abs

/**
 * Handles collision against the background.  Snaps colliding objects out of collision and reports
 * the hit to the parent game object.
 */
class BackgroundCollisionComponent : GameComponent {
    private var mPreviousPosition: Vector2
    private var mWidth = 0
    private var mHeight = 0
    private var mHorizontalOffset = 0
    private var mVerticalOffset = 0

    // Workspace vectors.  Allocated up front for speed.
    private var mCurrentPosition: Vector2
    private var mPreviousCenter: Vector2
    private var mDelta: Vector2
    private var mFilterDirection: Vector2
    private var mHorizontalHitPoint: Vector2
    private var mHorizontalHitNormal: Vector2
    private var mVerticalHitPoint: Vector2
    private var mVerticalHitNormal: Vector2
    private var mRayStart: Vector2
    private var mRayEnd: Vector2
    private var mTestPointStart: Vector2
    private var mTestPointEnd: Vector2
    private var mMergedNormal: Vector2

    /**
     * Sets up the collision bounding box.  This box may be a different size than the bounds of the
     * sprite that this object controls.
     * @param width  The width of the collision box.
     * @param height  The height of the collision box.
     * @param horzOffset  The offset of the collision box from the object's origin in the x axis.
     * @param vertOffset  The offset of the collision box from the object's origin in the y axis.
     */
    constructor(width: Int, height: Int, horzOffset: Int, vertOffset: Int) : super() {
        setPhaseToThis(ComponentPhases.COLLISION_RESPONSE.ordinal)
        mPreviousPosition = Vector2()
        mWidth = width
        mHeight = height
        mHorizontalOffset = horzOffset
        mVerticalOffset = vertOffset
        mCurrentPosition = Vector2()
        mPreviousCenter = Vector2()
        mDelta = Vector2()
        mFilterDirection = Vector2()
        mHorizontalHitPoint = Vector2()
        mHorizontalHitNormal = Vector2()
        mVerticalHitPoint = Vector2()
        mVerticalHitNormal = Vector2()
        mRayStart = Vector2()
        mRayEnd = Vector2()
        mTestPointStart = Vector2()
        mTestPointEnd = Vector2()
        mMergedNormal = Vector2()
    }

    constructor() : super() {
        setPhaseToThis(ComponentPhases.COLLISION_RESPONSE.ordinal)
        mPreviousPosition = Vector2()
        mCurrentPosition = Vector2()
        mPreviousCenter = Vector2()
        mDelta = Vector2()
        mFilterDirection = Vector2()
        mHorizontalHitPoint = Vector2()
        mHorizontalHitNormal = Vector2()
        mVerticalHitPoint = Vector2()
        mVerticalHitNormal = Vector2()
        mRayStart = Vector2()
        mRayEnd = Vector2()
        mTestPointStart = Vector2()
        mTestPointEnd = Vector2()
        mMergedNormal = Vector2()
    }

    override fun reset() {
        mPreviousPosition.zero()
    }

    fun setSize(width: Int, height: Int) {
        mWidth = width
        mHeight = height
        // TODO: Resize might cause new collisions.
    }

    fun setOffset(horzOffset: Int, vertOffset: Int) {
        mHorizontalOffset = horzOffset
        mVerticalOffset = vertOffset
    }

    /**
     * This function is the meat of the collision response logic.  Our collision detection and
     * response must be capable of dealing with arbitrary surfaces and must be frame rate
     * independent (we must sweep the space in-between frames to find collisions reliably).  The
     * following algorithm is used to keep the collision box out of the collision world.
     * 1.  Cast a ray from the center point of the box at its position last frame to the edge
     * of the box at its current position.  If the ray intersects anything, snap the box
     * back to the point of intersection.
     * 2.  Perform Step 1 twice: once looking for surfaces opposing horizontal movement and
     * again for surfaces opposing vertical movement.  These two ray tests approximate the
     * movement of the box between the previous frame and this one.
     * 3.  Since most collisions are collisions with the ground, more precision is required for
     * vertical intersections.  Perform another ray test, this time from the top of the
     * box's position (after snapping in Step 2) to the bottom.  Snap out of any vertical
     * surfaces that the ray encounters.  This will ensure consistent snapping behavior on
     * incline surfaces.
     * 4.  Add the normals of the surfaces that were hit up and normalize the result to produce
     * a direction describing the average slope of the surfaces that the box is resting on.
     * Physics will use this value as a normal to resolve collisions with the background.
     */
    override fun update(timeDelta: Float, parent: BaseObject?) {
        val parentObject = parent as GameObject
        parentObject.backgroundCollisionNormal = Vector2.ZERO
        if (mPreviousPosition.length2() != 0f) {
            val collision = sSystemRegistry.collisionSystem
            if (collision != null) {
                val left = mHorizontalOffset
                val bottom = mVerticalOffset
                val right = left + mWidth
                val top = bottom + mHeight
                val centerOffsetX = mWidth / 2.0f + left
                val centerOffsetY = mHeight / 2.0f + bottom
                mCurrentPosition.set(parentObject.position)
                mDelta.set(mCurrentPosition)
                mDelta.subtract(mPreviousPosition)
                mPreviousCenter[centerOffsetX] = centerOffsetY
                mPreviousCenter.add(mPreviousPosition)
                var horizontalHit = false
                var verticalHit = false
                mVerticalHitPoint.zero()
                mVerticalHitNormal.zero()
                mHorizontalHitPoint.zero()
                mHorizontalHitNormal.zero()


                // The order in which we sweep the horizontal and vertical space can affect the
                // final result because we perform incremental snapping mid-sweep.  So it is
                // necessary to sweep in the primary direction of movement first.
                if (abs(mDelta.x) > abs(mDelta.y)) {
                    horizontalHit = sweepHorizontal(mPreviousCenter, mCurrentPosition, mDelta, left,
                            right, centerOffsetY, mHorizontalHitPoint, mHorizontalHitNormal,
                            parentObject)
                    verticalHit = sweepVertical(mPreviousCenter, mCurrentPosition, mDelta, bottom,
                            top, centerOffsetX, mVerticalHitPoint, mVerticalHitNormal,
                            parentObject)
                } else {
                    verticalHit = sweepVertical(mPreviousCenter, mCurrentPosition, mDelta, bottom,
                            top, centerOffsetX, mVerticalHitPoint, mVerticalHitNormal,
                            parentObject)
                    horizontalHit = sweepHorizontal(mPreviousCenter, mCurrentPosition, mDelta, left,
                            right, centerOffsetY, mHorizontalHitPoint, mHorizontalHitNormal,
                            parentObject)
                }

                // force the collision volume to stay within the bounds of the world.
                val level = sSystemRegistry.levelSystem
                if (level != null) {
                    if (mCurrentPosition.x + left < 0.0f) {
                        mCurrentPosition.x = (-left + 1).toFloat()
                        horizontalHit = true
                        mHorizontalHitNormal.x = mHorizontalHitNormal.x + 1.0f
                        mHorizontalHitNormal.normalize()
                    } else if (mCurrentPosition.x + right > level.levelWidth) {
                        mCurrentPosition.x = level.levelWidth - right - 1
                        mHorizontalHitNormal.x = mHorizontalHitNormal.x - 1.0f
                        mHorizontalHitNormal.normalize()
                        horizontalHit = true
                    }

                    /*if (mCurrentPosition.y + bottom < 0.0f) {
                        mCurrentPosition.y = (-bottom + 1);
                        verticalHit = true;
                        mVerticalHitNormal.y = (mVerticalHitNormal.y + 1.0f);
                        mVerticalHitNormal.normalize();
                    } else*/if (mCurrentPosition.y + top > level.levelHeight) {
                        mCurrentPosition.y = level.levelHeight - top - 1
                        mVerticalHitNormal.y = mVerticalHitNormal.y - 1.0f
                        mVerticalHitNormal.normalize()
                        verticalHit = true
                    }
                }


                // One more set of tests to make sure that we are aligned with the surface.
                // This time we will just check the inside of the bounding box for intersections.
                // The sweep tests above will keep us out of collision in most cases, but this
                // test will ensure that we are aligned to incline surfaces correctly.

                // Shoot a vertical line through the middle of the box.
                if (mDelta.x != 0.0f && mDelta.y != 0.0f) {
                    val yStart = top.toFloat()
                    val yEnd = bottom.toFloat()
                    mRayStart[centerOffsetX] = yStart
                    mRayStart.add(mCurrentPosition)
                    mRayEnd[centerOffsetX] = yEnd
                    mRayEnd.add(mCurrentPosition)
                    mFilterDirection.set(mDelta)
                    if (collision.castRay(mRayStart, mRayEnd, mFilterDirection, mVerticalHitPoint,
                                    mVerticalHitNormal, parentObject)) {

                        // If we found a collision, use this surface as our vertical intersection
                        // for this frame, even if the sweep above also found something.
                        verticalHit = true
                        // snap
                        if (mVerticalHitNormal.y > 0.0f) {
                            mCurrentPosition.y = mVerticalHitPoint.y - bottom
                        } else if (mVerticalHitNormal.y < 0.0f) {
                            mCurrentPosition.y = mVerticalHitPoint.y - top
                        }
                    }


                    // Now the horizontal version of the same test
                    var xStart = left.toFloat()
                    var xEnd = right.toFloat()
                    if (mDelta.x < 0.0f) {
                        xStart = right.toFloat()
                        xEnd = left.toFloat()
                    }
                    mRayStart[xStart] = centerOffsetY
                    mRayStart.add(mCurrentPosition)
                    mRayEnd[xEnd] = centerOffsetY
                    mRayEnd.add(mCurrentPosition)
                    mFilterDirection.set(mDelta)
                    if (collision.castRay(mRayStart, mRayEnd, mFilterDirection, mHorizontalHitPoint,
                                    mHorizontalHitNormal, parentObject)) {

                        // If we found a collision, use this surface as our horizontal intersection
                        // for this frame, even if the sweep above also found something.
                        horizontalHit = true
                        // snap
                        if (mHorizontalHitNormal.x > 0.0f) {
                            mCurrentPosition.x = mHorizontalHitPoint.x - left
                        } else if (mHorizontalHitNormal.x < 0.0f) {
                            mCurrentPosition.x = mHorizontalHitPoint.x - right
                        }
                    }
                }


                // Record the intersection for other systems to use.
                val timeSystem = sSystemRegistry.timeSystem
                if (timeSystem != null) {
                    val time = timeSystem.gameTime
                    if (horizontalHit) {
                        if (mHorizontalHitNormal.x > 0.0f) {
                            parentObject.lastTouchedLeftWallTime = time
                        } else {
                            parentObject.lastTouchedRightWallTime = time
                        }
                        //parentObject.setBackgroundCollisionNormal(mHorizontalHitNormal);
                    }
                    if (verticalHit) {
                        if (mVerticalHitNormal.y > 0.0f) {
                            parentObject.lastTouchedFloorTime = time
                        } else {
                            parentObject.lastTouchedCeilingTime = time
                        }
                        //parentObject.setBackgroundCollisionNormal(mVerticalHitNormal);
                    }


                    // If we hit multiple surfaces, merge their normals together to produce an
                    // average direction of obstruction.
                    if (true) { //(verticalHit && horizontalHit) {
                        mMergedNormal.set(mVerticalHitNormal)
                        mMergedNormal.add(mHorizontalHitNormal)
                        mMergedNormal.normalize()
                        parentObject.backgroundCollisionNormal = mMergedNormal
                    }
                    parentObject.position = mCurrentPosition
                }
            }
        }
        mPreviousPosition.set(parentObject.position)
    }

    /* Sweeps the space between two points looking for surfaces that oppose horizontal movement. */
    private fun sweepHorizontal(previousPosition: Vector2?, currentPosition: Vector2, delta: Vector2,
                                  left: Int, right: Int, centerY: Float, hitPoint: Vector2, hitNormal: Vector2?,
                                  parentObject: GameObject): Boolean {
        var hit = false
        if (!Utils.close(delta.x, 0.0f)) {
            val collision = sSystemRegistry.collisionSystem

            // Shoot a ray from the center of the previous frame's box to the edge (left or right,
            // depending on the direction of movement) of the current box.
            mTestPointStart.y = centerY
            mTestPointStart.x = left.toFloat()
            var offset = -left
            if (delta.x > 0.0f) {
                mTestPointStart.x = right.toFloat()
                offset = -right
            }

            // Filter out surfaces that do not oppose motion in the horizontal direction, or
            // push in the same direction as movement.
            mFilterDirection.set(delta)
            mFilterDirection.y = 0f
            mTestPointEnd.set(currentPosition)
            mTestPointEnd.add(mTestPointStart)
            if (collision!!.castRay(previousPosition!!, mTestPointEnd, mFilterDirection,
                            hitPoint, hitNormal!!, parentObject)) {
                // snap
                currentPosition.x = hitPoint.x + offset
                hit = true
            }
        }
        return hit
    }

    /* Sweeps the space between two points looking for surfaces that oppose vertical movement. */
    private fun sweepVertical(previousPosition: Vector2?, currentPosition: Vector2, delta: Vector2,
                                bottom: Int, top: Int, centerX: Float, hitPoint: Vector2, hitNormal: Vector2?,
                                parentObject: GameObject): Boolean {
        var hit = false
        if (!Utils.close(delta.y, 0.0f)) {
            val collision = sSystemRegistry.collisionSystem
            // Shoot a ray from the center of the previous frame's box to the edge (top or bottom,
            // depending on the direction of movement) of the current box.
            mTestPointStart.x = centerX
            mTestPointStart.y = bottom.toFloat()
            var offset = -bottom
            if (delta.y > 0.0f) {
                mTestPointStart.y = top.toFloat()
                offset = -top
            }
            mFilterDirection.set(delta)
            mFilterDirection.x = 0f
            mTestPointEnd.set(currentPosition)
            mTestPointEnd.add(mTestPointStart)
            if (collision!!.castRay(previousPosition!!, mTestPointEnd, mFilterDirection,
                            hitPoint, hitNormal!!, parentObject)) {
                hit = true
                // snap
                currentPosition.y = hitPoint.y + offset
            }
        }
        return hit
    }

    /** Comparator for hit points.  */
    private class HitPointDistanceComparator : Comparator<HitPoint?> {
        private val mOrigin: Vector2 = Vector2()
        fun setOrigin(origin: Vector2?) {
            mOrigin.set(origin!!)
        }

        fun setOrigin(x: Float, y: Float) {
            mOrigin[x] = y
        }

        override fun compare(object1: HitPoint?, object2: HitPoint?): Int {
            var result = 0
            if (object1 != null && object2 != null) {
                val obj1Distance = object1.hitPoint!!.distance2(mOrigin)
                val obj2Distance = object2.hitPoint!!.distance2(mOrigin)
                val distanceDelta = obj1Distance - obj2Distance
                result = if (distanceDelta < 0.0f) -1 else 1
            } else if (object1 == null && object2 != null) {
                result = 1
            } else if (object2 == null && object1 != null) {
                result = -1
            }
            return result
        }

    }
}
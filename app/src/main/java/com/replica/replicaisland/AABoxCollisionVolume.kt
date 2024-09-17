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

import kotlin.math.max

/**
 * An Axis-Aligned rectangular collision volume.  This code treats other volumes as if they are
 * also rectangles when calculating intersections.  Therefore certain types of intersections, such
 * as sphere vs rectangle, may not be absolutely precise (in the case of a sphere vs a rectangle,
 * for example, a new rectangle that fits the sphere is used to perform the intersection test, so
 * there is some potential for false-positives at the corners).  However, for our purposes absolute
 * precision isn't necessary, so this simple implementation is sufficient.
 */
class AABoxCollisionVolume : CollisionVolume {
    private var widthHeight: Vector2
    private var bottomLeft: Vector2

    constructor(offsetX: Float, offsetY: Float, width: Float, height: Float) : super() {
        bottomLeft = Vector2(offsetX, offsetY)
        widthHeight = Vector2(width, height)
    }

    constructor(offsetX: Int, offsetY: Int, width: Int, height: Int) : super() {
        bottomLeft = Vector2(offsetX, offsetY)
        widthHeight = Vector2(width, height)
    }
    
    constructor(offsetX: Float, offsetY: Float, width: Float, height: Float,
                hit: Int) : super(hit) {
        bottomLeft = Vector2(offsetX, offsetY)
        widthHeight = Vector2(width, height)
    }

    constructor(offsetX: Int, offsetY: Int, width: Int, height: Int,
                hit: Int) : super(hit) {
        bottomLeft = Vector2(offsetX.toFloat(), offsetY.toFloat())
        widthHeight = Vector2(width.toFloat(), height.toFloat())
    }

    override fun fetchMaxX(): Float {
        return bottomLeft.x + widthHeight.x
    }

    override fun fetchMinX(): Float {
        return bottomLeft.x
    }

    override fun fetchMaxY(): Float {
        return bottomLeft.y + widthHeight.y
    }

    override fun fetchMinY(): Float {
        return bottomLeft.y
    }

    /**
     * Calculates the intersection of this volume and another, and returns true if the
     * volumes intersect.  This test treats the other volume as an AABox.
     * @param position The world position of this volume.
     * @param other The volume to test for intersections.
     * @param otherPosition The world position of the other volume.
     * @return true if the volumes overlap, false otherwise.
     */
    override fun intersects(position: Vector2?, flip: FlipInfo?, other: CollisionVolume?,
                            otherPosition: Vector2?, otherFlip: FlipInfo?): Boolean {
        val left = minXPosition(flip) + position!!.x
        val right = maxXPosition(flip) + position.x
        val bottom = minYPosition(flip) + position.y
        val top = maxYPosition(flip) + position.y
        val otherLeft = other!!.minXPosition(otherFlip) + otherPosition!!.x
        val otherRight = other.maxXPosition(otherFlip) + otherPosition.x
        val otherBottom = other.minYPosition(otherFlip) + otherPosition.y
        val otherTop = other.maxYPosition(otherFlip) + otherPosition.y
        return (boxIntersect(left, right, top, bottom,
                otherLeft, otherRight, otherTop, otherBottom)
                || boxIntersect(otherLeft, otherRight, otherTop, otherBottom,
                left, right, top, bottom))
    }

    /** Tests two axis-aligned boxes for overlap.  */
    private fun boxIntersect(left1: Float, right1: Float, top1: Float, bottom1: Float,
                             left2: Float, right2: Float, top2: Float, bottom2: Float): Boolean {
        val horizontalIntersection = left1 < right2 && left2 < right1
        val verticalIntersection = top1 > bottom2 && top2 > bottom1
        return horizontalIntersection && verticalIntersection
    }

    /** Increases the size of this volume as necessary to fit the passed volume.  */
    fun growBy(other: CollisionVolume) {
        val maxX: Float
        val minX: Float
        val maxY: Float
        val minY: Float
        if (widthHeight.length2() > 0) {
            maxX = max(fetchMaxX(), other.fetchMaxX())
            minX = max(fetchMinX(), other.fetchMinX())
            maxY = max(fetchMaxY(), other.fetchMaxY())
            minY = max(fetchMinY(), other.fetchMinY())
        } else {
            maxX = other.fetchMaxX()
            minX = other.fetchMinX()
            maxY = other.fetchMaxY()
            minY = other.fetchMinY()
        }
        val horizontalDelta = maxX - minX
        val verticalDelta = maxY - minY
        bottomLeft[minX] = minY
        widthHeight[horizontalDelta] = verticalDelta
    }
}
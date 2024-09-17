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

/**
 * A component that allows a game object to act like a solid object by submitting surfaces to the
 * background collision system every frame.
 */
class SolidSurfaceComponent : GameComponent {
    private var mStartPoints: FixedSizeArray<Vector2>? = null
    private var mEndPoints: FixedSizeArray<Vector2>? = null
    private var mNormals: FixedSizeArray<Vector2>? = null
    private var mStart: Vector2
    private var mEnd: Vector2
    private var mNormal: Vector2

    constructor(maxSurfaceCount: Int) : super() {
        inititalize(maxSurfaceCount)
        mStart = Vector2()
        mEnd = Vector2()
        mNormal = Vector2()
        setPhaseToThis(ComponentPhases.POST_COLLISION.ordinal)
        reset()
    }

    override fun reset() {
        mStartPoints!!.clear()
        mEndPoints!!.clear()
        mNormals!!.clear()
    }

    constructor() : super() {
        mStart = Vector2()
        mEnd = Vector2()
        mNormal = Vector2()
        setPhaseToThis(ComponentPhases.POST_COLLISION.ordinal)
    }

    fun inititalize(maxSurfaceCount: Int) {
        if (mStartPoints == null
                || mStartPoints != null && mStartPoints!!.count != maxSurfaceCount) {
            mStartPoints = FixedSizeArray(maxSurfaceCount)
            mEndPoints = FixedSizeArray(maxSurfaceCount)
            mNormals = FixedSizeArray(maxSurfaceCount)
        }
        mStartPoints!!.clear()
        mEndPoints!!.clear()
        mNormals!!.clear()
    }

    // Note that this function keeps direct references to the arguments it is passed.
    fun addSurface(startPoint: Vector2, endPoint: Vector2, normal: Vector2) {
        mStartPoints!!.add(startPoint)
        mEndPoints!!.add(endPoint)
        mNormals!!.add(normal)
    }

    override fun update(timeDelta: Float, parent: BaseObject?) {
        val collision = sSystemRegistry.collisionSystem
        val startPoints = mStartPoints
        val endPoints = mEndPoints
        val normals = mNormals
        val surfaceCount = startPoints!!.count
        if (collision != null && surfaceCount > 0) {
            val parentObject = parent as GameObject
            val position = parentObject.position
            val start = mStart
            val end = mEnd
            val normal = mNormal
            for (x in 0 until surfaceCount) {
                start.set(startPoints[x]!!)
                if (parentObject.facingDirection.x < 0.0f) {
                    start.flipHorizontal(parentObject.width)
                }
                if (parentObject.facingDirection.y < 0.0f) {
                    start.flipVertical(parentObject.height)
                }
                start.add(position)
                end.set(endPoints!![x]!!)
                if (parentObject.facingDirection.x < 0.0f) {
                    end.flipHorizontal(parentObject.width)
                }
                if (parentObject.facingDirection.y < 0.0f) {
                    end.flipVertical(parentObject.height)
                }
                end.add(position)
                normal.set(normals!![x]!!)
                if (parentObject.facingDirection.x < 0.0f) {
                    normal.flipHorizontal(0f)
                }
                if (parentObject.facingDirection.y < 0.0f) {
                    normal.flipVertical(0f)
                }
                collision.addTemporarySurface(start, end, normal, parentObject)
            }
        }
    }
}
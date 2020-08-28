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

import com.replica.replicaisland.Lerp.ease
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sin


/**
 * Manages the position of the camera based on a target game object.
 */
class CameraSystem : BaseObject() {
    private var mTarget: GameObject? = null
    private var mShakeTime = 0f
    private var mShakeMagnitude = 0f
    private var mShakeOffsetY = 0f
    private val mCurrentCameraPosition = Vector2()
    private val mFocalPosition = Vector2()
    private val mPreInterpolateCameraPosition = Vector2()
    private val mTargetPosition = Vector2()
    private val mBias = Vector2()
    private var mTargetChangedTime = 0f
    override fun reset() {
        mTarget = null
        mCurrentCameraPosition.zero()
        mShakeTime = 0.0f
        mShakeMagnitude = 0.0f
        mFocalPosition.zero()
        mTargetChangedTime = 0.0f
        mPreInterpolateCameraPosition.zero()
        mTargetPosition.zero()
    }

    fun fetchFocusPositionX() : Float {
        return mFocalPosition.x
    }

    fun fetchFocusPositionY() : Float {
        return mFocalPosition.y
    }

    var target: GameObject?
        get() = mTarget
        set(target) {
            if (target != null && mTarget !== target) {
                mPreInterpolateCameraPosition.set(mCurrentCameraPosition)
                mPreInterpolateCameraPosition.subtract(target.position)
                if (mPreInterpolateCameraPosition.length2() <
                        MAX_INTERPOLATE_TO_TARGET_DISTANCE * MAX_INTERPOLATE_TO_TARGET_DISTANCE) {
                    val time = sSystemRegistry.timeSystem
                    mTargetChangedTime = time!!.gameTime
                    mPreInterpolateCameraPosition.set(mCurrentCameraPosition)
                } else {
                    mTargetChangedTime = 0.0f
                    mCurrentCameraPosition.set(target.position)
                }
            }
            mTarget = target
        }

    fun shake(duration: Float, magnitude: Float) {
        mShakeTime = duration
        mShakeMagnitude = magnitude
    }

    fun shaking(): Boolean {
        return mShakeTime > 0.0f
    }

    override fun update(timeDelta: Float, parent: BaseObject?) {
        mShakeOffsetY = 0.0f
        if (mShakeTime > 0.0f) {
            mShakeTime -= timeDelta
            mShakeOffsetY = (sin(mShakeTime * SHAKE_FREQUENCY.toDouble()) * mShakeMagnitude).toFloat()
        }
        if (mTarget != null) {
            mTargetPosition[mTarget!!.centeredPositionX] = mTarget!!.centeredPositionY
            val targetPosition = mTargetPosition
            if (mTargetChangedTime > 0.0f) {
                val time = sSystemRegistry.timeSystem
                val delta = time!!.gameTime - mTargetChangedTime
                mCurrentCameraPosition.x = ease(mPreInterpolateCameraPosition.x,
                        targetPosition.x, INTERPOLATE_TO_TARGET_TIME, delta)
                mCurrentCameraPosition.y = ease(mPreInterpolateCameraPosition.y,
                        targetPosition.y, INTERPOLATE_TO_TARGET_TIME, delta)
                if (delta > INTERPOLATE_TO_TARGET_TIME) {
                    mTargetChangedTime = -1f
                }
            } else {

                // Only respect the bias if the target is moving.  No camera motion without
                // player input!
                if (mBias.length2() > 0.0f && mTarget!!.velocity.length2() > 1.0f) {
                    mBias.normalize()
                    mBias.multiply(BIAS_SPEED * timeDelta)
                    mCurrentCameraPosition.add(mBias)
                }
                val xDelta: Float = targetPosition.x - mCurrentCameraPosition.x
                if (abs(xDelta) > X_FOLLOW_DISTANCE) {
                    mCurrentCameraPosition.x = targetPosition.x - X_FOLLOW_DISTANCE * Utils.sign(xDelta)
                }
                val yDelta: Float = targetPosition.y - mCurrentCameraPosition.y
                if (yDelta > Y_UP_FOLLOW_DISTANCE) {
                    mCurrentCameraPosition.y = targetPosition.y - Y_UP_FOLLOW_DISTANCE
                } else if (yDelta < -Y_DOWN_FOLLOW_DISTANCE) {
                    mCurrentCameraPosition.y = targetPosition.y + Y_DOWN_FOLLOW_DISTANCE
                }
            }
            mBias.zero()
        }
        mFocalPosition.x = floor(mCurrentCameraPosition.x)
        mFocalPosition.x = snapFocalPointToWorldBoundsX(mFocalPosition.x)
        mFocalPosition.y = floor(mCurrentCameraPosition.y + mShakeOffsetY.toDouble()).toFloat()
        mFocalPosition.y = snapFocalPointToWorldBoundsY(mFocalPosition.y)
    }

    fun pointVisible(point: Vector2, radius: Float): Boolean {
        var visible = false
        val width = sSystemRegistry.contextParameters!!.gameWidth / 2.0f
        val height = sSystemRegistry.contextParameters!!.gameHeight / 2.0f
        if (abs(mFocalPosition.x - point.x) < width + radius) {
            if (abs(mFocalPosition.y - point.y) < height + radius) {
                visible = true
            }
        }
        return visible
    }

    /** Snaps a coordinate against the bounds of the world so that it may not pass out
     * of the visible area of the world.
     * @param worldX An x-coordinate in world units.
     * @return An x-coordinate that is guaranteed not to expose the edges of the world.
     */
    private fun snapFocalPointToWorldBoundsX(worldX: Float): Float {
        var focalPositionX = worldX
        val width = sSystemRegistry.contextParameters!!.gameWidth.toFloat()
        val level = sSystemRegistry.levelSystem
        if (level != null) {
            val worldPixelWidth = max(level.levelWidth, width)
            val rightEdge = focalPositionX + width / 2.0f
            val leftEdge = focalPositionX - width / 2.0f
            if (rightEdge > worldPixelWidth) {
                focalPositionX = worldPixelWidth - width / 2.0f
            } else if (leftEdge < 0) {
                focalPositionX = width / 2.0f
            }
        }
        return focalPositionX
    }

    /** Snaps a coordinate against the bounds of the world so that it may not pass out
     * of the visible area of the world.
     * @param worldY A y-coordinate in world units.
     * @return A y-coordinate that is guaranteed not to expose the edges of the world.
     */
    private fun snapFocalPointToWorldBoundsY(worldY: Float): Float {
        var focalPositionY = worldY
        val height = sSystemRegistry.contextParameters!!.gameHeight.toFloat()
        val level = sSystemRegistry.levelSystem
        if (level != null) {
            val worldPixelHeight: Float = max(level.levelHeight.toInt(), sSystemRegistry.contextParameters!!.gameHeight).toFloat()
            val topEdge = focalPositionY + height / 2.0f
            val bottomEdge = focalPositionY - height / 2.0f
            if (topEdge > worldPixelHeight) {
                focalPositionY = worldPixelHeight - height / 2.0f
            } else if (bottomEdge < 0) {
                focalPositionY = height / 2.0f
            }
        }
        return focalPositionY
    }

    fun addCameraBias(bias: Vector2) {
        val x: Float = bias.x - mFocalPosition.x
        val y: Float = bias.y - mFocalPosition.y
        val biasX: Float = mBias.x
        val biasY: Float = mBias.y
        mBias[x] = y
        mBias.normalize()
        mBias.add(biasX, biasY)
    }

    companion object {
        private const val X_FOLLOW_DISTANCE = 0.0f
        private const val Y_UP_FOLLOW_DISTANCE = 90.0f
        private const val Y_DOWN_FOLLOW_DISTANCE = 0.0f
        private const val MAX_INTERPOLATE_TO_TARGET_DISTANCE = 300.0f
        private const val INTERPOLATE_TO_TARGET_TIME = 1.0f
        private const val SHAKE_FREQUENCY = 40
        private const val BIAS_SPEED = 400.0f
    }

}
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

import com.replica.replicaisland.GameObject.ActionType

/**
 * A component that implements the "pop-out" AI behavior.  Pop-out characters alternate between
 * hiding and appearing based on their distance from the player.  They do not move or normally
 * attack.
 */
class SleeperComponent : GameComponent() {
    private var mWakeUpDuration = 0f
    private var mStateTime = 0f
    private var mState = 0
    private var mSlamDuration = 0f
    private var mSlamMagnitude = 0f
    private var mAttackImpulseX = 0f
    private var mAttackImpulseY = 0f
    override fun reset() {
        mWakeUpDuration = DEFAULT_WAKE_UP_DURATION
        mState = STATE_SLEEPING
        mStateTime = 0.0f
        mSlamDuration = 0.0f
        mSlamMagnitude = 0.0f
        mAttackImpulseX = 0.0f
        mAttackImpulseY = 0.0f
    }

    override fun update(timeDelta: Float, parent: BaseObject?) {
        val parentObject = parent as GameObject
        if (parentObject.currentAction == ActionType.INVALID) {
            parentObject.currentAction = ActionType.IDLE
            mState = STATE_SLEEPING
        }
        val camera = sSystemRegistry.cameraSystem
        when (mState) {
            STATE_SLEEPING -> if (camera!!.shaking() && camera.pointVisible(parentObject.position, parentObject.width / 2.0f)) {
                mState = STATE_WAKING
                mStateTime = mWakeUpDuration
                parentObject.currentAction = ActionType.MOVE
            }
            STATE_WAKING -> {
                mStateTime -= timeDelta
                if (mStateTime <= 0.0f) {
                    mState = STATE_ATTACKING
                    parentObject.currentAction = ActionType.ATTACK
                    parentObject.impulse.x += mAttackImpulseX * parentObject.facingDirection.x
                    parentObject.impulse.y += mAttackImpulseY
                }
            }
            STATE_ATTACKING -> if (parentObject.touchingGround() && parentObject.velocity.y < 0.0f) {
                mState = STATE_SLAM
                camera!!.shake(mSlamDuration, mSlamMagnitude)
                parentObject.velocity.zero()
            }
            STATE_SLAM -> if (!camera!!.shaking()) {
                mState = STATE_SLEEPING
                parentObject.currentAction = ActionType.IDLE
            }
        }
    }

    fun setWakeUpDuration(duration: Float) {
        mWakeUpDuration = duration
    }

    fun setSlam(duration: Float, magnitude: Float) {
        mSlamDuration = duration
        mSlamMagnitude = magnitude
    }

    fun setAttackImpulse(x: Float, y: Float) {
        mAttackImpulseX = x
        mAttackImpulseY = y
    }

    companion object {
        private const val STATE_SLEEPING = 0
        private const val STATE_WAKING = 1
        private const val STATE_ATTACKING = 2
        private const val STATE_SLAM = 3
        private const val DEFAULT_WAKE_UP_DURATION = 3.0f
    }

    init {
        setPhaseToThis(ComponentPhases.THINK.ordinal)
        reset()
    }
}
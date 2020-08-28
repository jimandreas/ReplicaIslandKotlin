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

/**
 * A component that implements the "pop-out" AI behavior.  Pop-out characters alternate between
 * hiding and appearing based on their distance from the player.  They do not move or normally
 * attack.
 */
class PopOutComponent : GameComponent() {
    private var mAppearDistance = 0f
    private var mHideDistance = 0f
    private var mAttackDistance = 0f
    private var mAttackDelay = 0f
    private var mAttackLength = 0f
    private var mAttackStartTime = 0f
    private val mDistance: Vector2
    private var mState = 0
    private var mLastAttackCompletedTime = 0f
    override fun reset() {
        mAttackDelay = 0f
        mAttackLength = 0f
        mAttackDistance = DEFAULT_ATTACK_DISTANCE.toFloat()
        mAppearDistance = DEFAULT_APPEAR_DISTANCE.toFloat()
        mHideDistance = DEFAULT_HIDE_DISTANCE.toFloat()
        mState = STATE_HIDDEN
        mLastAttackCompletedTime = 0.0f
    }

    override fun update(timeDelta: Float, parent: BaseObject?) {
        val parentObject = parent as GameObject
        val manager = sSystemRegistry.gameObjectManager
        if (manager != null) {
            val player = manager.player
            if (player != null) {
                mDistance.set(player.position)
                mDistance.subtract(parentObject!!.position)
                val time = sSystemRegistry.timeSystem
                val currentTime = time!!.gameTime
                when (mState) {
                    STATE_HIDDEN -> {
                        parentObject.currentAction = GameObject.ActionType.HIDE
                        if (mDistance.length2() < mAppearDistance * mAppearDistance) {
                            mState = STATE_VISIBLE
                            mLastAttackCompletedTime = currentTime
                        }
                    }
                    STATE_VISIBLE -> {
                        parentObject.currentAction = GameObject.ActionType.IDLE
                        if (mDistance.length2() > mHideDistance * mHideDistance) {
                            mState = STATE_HIDDEN
                        } else if (mDistance.length2() < mAttackDistance * mAttackDistance
                                && currentTime > mLastAttackCompletedTime + mAttackDelay) {
                            mAttackStartTime = currentTime
                            mState = STATE_ATTACKING
                        }
                    }
                    STATE_ATTACKING -> {
                        parentObject.currentAction = GameObject.ActionType.ATTACK
                        if (currentTime > mAttackStartTime + mAttackLength) {
                            mState = STATE_VISIBLE
                            mLastAttackCompletedTime = currentTime
                        }
                    }
                    // TODO: handle assert: else -> assert(false)
                }
            }
        }
    }

    fun setupAttack(distance: Float, delay: Float, duration: Float) {
        mAttackDistance = distance
        mAttackDelay = delay
        mAttackLength = duration
    }

    fun setAppearDistance(appearDistance: Float) {
        mAppearDistance = appearDistance
    }

    fun setHideDistance(hideDistance: Float) {
        mHideDistance = hideDistance
    }

    companion object {
        private const val DEFAULT_APPEAR_DISTANCE = 120
        private const val DEFAULT_HIDE_DISTANCE = 190
        private const val DEFAULT_ATTACK_DISTANCE = 0 // No attacking by default.
        private const val STATE_HIDDEN = 0
        private const val STATE_VISIBLE = 1
        private const val STATE_ATTACKING = 2
    }

    init {
        setPhaseToThis(ComponentPhases.THINK.ordinal)
        mDistance = Vector2()
        reset()
    }
}
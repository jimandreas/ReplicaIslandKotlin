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
    private var attackDistance = 0f
    private var attackDelay = 0f
    private var attackLength = 0f
    private var attackStartTime = 0f
    private val mDistance: Vector2
    private var state = 0
    private var lastAttackCompletedTime = 0f
    override fun reset() {
        attackDelay = 0f
        attackLength = 0f
        attackDistance = DEFAULT_ATTACK_DISTANCE.toFloat()
        mAppearDistance = DEFAULT_APPEAR_DISTANCE.toFloat()
        mHideDistance = DEFAULT_HIDE_DISTANCE.toFloat()
        state = STATE_HIDDEN
        lastAttackCompletedTime = 0.0f
    }

    override fun update(timeDelta: Float, parent: BaseObject?) {
        val parentObject = parent as GameObject
        val manager = sSystemRegistry.gameObjectManager
        if (manager != null) {
            val player = manager.player
            if (player != null) {
                mDistance.set(player.position)
                mDistance.subtract(parentObject.position)
                val time = sSystemRegistry.timeSystem
                val currentTime = time!!.gameTime
                when (state) {
                    STATE_HIDDEN -> {
                        parentObject.currentAction = GameObject.ActionType.HIDE
                        if (mDistance.length2() < mAppearDistance * mAppearDistance) {
                            state = STATE_VISIBLE
                            lastAttackCompletedTime = currentTime
                        }
                    }
                    STATE_VISIBLE -> {
                        parentObject.currentAction = GameObject.ActionType.IDLE
                        if (mDistance.length2() > mHideDistance * mHideDistance) {
                            state = STATE_HIDDEN
                        } else if (mDistance.length2() < attackDistance * attackDistance
                                && currentTime > lastAttackCompletedTime + attackDelay) {
                            attackStartTime = currentTime
                            state = STATE_ATTACKING
                        }
                    }
                    STATE_ATTACKING -> {
                        parentObject.currentAction = GameObject.ActionType.ATTACK
                        if (currentTime > attackStartTime + attackLength) {
                            state = STATE_VISIBLE
                            lastAttackCompletedTime = currentTime
                        }
                    }
                    // TODO: handle assert: else -> assert(false)
                }
            }
        }
    }

    fun setupAttack(distance: Float, delay: Float, duration: Float) {
        attackDistance = distance
        attackDelay = delay
        attackLength = duration
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
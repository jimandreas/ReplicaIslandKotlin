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

class AttackAtDistanceComponent : GameComponent() {
    private var mAttackDistance = 0f
    private var mAttackDelay = 0f
    private var mAttackLength = 0f
    private var mAttackStartTime = 0f
    private var mRequireFacing = false
    private val mDistance: Vector2
    override fun reset() {
        mAttackDelay = 0f
        mAttackLength = 0f
        mAttackDistance = DEFAULT_ATTACK_DISTANCE.toFloat()
        mRequireFacing = false
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
                val facingPlayer = (Utils.sign(player.position.x - parentObject.position.x)
                        == Utils.sign(parentObject.facingDirection.x))
                val facingDirectionCorrect = (mRequireFacing && facingPlayer
                        || !mRequireFacing)
                if (parentObject.currentAction == GameObject.ActionType.ATTACK) {
                    if (currentTime > mAttackStartTime + mAttackLength) {
                        parentObject.currentAction = GameObject.ActionType.IDLE
                    }
                } else if (mDistance.length2() < mAttackDistance * mAttackDistance
                        && currentTime > mAttackStartTime + mAttackLength + mAttackDelay
                        && facingDirectionCorrect) {
                    mAttackStartTime = currentTime
                    parentObject.currentAction = GameObject.ActionType.ATTACK
                } else {
                    parentObject.currentAction = GameObject.ActionType.IDLE
                }
            }
        }
    }

    fun setupAttack(distance: Float, delay: Float, duration: Float, requireFacing: Boolean) {
        mAttackDistance = distance
        mAttackDelay = delay
        mAttackLength = duration
        mRequireFacing = requireFacing
    }

    companion object {
        private const val DEFAULT_ATTACK_DISTANCE = 100
    }

    init {
        setPhaseToThis(ComponentPhases.THINK.ordinal)
        mDistance = Vector2()
        reset()
    }
}
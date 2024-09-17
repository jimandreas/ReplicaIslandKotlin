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
package com.replica.replicaisland

import com.replica.replicaisland.GameObject.ActionType

/**
 * A component that implements the "pop-out" AI behavior.  Pop-out characters alternate between
 * hiding and appearing based on their distance from the player.  They do not move or normally
 * attack.
 */
class SleeperComponent : GameComponent() {
    private var wakeUpDuration = 0f
    private var stateTime = 0f
    private var state = 0
    private var slamDuration = 0f
    private var slamMagnitude = 0f
    private var attackImpulseX = 0f
    private var attackImpulseY = 0f
    override fun reset() {
        wakeUpDuration = DEFAULT_WAKE_UP_DURATION
        state = STATE_SLEEPING
        stateTime = 0.0f
        slamDuration = 0.0f
        slamMagnitude = 0.0f
        attackImpulseX = 0.0f
        attackImpulseY = 0.0f
    }

    override fun update(timeDelta: Float, parent: BaseObject?) {
        val parentObject = parent as GameObject
        if (parentObject.currentAction == ActionType.INVALID) {
            parentObject.currentAction = ActionType.IDLE
            state = STATE_SLEEPING
        }
        val camera = sSystemRegistry.cameraSystem
        when (state) {
            STATE_SLEEPING -> if (camera!!.shaking() && camera.pointVisible(parentObject.position, parentObject.width / 2.0f)) {
                state = STATE_WAKING
                stateTime = wakeUpDuration
                parentObject.currentAction = ActionType.MOVE
            }
            STATE_WAKING -> {
                stateTime -= timeDelta
                if (stateTime <= 0.0f) {
                    state = STATE_ATTACKING
                    parentObject.currentAction = ActionType.ATTACK
                    parentObject.impulse.x += attackImpulseX * parentObject.facingDirection.x
                    parentObject.impulse.y += attackImpulseY
                }
            }
            STATE_ATTACKING -> if (parentObject.touchingGround() && parentObject.velocity.y < 0.0f) {
                state = STATE_SLAM
                camera!!.shake(slamDuration, slamMagnitude)
                parentObject.velocity.zero()
            }
            STATE_SLAM -> if (!camera!!.shaking()) {
                state = STATE_SLEEPING
                parentObject.currentAction = ActionType.IDLE
            }
        }
    }

    fun setWakeUpDuration(duration: Float) {
        wakeUpDuration = duration
    }

    fun setSlam(duration: Float, magnitude: Float) {
        slamDuration = duration
        slamMagnitude = magnitude
    }

    fun setAttackImpulse(x: Float, y: Float) {
        attackImpulseX = x
        attackImpulseY = y
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
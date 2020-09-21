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
@file:Suppress("UNUSED_PARAMETER")

package com.replica.replicaisland

import com.replica.replicaisland.ChannelSystem.ChannelBooleanValue
import com.replica.replicaisland.GameObject.ActionType
import kotlin.math.abs

class NPCAnimationComponent : GameComponent() {
    private var currentAnimation = 0
    private var mSprite: SpriteComponent? = null
    private var mChannel: ChannelSystem.Channel? = null
    private var channelTrigger = 0
    private var mFlying = false
    private var stopAtWalls // Controls whether or not the character will go back
            = false

    override fun reset() {
        currentAnimation = IDLE
        mChannel = null
        mSprite = null
        mFlying = false
        stopAtWalls = true
    }

    override fun update(timeDelta: Float, parent: BaseObject?) {
        if (mSprite != null) {
            val parentObject = parent as GameObject?
            val oldAnimation = currentAnimation
            when (currentAnimation) {
                IDLE -> idle(parentObject)
                WALK -> walk(parentObject)
                RUN_START -> runStart(parentObject)
                RUN -> run(parentObject)
                SHOOT -> shoot(parentObject)
                JUMP_START -> jumpStart(parentObject)
                JUMP_AIR -> jumpAir(parentObject)
                TAKE_HIT -> takeHit(parentObject)
                SURPRISED -> surprised(parentObject)
                DEATH -> death(parentObject)
                else -> {} // TODO 2 fix: assert(false)
            }
            if (mChannel != null) {
                if (mChannel!!.value != null
                        && (mChannel!!.value as ChannelBooleanValue?)!!.value) {
                    currentAnimation = channelTrigger
                }
            }
            if (oldAnimation != currentAnimation) {
                mSprite!!.playAnimation(currentAnimation)
            }
        }
    }

    private fun shouldFall(parentObject: GameObject?): Boolean {
        var result = false
        val time = sSystemRegistry.timeSystem
        val airTime = time!!.gameTime - parentObject!!.lastTouchedFloorTime
        if (!mFlying && !parentObject.touchingGround() && airTime > FALL_TIME_THRESHOLD) {
            val velocity = parentObject.velocity
            if (velocity.y < FALL_SPEED_THRESHOLD) {
                result = true
            }
        }
        return result
    }

    private fun shouldJump(parentObject: GameObject?): Boolean {
        var result = false
        if (!mFlying) {
            val velocity = parentObject!!.velocity
            if (velocity.y > JUMP_SPEED_THRESHOLD) {
                result = true
            }
        }
        return result
    }

    private fun shouldRun(parentObject: GameObject?): Boolean {
        var result = false
        if (!mFlying && parentObject!!.touchingGround()) {
            val velocity = parentObject.velocity
            if (abs(velocity.x) >= RUN_SPEED_THRESHOLD) {
                result = true
            }
        }
        return result
    }

    private fun shouldMove(parentObject: GameObject?): Boolean {
        var result = true
        val velocity = parentObject!!.velocity
        if (stopAtWalls) {
            if (velocity.x < 0.0f && parentObject.touchingLeftWall()
                    || velocity.x > 0.0f && parentObject.touchingRightWall()) {
                result = false
            }
        }
        return result
    }

    private fun shouldTakeHit(parentObject: GameObject?): Boolean {
        var result = false
        if (parentObject!!.currentAction === ActionType.HIT_REACT
                && mSprite!!.findAnimation(TAKE_HIT) != null) {
            result = true
        }
        return result
    }

    private fun gotoRunStart() {
        currentAnimation = if (mSprite!!.findAnimation(RUN_START) != null) {
            RUN_START
        } else {
            RUN
        }
    }

    private fun gotoRun() {
        currentAnimation = RUN
    }

    private fun idle(parentObject: GameObject?) {
        val currentAction = parentObject!!.currentAction
        if (currentAction === ActionType.MOVE) {
            val velocity = parentObject.velocity
            if (shouldFall(parentObject)) {
                currentAnimation = JUMP_AIR
            } else if (shouldJump(parentObject)) {
                currentAnimation = JUMP_START
                parentObject.positionLocked = true
            } else if (abs(velocity.x) > 0.0f && shouldMove(parentObject)) {
                if (shouldRun(parentObject)) {
                    gotoRunStart()
                    parentObject.positionLocked = true
                } else {
                    currentAnimation = WALK
                }
            }
        } else if (currentAction === ActionType.ATTACK) {
            currentAnimation = SHOOT
        } else if (shouldTakeHit(parentObject)) {
            currentAnimation = TAKE_HIT
        } else if (parentObject.currentAction === ActionType.DEATH) {
            currentAnimation = DEATH
        }
    }

    private fun walk(parentObject: GameObject?) {
        val currentAction = parentObject!!.currentAction
        if (currentAction === ActionType.MOVE) {
            val velocity = parentObject.velocity
            if (shouldFall(parentObject)) {
                currentAnimation = JUMP_AIR
            } else if (shouldJump(parentObject)) {
                currentAnimation = JUMP_START
                parentObject.positionLocked = true
            } else if (abs(velocity.x) > 0.0f) {
                if (shouldRun(parentObject)) {
                    gotoRun()
                }
                if (velocity.x > 0.0f) {
                    parentObject.facingDirection.x = 1f
                } else {
                    parentObject.facingDirection.x = (-1).toFloat()
                }
            } else {
                currentAnimation = IDLE
            }
        } else if (currentAction === ActionType.ATTACK) {
            currentAnimation = SHOOT
        } else if (shouldTakeHit(parentObject)) {
            currentAnimation = TAKE_HIT
        } else if (parentObject.currentAction === ActionType.DEATH) {
            currentAnimation = DEATH
        }
    }

    private fun runStart(parentObject: GameObject?) {
        parentObject!!.positionLocked = true
        if (mSprite!!.animationFinished()) {
            currentAnimation = RUN
            parentObject.positionLocked = false
        }
    }

    private fun run(parentObject: GameObject?) {
        val currentAction = parentObject!!.currentAction
        if (currentAction === ActionType.MOVE) {
            val velocity = parentObject.velocity
            if (shouldFall(parentObject)) {
                currentAnimation = JUMP_AIR
            } else if (shouldJump(parentObject)) {
                parentObject.positionLocked = true
                currentAnimation = JUMP_START
            } else if (abs(velocity.x) > 0.0f) {
                if (!shouldRun(parentObject)) {
                    currentAnimation = WALK
                }
                if (velocity.x > 0.0f) {
                    parentObject.facingDirection.x = 1f
                } else {
                    parentObject.facingDirection.x = (-1).toFloat()
                }
            } else {
                currentAnimation = IDLE
            }
        } else if (currentAction === ActionType.ATTACK) {
            currentAnimation = SHOOT
        } else if (shouldTakeHit(parentObject)) {
            currentAnimation = TAKE_HIT
        } else if (parentObject.currentAction === ActionType.DEATH) {
            currentAnimation = DEATH
        }
    }

    private fun shoot(parentObject: GameObject?) {
        if (mSprite!!.animationFinished() || parentObject!!.currentAction !== ActionType.ATTACK) {
            currentAnimation = IDLE
        } else if (shouldTakeHit(parentObject)) {
            currentAnimation = TAKE_HIT
        } else if (parentObject!!.currentAction === ActionType.DEATH) {
            currentAnimation = DEATH
        } else {
            val velocity = parentObject!!.velocity
            if (velocity.x > 0.0f) {
                parentObject.facingDirection.x = 1f
            } else if (velocity.x < 0.0f) {
                parentObject.facingDirection.x = (-1).toFloat()
            }
        }
    }

    private fun jumpStart(parentObject: GameObject?) {
        val velocity = parentObject!!.velocity
        if (velocity.x > 0.0f) {
            parentObject.facingDirection.x = 1f
        } else if (velocity.x < 0.0f) {
            parentObject.facingDirection.x = (-1).toFloat()
        }
        parentObject.positionLocked = true
        if (mSprite!!.animationFinished()) {
            currentAnimation = JUMP_AIR
            parentObject.positionLocked = false
        }
    }

    private fun jumpAir(parentObject: GameObject?) {
        val currentAction = parentObject!!.currentAction
        if (currentAction === ActionType.MOVE) {
            val velocity = parentObject.velocity
            if (parentObject.touchingGround()) {
                currentAnimation = if (abs(velocity.x) > 0.0f) {
                    if (shouldRun(parentObject)) {
                        RUN
                    } else {
                        WALK
                    }
                } else {
                    IDLE
                }
            } else {
                if (velocity.x > 0.0f) {
                    parentObject.facingDirection.x = 1f
                } else if (velocity.x < 0.0f) {
                    parentObject.facingDirection.x = (-1).toFloat()
                }
            }
        } else {
            currentAnimation = IDLE
        }
    }

    private fun takeHit(parentObject: GameObject?) {
        if (mSprite!!.animationFinished()) {
            if (parentObject!!.life > 0 && parentObject.currentAction !== ActionType.DEATH) {
                if (parentObject.currentAction !== ActionType.HIT_REACT) {
                    currentAnimation = IDLE
                }
            } else {
                currentAnimation = DEATH
            }
        }
    }

    private fun surprised(parentObject: GameObject?) {
        if (mSprite!!.animationFinished()) {
            currentAnimation = IDLE
        }
    }

    private fun death(parentObject: GameObject?) {}
    fun setSprite(sprite: SpriteComponent?) {
        mSprite = sprite
    }

    fun setChannel(channel: ChannelSystem.Channel?) {
        mChannel = channel
    }

    fun setChannelTrigger(animation: Int) {
        channelTrigger = animation
    }

    fun setFlying(flying: Boolean) {
        mFlying = flying
    }

    fun setStopAtWalls(stop: Boolean) {
        stopAtWalls = stop
    }

    companion object {
        // Animations
        const val IDLE = 0
        const val WALK = 1
        const val RUN_START = 2
        const val RUN = 3
        const val SHOOT = 4
        const val JUMP_START = 5
        const val JUMP_AIR = 6
        const val TAKE_HIT = 7
        const val SURPRISED = 8
        const val DEATH = 9
        private const val RUN_SPEED_THRESHOLD = 100.0f
        private const val JUMP_SPEED_THRESHOLD = 25.0f
        private const val FALL_SPEED_THRESHOLD = -25.0f
        private const val FALL_TIME_THRESHOLD = 0.2f
    }

    // to idle when running into a wall
    init {
        reset()
        setPhaseToThis(ComponentPhases.ANIMATION.ordinal)
    }
}
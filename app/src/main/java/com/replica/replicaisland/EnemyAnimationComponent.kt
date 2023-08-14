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
@file:Suppress("CascadeIf")

package com.replica.replicaisland

import com.replica.replicaisland.GameObject.ActionType
import kotlin.math.abs

/**
 * A general-purpose animation selection system for animating enemy characters.  Most enemy
 * characters behave similarly, so this code tries to decide which animation bets fits their current
 * state.  Other code (such as enemy AI) may move these characters around and change the current
 * ActionType, which will result in this code figuring out which sequence of animations is best to
 * play.
 */
class EnemyAnimationComponent : GameComponent() {
    enum class EnemyAnimations {
        IDLE, MOVE, ATTACK, HIDDEN, APPEAR
    }

    private enum class AnimationState {
        IDLING, MOVING, HIDING, APPEARING, ATTACKING
    }

    private var mSprite: SpriteComponent? = null
    private var mState: AnimationState? = null
    private var mFacePlayer = false
    override fun reset() {
        mState = AnimationState.IDLING
        mFacePlayer = false
        mSprite = null
    }

    override fun update(timeDelta: Float, parent: BaseObject?) {
        if (mSprite != null) {
            val parentObject = parent as GameObject
            val velocityX = parentObject.velocity.x
            val currentAction = parentObject.currentAction
            when (mState) {
                AnimationState.IDLING -> {
                    mSprite!!.playAnimation(EnemyAnimations.IDLE.ordinal)
                    if (mFacePlayer) {
                        facePlayer(parentObject)
                    }
                    if (currentAction == ActionType.ATTACK) {
                        mState = AnimationState.ATTACKING
                    } else if (currentAction == ActionType.HIDE) {
                        mState = AnimationState.HIDING
                    } else if (abs(velocityX) > 0.0f) {
                        mState = AnimationState.MOVING
                    }
                }
                AnimationState.MOVING -> {
                    mSprite!!.playAnimation(EnemyAnimations.MOVE.ordinal)
                    val targetVelocityX = parentObject.targetVelocity.x
                    if (!Utils.close(velocityX, 0.0f)) {
                        if (velocityX < 0.0f && targetVelocityX < 0.0f) {
                            parentObject.facingDirection.x = -1.0f
                        } else if (velocityX > 0.0f && targetVelocityX > 0.0f) {
                            parentObject.facingDirection.x = 1.0f
                        }
                    }
                    if (currentAction == ActionType.ATTACK) {
                        mState = AnimationState.ATTACKING
                    } else if (currentAction == ActionType.HIDE) {
                        mState = AnimationState.HIDING
                    } else if (abs(velocityX) == 0.0f) {
                        mState = AnimationState.IDLING
                    }
                }
                AnimationState.ATTACKING -> {
                    mSprite!!.playAnimation(EnemyAnimations.ATTACK.ordinal)
                    if (currentAction != ActionType.ATTACK
                            && mSprite!!.animationFinished()) {
                        mState = AnimationState.IDLING
                    }
                }
                AnimationState.HIDING -> {
                    mSprite!!.playAnimation(EnemyAnimations.HIDDEN.ordinal)
                    if (currentAction != ActionType.HIDE) {
                        mState = AnimationState.APPEARING
                    }
                }
                AnimationState.APPEARING -> {
                    if (mFacePlayer) {
                        facePlayer(parentObject)
                    }
                    mSprite!!.playAnimation(EnemyAnimations.APPEAR.ordinal)
                    if (mSprite!!.animationFinished()) {
                        mState = AnimationState.IDLING
                    }
                }

                else -> {}
            }
        }
    }

    private fun facePlayer(parentObject: GameObject) {
        val manager = sSystemRegistry.gameObjectManager
        if (manager != null) {
            val player = manager.player
            if (player != null) {
                if (player.position.x < parentObject.position.x) {
                    parentObject.facingDirection.x = -1.0f
                } else {
                    parentObject.facingDirection.x = 1.0f
                }
            }
        }
    }

    fun setSprite(sprite: SpriteComponent?) {
        mSprite = sprite
    }

    fun setFacePlayer(facePlayer: Boolean) {
        mFacePlayer = facePlayer
    }

    init {
        setPhaseToThis(ComponentPhases.ANIMATION.ordinal)
        reset()
    }
}
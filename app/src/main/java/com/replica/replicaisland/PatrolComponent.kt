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
import com.replica.replicaisland.HotSpotSystem.HotSpotType
import kotlin.math.abs

/**
 * This component implements the "patrolling" behavior for AI characters.  Patrolling characters
 * will walk forward on the map until they hit a direction hot spot or a wall, in which case they
 * may change direction.  Patrollers can also be configured via this component to attack the player
 * if appropriate conditions are met.
 */
class PatrolComponent : GameComponent() {
    private var maxSpeed = 0f
    private var mAcceleration = 0f
    private var mAttack = false
    private var attackAtDistance = 0f
    private var attackStopsMovement = false
    private var attackDuration = 0f
    private var attackDelay = 0f
    private var turnToFacePlayer = false
    private var mFlying = false
    private var lastAttackTime = 0f
    private var workingVector: Vector2 = Vector2()
    private var workingVector2: Vector2 = Vector2()
    override fun reset() {
        turnToFacePlayer = false
        maxSpeed = 0.0f
        mAcceleration = 0.0f
        mAttack = false
        attackAtDistance = 0.0f
        attackStopsMovement = false
        attackDuration = 0.0f
        attackDelay = 0.0f
        workingVector.zero()
        workingVector2.zero()
        mFlying = false
    }

    override fun update(timeDelta: Float, parent: BaseObject?) {
        val parentObject = parent as GameObject
        if (parentObject.currentAction == ActionType.INVALID
                || parentObject.currentAction == ActionType.HIT_REACT) {
            parentObject.currentAction = ActionType.MOVE
        }
        if ((mFlying || parentObject.touchingGround()) && parentObject.life > 0) {
            val manager = sSystemRegistry.gameObjectManager
            var player: GameObject? = null
            if (manager != null) {
                player = manager.player
            }
            if (mAttack) {
                updateAttack(player, parentObject)
            }
            if (parentObject.currentAction == ActionType.MOVE
                    && maxSpeed > 0.0f) {
                var hotSpot = HotSpotType.NONE
                val hotSpotSystem = sSystemRegistry.hotSpotSystem
                if (hotSpotSystem != null) {
                    // TODO: ack, magic number
                    hotSpot = hotSpotSystem.getHotSpot(parentObject.centeredPositionX,
                            parentObject.position.y + 10.0f)
                }
                val targetVelocityX = parentObject.targetVelocity.x
                val targetVelocityY = parentObject.targetVelocity.y
                var goLeft = (parentObject.touchingRightWall()
                        || hotSpot == HotSpotType.GO_LEFT) && targetVelocityX >= 0.0f
                var goRight = (parentObject.touchingLeftWall()
                        || hotSpot == HotSpotType.GO_RIGHT) && targetVelocityX <= 0.0f
                var pause = maxSpeed == 0.0f || hotSpot == HotSpotType.GO_DOWN
                if (turnToFacePlayer && player != null && player.life > 0) {
                    val horizontalDelta = (player.centeredPositionX
                            - parentObject.centeredPositionX)
                    val targetFacingDirection = Utils.sign(horizontalDelta)
                    val closestDistance = player.width / 2.0f
                    if (targetFacingDirection < 0.0f) { // we want to turn to the left
                        if (goRight) {
                            goRight = false
                            pause = true
                        } else if (targetFacingDirection
                                != Utils.sign(parentObject.facingDirection.x)) {
                            goLeft = true
                        }
                    } else if (targetFacingDirection > 0.0f) { // we want to turn to the right
                        if (goLeft) {
                            goLeft = false
                            pause = true
                        } else if (targetFacingDirection
                                != Utils.sign(parentObject.facingDirection.x)) {
                            goRight = true
                        }
                    }
                    if (abs(horizontalDelta) < closestDistance) {
                        goRight = false
                        goLeft = false
                        pause = true
                    }
                }
                if (!mFlying) {
                    if (!pause && !goLeft && !goRight && targetVelocityX == 0.0f) {
                        if (parentObject.facingDirection.x < 0.0f) {
                            goLeft = true
                        } else {
                            goRight = true
                        }
                    }
                    if (goRight) {
                        parentObject.targetVelocity.x = maxSpeed
                        parentObject.acceleration.x = mAcceleration
                    } else if (goLeft) {
                        parentObject.targetVelocity.x = -maxSpeed
                        parentObject.acceleration.x = mAcceleration
                    } else if (pause) {
                        parentObject.targetVelocity.x = 0f
                        parentObject.acceleration.x = mAcceleration
                    }
                } else {
                    val goUp = (parentObject.touchingGround() && targetVelocityY < 0.0f
                            || hotSpot == HotSpotType.GO_UP)
                    val goDown = (parentObject.touchingCeiling() && targetVelocityY > 0.0f
                            || hotSpot == HotSpotType.GO_DOWN)
                    if (goUp) {
                        parentObject.targetVelocity.x = 0.0f
                        parentObject.targetVelocity.y = maxSpeed
                        parentObject.acceleration.y = mAcceleration
                        parentObject.acceleration.x = mAcceleration
                    } else if (goDown) {
                        parentObject.targetVelocity.x = 0.0f
                        parentObject.targetVelocity.y = -maxSpeed
                        parentObject.acceleration.y = mAcceleration
                        parentObject.acceleration.x = mAcceleration
                    } else if (goRight) {
                        parentObject.targetVelocity.x = maxSpeed
                        parentObject.acceleration.x = mAcceleration
                        parentObject.acceleration.y = mAcceleration
                        parentObject.targetVelocity.y = 0.0f
                    } else if (goLeft) {
                        parentObject.targetVelocity.x = -maxSpeed
                        parentObject.acceleration.x = mAcceleration
                        parentObject.acceleration.y = mAcceleration
                        parentObject.targetVelocity.y = 0.0f
                    }
                }
            }
        } else if (!mFlying && !parentObject.touchingGround() && parentObject.life > 0) {
            // A non-flying unit is in the air.  In this case, just watch for bounces off walls.
            if (Utils.sign(parentObject.targetVelocity.x) != Utils.sign(parentObject.velocity.x)) {
                // Todo: maybe the physics code should adjust target velocity instead in this case?
                parentObject.targetVelocity.x *= -1.0f
            }
        }
    }

    private fun updateAttack(player: GameObject?, parentObject: GameObject) {
        val time = sSystemRegistry.timeSystem
        val gameTime = time!!.gameTime
        var visible = true
        val camera = sSystemRegistry.cameraSystem
        val context = sSystemRegistry.contextParameters
        val dx = abs(parentObject.centeredPositionX - camera!!.fetchFocusPositionX())
        val dy = abs(parentObject.centeredPositionY - camera.fetchFocusPositionY())
        if (dx > context!!.gameWidth / 2.0f || dy > context.gameHeight / 2.0f) {
            visible = false
        }
        if (visible && parentObject.currentAction == ActionType.MOVE) {
            var closeEnough = false
            val timeToAttack = gameTime - lastAttackTime > attackDelay
            if (attackAtDistance > 0 && player != null && player.life > 0 && timeToAttack) {
                // only attack if we are facing the player
                if (Utils.sign(player.position.x - parentObject.position.x)
                        == Utils.sign(parentObject.facingDirection.x)) {
                    workingVector.set(parentObject.position)
                    workingVector.x = parentObject.centeredPositionX
                    workingVector2.set(player.position)
                    workingVector2.x = player.centeredPositionX
                    if (workingVector2.distance2(workingVector) <
                            attackAtDistance * attackAtDistance) {
                        closeEnough = true
                    }
                }
            } else {
                closeEnough = true // If no distance has been set, don't worry about
                // the player's position.
            }
            if (timeToAttack && closeEnough) {
                // Time to attack.
                parentObject.currentAction = ActionType.ATTACK
                lastAttackTime = gameTime
                if (attackStopsMovement) {
                    parentObject.velocity.zero()
                    parentObject.targetVelocity.zero()
                }
            }
        } else if (parentObject.currentAction == ActionType.ATTACK) {
            if (gameTime - lastAttackTime > attackDuration) {
                parentObject.currentAction = ActionType.MOVE
                if (attackStopsMovement) {
                    parentObject.targetVelocity.x = maxSpeed * Utils.sign(parentObject.facingDirection.x)
                    parentObject.acceleration.x = mAcceleration
                }
            }
        }
    }

    fun setMovementSpeed(speed: Float, acceleration: Float) {
        maxSpeed = speed
        mAcceleration = acceleration
    }

    fun setupAttack(distance: Float, duration: Float, delay: Float, stopMovement: Boolean) {
        mAttack = true
        attackAtDistance = distance
        attackStopsMovement = stopMovement
        attackDuration = duration
        attackDelay = delay
    }

    fun setTurnToFacePlayer(turn: Boolean) {
        turnToFacePlayer = turn
    }

    fun setFlying(flying: Boolean) {
        mFlying = flying
    }

    init {
        reset()
        setPhaseToThis(ComponentPhases.THINK.ordinal)
    }
}
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
@file:Suppress("ControlFlowWithEmptyBody", "UNUSED_PARAMETER")

package com.replica.replicaisland

import com.replica.replicaisland.CollisionParameters.HitType
import com.replica.replicaisland.GameObject.ActionType

class NPCComponent : GameComponent() {
    private var mPauseTime = 0f
    private var mTargetXVelocity = 0f
    private var mLastHitTileX = 0
    private var mLastHitTileY = 0
    private var mDialogEvent = 0
    private var mDialogIndex = 0
    private var mHitReactComponent: HitReactionComponent? = null
    private val mQueuedCommands: IntArray
    private var mQueueTop = 0
    private var mQueueBottom = 0
    private var mExecutingQueue = false
    private val mPreviousPosition: Vector2
    private var mUpImpulse = 0f
    private var mDownImpulse = 0f
    private var mHorizontalImpulse = 0f
    private var mSlowHorizontalImpulse = 0f
    private var mAcceleration = 0f
    private var mGameEvent = 0
    private var mGameEventIndex = 0
    private var mSpawnGameEventOnDeath = false
    private var mReactToHits = false
    private var mFlying = false
    private var mPauseOnAttack = false
    private var mDeathTime = 0f
    private var mDeathFadeDelay = 0f
    override fun reset() {
        mPauseTime = 0.0f
        mTargetXVelocity = 0.0f
        mLastHitTileX = 0
        mLastHitTileY = 0
        mDialogEvent = GameFlowEvent.EVENT_SHOW_DIALOG_CHARACTER1
        mDialogIndex = 0
        mHitReactComponent = null
        mQueueTop = 0
        mQueueBottom = 0
        mPreviousPosition.zero()
        mExecutingQueue = false
        mUpImpulse = UP_IMPULSE
        mDownImpulse = DOWN_IMPULSE
        mHorizontalImpulse = HORIZONTAL_IMPULSE
        mSlowHorizontalImpulse = SLOW_HORIZONTAL_IMPULSE
        mAcceleration = ACCELERATION
        mGameEvent = -1
        mGameEventIndex = -1
        mSpawnGameEventOnDeath = false
        mReactToHits = false
        mFlying = false
        mDeathTime = 0.0f
        mDeathFadeDelay = DEATH_FADE_DELAY
        mPauseOnAttack = true
    }

    override fun update(timeDelta: Float, parent: BaseObject?) {
        val parentObject = parent as GameObject?
        if (mReactToHits && mPauseTime <= 0.0f && parentObject!!.currentAction === ActionType.HIT_REACT) {
            mPauseTime = PAUSE_TIME_HIT_REACT
            pauseMovement(parentObject)
            parentObject!!.velocity.x = -parentObject.facingDirection.x * HIT_IMPULSE
            parentObject.acceleration.x = HIT_ACCELERATION
        } else if (parentObject!!.currentAction === ActionType.DEATH) {
            if (mSpawnGameEventOnDeath && mGameEvent != -1) {
                if (Utils.close(parentObject!!.velocity.x, 0.0f)
                        && parentObject.touchingGround()) {
                    if (mDeathTime < mDeathFadeDelay && mDeathTime + timeDelta >= mDeathFadeDelay) {
                        val hud = sSystemRegistry.hudSystem
                        if (hud != null) {
                            hud.startFade(false, 1.5f)
                            hud.sendGameEventOnFadeComplete(mGameEvent, mGameEventIndex)
                            mGameEvent = -1
                        }
                    }
                    mDeathTime += timeDelta
                }
            }
            // nothing else to do.
            return
        } else if (parentObject!!.life <= 0) {
            parentObject.currentAction = ActionType.DEATH
            parentObject.targetVelocity.x = 0f
            return
        } else if (parentObject.currentAction === ActionType.INVALID ||
                !mReactToHits && parentObject.currentAction === ActionType.HIT_REACT) {
            parentObject.currentAction = ActionType.MOVE
        }
        if (mPauseTime <= 0.0f) {
            val hotSpotSystem = sSystemRegistry.hotSpotSystem
            if (hotSpotSystem != null) {
                val centerX = parentObject.centeredPositionX
                val hitTileX = hotSpotSystem.getHitTileX(centerX)
                val hitTileY = hotSpotSystem.getHitTileY(parentObject.position.y + 10.0f)
                var accepted = true
                if (hitTileX != mLastHitTileX || hitTileY != mLastHitTileY) {
                    val hotSpot = hotSpotSystem.getHotSpotByTile(hitTileX, hitTileY)
                    if (hotSpot >= HotSpotSystem.HotSpotType.NPC_GO_RIGHT && hotSpot <= HotSpotSystem.HotSpotType.NPC_SLOW) {
                        // movement-related commands are immediate
                        parentObject.currentAction = ActionType.MOVE
                        accepted = executeCommand(hotSpot, parentObject, timeDelta)
                    } else if (hotSpot == HotSpotSystem.HotSpotType.ATTACK && !mPauseOnAttack) {
                        // when mPauseOnAttack is false, attacks are also immediate.
                        accepted = executeCommand(hotSpot, parentObject, timeDelta)
                    } else if (hotSpot == HotSpotSystem.HotSpotType.NPC_RUN_QUEUED_COMMANDS) {
                        if (!mExecutingQueue && mQueueTop != mQueueBottom) {
                            mExecutingQueue = true
                        }
                    } else if (hotSpot > HotSpotSystem.HotSpotType.NONE) {
                        queueCommand(hotSpot)
                    }
                }
                if (mExecutingQueue) {
                    if (mQueueTop != mQueueBottom) {
                        accepted = executeCommand(nextCommand(), parentObject, timeDelta)
                        if (accepted) {
                            advanceQueue()
                        }
                    } else {
                        mExecutingQueue = false
                    }
                }
                if (accepted) {
                    mLastHitTileX = hitTileX
                    mLastHitTileY = hitTileY
                }
            }
        } else {
            mPauseTime -= timeDelta
            if (mPauseTime < 0.0f) {
                resumeMovement(parentObject)
                mPauseTime = 0.0f
                parentObject.currentAction = ActionType.MOVE
            }
        }
        mPreviousPosition.set(parentObject.position)
    }

    private fun executeCommand(hotSpot: Int, parentObject: GameObject?, timeDelta: Float): Boolean {
        var hitAccepted = true
        val camera = sSystemRegistry.cameraSystem
        when (hotSpot) {
            HotSpotSystem.HotSpotType.WAIT_SHORT -> if (mPauseTime == 0.0f) {
                mPauseTime = PAUSE_TIME_SHORT
                pauseMovement(parentObject)
            }
            HotSpotSystem.HotSpotType.WAIT_MEDIUM -> if (mPauseTime == 0.0f) {
                mPauseTime = PAUSE_TIME_MEDIUM
                pauseMovement(parentObject)
            }
            HotSpotSystem.HotSpotType.WAIT_LONG -> if (mPauseTime == 0.0f) {
                mPauseTime = PAUSE_TIME_LONG
                pauseMovement(parentObject)
            }
            HotSpotSystem.HotSpotType.ATTACK -> {
                if (mPauseOnAttack) {
                    if (mPauseTime == 0.0f) {
                        mPauseTime = PAUSE_TIME_ATTACK
                        pauseMovement(parentObject)
                    }
                }
                parentObject!!.currentAction = ActionType.ATTACK
            }
            HotSpotSystem.HotSpotType.TALK -> if (mHitReactComponent != null) {
                if (parentObject!!.lastReceivedHitType != HitType.COLLECT) {
                    mHitReactComponent!!.setSpawnGameEventOnHit(
                            HitType.COLLECT, mDialogEvent, mDialogIndex)
                    if (parentObject.velocity.x != 0.0f) {
                        pauseMovement(parentObject)
                    }
                    hitAccepted = false
                } else {
                    parentObject.currentAction = ActionType.MOVE
                    resumeMovement(parentObject)
                    mHitReactComponent!!.setSpawnGameEventOnHit(HitType.INVALID, 0, 0)
                    parentObject.lastReceivedHitType = HitType.INVALID
                }
            }
            HotSpotSystem.HotSpotType.WALK_AND_TALK -> if (mDialogEvent != GameFlowEvent.EVENT_INVALID) {
                val level = sSystemRegistry.levelSystem
                level!!.sendGameEvent(mDialogEvent, mDialogIndex, true)
                mDialogEvent = GameFlowEvent.EVENT_INVALID
            }
            HotSpotSystem.HotSpotType.TAKE_CAMERA_FOCUS -> if (camera != null) {
                camera.target = parentObject
            }
            HotSpotSystem.HotSpotType.RELEASE_CAMERA_FOCUS -> if (camera != null) {
                val gameObjectManager = sSystemRegistry.gameObjectManager
                camera.target = gameObjectManager!!.player
            }
            HotSpotSystem.HotSpotType.END_LEVEL -> {
                val hud = sSystemRegistry.hudSystem
                if (hud != null) {
                    hud.startFade(false, 1.5f)
                    hud.sendGameEventOnFadeComplete(GameFlowEvent.EVENT_GO_TO_NEXT_LEVEL, 0)
                }
            }
            HotSpotSystem.HotSpotType.GAME_EVENT -> if (mGameEvent != -1) {
                val level = sSystemRegistry.levelSystem
                if (level != null) {
                    level.sendGameEvent(mGameEvent, mGameEventIndex, true)
                    mGameEvent = -1
                }
            }
            HotSpotSystem.HotSpotType.NPC_GO_UP_FROM_GROUND -> {
                if (!parentObject!!.touchingGround()) {
                    hitAccepted = false
                } else {
                    parentObject.velocity.y = mUpImpulse
                    parentObject.targetVelocity.y = 0.0f
                    mTargetXVelocity = 0.0f
                }
            }
            HotSpotSystem.HotSpotType.NPC_GO_UP -> {
                parentObject!!.velocity.y = mUpImpulse
                parentObject.targetVelocity.y = 0.0f
                mTargetXVelocity = 0.0f
            }
            HotSpotSystem.HotSpotType.NPC_GO_DOWN_FROM_CEILING -> {
                if (!parentObject!!.touchingCeiling()) {
                    hitAccepted = false
                } else {
                    parentObject.velocity.y = mDownImpulse
                    parentObject.targetVelocity.y = 0.0f
                    if (mFlying) {
                        mTargetXVelocity = 0.0f
                    }
                }
            }
            HotSpotSystem.HotSpotType.NPC_GO_DOWN -> {
                parentObject!!.velocity.y = mDownImpulse
                parentObject.targetVelocity.y = 0.0f
                if (mFlying) {
                    mTargetXVelocity = 0.0f
                }
            }
            HotSpotSystem.HotSpotType.NPC_GO_LEFT -> {
                parentObject!!.targetVelocity.x = -mHorizontalImpulse
                parentObject.acceleration.x = mAcceleration
                if (mFlying) {
                    parentObject.velocity.y = 0.0f
                    parentObject.targetVelocity.y = 0.0f
                }
            }
            HotSpotSystem.HotSpotType.NPC_GO_RIGHT -> {
                parentObject!!.targetVelocity.x = mHorizontalImpulse
                parentObject.acceleration.x = mAcceleration
                if (mFlying) {
                    parentObject.velocity.y = 0.0f
                    parentObject.targetVelocity.y = 0.0f
                }
            }
            HotSpotSystem.HotSpotType.NPC_GO_UP_RIGHT -> {
                parentObject!!.velocity.y = mUpImpulse
                parentObject.targetVelocity.x = mHorizontalImpulse
                parentObject.acceleration.x = mAcceleration
            }
            HotSpotSystem.HotSpotType.NPC_GO_UP_LEFT -> {
                parentObject!!.velocity.y = mUpImpulse
                parentObject.targetVelocity.x = -mHorizontalImpulse
                parentObject.acceleration.x = mAcceleration
            }
            HotSpotSystem.HotSpotType.NPC_GO_DOWN_RIGHT -> {
                parentObject!!.velocity.y = mDownImpulse
                parentObject.targetVelocity.x = mHorizontalImpulse
                parentObject.acceleration.x = mAcceleration
            }
            HotSpotSystem.HotSpotType.NPC_GO_DOWN_LEFT -> {
                parentObject!!.velocity.y = mDownImpulse
                parentObject.targetVelocity.x = -mHorizontalImpulse
                parentObject.acceleration.x = mAcceleration
            }
            HotSpotSystem.HotSpotType.NPC_GO_TOWARDS_PLAYER -> {
                var direction = 1
                val manager = sSystemRegistry.gameObjectManager
                if (manager != null) {
                    val player = manager.player
                    if (player != null) {
                        direction = Utils.sign(
                                player.centeredPositionX -
                                        parentObject!!.centeredPositionX)
                    }
                }
                parentObject!!.targetVelocity.x = mHorizontalImpulse * direction
                if (mFlying) {
                    parentObject.velocity.y = 0.0f
                    parentObject.targetVelocity.y = 0.0f
                }
            }
            HotSpotSystem.HotSpotType.NPC_GO_RANDOM -> {
                parentObject!!.targetVelocity.x = mHorizontalImpulse * if (Math.random() > 0.5f) -1.0f else 1.0f
                if (mFlying) {
                    parentObject.velocity.y = 0.0f
                    parentObject.targetVelocity.y = 0.0f
                }
            }
            HotSpotSystem.HotSpotType.NPC_STOP -> {
                parentObject!!.targetVelocity.x = 0.0f
                parentObject.velocity.x = 0.0f
            }
            HotSpotSystem.HotSpotType.NPC_SLOW -> parentObject!!.targetVelocity.x = mSlowHorizontalImpulse * Utils.sign(parentObject.targetVelocity.x)
            HotSpotSystem.HotSpotType.NPC_SELECT_DIALOG_1_1, HotSpotSystem.HotSpotType.NPC_SELECT_DIALOG_1_2, HotSpotSystem.HotSpotType.NPC_SELECT_DIALOG_1_3, HotSpotSystem.HotSpotType.NPC_SELECT_DIALOG_1_4, HotSpotSystem.HotSpotType.NPC_SELECT_DIALOG_1_5, HotSpotSystem.HotSpotType.NPC_SELECT_DIALOG_2_1, HotSpotSystem.HotSpotType.NPC_SELECT_DIALOG_2_2, HotSpotSystem.HotSpotType.NPC_SELECT_DIALOG_2_3, HotSpotSystem.HotSpotType.NPC_SELECT_DIALOG_2_4, HotSpotSystem.HotSpotType.NPC_SELECT_DIALOG_2_5 -> selectDialog(hotSpot)
            HotSpotSystem.HotSpotType.NONE -> {
                if (parentObject!!.touchingGround() && parentObject.velocity.y <= 0.0f) {
                    // ??
                    //resumeMovement(parentObject);
                }
            }
        }
        return hitAccepted
    }

    private fun pauseMovement(parentObject: GameObject?) {
        mTargetXVelocity = parentObject!!.targetVelocity.x
        parentObject.targetVelocity.x = 0.0f
        parentObject.velocity.x = 0.0f
    }

    private fun resumeMovement(parentObject: GameObject?) {
        parentObject!!.targetVelocity.x = mTargetXVelocity
        parentObject.acceleration.x = mAcceleration
    }

    private fun selectDialog(hitSpot: Int) {
        mDialogEvent = GameFlowEvent.EVENT_SHOW_DIALOG_CHARACTER1
        mDialogIndex = hitSpot - HotSpotSystem.HotSpotType.NPC_SELECT_DIALOG_1_1
        if (hitSpot >= HotSpotSystem.HotSpotType.NPC_SELECT_DIALOG_2_1) {
            mDialogEvent = GameFlowEvent.EVENT_SHOW_DIALOG_CHARACTER2
            mDialogIndex = hitSpot - HotSpotSystem.HotSpotType.NPC_SELECT_DIALOG_2_1
        }
    }

    private fun nextCommand(): Int {
        var result = HotSpotSystem.HotSpotType.NONE
        if (mQueueTop != mQueueBottom) {
            result = mQueuedCommands[mQueueTop]
        }
        return result
    }

    private fun advanceQueue(): Int {
        var result = HotSpotSystem.HotSpotType.NONE
        if (mQueueTop != mQueueBottom) {
            result = mQueuedCommands[mQueueTop]
            mQueueTop = (mQueueTop + 1) % COMMAND_QUEUE_SIZE
        }
        return result
    }

    private fun queueCommand(hotspot: Int) {
        val nextSlot = (mQueueBottom + 1) % COMMAND_QUEUE_SIZE
        if (nextSlot != mQueueTop) { // only comply if there is space left in the buffer
            mQueuedCommands[mQueueBottom] = hotspot
            mQueueBottom = nextSlot
        }
    }

    fun setHitReactionComponent(hitReact: HitReactionComponent?) {
        mHitReactComponent = hitReact
    }

    fun setSpeeds(horizontalImpulse: Float, slowHorizontalImpulse: Float, upImpulse: Float, downImpulse: Float, acceleration: Float) {
        mHorizontalImpulse = horizontalImpulse
        mSlowHorizontalImpulse = slowHorizontalImpulse
        mUpImpulse = upImpulse
        mDownImpulse = downImpulse
        mAcceleration = acceleration
    }

    fun setGameEvent(event: Int, index: Int, spawnOnDeath: Boolean) {
        mGameEvent = event
        mGameEventIndex = index
        mSpawnGameEventOnDeath = spawnOnDeath
    }

    fun setReactToHits(react: Boolean) {
        mReactToHits = react
    }

    fun setFlying(flying: Boolean) {
        mFlying = flying
    }

    fun setPauseOnAttack(pauseOnAttack: Boolean) {
        mPauseOnAttack = pauseOnAttack
    }

    companion object {
        private const val UP_IMPULSE = 400.0f
        private const val DOWN_IMPULSE = -10.0f
        private const val HORIZONTAL_IMPULSE = 200.0f
        private const val SLOW_HORIZONTAL_IMPULSE = 50.0f
        private const val ACCELERATION = 300.0f
        private const val HIT_IMPULSE = 300.0f
        private const val HIT_ACCELERATION = 700.0f
        private const val DEATH_FADE_DELAY = 4.0f
        private const val PAUSE_TIME_SHORT = 1.0f
        private const val PAUSE_TIME_MEDIUM = 4.0f
        private const val PAUSE_TIME_LONG = 8.0f
        private const val PAUSE_TIME_ATTACK = 1.0f
        private const val PAUSE_TIME_HIT_REACT = 1.0f
        private const val COMMAND_QUEUE_SIZE = 16
    }

    init {
        setPhaseToThis(ComponentPhases.THINK.ordinal)
        mQueuedCommands = IntArray(COMMAND_QUEUE_SIZE)
        mPreviousPosition = Vector2()
        reset()
    }
}
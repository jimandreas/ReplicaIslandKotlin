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
@file:Suppress("UNUSED_VARIABLE", "UNUSED_PARAMETER")

package com.replica.replicaisland

import com.replica.replicaisland.CollisionParameters.HitType
import com.replica.replicaisland.GameObject.ActionType
import kotlin.math.abs

class PlayerComponent : GameComponent() {
    enum class State {
        MOVE, STOMP, HIT_REACT, DEAD, WIN, FROZEN, POST_GHOST_DELAY
    }

    private var mTouchingGround = false
    private var mState: State? = null
    private var mTimer = 0f
    private var timer2 = 0f
    private var mFuel = 0f
    private var jumpTime = 0f
    private var ghostActive = false
    private var ghostDeactivatedTime = 0f
    private var ghostChargeTime = 0f
    private var mInventory: InventoryComponent? = null
    private val hotSpotTestPoint: Vector2 = Vector2()
    private var mInvincibleSwap: ChangeComponentsComponent? = null
    private var invincibleEndTime = 0f
    private var hitReaction: HitReactionComponent? = null
    private var fuelAirRefillSpeed = 0f
    private var mDifficultyConstants: DifficultyConstants? = null
    private var invincibleFader // HACK!
            : FadeDrawableComponent? = null

    // Variables recorded for animation decisions.
    var rocketsOn = false
        private set

    override fun reset() {
        mTouchingGround = false
        mState = State.MOVE
        mTimer = 0.0f
        timer2 = 0.0f
        mFuel = 0.0f
        jumpTime = 0.0f
        ghostActive = false
        ghostDeactivatedTime = 0.0f
        mInventory = null
        ghostChargeTime = 0.0f
        hotSpotTestPoint.zero()
        mInvincibleSwap = null
        invincibleEndTime = 0.0f
        hitReaction = null
        mDifficultyConstants = difficultyConstants
        fuelAirRefillSpeed = mDifficultyConstants!!.whatIsFuelAirRefillSpeed()
        invincibleFader = null
    }

    private fun move(time: Float, timeDelta: Float, parentObject: GameObject?) {
        val pool = sSystemRegistry.vectorPool
        val input = sSystemRegistry.inputGameInterface
        if (pool != null && input != null) {
            if (mFuel < FUEL_AMOUNT) {
                mFuel += if (mTouchingGround) {
                    mDifficultyConstants!!.whatIsFuelGroundRefillSpeed() * timeDelta
                } else {
                    fuelAirRefillSpeed * timeDelta
                }
                if (mFuel > FUEL_AMOUNT) {
                    mFuel = FUEL_AMOUNT
                }
            }
            val dpad = input.directionalPad
            val jumpButton = input.jumpButton
            if (dpad.pressed || jumpButton.pressed) {
                val impulse = pool.allocate()
                if (dpad.pressed) {
                    impulse!![dpad.retreiveXaxisMagnitude()] = 0.0f
                }
                if (jumpButton.pressed) {
                    if (jumpButton.getTriggered(time) && mTouchingGround) {
                        // In this case, velocity is instant so we don't need to scale
                        // it by time.
                        impulse!!.y = AIR_VERTICAL_IMPULSE_SPEED_FROM_GROUND
                        jumpTime = time
                    } else if (time > jumpTime + JUMP_TO_JETS_DELAY) {
                        if (mFuel > 0.0f) {
                            mFuel -= timeDelta
                            impulse!!.y = AIR_VERTICAL_IMPULSE_SPEED * timeDelta
                            rocketsOn = true
                        }
                    }
                }
                var horziontalSpeed = GROUND_IMPULSE_SPEED
                var maxHorizontalSpeed = MAX_GROUND_HORIZONTAL_SPEED
                val inTheAir = (!mTouchingGround
                        || impulse!!.y > VERTICAL_IMPULSE_TOLERANCE)
                if (inTheAir) {
                    horziontalSpeed = AIR_HORIZONTAL_IMPULSE_SPEED
                    maxHorizontalSpeed = MAX_AIR_HORIZONTAL_SPEED
                }
                impulse!!.x = impulse.x * horziontalSpeed * timeDelta

                // Don't let our jets move us past specific speed thresholds.
                var currentSpeed = parentObject!!.velocity.x
                val newSpeed = abs(currentSpeed + impulse.x)
                if (newSpeed > maxHorizontalSpeed) {
                    if (abs(currentSpeed) < maxHorizontalSpeed) {
                        currentSpeed = maxHorizontalSpeed * Utils.sign(impulse.x)
                        parentObject.velocity.x = currentSpeed
                    }
                    impulse.x = 0.0f
                }
                if (parentObject.velocity.y + impulse.y > MAX_UPWARD_SPEED
                        && Utils.sign(impulse.y) > 0) {
                    impulse.y = 0.0f
                    if (parentObject.velocity.y < MAX_UPWARD_SPEED) {
                        parentObject.velocity.y = MAX_UPWARD_SPEED
                    }
                }
                if (inTheAir) {
                    // Apply drag while in the air.
                    if (abs(currentSpeed) > maxHorizontalSpeed) {
                        var postDragSpeed = currentSpeed -
                                AIR_DRAG_SPEED * timeDelta * Utils.sign(currentSpeed)
                        if (Utils.sign(currentSpeed) != Utils.sign(postDragSpeed)) {
                            postDragSpeed = 0.0f
                        } else if (abs(postDragSpeed) < maxHorizontalSpeed) {
                            postDragSpeed = maxHorizontalSpeed * Utils.sign(postDragSpeed)
                        }
                        parentObject.velocity.x = postDragSpeed
                    }
                }
                parentObject.impulse.add(impulse)
                pool.release(impulse)
            }
        }
    }

    override fun update(timeDelta: Float, parent: BaseObject?) {
        val time = sSystemRegistry.timeSystem
        val parentObject = parent as GameObject?
        val gameTime = time!!.gameTime
        mTouchingGround = parentObject!!.touchingGround()
        rocketsOn = false
        if (parentObject.currentAction === ActionType.INVALID) {
            gotoMove(parentObject)
        }
        if (mInventory != null && mState != State.WIN) {
            val inventory = mInventory!!.fetchRecord()
            if (inventory.coinCount >= mDifficultyConstants!!.whatIsCoinsPerPowerup()) {
                inventory.coinCount = 0
                mInventory!!.setChangedValue()
                parentObject.life = mDifficultyConstants!!.whatIsMaxPlayerLife()
                if (invincibleEndTime < gameTime) {
                    mInvincibleSwap!!.activate(parentObject)
                    invincibleEndTime = gameTime + mDifficultyConstants!!.whatIsGlowDuration()
                    if (hitReaction != null) {
                        hitReaction!!.setForceInvincible(true)
                    }
                } else {
                    // invincibility is already active, extend it.
                    invincibleEndTime = gameTime + mDifficultyConstants!!.whatIsGlowDuration()
                    // HACK HACK HACK.  This really doesn't go here.
                    // To extend the invincible time we need to increment the value above (easy)
                    // and also tell the component managing the glow sprite to reset its
                    // timer (not easy).  Next time, make a shared value system for this
                    // kind of case!!
                    if (invincibleFader != null) {
                        invincibleFader!!.resetPhase()
                    }
                }
            }
            if (inventory.rubyCount >= MAX_GEMS_PER_LEVEL) {
                gotoWin(gameTime)
            }
        }
        if (invincibleEndTime > 0.0f && (invincibleEndTime < gameTime || mState == State.DEAD)) {
            mInvincibleSwap!!.activate(parentObject)
            invincibleEndTime = 0.0f
            if (hitReaction != null) {
                hitReaction!!.setForceInvincible(false)
            }
        }


        // Watch for hit reactions or death interrupting the state machine.
        if (mState != State.DEAD && mState != State.WIN) {
            if (parentObject.life <= 0) {
                gotoDead(gameTime)
            } else if (parentObject.position.y < -parentObject.height) {
                // we fell off the bottom of the screen, die.
                parentObject.life = 0
                gotoDead(gameTime)
            } else if (mState != State.HIT_REACT && parentObject.lastReceivedHitType != HitType.INVALID && parentObject.currentAction === ActionType.HIT_REACT) {
                gotoHitReact(parentObject, gameTime)
            } else {
                val hotSpot = sSystemRegistry.hotSpotSystem
                if (hotSpot != null) {
                    // TODO: HACK!  Unify all this code.
                    if (hotSpot.getHotSpot(parentObject.centeredPositionX,
                                    parentObject.position.y + 10.0f) == HotSpotSystem.HotSpotType.DIE) {
                        parentObject.life = 0
                        gotoDead(gameTime)
                    }
                }
            }
        }
        when (mState) {
            State.MOVE -> stateMove(gameTime, timeDelta, parentObject)
            State.STOMP -> stateStomp(gameTime, timeDelta, parentObject)
            State.HIT_REACT -> stateHitReact(gameTime, timeDelta, parentObject)
            State.DEAD -> stateDead(gameTime, timeDelta, parentObject)
            State.WIN -> stateWin(gameTime, timeDelta, parentObject)
            State.FROZEN -> stateFrozen(gameTime, timeDelta, parentObject)
            State.POST_GHOST_DELAY -> statePostGhostDelay(gameTime, timeDelta, parentObject)
            else -> {
            }
        }
        val hud = sSystemRegistry.hudSystem
        val input = sSystemRegistry.inputGameInterface
        hud?.setFuelPercent(mFuel / FUEL_AMOUNT)
    }

    private fun gotoMove(parentObject: GameObject?) {
        parentObject!!.currentAction = ActionType.MOVE
        mState = State.MOVE
    }

    private fun stateMove(time: Float, timeDelta: Float, parentObject: GameObject?) {
        if (!ghostActive) {
            move(time, timeDelta, parentObject)
            val input = sSystemRegistry.inputGameInterface
            val attackButton = input!!.attackButton
            if (attackButton.getTriggered(time) && !mTouchingGround) {
                gotoStomp(parentObject)
            } else if (attackButton.pressed && mTouchingGround
                    && ghostDeactivatedTime + GHOST_REACTIVATION_DELAY < time) {
                ghostChargeTime += timeDelta
                if (ghostChargeTime > GHOST_CHARGE_TIME) {
                    val factory = sSystemRegistry.gameObjectFactory
                    val manager = sSystemRegistry.gameObjectManager
                    if (factory != null && manager != null) {
                        val x = parentObject!!.position.x
                        val y = parentObject.position.y
                        var ghostTime = NO_GEMS_GHOST_TIME
                        if (mInventory != null) {
                            val inventory = mInventory!!.fetchRecord()
                            if (inventory.rubyCount == 1) {
                                ghostTime = ONE_GEM_GHOST_TIME
                            } else if (inventory.rubyCount == 2) {
                                ghostTime = TWO_GEMS_GHOST_TIME
                            }
                        }
                        val ghost = factory.spawnPlayerGhost(x, y, parentObject, ghostTime)
                        manager.add(ghost!!)
                        ghostActive = true
                        val camera = sSystemRegistry.cameraSystem
                        if (camera != null) {
                            camera.target = ghost
                        }
                    }
                }
            } else if (!attackButton.pressed) {
                ghostChargeTime = 0.0f
            }
        }
    }

    private fun gotoStomp(parentObject: GameObject?) {
        parentObject!!.currentAction = ActionType.ATTACK
        mState = State.STOMP
        mTimer = -1.0f
        timer2 = -1.0f
        parentObject.impulse.zero()
        parentObject.velocity[0.0f] = 0.0f
        parentObject.positionLocked = true
    }

    private fun stateStomp(time: Float, timeDelta: Float, parentObject: GameObject?) {
        if (mTimer < 0.0f) {
            // first frame
            mTimer = time
        } else if (time - mTimer > STOMP_AIR_HANG_TIME) {
            // hang time complete
            parentObject!!.velocity[0.0f] = STOMP_VELOCITY
            parentObject.positionLocked = false
        }
        if (mTouchingGround && timer2 < 0.0f) {
            timer2 = time
            val camera = sSystemRegistry.cameraSystem
            camera?.shake(STOMP_DELAY_TIME, STOMP_SHAKE_MAGNITUDE)
            val vibrator = sSystemRegistry.vibrationSystem
            vibrator?.vibrate(STOMP_VIBRATE_TIME)
            val factory = sSystemRegistry.gameObjectFactory
            val manager = sSystemRegistry.gameObjectManager
            if (factory != null && manager != null) {
                val x = parentObject!!.position.x
                val y = parentObject.position.y
                val smoke1 = factory.spawnDust(x, y - 16, true)
                val smoke2 = factory.spawnDust(x + 32, y - 16, false)
                manager.add(smoke1!!)
                manager.add(smoke2!!)
            }
        }
        if (timer2 > 0.0f && time - timer2 > STOMP_DELAY_TIME) {
            parentObject!!.positionLocked = false
            gotoMove(parentObject)
        }
    }

    private fun gotoHitReact(parentObject: GameObject?, time: Float) {
        if (parentObject!!.lastReceivedHitType == HitType.LAUNCH) {
            if (mState != State.FROZEN) {
                gotoFrozen(parentObject)
            }
        } else {
            mState = State.HIT_REACT
            mTimer = time
        }
    }

    private fun stateHitReact(time: Float, timeDelta: Float, parentObject: GameObject?) {
        // This state just waits until the timer is expired.
        if (time - mTimer > HIT_REACT_TIME) {
            gotoMove(parentObject)
        }
    }

    private fun gotoDead(time: Float) {
        mState = State.DEAD
        mTimer = time
    }

    private fun stateDead(time: Float, timeDelta: Float, parentObject: GameObject?) {
        if (mTouchingGround && parentObject!!.currentAction !== ActionType.DEATH) {
            parentObject!!.currentAction = ActionType.DEATH
            parentObject.velocity.zero()
            parentObject.targetVelocity.zero()
        }
        if (parentObject!!.position.y < -parentObject.height) {
            // fell off the bottom of the screen.
            parentObject.currentAction = ActionType.DEATH
            parentObject.velocity.zero()
            parentObject.targetVelocity.zero()
        }
        if (parentObject.currentAction === ActionType.DEATH && mTimer > 0.0f) {
            val elapsed = time - mTimer
            val hud = sSystemRegistry.hudSystem
            if (hud != null && !hud.isFading) {
                if (elapsed > 2.0f) {
                    hud.startFade(false, 1.5f)
                    hud.sendGameEventOnFadeComplete(GameFlowEvent.EVENT_RESTART_LEVEL, 0)
                    val recorder = sSystemRegistry.eventRecorder
                    if (recorder != null) {
                        recorder.lastDeathPosition = parentObject.position
                    }
                }
            }
        }
    }

    private fun gotoWin(time: Float) {
        mState = State.WIN
        val timeSystem = sSystemRegistry.timeSystem
        mTimer = timeSystem!!.realTime
        timeSystem.appyScale(0.1f, 8.0f, true)
    }

    private fun stateWin(time: Float, timeDelta: Float, parentObject: GameObject?) {
        if (mTimer > 0.0f) {
            val timeSystem = sSystemRegistry.timeSystem
            val elapsed = timeSystem!!.realTime - mTimer
            val hud = sSystemRegistry.hudSystem
            if (hud != null && !hud.isFading) {
                if (elapsed > 2.0f) {
                    hud.startFade(false, 1.5f)
                    hud.sendGameEventOnFadeComplete(GameFlowEvent.EVENT_GO_TO_NEXT_LEVEL, 0)
                }
            }
        }
    }

    private fun gotoFrozen(parentObject: GameObject?) {
        mState = State.FROZEN
        parentObject!!.currentAction = ActionType.FROZEN
    }

    private fun stateFrozen(time: Float, timeDelta: Float, parentObject: GameObject?) {
        if (parentObject!!.currentAction === ActionType.MOVE) {
            gotoMove(parentObject)
        }
    }

    private fun gotoPostGhostDelay() {
        mState = State.POST_GHOST_DELAY
    }

    private fun statePostGhostDelay(time: Float, timeDelta: Float, parentObject: GameObject?) {
        if (time > ghostDeactivatedTime) {
            if (!ghostActive) { // The ghost might have activated again during this delay.
                val camera = sSystemRegistry.cameraSystem
                if (camera != null) {
                    camera.target = parentObject
                }
            }
            gotoMove(parentObject)
        }
    }

    fun deactivateGhost(delay: Float) {
        ghostActive = false
        ghostDeactivatedTime = sSystemRegistry.timeSystem!!.gameTime + delay
        gotoPostGhostDelay()
    }

    fun setInventory(inventory: InventoryComponent?) {
        mInventory = inventory
    }

    fun setInvincibleSwap(invincibleSwap: ChangeComponentsComponent?) {
        mInvincibleSwap = invincibleSwap
    }

    fun setHitReactionComponent(hitReact: HitReactionComponent?) {
        hitReaction = hitReact
    }

    fun setInvincibleFader(fader: FadeDrawableComponent?) {
        invincibleFader = fader
    }

    fun adjustDifficulty(parent: GameObject, levelAttemps: Int) {
        // Super basic DDA.
        // If we've tried this levels several times secretly increase our
        // hit points so the level gets easier.
        // Also make fuel refill faster in the air after we've died too many times.
        if (levelAttemps >= mDifficultyConstants!!.whatIsDDAStage1Attempts()) {
            if (levelAttemps >= mDifficultyConstants!!.whatIsDDAStage2Attempts()) {
                parent.life += mDifficultyConstants!!.whatIsDDAStage2LifeBoost()
                fuelAirRefillSpeed = mDifficultyConstants!!.whatIsDDAStage2FuelAirRefillSpeed()
            } else {
                parent.life += mDifficultyConstants!!.whatIsDDAStage1LifeBoost()
                fuelAirRefillSpeed = mDifficultyConstants!!.whatIsDDAStage1FuelAirRefillSpeed()
            }
        }
    }

    companion object {
        private const val GROUND_IMPULSE_SPEED = 5000.0f
        private const val AIR_HORIZONTAL_IMPULSE_SPEED = 4000.0f
        private const val AIR_VERTICAL_IMPULSE_SPEED = 1200.0f
        private const val AIR_VERTICAL_IMPULSE_SPEED_FROM_GROUND = 250.0f
        private const val AIR_DRAG_SPEED = 4000.0f
        private const val MAX_GROUND_HORIZONTAL_SPEED = 500.0f
        private const val MAX_AIR_HORIZONTAL_SPEED = 150.0f
        private const val MAX_UPWARD_SPEED = 250.0f
        private const val VERTICAL_IMPULSE_TOLERANCE = 50.0f
        private const val FUEL_AMOUNT = 1.0f
        private const val JUMP_TO_JETS_DELAY = 0.5f
        private const val STOMP_VELOCITY = -1000.0f
        private const val STOMP_DELAY_TIME = 0.15f
        private const val STOMP_AIR_HANG_TIME = 0.0f //0.25f;
        private const val STOMP_SHAKE_MAGNITUDE = 15.0f
        private const val STOMP_VIBRATE_TIME = 0.05f
        private const val HIT_REACT_TIME = 0.5f
        private const val GHOST_REACTIVATION_DELAY = 0.3f
        private const val GHOST_CHARGE_TIME = 0.75f
        private const val MAX_GEMS_PER_LEVEL = 3
        private const val NO_GEMS_GHOST_TIME = 3.0f
        private const val ONE_GEM_GHOST_TIME = 8.0f
        private const val TWO_GEMS_GHOST_TIME = 0.0f // no limit.
        private val sDifficultyArray = arrayOf(
                BabyDifficultyConstants(),
                KidsDifficultyConstants(),
                AdultsDifficultyConstants()
        )
        @JvmStatic
        val difficultyConstants: DifficultyConstants
            get() = sDifficultyArray[sSystemRegistry.contextParameters!!.difficulty]
    }

    init {
        reset()
        setPhaseToThis(ComponentPhases.THINK.ordinal)
    }
}
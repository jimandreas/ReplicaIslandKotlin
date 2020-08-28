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
@file:Suppress("SENSELESS_COMPARISON", "CascadeIf")

package com.replica.replicaisland

import com.replica.replicaisland.CollisionParameters.HitType
import com.replica.replicaisland.GameObject.ActionType
import com.replica.replicaisland.SoundSystem.Sound
import kotlin.math.abs
import kotlin.math.cos

/**
 * Player Animation game object component.  Responsible for selecting an animation to describe the
 * player's current state.  Requires the object to contain a SpriteComponent to play animations.
 */
class AnimationComponent : GameComponent() {
    enum class PlayerAnimations {
        IDLE, MOVE, MOVE_FAST, BOOST_UP, BOOST_MOVE, BOOST_MOVE_FAST, STOMP, HIT_REACT, DEATH, FROZEN
    }

    private var mSprite: SpriteComponent? = null
    private var mJetSprite: SpriteComponent? = null
    private var mSparksSprite: SpriteComponent? = null
    private var mPlayer: PlayerComponent? = null
    private var mLastFlickerTime = 0f
    private var mFlickerOn = false
    private var mFlickerTimeRemaining = 0f
    private var mPreviousAction: ActionType? = null
    private var mLastRocketsOnTime = 0f
    private var mExplodingDeath = false
    private var mDamageSwap: ChangeComponentsComponent? = null
    private var mLandThump: Sound? = null
    private var mRocketSound: Sound? = null
    private var mExplosionSound: Sound? = null
    private var mLandThumpDelay = 0f
    private var mRocketSoundStream = 0
    private var mRocketSoundPaused = false
    private var mLastRubyCount = 0
    private var mRubySound1: Sound? = null
    private var mRubySound2: Sound? = null
    private var mRubySound3: Sound? = null
    private var mInventory: InventoryComponent? = null
    override fun reset() {
        mPreviousAction = ActionType.INVALID
        mSprite = null
        mJetSprite = null
        mSparksSprite = null
        mPlayer = null
        mLastFlickerTime = 0.0f
        mFlickerOn = false
        mFlickerTimeRemaining = 0.0f
        mLastRocketsOnTime = 0.0f
        mExplodingDeath = false
        mDamageSwap = null
        mLandThump = null
        mLandThumpDelay = 0.0f
        mRocketSound = null
        mRocketSoundStream = -1
        mLastRubyCount = 0
        mInventory = null
        mExplosionSound = null
    }

    override fun update(timeDelta: Float, parent: BaseObject?) {
        if (mSprite != null) {
            val parentObject = parent as GameObject
            val velocityX = parentObject!!.velocity.x
            val velocityY = parentObject.velocity.y
            val currentAction = parentObject.currentAction
            if (mJetSprite != null) {
                mJetSprite!!.visible = false
            }
            if (mSparksSprite != null) {
                mSparksSprite!!.visible = false
            }
            val time = sSystemRegistry.timeSystem
            val gameTime = time!!.gameTime
            if (currentAction != ActionType.HIT_REACT && mPreviousAction == ActionType.HIT_REACT) {
                mFlickerTimeRemaining = FLICKER_DURATION
            }
            val touchingGround = parentObject.touchingGround()
            var boosting = if (mPlayer != null) mPlayer!!.rocketsOn else false
            var visible = true
            val sound = sSystemRegistry.soundSystem

            // It's usually not necessary to test to see if sound is enabled or not (when it's disabled,
            // play() is just a nop), but in this case I have a stream that is maintained for the rocket
            // sounds.  So it's simpler to just avoid that code if sound is off.
            if (sound!!.soundEnabled) {
                if (boosting) {
                    mLastRocketsOnTime = gameTime
                } else {
                    if (gameTime - mLastRocketsOnTime < MIN_ROCKET_TIME
                            && velocityY >= 0.0f) {
                        boosting = true
                    }
                }
                if (mRocketSound != null) {
                    if (boosting) {
                        if (mRocketSoundStream == -1) {
                            mRocketSoundStream = sound.play(mRocketSound!!, true, SoundSystem.PRIORITY_HIGH)
                            mRocketSoundPaused = false
                        } else if (mRocketSoundPaused) {
                            sound.resume(mRocketSoundStream)
                            mRocketSoundPaused = false
                        }
                    } else {
                        sound.pause(mRocketSoundStream)
                        mRocketSoundPaused = true
                    }
                }
            }

            // Normally, for collectables like the coin, we could just tell the object to play
            // a sound when it is collected.  The gems are a special case, though, as we
            // want to pick a different sound depending on how many have been collected.
            if (mInventory != null && mRubySound1 != null && mRubySound2 != null && mRubySound3 != null) {
                val inventory = mInventory!!.fetchRecord()
                val rubyCount = inventory.rubyCount
                if (rubyCount != mLastRubyCount) {
                    mLastRubyCount = rubyCount
                    when (rubyCount) {
                        1 -> sound.play(mRubySound1!!, false, SoundSystem.PRIORITY_NORMAL)
                        2 -> sound.play(mRubySound2!!, false, SoundSystem.PRIORITY_NORMAL)
                        3 -> sound.play(mRubySound3!!, false, SoundSystem.PRIORITY_NORMAL)
                    }
                }
            }

            // Turn on visual effects (smoke, etc) when the player's life reaches 1.
            if (mDamageSwap != null) {
                if (parentObject.life == 1 && !mDamageSwap!!.currentlySwapped) {
                    mDamageSwap!!.activate(parentObject)
                } else if (parentObject.life != 1 && mDamageSwap!!.currentlySwapped) {
                    mDamageSwap!!.activate(parentObject)
                }
            }
            var opacity = 1.0f
            if (currentAction == ActionType.MOVE) {
                val input = sSystemRegistry.inputGameInterface
                val dpad = input!!.directionalPad
                if (dpad.retreiveXaxisMagnitude() < 0.0f) {
                    parentObject.facingDirection.x = -1.0f
                } else if (dpad.retreiveXaxisMagnitude() > 0.0f) {
                    parentObject.facingDirection.x = 1.0f
                }

                // TODO: get rid of these magic numbers!
                if (touchingGround) {
                    if (Utils.close(velocityX, 0.0f, 30.0f)) {
                        mSprite!!.playAnimation(PlayerAnimations.IDLE.ordinal)
                    } else if (abs(velocityX) > 300.0f) {
                        mSprite!!.playAnimation(PlayerAnimations.MOVE_FAST.ordinal)
                    } else {
                        mSprite!!.playAnimation(PlayerAnimations.MOVE.ordinal)
                    }
                    val attackButton = input.attackButton
                    if (attackButton.pressed) {
                        // charge
                        val pressedTime = gameTime - attackButton.lastPressedTime
                        val wave = cos(pressedTime * Math.PI.toFloat() * 2.0f.toDouble()).toFloat()
                        opacity = wave * 0.25f + 0.75f
                    }
                } else {
                    if (boosting) {
                        if (mJetSprite != null) {
                            mJetSprite!!.visible = true
                        }
                        if (abs(velocityX) < 100.0f && velocityY > 10.0f) {
                            mSprite!!.playAnimation(PlayerAnimations.BOOST_UP.ordinal)
                        } else if (abs(velocityX) > 300.0f) {
                            mSprite!!.playAnimation(PlayerAnimations.BOOST_MOVE_FAST.ordinal)
                        } else {
                            mSprite!!.playAnimation(PlayerAnimations.BOOST_MOVE.ordinal)
                        }
                    } else {
                        if (Utils.close(velocityX, 0.0f, 1.0f)) {
                            mSprite!!.playAnimation(PlayerAnimations.IDLE.ordinal)
                        } else if (abs(velocityX) > 300.0f) {
                            mSprite!!.playAnimation(PlayerAnimations.MOVE_FAST.ordinal)
                        } else {
                            mSprite!!.playAnimation(PlayerAnimations.MOVE.ordinal)
                        }
                    }
                }
            } else if (currentAction == ActionType.ATTACK) {
                mSprite!!.playAnimation(PlayerAnimations.STOMP.ordinal)
                if (touchingGround && gameTime > mLandThumpDelay) {
                    if (mLandThump != null && sound != null) {
                        // modulate the sound slightly to avoid sounding too similar
                        sound.play(mLandThump!!, false, SoundSystem.PRIORITY_HIGH, 1.0f,
                                (Math.random() * 0.5f).toFloat() + 0.75f)
                        mLandThumpDelay = gameTime + LAND_THUMP_DELAY
                    }
                }
            } else if (currentAction == ActionType.HIT_REACT) {
                mSprite!!.playAnimation(PlayerAnimations.HIT_REACT.ordinal)
                if (velocityX > 0.0f) {
                    parentObject.facingDirection.x = -1.0f
                } else if (velocityX < 0.0f) {
                    parentObject.facingDirection.x = 1.0f
                }
                if (mSparksSprite != null) {
                    mSparksSprite!!.visible = true
                }
            } else if (currentAction == ActionType.DEATH) {
                if (mPreviousAction != currentAction) {
                    if (mExplosionSound != null) {
                        sound.play(mExplosionSound!!, false, SoundSystem.PRIORITY_NORMAL)
                    }
                    // by default, explode when hit with the DEATH hit type.
                    var explodingDeath = parentObject.lastReceivedHitType == HitType.DEATH
                    // or if touching a death tile.
                    val hotSpot = sSystemRegistry.hotSpotSystem
                    if (hotSpot != null) {
                        // TODO: HACK!  Unify all this code.
                        if (hotSpot.getHotSpot(parentObject.centeredPositionX,
                                        parentObject.position.y + 10.0f) == HotSpotSystem.HotSpotType.DIE) {
                            explodingDeath = true
                        }
                    }
                    if (explodingDeath) {
                        mExplodingDeath = true
                        val factory = sSystemRegistry.gameObjectFactory
                        val manager = sSystemRegistry.gameObjectManager
                        if (factory != null && manager != null) {
                            val explosion = factory.spawnEffectExplosionGiant(parentObject.position.x, parentObject.position.y)
                            if (explosion != null) {
                                manager.add(explosion)
                            }
                        }
                    } else {
                        mSprite!!.playAnimation(PlayerAnimations.DEATH.ordinal)
                        mExplodingDeath = false
                    }
                    mFlickerTimeRemaining = 0.0f
                    if (mSparksSprite != null) {
                        if (!mSprite!!.animationFinished()) {
                            mSparksSprite!!.visible = true
                        }
                    }
                }
                if (mExplodingDeath) {
                    visible = false
                }
            } else if (currentAction == ActionType.FROZEN) {
                mSprite!!.playAnimation(PlayerAnimations.FROZEN.ordinal)
            }
            if (mFlickerTimeRemaining > 0.0f) {
                mFlickerTimeRemaining -= timeDelta
                if (gameTime > mLastFlickerTime + FLICKER_INTERVAL) {
                    mLastFlickerTime = gameTime
                    mFlickerOn = !mFlickerOn
                }
                mSprite!!.visible = mFlickerOn
                if (mJetSprite != null && mJetSprite!!.visible) {
                    mJetSprite!!.visible = mFlickerOn
                }
            } else {
                mSprite!!.visible = visible
                mSprite!!.setOpacity(opacity)
            }
            mPreviousAction = currentAction
        }
    }

    fun setSprite(sprite: SpriteComponent?) {
        mSprite = sprite
    }

    fun setJetSprite(sprite: SpriteComponent?) {
        mJetSprite = sprite
    }

    fun setSparksSprite(sprite: SpriteComponent?) {
        mSparksSprite = sprite
    }

    fun setPlayer(player: PlayerComponent?) {
        mPlayer = player
    }

    fun setDamageSwap(damageSwap: ChangeComponentsComponent?) {
        mDamageSwap = damageSwap
    }

    fun setLandThump(land: Sound?) {
        mLandThump = land
    }

    fun setRocketSound(sound: Sound?) {
        mRocketSound = sound
    }

    fun setRubySounds(one: Sound?, two: Sound?, three: Sound?) {
        mRubySound1 = one
        mRubySound2 = two
        mRubySound3 = three
    }

    fun setInventory(inventory: InventoryComponent?) {
        mInventory = inventory
    }

    fun setExplosionSound(sound: Sound?) {
        mExplosionSound = sound
    }

    companion object {
        private const val MIN_ROCKET_TIME = 0.0f
        private const val FLICKER_INTERVAL = 0.15f
        private const val FLICKER_DURATION = 3.0f
        private const val LAND_THUMP_DELAY = 0.5f
    }

    init {
        reset()
        setPhaseToThis(ComponentPhases.ANIMATION.ordinal)
    }
}
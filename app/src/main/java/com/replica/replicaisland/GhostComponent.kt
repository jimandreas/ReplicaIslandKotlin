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
@file:Suppress("IfThenToSafeAccess")

package com.replica.replicaisland

import com.replica.replicaisland.GameObject.ActionType
import com.replica.replicaisland.SoundSystem.Sound

class GhostComponent : GameComponent() {
    private var mMovementSpeed = 0f
    private var mJumpImpulse = 0f
    private var mAcceleration = 0f
    private var mUseOrientationSensor = false
    private var mDelayOnRelease = 0f
    private var mKillOnRelease = false
    private var mTargetAction: ActionType? = null
    private var mLifeTime = 0f
    private var mChangeActionOnButton = false
    private var mButtonPressedAction: ActionType? = null
    private var mAmbientSound: Sound? = null
    private var mAmbientSoundStream = 0
    override fun reset() {
        mMovementSpeed = 0.0f
        mJumpImpulse = 0.0f
        mAcceleration = 0.0f
        mUseOrientationSensor = false
        mDelayOnRelease = 0.0f
        mKillOnRelease = false
        mTargetAction = ActionType.MOVE
        mLifeTime = 0.0f
        mChangeActionOnButton = false
        mButtonPressedAction = ActionType.INVALID
        mAmbientSound = null
        mAmbientSoundStream = -1
    }

    override fun update(timeDelta: Float, parent: BaseObject?) {
        val parentObject = parent as GameObject
        var timeToRelease = false
        val input = sSystemRegistry.inputGameInterface
        val camera = sSystemRegistry.cameraSystem
        if (parentObject.life > 0) {
            if (mLifeTime > 0.0f) {
                mLifeTime -= timeDelta
                if (mLifeTime <= 0.0f) {
                    timeToRelease = true
                } else if (mLifeTime < 1.0f) {
                    // Do we have a sprite we can fade out?
                    val sprite = parentObject.findByClass(SpriteComponent::class.java)
                    if (sprite != null) {
                        sprite.setOpacity(mLifeTime)
                    }
                }
            }
            if (parentObject.position.y < -parentObject.height) {
                // we fell off the bottom of the screen, die.
                parentObject.life = 0
                timeToRelease = true
            }
            parentObject.currentAction = mTargetAction
            if (camera != null) {
                camera.target = parentObject
            }
            if (input != null) {
                if (mUseOrientationSensor) {
                    val tilt = input.tilt
                    parentObject.targetVelocity.x = tilt.retreiveXaxisMagnitude() * mMovementSpeed
                    parentObject.targetVelocity.y = tilt.retreiveYaxisMagnitude() * mMovementSpeed
                    parentObject.acceleration.x = mAcceleration
                    parentObject.acceleration.y = mAcceleration
                } else {
                    val dpad = input.directionalPad
                    parentObject.targetVelocity.x = dpad.retreiveXaxisMagnitude() * mMovementSpeed
                    parentObject.acceleration.x = mAcceleration
                }
                val jumpButton = input.jumpButton
                val time = sSystemRegistry.timeSystem
                val gameTime = time!!.gameTime
                if (jumpButton.getTriggered(gameTime)
                        && parentObject.touchingGround()
                        && parentObject.velocity.y <= 0.0f && !mChangeActionOnButton) {
                    parentObject.impulse.y += mJumpImpulse
                } else if (mChangeActionOnButton && jumpButton.pressed) {
                    parentObject.currentAction = mButtonPressedAction
                }
                val attackButton = input.attackButton
                if (attackButton.getTriggered(gameTime)) {
                    timeToRelease = true
                }
            }
            if (!timeToRelease && mAmbientSound != null && mAmbientSoundStream == -1) {
                val sound = sSystemRegistry.soundSystem
                if (sound != null) {
                    mAmbientSoundStream = sound.play(mAmbientSound!!, true, SoundSystem.PRIORITY_NORMAL)
                }
            }
        }
        if (parentObject.life == 0) {
            if (mAmbientSoundStream > -1) {
                val sound = sSystemRegistry.soundSystem
                if (sound != null) {
                    sound.stop(mAmbientSoundStream)
                    mAmbientSoundStream = -1
                }
            }
        }
        if (timeToRelease) {
            releaseControl(parentObject)
        }
    }

    fun releaseControl(parentObject: GameObject) {
        val manager = sSystemRegistry.gameObjectManager
        var player: GameObject? = null
        if (manager != null) {
            player = manager.player
        }
        val camera = sSystemRegistry.cameraSystem
        if (camera != null) {
            camera.target = null
        }
        if (player != null) {
            if (mKillOnRelease) {
                parentObject.life = 0
            } else {
                // See if there's a component swap we can run.
                val swap = parentObject.findByClass(ChangeComponentsComponent::class.java)
                if (swap != null) {
                    swap.activate(parentObject)
                }

            }
            val control = player.findByClass(PlayerComponent::class.java)
            if (camera!!.pointVisible(player.position, player.width)) {
                (control as PlayerComponent).deactivateGhost(0.0f)
            } else {
                (control as PlayerComponent).deactivateGhost(mDelayOnRelease)
            }
            /* final InputSystem input = sSystemRegistry.inputSystem;
            if (input != null) {
                input.clearClickTriggered();
            }*/
        }
        if (mAmbientSoundStream > -1) {
            val sound = sSystemRegistry.soundSystem
            if (sound != null) {
                sound.stop(mAmbientSoundStream)
                mAmbientSoundStream = -1
            }
        }
    }

    fun setMovementSpeed(movementSpeed: Float) {
        mMovementSpeed = movementSpeed
    }

    fun setJumpImpulse(jumpImpulse: Float) {
        mJumpImpulse = jumpImpulse
    }

    fun setAcceleration(accceleration: Float) {
        mAcceleration = accceleration
    }

    fun setUseOrientationSensor(useSensor: Boolean) {
        mUseOrientationSensor = useSensor
    }

    fun setDelayOnRelease(delayOnRelease: Float) {
        mDelayOnRelease = delayOnRelease
    }

    fun setKillOnRelease(killOnRelease: Boolean) {
        mKillOnRelease = killOnRelease
    }

    fun setTargetAction(action: ActionType?) {
        mTargetAction = action
    }

    fun setLifeTime(lifeTime: Float) {
        mLifeTime = lifeTime
    }

    fun changeActionOnButton(pressedAction: ActionType?) {
        mButtonPressedAction = pressedAction
        mChangeActionOnButton = true
    }

    fun setAmbientSound(sound: Sound?) {
        mAmbientSound = sound
    }

    init {
        setPhaseToThis(ComponentPhases.THINK.ordinal)
        reset()
    }
}
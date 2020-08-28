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

import com.replica.replicaisland.GameObject.ActionType
import com.replica.replicaisland.GameObjectFactory.GameObjectType
import com.replica.replicaisland.SoundSystem.Sound
import kotlin.math.sin

class LauncherComponent : GameComponent() {
    private var mShot: GameObject? = null
    private var mLaunchTime = 0f
    private var mAngle = 0f
    private var mLaunchDelay = 0f
    private val mLaunchDirection: Vector2 = Vector2()
    private var mLaunchMagnitude = 0f
    private var mPostLaunchDelay = 0f
    private var mDriveActions = false
    private var mLaunchEffect: GameObjectType? = null
    private var mLaunchEffectOffsetX = 0f
    private var mLaunchEffectOffsetY = 0f
    private var mLaunchSound: Sound? = null
    override fun reset() {
        mShot = null
        mLaunchTime = 0.0f
        mAngle = 0.0f
        mLaunchDelay = DEFAULT_LAUNCH_DELAY
        mLaunchMagnitude = DEFAULT_LAUNCH_MAGNITUDE
        mPostLaunchDelay = DEFAULT_POST_LAUNCH_DELAY
        mDriveActions = true
        mLaunchEffect = GameObjectType.INVALID
        mLaunchEffectOffsetX = 0.0f
        mLaunchEffectOffsetY = 0.0f
        mLaunchSound = null
    }

    override fun update(timeDelta: Float, parent: BaseObject?) {
        val time = sSystemRegistry.timeSystem
        val gameTime = time!!.gameTime
        val parentObject = parent as GameObject?
        if (mShot != null) {
            if (mShot!!.life <= 0) {
                // Looks like the shot is dead.  Let's forget about it.
                // TODO: this is unreliable.  We should have a "notify on death" event or something.
                mShot = null
            } else {
                if (gameTime > mLaunchTime) {
                    fire(mShot!!, parentObject, mAngle)
                    mShot = null
                    if (mDriveActions) {
                        parentObject!!.currentAction = ActionType.ATTACK
                    }
                } else {
                    mShot!!.position = parentObject!!.position
                }
            }
        } else if (gameTime > mLaunchTime + mPostLaunchDelay) {
            if (mDriveActions) {
                parentObject!!.currentAction = ActionType.IDLE
            }
        }
    }

    fun prepareToLaunch(thing: GameObject, parentObject: GameObject?) {
        if (mShot != thing) {
            if (mShot != null) {
                // We already have a shot loaded and we are asked to shoot something else.
                // Shoot the current shot off and then load the new one.
                fire(mShot!!, parentObject, mAngle)
            }
            val time = sSystemRegistry.timeSystem
            val gameTime = time!!.gameTime
            mShot = thing
            mLaunchTime = gameTime + mLaunchDelay
        }
    }

    private fun fire(thing: GameObject, parentObject: GameObject?, mAngle: Float) {
        if (mDriveActions) {
            thing.currentAction = ActionType.MOVE
        }
        mLaunchDirection[sin(mAngle.toDouble()).toFloat()] = Math.cos(mAngle.toDouble()).toFloat()
        mLaunchDirection.multiply(parentObject!!.facingDirection)
        mLaunchDirection.multiply(mLaunchMagnitude)
        thing.velocity = mLaunchDirection
        if (mLaunchSound != null) {
            val sound = sSystemRegistry.soundSystem
            sound?.play(mLaunchSound!!, false, SoundSystem.PRIORITY_NORMAL)
        }
        if (mLaunchEffect !== GameObjectType.INVALID) {
            val factory = sSystemRegistry.gameObjectFactory
            val manager = sSystemRegistry.gameObjectManager
            if (factory != null && manager != null) {
                val position = parentObject.position
                val effect = factory.spawn(mLaunchEffect!!,
                        position.x + mLaunchEffectOffsetX * parentObject.facingDirection.x,
                        position.y + mLaunchEffectOffsetY * parentObject.facingDirection.y,
                        false)
                if (effect != null) {
                    manager.add(effect)
                }
            }
        }
    }

    fun setup(angle: Float, magnitude: Float, launchDelay: Float, postLaunchDelay: Float, driveActions: Boolean) {
        mAngle = angle
        mLaunchMagnitude = magnitude
        mLaunchDelay = launchDelay
        mPostLaunchDelay = postLaunchDelay
        mDriveActions = driveActions
    }

    fun setLaunchEffect(effectType: GameObjectType?, offsetX: Float, offsetY: Float) {
        mLaunchEffect = effectType
        mLaunchEffectOffsetX = offsetX
        mLaunchEffectOffsetY = offsetY
    }

    fun setLaunchSound(sound: Sound?) {
        mLaunchSound = sound
    }

    companion object {
        private const val DEFAULT_LAUNCH_DELAY = 2.0f
        private const val DEFAULT_LAUNCH_MAGNITUDE = 2000.0f
        private const val DEFAULT_POST_LAUNCH_DELAY = 1.0f
    }

    init {
        reset()
        setPhaseToThis(ComponentPhases.THINK.ordinal)
    }
}
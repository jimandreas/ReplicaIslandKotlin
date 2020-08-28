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
@file:Suppress("UNUSED_VALUE")

package com.replica.replicaisland

import com.replica.replicaisland.ChannelSystem.ChannelBooleanValue
import com.replica.replicaisland.GameObject.ActionType
import kotlin.math.sin

class TheSourceComponent : GameComponent() {
    private var mTimer = 0f
    private var mExplosionTimer = 0f
    private var mShakeStartPosition = 0f
    private var mChannel: ChannelSystem.Channel? = null
    private var mGameEvent = 0
    private var mGameEventIndex = 0
    private var mDead = false
    override fun reset() {
        mTimer = 0.0f
        mExplosionTimer = 0.0f
        mShakeStartPosition = 0.0f
        mChannel = null
        sChannelValue.value = false
        mGameEvent = -1
        mGameEventIndex = -1
        mDead = false
    }

    override fun update(timeDelta: Float, parent: BaseObject?) {
        val parentObject = parent as GameObject
        var currentAction = parentObject.currentAction
        val camera = sSystemRegistry.cameraSystem
        if (currentAction == ActionType.HIT_REACT) {
            if (parentObject.life > 0) {
                mTimer = SHAKE_TIME
                camera!!.shake(SHAKE_TIME, CAMERA_HIT_SHAKE_MAGNITUDE)
                mShakeStartPosition = parentObject.position.x
                parentObject.currentAction = ActionType.IDLE
                currentAction = ActionType.IDLE
            } else {
                parentObject.currentAction = ActionType.DEATH
                currentAction = ActionType.DEATH
                mTimer = DIE_TIME
                mExplosionTimer = EXPLOSION_TIME
                if (mChannel != null) {
                    mChannel!!.value = sChannelValue
                    sChannelValue.value = true
                }
                mDead = true
            }
        }
        mTimer -= timeDelta
        if (mDead) {
            // Wait for the player to take the camera back, then steal it!
            val manager = sSystemRegistry.gameObjectManager
            if (camera != null && manager != null && camera.target === manager.player) {
                camera.target = parentObject
            }
            val offset = SINK_SPEED * timeDelta
            parentObject.position.y += offset
            mExplosionTimer -= timeDelta
            if (mExplosionTimer < 0.0f) {
                val factory = sSystemRegistry.gameObjectFactory
                if (factory != null) {
                    val x = (Math.random().toFloat() - 0.5f) * (parentObject.width * 0.75f)
                    val y = (Math.random().toFloat() - 0.5f) * (parentObject.height * 0.75f)
                    val `object` = factory.spawn(GameObjectFactory.GameObjectType.EXPLOSION_GIANT,
                            parentObject.centeredPositionX + x,
                            parentObject.centeredPositionY + y,
                            false)
                    if (`object` != null) {
                        manager!!.add(`object`)
                    }
                    mExplosionTimer = EXPLOSION_TIME
                }
            }
            if (mTimer - timeDelta <= 0.0f) {
                mTimer = 0.0f
                if (mGameEvent != -1) {
                    val hud = sSystemRegistry.hudSystem
                    if (hud != null) {
                        hud.startFade(false, 1.5f)
                        hud.sendGameEventOnFadeComplete(mGameEvent, mGameEventIndex)
                        mGameEvent = -1
                    }
                }
            }
        } else if (mTimer > 0) {
            // shake
            var delta = sin(mTimer * SHAKE_SCALE.toDouble()).toFloat()
            delta *= SHAKE_MAGNITUDE
            parentObject.position.x = mShakeStartPosition + delta
            if (mTimer - timeDelta <= 0.0f) {
                // end one step early and fix the position.
                mTimer = 0f
                parentObject.position.x = mShakeStartPosition
            }
        }
    }

    fun setChannel(channel: ChannelSystem.Channel?) {
        mChannel = channel
    }

    fun setGameEvent(event: Int, index: Int) {
        mGameEvent = event
        mGameEventIndex = index
    }

    companion object {
        const val SHAKE_TIME = 0.6f
        private const val DIE_TIME = 30.0f
        private const val EXPLOSION_TIME = 0.1f
        private const val SHAKE_MAGNITUDE = 5.0f
        private const val SHAKE_SCALE = 300.0f
        private const val CAMERA_HIT_SHAKE_MAGNITUDE = 3.0f
        private const val SINK_SPEED = -20.0f
        private val sChannelValue = ChannelBooleanValue()
    }

    init {
        reset()
        setPhaseToThis(ComponentPhases.THINK.ordinal)
    }
}
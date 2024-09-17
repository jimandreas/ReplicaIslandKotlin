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

import com.replica.replicaisland.ChannelSystem.ChannelFloatValue
import com.replica.replicaisland.SoundSystem.Sound

class ButtonAnimationComponent : GameComponent() {
    object Animation {
        // Animations
        const val UP = 0
        const val DOWN = 1
    }

    private var mChannel: ChannelSystem.Channel? = null
    private var mSprite: SpriteComponent? = null
    private val lastPressedTime: ChannelFloatValue
    private var depressSound: Sound? = null
    override fun reset() {
        mSprite = null
        mChannel = null
        lastPressedTime.value = 0.0f
        depressSound = null
    }

    override fun update(timeDelta: Float, parent: BaseObject?) {
        if (mSprite != null) {
            val parentObject = parent as GameObject
            if (parentObject.currentAction == GameObject.ActionType.HIT_REACT &&
                    parentObject.lastReceivedHitType == CollisionParameters.HitType.DEPRESS) {
                if (mSprite!!.currentAnimation == Animation.UP) {
                    val sound = sSystemRegistry.soundSystem
                    sound?.play(depressSound!!, false, SoundSystem.PRIORITY_NORMAL)
                }
                mSprite!!.playAnimation(Animation.DOWN)
                parentObject.currentAction = GameObject.ActionType.IDLE
                if (mChannel != null) {
                    val time = sSystemRegistry.timeSystem
                    lastPressedTime.value = time!!.gameTime
                    mChannel!!.value = lastPressedTime
                }
            } else {
                mSprite!!.playAnimation(Animation.UP)
            }
        }
    }

    fun setSprite(sprite: SpriteComponent?) {
        mSprite = sprite
    }

    fun setChannel(channel: ChannelSystem.Channel?) {
        mChannel = channel
    }

    fun setDepressSound(sound: Sound?) {
        depressSound = sound
    }

    init {
        setPhaseToThis(ComponentPhases.ANIMATION.ordinal)
        lastPressedTime = ChannelFloatValue()
        reset()
    }
}
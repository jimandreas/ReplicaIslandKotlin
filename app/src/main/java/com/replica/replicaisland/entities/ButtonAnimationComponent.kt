package com.replica.replicaisland.entities

import com.replica.replicaisland.mechanics.ChannelSystem
import com.replica.replicaisland.mechanics.CollisionParameters
import com.replica.replicaisland.GameComponent
import com.replica.replicaisland.SpriteComponent
import com.replica.replicaisland.core.BaseObject
import com.replica.replicaisland.core.GameObject
import com.replica.replicaisland.sound.SoundSystem

class ButtonAnimationComponent : GameComponent() {
    object Animation {
        // Animations
        const val UP = 0
        const val DOWN = 1
    }

    private var mChannel: ChannelSystem.Channel? = null
    private var mSprite: SpriteComponent? = null
    private val lastPressedTime: ChannelSystem.ChannelFloatValue
    private var depressSound: SoundSystem.Sound? = null
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
                    sound?.play(depressSound!!, false, SoundSystem.Companion.PRIORITY_NORMAL)
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

    fun setDepressSound(sound: SoundSystem.Sound?) {
        depressSound = sound
    }

    init {
        setPhaseToThis(ComponentPhases.ANIMATION.ordinal)
        lastPressedTime = ChannelSystem.ChannelFloatValue()
        reset()
    }
}
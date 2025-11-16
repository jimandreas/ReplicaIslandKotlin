package com.replica.replicaisland.entities

import com.replica.replicaisland.mechanics.ChannelSystem
import com.replica.replicaisland.GameComponent
import com.replica.replicaisland.SolidSurfaceComponent
import com.replica.replicaisland.rendering.SpriteComponent
import com.replica.replicaisland.core.BaseObject
import com.replica.replicaisland.core.GameObject
import com.replica.replicaisland.sound.SoundSystem

class DoorAnimationComponent : GameComponent() {
    object Animation {
        // Animations
        const val CLOSED = 0
        const val OPEN = 1
        const val CLOSING = 2
        const val OPENING = 3
    }

    private var mSprite: SpriteComponent? = null
    private var mState = 0
    private var mChannel: ChannelSystem.Channel? = null
    private var solidSurface: SolidSurfaceComponent? = null
    private var stayOpenTime = 0f
    private var mCloseSound: SoundSystem.Sound? = null
    private var mOpenSound: SoundSystem.Sound? = null
    override fun reset() {
        mSprite = null
        mState = STATE_CLOSED
        mChannel = null
        solidSurface = null
        stayOpenTime = DEFAULT_STAY_OPEN_TIME
        mCloseSound = null
        mOpenSound = null
    }

    private fun open(timeSinceTriggered: Float, parentObject: GameObject) {
        if (mSprite != null) {
            val openAnimationLength = mSprite!!.findAnimation(Animation.OPENING)!!.length
            if (timeSinceTriggered > openAnimationLength) {
                // snap open.
                mSprite!!.playAnimation(Animation.OPEN)
                mState = STATE_OPEN
                if (solidSurface != null) {
                    parentObject.remove(solidSurface)
                }
            } else {
                var timeOffset = timeSinceTriggered
                if (mState == STATE_CLOSING) {
                    // opening and closing animations are the same length.
                    // if we're in the middle of one and need to go to the other,
                    // we can start the new one mid-way through so that the door appears to
                    // simply reverse direction.
                    timeOffset = openAnimationLength - mSprite!!.currentAnimationTime
                } else {
                    if (solidSurface != null) {
                        parentObject.remove(solidSurface)
                    }
                }
                mState = STATE_OPENING
                mSprite!!.playAnimation(Animation.OPENING)
                mSprite!!.currentAnimationTime = timeOffset
                if (mOpenSound != null) {
                    val sound = sSystemRegistry.soundSystem
                    sound?.play(mOpenSound!!, false, SoundSystem.Companion.PRIORITY_NORMAL)
                }
            }
        }
    }

    private fun close(timeSinceTriggered: Float, parentObject: GameObject) {
        if (mSprite != null) {
            val closeAnimationLength = mSprite!!.findAnimation(Animation.CLOSING)!!.length
            if (timeSinceTriggered > stayOpenTime + closeAnimationLength) {
                // snap open.
                mSprite!!.playAnimation(Animation.CLOSED)
                mState = STATE_CLOSED
                if (solidSurface != null) {
                    parentObject.add(solidSurface as BaseObject)
                }
            } else {
                var timeOffset = timeSinceTriggered - stayOpenTime
                if (mState == STATE_OPENING) {
                    timeOffset = closeAnimationLength - mSprite!!.currentAnimationTime
                }
                mState = STATE_CLOSING
                mSprite!!.playAnimation(Animation.CLOSING)
                mSprite!!.currentAnimationTime = timeOffset
                if (mCloseSound != null) {
                    val sound = sSystemRegistry.soundSystem
                    sound?.play(mCloseSound!!, false, SoundSystem.Companion.PRIORITY_NORMAL)
                }
            }
        }
    }

    override fun update(timeDelta: Float, parent: BaseObject?) {
        if (mChannel != null) {
            if (mChannel!!.value != null && mChannel!!.value is ChannelSystem.ChannelFloatValue) {
                val lastPressedTime = (mChannel!!.value as ChannelSystem.ChannelFloatValue?)!!.value
                val time = sSystemRegistry.timeSystem
                val gameTime = time!!.gameTime
                val delta = gameTime - lastPressedTime
                if (delta < stayOpenTime
                        && (mState == STATE_CLOSED || mState == STATE_CLOSING)) {
                    open(delta, parent as GameObject)
                } else if (delta > stayOpenTime
                        && (mState == STATE_OPEN || mState == STATE_OPENING)) {
                    close(delta, parent as GameObject)
                }
            }
        }
        if (mSprite != null) {
            if (mState == STATE_OPENING && mSprite!!.animationFinished()) {
                mSprite!!.playAnimation(Animation.OPEN)
                mState = STATE_OPEN
            } else if (mState == STATE_CLOSING && mSprite!!.animationFinished()) {
                mSprite!!.playAnimation(Animation.CLOSED)
                mState = STATE_CLOSED
                if (solidSurface != null) {
                    (parent as GameObject).add(solidSurface as BaseObject)
                }
            }

            // Deal with the case where the animation and state are out of sync
            // (side-effect of possession).
            // TODO: figure out a better way to do this.
            if (mSprite!!.currentAnimation == Animation.OPENING && mState == STATE_CLOSED) {
                mSprite!!.playAnimation(Animation.CLOSING)
                mState = STATE_CLOSING
            }
        }
    }

    fun setSprite(sprite: SpriteComponent?) {
        mSprite = sprite
    }

    fun setChannel(channel: ChannelSystem.Channel?) {
        mChannel = channel
    }

    fun setSolidSurface(surface: SolidSurfaceComponent?) {
        solidSurface = surface
    }

    fun setStayOpenTime(time: Float) {
        stayOpenTime = time
    }

    fun setSounds(openSound: SoundSystem.Sound?, closeSound: SoundSystem.Sound?) {
        mOpenSound = openSound
        mCloseSound = closeSound
    }

    companion object {
        // State
        private const val STATE_CLOSED = 0
        private const val STATE_OPEN = 1
        private const val STATE_CLOSING = 2
        private const val STATE_OPENING = 3
        private const val DEFAULT_STAY_OPEN_TIME = 5.0f
    }

    init {
        setPhaseToThis(ComponentPhases.ANIMATION.ordinal)
        reset()
    }
}
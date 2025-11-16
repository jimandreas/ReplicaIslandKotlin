package com.replica.replicaisland.entities

import com.replica.replicaisland.GameComponent
import com.replica.replicaisland.core.BaseObject
import com.replica.replicaisland.sound.SoundSystem

class PlaySingleSoundComponent : GameComponent() {
    private var mSound: SoundSystem.Sound? = null
    private var soundHandle = 0
    override fun reset() {
        soundHandle = -1
        mSound = null
    }

    fun setSound(sound: SoundSystem.Sound?) {
        mSound = sound
    }

    override fun update(timeDelta: Float, parent: BaseObject?) {
        if (soundHandle == -1 && mSound != null) {
            val sound = sSystemRegistry.soundSystem
            soundHandle = sound!!.play(mSound!!, false, SoundSystem.PRIORITY_NORMAL)
        }
    }

    init {
        reset()
        setPhaseToThis(ComponentPhases.THINK.ordinal)
    }
}
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

import com.replica.replicaisland.SoundSystem.Sound

class PlaySingleSoundComponent : GameComponent() {
    private var mSound: Sound? = null
    private var soundHandle = 0
    override fun reset() {
        soundHandle = -1
        mSound = null
    }

    fun setSound(sound: Sound?) {
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
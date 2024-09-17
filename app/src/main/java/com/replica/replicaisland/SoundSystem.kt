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
@file:Suppress("unused")

package com.replica.replicaisland

import android.media.AudioManager
import android.media.SoundPool
import java.util.*

class SoundSystem : BaseObject() {
    private val soundPool: SoundPool
    private val mSounds: FixedSizeArray<Sound>
    private val searchDummy: Sound

    @set:Synchronized
    var soundEnabled = false
    private val loopingStreams: IntArray
    override fun reset() {
        soundPool.release()
        mSounds.clear()
        soundEnabled = true
        for (x in loopingStreams.indices) {
            loopingStreams[x] = -1
        }
    }

    fun load(resource: Int): Sound? {
        val index = findSound(resource)
        var result: Sound? = null
        if (index < 0) {
            // new sound.
            if (sSystemRegistry.contextParameters != null) {
                val context = sSystemRegistry.contextParameters!!.context
                result = Sound()
                result.resource = resource
                result.soundId = soundPool.load(context, resource, 1)
                mSounds.add(result)
                mSounds.sort(false)
            }
        } else {
            result = mSounds[index]
        }
        return result
    }

    @Synchronized
    fun play(sound: Sound, loop: Boolean, priority: Int): Int {
        var stream = -1
        if (soundEnabled) {
            stream = soundPool.play(sound.soundId, 1.0f, 1.0f, priority, if (loop) -1 else 0, 1.0f)
            if (loop) {
                addLoopingStream(stream)
            }
        }
        return stream
    }

    @Synchronized
    fun play(sound: Sound, loop: Boolean, priority: Int, volume: Float, rate: Float): Int {
        var stream = -1
        if (soundEnabled) {
            stream = soundPool.play(sound.soundId, volume, volume, priority, if (loop) -1 else 0, rate)
            if (loop) {
                addLoopingStream(stream)
            }
        }
        return stream
    }

    fun stop(stream: Int) {
        soundPool.stop(stream)
        removeLoopingStream(stream)
    }

    fun pause(stream: Int) {
        soundPool.pause(stream)
    }

    fun resume(stream: Int) {
        soundPool.resume(stream)
    }

    fun stopAll() {
        val count = loopingStreams.size
        for (x in count - 1 downTo 0) {
            if (loopingStreams[x] >= 0) {
                stop(loopingStreams[x])
            }
        }
    }

    // HACK: There's no way to pause an entire sound pool, but if we
    // don't do something when our parent activity is paused, looping
    // sounds will continue to play.  Rather that reproduce all the bookkeeping
    // that SoundPool does internally here, I've opted to just pause looping
    // sounds when the Activity is paused.
    fun pauseAll() {
        val count = loopingStreams.size
        for (x in 0 until count) {
            if (loopingStreams[x] >= 0) {
                pause(loopingStreams[x])
            }
        }
    }

    private fun addLoopingStream(stream: Int) {
        val count = loopingStreams.size
        for (x in 0 until count) {
            if (loopingStreams[x] < 0) {
                loopingStreams[x] = stream
                break
            }
        }
    }

    private fun removeLoopingStream(stream: Int) {
        val count = loopingStreams.size
        for (x in 0 until count) {
            if (loopingStreams[x] == stream) {
                loopingStreams[x] = -1
                break
            }
        }
    }

    private fun findSound(resource: Int): Int {
        searchDummy.resource = resource
        return mSounds.find(searchDummy, false)
    }

    class Sound : AllocationGuard() {
        var resource = 0
        var soundId = 0
    }

    /** Comparator for sounds.  */
    private class SoundComparator : Comparator<Sound?> {
        override fun compare(object1: Sound?, object2: Sound?): Int {
            var result = 0
            if (object1 == null && object2 != null) {
                result = 1
            } else if (object1 != null && object2 == null) {
                result = -1
            } else if (object1 != null && object2 != null) {
                result = object1.resource - object2.resource
            }
            return result
        }
    }

    companion object {
        private const val MAX_STREAMS = 8
        private const val MAX_SOUNDS = 32
        private val sSoundComparator = SoundComparator()
        const val PRIORITY_LOW = 0
        const val PRIORITY_NORMAL = 1
        const val PRIORITY_HIGH = 2
        const val PRIORITY_MUSIC = 3
    }

    init {
        @Suppress("DEPRECATION")
        soundPool = SoundPool(MAX_STREAMS, AudioManager.STREAM_MUSIC, 0)
        mSounds = FixedSizeArray(MAX_SOUNDS, sSoundComparator)
        searchDummy = Sound()
        loopingStreams = IntArray(MAX_STREAMS)
        for (x in loopingStreams.indices) {
            loopingStreams[x] = -1
        }
    }
}
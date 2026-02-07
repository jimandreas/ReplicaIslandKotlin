/*
 * Copyright (C) 2010 The Android Open Source Project
 * Copyright (C) 2025 Jim Andreas kotlin conversion
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

package com.replica.replicaisland.sound

import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Handler
import android.os.HandlerThread
import android.util.SparseIntArray
import com.replica.replicaisland.utils.AllocationGuard
import com.replica.replicaisland.core.BaseObject
import com.replica.replicaisland.utils.FixedSizeArray
import java.util.concurrent.atomic.AtomicInteger

class SoundSystem : BaseObject() {
    private val soundPool: SoundPool
    private val soundsArray: FixedSizeArray<Sound>
    private val searchDummy: Sound

    @set:Synchronized
    var soundEnabled = false
    private val loopingStreams: IntArray

    private val soundThread = HandlerThread("GameSound").apply { start() }
    private val soundHandler = Handler(soundThread.looper)

    private val handleCounter = AtomicInteger(1)
    private val handleToStream = SparseIntArray()  // accessed only on sound thread

    override fun reset() {
        soundHandler.removeCallbacksAndMessages(null)
        soundPool.release()
        soundsArray.clear()
        soundEnabled = true
        for (x in loopingStreams.indices) {
            loopingStreams[x] = -1
        }
        handleToStream.clear()
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
                soundsArray.add(result)
                soundsArray.sort(false)
            }
        } else {
            result = soundsArray[index]
        }
        return result
    }

    @Synchronized
    fun play(sound: Sound, loop: Boolean, priority: Int): Int {
        if (!soundEnabled) return -1
        val handle = handleCounter.getAndIncrement()
        if (!loop) {
            soundHandler.post {
                soundPool.play(sound.soundId, 1.0f, 1.0f, priority, 0, 1.0f)
            }
            return handle
        }
        soundHandler.post {
            val stream = soundPool.play(sound.soundId, 1.0f, 1.0f, priority, -1, 1.0f)
            if (stream > 0) {
                handleToStream.put(handle, stream)
                addLoopingStream(stream)
            }
        }
        return handle
    }

    @Synchronized
    fun play(sound: Sound, loop: Boolean, priority: Int, volume: Float, rate: Float): Int {
        if (!soundEnabled) return -1
        val handle = handleCounter.getAndIncrement()
        if (!loop) {
            soundHandler.post {
                soundPool.play(sound.soundId, volume, volume, priority, 0, rate)
            }
            return handle
        }
        soundHandler.post {
            val stream = soundPool.play(sound.soundId, volume, volume, priority, -1, rate)
            if (stream > 0) {
                handleToStream.put(handle, stream)
                addLoopingStream(stream)
            }
        }
        return handle
    }

    fun stop(handle: Int) {
        soundHandler.post {
            val stream = handleToStream.get(handle, -1)
            if (stream >= 0) {
                soundPool.stop(stream)
                removeLoopingStream(stream)
                handleToStream.delete(handle)
            }
        }
    }

    fun pause(handle: Int) {
        soundHandler.post {
            val stream = handleToStream.get(handle, -1)
            if (stream >= 0) {
                soundPool.pause(stream)
            }
        }
    }

    fun resume(handle: Int) {
        soundHandler.post {
            val stream = handleToStream.get(handle, -1)
            if (stream >= 0) {
                soundPool.resume(stream)
            }
        }
    }

    fun stopAll() {
        soundHandler.post {
            val count = loopingStreams.size
            for (x in count - 1 downTo 0) {
                if (loopingStreams[x] >= 0) {
                    soundPool.stop(loopingStreams[x])
                    loopingStreams[x] = -1
                }
            }
            handleToStream.clear()
        }
    }

    // HACK: There's no way to pause an entire sound pool, but if we
    // don't do something when our parent activity is paused, looping
    // sounds will continue to play.  Rather that reproduce all the bookkeeping
    // that SoundPool does internally here, I've opted to just pause looping
    // sounds when the Activity is paused.
    fun pauseAll() {
        soundHandler.post {
            val count = loopingStreams.size
            for (x in 0 until count) {
                if (loopingStreams[x] >= 0) {
                    soundPool.pause(loopingStreams[x])
                }
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
        return soundsArray.find(searchDummy, false)
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
//        const val PRIORITY_LOW = 0
        const val PRIORITY_NORMAL = 1
        const val PRIORITY_HIGH = 2
//        const val PRIORITY_MUSIC = 3
    }

    init {
        //soundPool = SoundPool(MAX_STREAMS, AudioManager.STREAM_MUSIC, 0)
        soundsArray = FixedSizeArray(MAX_SOUNDS, SoundComparator())
        searchDummy = Sound()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(MAX_STREAMS)
            .setAudioAttributes(audioAttributes)
            .build()

        loopingStreams = IntArray(MAX_STREAMS)
        for (x in loopingStreams.indices) {
            loopingStreams[x] = -1
        }
        soundPool.setOnLoadCompleteListener { soundPool, sampleId, status ->
            if (status == 0) {
                // Play each sound with zero volume to warm up the pipeline.
                // This reduces sound latency in the game by a lot!
                // Use PRIORITY_NORMAL (1) to ensure Android doesn't skip this play.
                soundPool.play(sampleId, 0f, 0f, PRIORITY_NORMAL, 0, 1f)
            }
        }
    }
}

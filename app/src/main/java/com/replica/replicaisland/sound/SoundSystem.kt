package com.replica.replicaisland.sound

import android.media.AudioAttributes
import android.media.SoundPool
import com.replica.replicaisland.AllocationGuard
import com.replica.replicaisland.BaseObject
import com.replica.replicaisland.FixedSizeArray

class SoundSystem : BaseObject() {
    private val soundPool: SoundPool
    private val soundsArray: FixedSizeArray<Sound>
    private val searchDummy: Sound

    @set:Synchronized
    var soundEnabled = false
    private val loopingStreams: IntArray
    override fun reset() {
        soundPool.release()
        soundsArray.clear()
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
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
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
                soundPool.play(sampleId, 0f, 0f, 0, 0, 1f)
            }
        }
    }
}
package com.replica.replicaisland.rendering

import com.replica.replicaisland.utils.AnimationFrame
import com.replica.replicaisland.utils.FixedSizeArray
import com.replica.replicaisland.core.PhasedObject
import java.util.Arrays

/**
 * Describes a single animation for a sprite.
 */
class SpriteAnimation(animationId: Int, frameCount: Int) : PhasedObject() {
    private val mFrames: FixedSizeArray<AnimationFrame> = FixedSizeArray(frameCount)
    private val frameStartTimes: FloatArray = FloatArray(frameCount)
    var loop: Boolean = false
    var length: Float
        private set

    fun getFrame(animationTime: Float): AnimationFrame? {
        var result: AnimationFrame? = null
        val length = length
        if (length > 0.0f) {
            val frames = mFrames
            // TODO: assert(frames.count == frames.getCapacity())
            val frameCount = frames.count
            result = frames[frameCount - 1]
            if (frameCount > 1) {
                var currentTime = 0.0f
                var cycleTime = animationTime
                if (loop) {
                    cycleTime = animationTime % length
                }
                if (cycleTime < length) {
                    // When there are very few frames it's actually slower to do a binary search
                    // of the frame list.  So we'll use a linear search for small animations
                    // and only pull the binary search out when the frame count is large.
                    if (frameStartTimes.size > LINEAR_SEARCH_CUTOFF) {
                        var index = Arrays.binarySearch(frameStartTimes, cycleTime)
                        if (index < 0) {
                            index = -(index + 1) - 1
                        }
                        result = frames[index]
                    } else {
                        for (x in 0 until frameCount) {
                            val frame = frames[x]
                            currentTime += frame!!.holdTime
                            if (currentTime > cycleTime) {
                                result = frame
                                break
                            }
                        }
                    }
                }
            }
        }
        return result
    }

    fun addFrame(frame: AnimationFrame) {
        frameStartTimes[mFrames.count] = length
        mFrames.add(frame)
        length += frame.holdTime
    }

    companion object {
        private const val LINEAR_SEARCH_CUTOFF = 16
    }

    init {
        length = 0.0f
        setPhaseToThis(animationId)
    }
}
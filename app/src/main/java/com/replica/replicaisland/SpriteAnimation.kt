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
package com.replica.replicaisland

import java.util.*

/**
 * Describes a single animation for a sprite.
 */
class SpriteAnimation(animationId: Int, frameCount: Int) : PhasedObject() {
    private val mFrames: FixedSizeArray<AnimationFrame> = FixedSizeArray(frameCount)
    private val mFrameStartTimes: FloatArray = FloatArray(frameCount)
    var loop: Boolean
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
                    if (mFrameStartTimes.size > LINEAR_SEARCH_CUTOFF) {
                        var index = Arrays.binarySearch(mFrameStartTimes, cycleTime)
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
        mFrameStartTimes[mFrames.count] = length
        mFrames.add(frame)
        length += frame.holdTime
    }

    companion object {
        private const val LINEAR_SEARCH_CUTOFF = 16
    }

    init {
        loop = false
        length = 0.0f
        setPhaseToThis(animationId)
    }
}
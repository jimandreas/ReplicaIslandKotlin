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
@file:Suppress("CascadeIf")

package com.replica.replicaisland

import com.replica.replicaisland.Lerp.ease

/**
 * Maintains a canonical time step, in seconds, for the entire game engine.  This time step
 * represents real changes in time but is only updated once per frame.
 */
// TODO: time distortion effects could go here, or they could go into a special object manager.
class TimeSystem : BaseObject() {
    var gameTime = 0f
        private set
    var realTime = 0f
        private set
    private var freezeDelay = 0f
    var frameDelta = 0f
        private set
    private var realTimeFrameDelta = 0f
    private var targetScale = 0f
    private var scaleDuration = 0f
    private var scaleStartTime = 0f
    private var easeScale = false
    override fun reset() {
        gameTime = 0.0f
        realTime = 0.0f
        freezeDelay = 0.0f
        frameDelta = 0.0f
        realTimeFrameDelta = 0.0f
        targetScale = 1.0f
        scaleDuration = 0.0f
        scaleStartTime = 0.0f
        easeScale = false
    }

    override fun update(timeDelta: Float, parent: BaseObject?) {
        realTime += timeDelta
        realTimeFrameDelta = timeDelta
        if (freezeDelay > 0.0f) {
            freezeDelay -= timeDelta
            frameDelta = 0.0f
        } else {
            var scale = 1.0f
            if (scaleStartTime > 0.0f) {
                val scaleTime = realTime - scaleStartTime
                if (scaleTime > scaleDuration) {
                    scaleStartTime = 0f
                } else {
                    scale = if (easeScale) {
                        if (scaleTime <= EASE_DURATION) {
                            // ease in
                            ease(1.0f, targetScale, EASE_DURATION, scaleTime)
                        } else if (scaleDuration - scaleTime < EASE_DURATION) {
                            // ease out
                            val easeOutTime = EASE_DURATION - (scaleDuration - scaleTime)
                            ease(targetScale, 1.0f, EASE_DURATION, easeOutTime)
                        } else {
                            targetScale
                        }
                    } else {
                        targetScale
                    }
                }
            }
            gameTime += timeDelta * scale
            frameDelta = timeDelta * scale
        }
    }

    fun freeze(seconds: Float) {
        freezeDelay = seconds
    }

    fun appyScale(scaleFactor: Float, duration: Float, ease: Boolean) {
        targetScale = scaleFactor
        scaleDuration = duration
        easeScale = ease
        if (scaleStartTime <= 0.0f) {
            scaleStartTime = realTime
        }
    }

    companion object {
        private const val EASE_DURATION = 0.5f
    }

    init {
        reset()
    }
}
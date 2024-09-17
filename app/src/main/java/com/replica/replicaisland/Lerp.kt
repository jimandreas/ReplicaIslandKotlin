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

object Lerp {
    @JvmStatic
    fun lerp(start: Float, target: Float, duration: Float, timeSinceStart: Float): Float {
        var value = start
        if (timeSinceStart > 0.0f && timeSinceStart < duration) {
            val range = target - start
            val percent = timeSinceStart / duration
            value = start + range * percent
        } else if (timeSinceStart >= duration) {
            value = target
        }
        return value
    }

    @JvmStatic
    fun ease(start: Float, target: Float, duration: Float, timeSinceStart: Float): Float {
        var value = start
        if (timeSinceStart > 0.0f && timeSinceStart < duration) {
            val range = target - start
            val percent = timeSinceStart / (duration / 2.0f)
            value = if (percent < 1.0f) {
                start + range / 2.0f * percent * percent * percent
            } else {
                val shiftedPercent = percent - 2.0f
                start + range / 2.0f *
                        (shiftedPercent * shiftedPercent * shiftedPercent + 2.0f)
            }
        } else if (timeSinceStart >= duration) {
            value = target
        }
        return value
    }
}
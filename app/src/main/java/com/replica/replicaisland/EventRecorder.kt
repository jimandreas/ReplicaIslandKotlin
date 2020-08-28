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

class EventRecorder : BaseObject() {
    private val mLastDeathPosition = Vector2()

    @get:Synchronized
    @set:Synchronized
    var lastEnding = -1

    @get:Synchronized
    var robotsDestroyed = 0
        private set

    @get:Synchronized
    var pearlsCollected = 0
        private set

    @get:Synchronized
    var pearlsTotal = 0
        private set

    override fun reset() {
        robotsDestroyed = 0
        pearlsCollected = 0
        pearlsTotal = 0
    }

    @get:Synchronized
    @set:Synchronized
    var lastDeathPosition: Vector2?
        get() = mLastDeathPosition
        set(position) {
            mLastDeathPosition.set(position!!)
        }

    @Synchronized
    fun incrementEventCounter(event: Int) {
        if (event == COUNTER_ROBOTS_DESTROYED) {
            robotsDestroyed++
        } else if (event == COUNTER_PEARLS_COLLECTED) {
            pearlsCollected++
        } else if (event == COUNTER_PEARLS_TOTAL) {
            pearlsTotal++
        }
    }

    companion object {
        const val COUNTER_ROBOTS_DESTROYED = 0
        const val COUNTER_PEARLS_COLLECTED = 1
        const val COUNTER_PEARLS_TOTAL = 2
    }
}
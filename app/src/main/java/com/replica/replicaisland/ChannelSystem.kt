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

import java.util.*

class ChannelSystem : BaseObject() {
    private val mChannels: FixedSizeArray<Channel>
    private val searchDummy: Channel
    private var registeredChannelCount: Int
    override fun reset() {
        for (x in 0 until CHANNEL_COUNT) {
            mChannels[x]!!.name = null
            mChannels[x]!!.value = null
        }
        registeredChannelCount = 0
    }

    fun registerChannel(name: String?): Channel? {
        var result: Channel? = null
        searchDummy.name = name
        val index = mChannels.find(searchDummy, false)
        if (index == -1) {
            // Add a new channel.
            // TODO: assert(registeredChannelCount < CHANNEL_COUNT) { "Channel pool exhausted!" }
            if (registeredChannelCount < CHANNEL_COUNT) {
                result = mChannels[registeredChannelCount]
                registeredChannelCount++
                result!!.name = name
                mChannels.sort(true)
            }
        } else {
            result = mChannels[index]
        }
        return result
    }

    class Channel {
        var name: String? = null
        @JvmField
        var value: Any? = null
    }

    class ChannelFloatValue {
        @JvmField
        var value = 0f
    }

    class ChannelBooleanValue {
        @JvmField
        var value = false
    }

    /** Comparator for channels.  */
    private class ChannelComparator : Comparator<Channel?> {
        override fun compare(object1: Channel?, object2: Channel?): Int {
            var result = 0
            if (object1 == null && object2 != null) {
                result = 1
            } else if (object1 != null && object2 == null) {
                result = -1
            } else if (object1 != null && object2 != null) {
                if (object1.name == null && object2.name != null) {
                    result = 1
                } else if (object1.name != null && object2.name == null) {
                    result = -1
                } else if (object1.name != null && object2.name != null) {
                    result = object1.name!!.compareTo(object2.name!!)
                }
            }
            return result
        }
    }

    companion object {
        private const val CHANNEL_COUNT = 8
        private val sChannelComparator = ChannelComparator()
    }

    init {
        mChannels = FixedSizeArray(CHANNEL_COUNT)
        mChannels.setComparator(sChannelComparator)
        searchDummy = Channel()
        for (x in 0 until CHANNEL_COUNT) {
            mChannels.add(Channel())
        }
        registeredChannelCount = 0
    }
}
package com.replica.replicaisland.mechanics

import com.replica.replicaisland.FixedSizeArray
import com.replica.replicaisland.core.BaseObject
import java.util.Comparator

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
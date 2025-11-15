package com.replica.replicaisland.mechanics

import com.replica.replicaisland.Vector2
import com.replica.replicaisland.core.BaseObject

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
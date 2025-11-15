package com.replica.replicaisland.entities

import com.replica.replicaisland.utils.TObjectPool

class HitPointPool : TObjectPool<HitPoint?>() {
    override fun fill() {
        val size = fetchSize()
        for (x in 0 until size) {
            fetchAvailable()!!.add(HitPoint())
        }
    }

    override fun release(entry: Any) {
        (entry as HitPoint).reset()
        super.release(entry)
    }
}
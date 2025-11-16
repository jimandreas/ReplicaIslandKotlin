package com.replica.replicaisland.utils

/**
 * A pool of 2D vectors.
 */
class VectorPool : TObjectPool<Vector2?>() {
    override fun fill() {
        for (x in 0 until fetchSize()) {
            fetchAvailable()!!.add(Vector2())
        }
    }

    override fun release(entry: Any) {
        (entry as Vector2).zero()
        super.release(entry)
    }

    /** Allocates a vector and assigns the value of the passed source vector to it.  */
    fun allocate(source: Vector2?): Vector2 {
        val entry = super.allocate()!!
        entry.set(source!!)
        return entry
    }
}
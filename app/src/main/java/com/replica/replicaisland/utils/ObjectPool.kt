package com.replica.replicaisland.utils

import com.replica.replicaisland.core.BaseObject

/**
 * A general-purpose pool of objects.  Objects in the pool are allocated up front and then
 * passed out to requesting objects until the pool is exhausted (at which point an error is thrown).
 * Code that requests objects from the pool should return them to the pool when they are finished.
 * This class is abstract; derivations need to implement the fill() function to fill the pool, and
 * may wish to override release() to clear state on objects as they are returned to the pool.
 */
abstract class ObjectPool : BaseObject {
    private var available: FixedSizeArray<Any>? = null
    private var mSize = 0

    constructor() : super() {
        setTheSize(DEFAULT_SIZE)
    }

    constructor(size: Int) : super() {
        setTheSize(size)
    }

    override fun reset() {}

    /** Allocates an object from the pool  */
    protected open fun allocate(): Any? {
        return available!!.removeLast() ?: error("Object pool of type " + javaClass.simpleName
                + " exhausted!!")
    }

    /** Returns an object to the pool.  */
    open fun release(entry: Any) {
        available!!.add(entry)
    }

    /** Returns the number of pooled elements that have been allocated but not released.  */
    fun fetchAllocatedCount(): Int {
        return available!!.getCapacity() - available!!.count
    }

    private fun setTheSize(size: Int) {
        mSize = size
        available = FixedSizeArray(mSize)
        fill()
    }

    protected abstract fun fill()
    protected fun fetchAvailable(): FixedSizeArray<Any>? {
        return available
    }

    fun fetchSize(): Int {
        return mSize
    }

    companion object {
        private const val DEFAULT_SIZE = 32
    }
}
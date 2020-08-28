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

/**
 * A general-purpose pool of objects.  Objects in the pool are allocated up front and then
 * passed out to requesting objects until the pool is exhausted (at which point an error is thrown).
 * Code that requests objects from the pool should return them to the pool when they are finished.
 * This class is abstract; derivations need to implement the fill() function to fill the pool, and
 * may wish to override release() to clear state on objects as they are returned to the pool.
 */
abstract class ObjectPool : BaseObject {
    private var mAvailable: FixedSizeArray<Any>? = null
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
        return mAvailable!!.removeLast() ?: error("Object pool of type " + javaClass.simpleName
                + " exhausted!!")
    }

    /** Returns an object to the pool.  */
    open fun release(entry: Any) {
        mAvailable!!.add(entry)
    }

    /** Returns the number of pooled elements that have been allocated but not released.  */
    fun fetchAllocatedCount(): Int {
        return mAvailable!!.getCapacity() - mAvailable!!.count
    }

    private fun setTheSize(size: Int) {
        mSize = size
        mAvailable = FixedSizeArray(mSize)
        fill()
    }

    protected abstract fun fill()
    protected fun fetchAvailable(): FixedSizeArray<Any>? {
        return mAvailable
    }

    fun fetchSize(): Int {
        return mSize
    }

    companion object {
        private const val DEFAULT_SIZE = 32
    }
}
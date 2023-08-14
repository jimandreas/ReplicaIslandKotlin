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
@file:Suppress("UNCHECKED_CAST")

package com.replica.replicaisland

/**
 * ObjectManagers are "group nodes" in the game graph.  They contain child objects, and updating
 * an object manager invokes update on its children.  ObjectManagers themselves are derived from
 * BaseObject, so they may be strung together into a hierarchy of objects.  ObjectManager may
 * be specialized to implement special types of traversals (e.g. PhasedObjectManager sorts its
 * children).
 */
open class ObjectManager : BaseObject {
    private var mObjects: FixedSizeArray<BaseObject?>
    private var pendingAdditions: FixedSizeArray<BaseObject>
    private var pendingRemovals: FixedSizeArray<BaseObject?>

    constructor() : super() {
        mObjects = FixedSizeArray(DEFAULT_ARRAY_SIZE)
        pendingAdditions = FixedSizeArray(DEFAULT_ARRAY_SIZE)
        pendingRemovals = FixedSizeArray(DEFAULT_ARRAY_SIZE)
    }

    constructor(arraySize: Int) : super() {
        mObjects = FixedSizeArray(arraySize)
        pendingAdditions = FixedSizeArray(arraySize)
        pendingRemovals = FixedSizeArray(arraySize)
    }

    override fun reset() {
        commitUpdates()
        val count = mObjects.count
        for (i in 0 until count) {
            val `object` = mObjects[i]
            `object`!!.reset()
        }
    }

    open fun commitUpdates() {
        val additionCount = pendingAdditions.count
        if (additionCount > 0) {
            val additionsArray: Array<Any?> = pendingAdditions.array  as Array<Any?>
            for (i in 0 until additionCount) {
                val `object` = additionsArray[i] as BaseObject?
                mObjects.add(`object`)
            }
            pendingAdditions.clear()
        }
        val removalCount = pendingRemovals.count
        if (removalCount > 0) {
            val removalsArray: Array<Any?> = pendingRemovals.array  as Array<Any?>
            for (i in 0 until removalCount) {
                val `object` = removalsArray[i] as BaseObject?
                mObjects.remove(`object`, true)
            }
            pendingRemovals.clear()
        }
    }

    override fun update(timeDelta: Float, parent: BaseObject?) {
        commitUpdates()
        val count = mObjects.count
        if (count > 0) {
            val objectArray: Array<Any?> = mObjects.array  as Array<Any?>
            for (i in 0 until count) {
                val thing = objectArray[i] as BaseObject?
                thing!!.update(timeDelta, this)
            }
        }
    }

    fun fetchObjects(): FixedSizeArray<BaseObject?> {
        return mObjects
    }

    fun fetchCount(): Int {
        return mObjects.count
    }

    /** Returns the count after the next commitUpdates() is called.  */
    fun fetchConcreteCount(): Int {
        return mObjects.count + pendingAdditions.count - pendingRemovals.count
    }

    fun fetch(index: Int): BaseObject? {
        return mObjects[index]
    }

    open fun add(thing: BaseObject) {
        pendingAdditions.add(thing)
    }

    open fun remove(thing: BaseObject?) {
        pendingRemovals.add(thing)
    }

    fun removeAll() {
        val count = mObjects.count
        val objectArray: Array<Any?> = mObjects.array  as Array<Any?>
        for (i in 0 until count) {
            pendingRemovals.add(objectArray[i] as BaseObject?)
        }
        pendingAdditions.clear()
    }

    /**
     * Finds a child object by its type.  Note that this may invoke the class loader and therefore
     * may be slow.
     * @param classObject The class type to search for (e.g. BaseObject.class).
     * @return
     */
    fun <T> findByClass(classObject: Class<T>): T? {
        val count = mObjects.count
        for (i in 0 until count) {
            val currentObject = mObjects[i]
            if (currentObject!!.javaClass == classObject) {
                return currentObject as T
            }
        }
        return null
    }

    protected fun fetchPendingObjects(): FixedSizeArray<BaseObject> {
        return pendingAdditions
    }

    companion object {
        protected const val DEFAULT_ARRAY_SIZE = 64
    }
}
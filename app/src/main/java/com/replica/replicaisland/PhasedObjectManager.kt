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
@file:Suppress("ControlFlowWithEmptyBody")

package com.replica.replicaisland

import java.util.*

/**
 * A derivation of ObjectManager that sorts its children if they are of type PhasedObject.
 * Sorting is performed on add.
 */
open class PhasedObjectManager : ObjectManager {
    private var mDirty: Boolean
    private var mSearchDummy // A dummy object allocated up-front for searching by phase.
            : PhasedObject

    constructor() : super() {
        mDirty = false
        fetchObjects().setComparator(sPhasedObjectComparator)
        fetchPendingObjects().setComparator(sPhasedObjectComparator)
        mSearchDummy = PhasedObject()
    }

    constructor(arraySize: Int) : super(arraySize) {
        mDirty = false
        fetchObjects().setComparator(sPhasedObjectComparator)
        fetchPendingObjects().setComparator(sPhasedObjectComparator)
        mSearchDummy = PhasedObject()
    }

    override fun commitUpdates() {
        super.commitUpdates()
        if (mDirty) {
            fetchObjects().sort(true)
            mDirty = false
        }
    }

    override fun add(thing: BaseObject) {
        if (thing is PhasedObject) {
            super.add(thing)
            mDirty = true
        } else {
            // The only reason to restrict PhasedObjectManager to PhasedObjects is so that
            // the PhasedObjectComparator can assume all of its contents are PhasedObjects and
            // avoid calling instanceof every time.
            // TODO 2 : fix assert(false) { "Can't add a non-PhasedObject to a PhasedObjectManager!" }
        }
    }

    fun find(phase: Int): BaseObject? {
        mSearchDummy.setPhaseToThis(phase)
        var index = fetchObjects().find(mSearchDummy, false)
        var result: BaseObject? = null
        if (index != -1) {
            result = fetchObjects()[index]
        } else {
            index = fetchPendingObjects().find(mSearchDummy, false)
            if (index != -1) {
                result = fetchPendingObjects()[index]
            }
        }
        return result
    }

    /** Comparator for phased objects.  */
    private class PhasedObjectComparator : Comparator<BaseObject?> {
        override fun compare(object1: BaseObject?, object2: BaseObject?): Int {
            var result = 0
            if (object1 != null && object2 != null) {
                result = (object1 as PhasedObject).phase - (object2 as PhasedObject).phase
            } else if (object1 == null && object2 != null) {
                result = 1
            } else if (object2 == null && object1 != null) {
                result = -1
            }
            return result
        }
    }

    companion object {
        private val sPhasedObjectComparator = PhasedObjectComparator()
    }
}
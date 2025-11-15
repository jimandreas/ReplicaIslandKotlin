package com.replica.replicaisland.core

import com.replica.replicaisland.ObjectManager
import java.util.Comparator

/**
 * A derivation of ObjectManager that sorts its children if they are of type PhasedObject.
 * Sorting is performed on add.
 */
open class PhasedObjectManager : ObjectManager {
    private var dirty: Boolean
    private var searchDummy // A dummy object allocated up-front for searching by phase.
            : PhasedObject

    constructor() : super() {
        dirty = false
        fetchObjects().setComparator(sPhasedObjectComparator)
        fetchPendingObjects().setComparator(sPhasedObjectComparator)
        searchDummy = PhasedObject()
    }

    constructor(arraySize: Int) : super(arraySize) {
        dirty = false
        fetchObjects().setComparator(sPhasedObjectComparator)
        fetchPendingObjects().setComparator(sPhasedObjectComparator)
        searchDummy = PhasedObject()
    }

    override fun commitUpdates() {
        super.commitUpdates()
        if (dirty) {
            fetchObjects().sort(true)
            dirty = false
        }
    }

    override fun add(thing: BaseObject) {
        if (thing is PhasedObject) {
            super.add(thing)
            dirty = true
        } else {
            // The only reason to restrict PhasedObjectManager to PhasedObjects is so that
            // the PhasedObjectComparator can assume all of its contents are PhasedObjects and
            // avoid calling instanceof every time.
            // TODO 2 : fix assert(false) { "Can't add a non-PhasedObject to a PhasedObjectManager!" }
        }
    }

    fun find(phase: Int): BaseObject? {
        searchDummy.setPhaseToThis(phase)
        var index = fetchObjects().find(searchDummy, false)
        var result: BaseObject? = null
        if (index != -1) {
            result = fetchObjects()[index]
        } else {
            index = fetchPendingObjects().find(searchDummy, false)
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
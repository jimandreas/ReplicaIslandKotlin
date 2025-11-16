package com.replica.replicaisland.core

/**
 * A basic object that adds an execution phase.  When PhasedObjects are combined with
 * PhasedObjectManagers, objects within the manager will be updated by phase.
 */
open class PhasedObject  // so that the function overhead of an getter is non-trivial.
    : BaseObject() {
    
    var phase // This is public because the phased is accessed extremely often, so much
            = 0

    override fun reset() {}
    // refactored to not override the built-in set function
    fun setPhaseToThis(phaseValue: Int) {
        phase = phaseValue
    }
}
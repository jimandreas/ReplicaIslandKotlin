package com.replica.replicaisland.utils

import com.replica.replicaisland.ui.DebugLog

/**
 * AllocationGuard is a utility class for tracking down memory leaks.  It implements a
 * "checkpoint" memory scheme.  After the static sGuardActive flag has been set, any further
 * allocation of AllocationGuard or its derivatives will cause an error log entry.  Note
 * that AllocationGuard requires all of its derivatives to call super() in their constructor.
 */
open class AllocationGuard {
    companion object {
        @JvmField
        var sGuardActive = false
    }

    init {
        if (sGuardActive) {
            // An allocation has occurred while the guard is active!  Report it.
            DebugLog.Companion.e("AllocGuard", "An allocation of type " + this.javaClass.name
                    + " occurred while the AllocGuard is active.")
        }
    }
}
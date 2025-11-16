package com.replica.replicaisland.ui

import android.app.Activity
import java.lang.reflect.Method

object UIConstants {
    // Some versions of Android can support custom Activity transitions.
    // If this method isn't null, we can use them.
    @JvmField
    var mOverridePendingTransition: Method? = null

    init {
        try {
            mOverridePendingTransition = Activity::class.java.getMethod(
                    "mOverridePendingTransition", Integer.TYPE, Integer.TYPE)
            /* success, this is a newer device */
        } catch (nsme: NoSuchMethodException) {
            /* failure, must be older device */
        }
    }
}
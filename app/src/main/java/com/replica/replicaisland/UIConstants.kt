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
                    "overridePendingTransition", *arrayOf<Class<*>>(Integer.TYPE, Integer.TYPE))
            /* success, this is a newer device */
        } catch (nsme: NoSuchMethodException) {
            /* failure, must be older device */
        }
    }
}
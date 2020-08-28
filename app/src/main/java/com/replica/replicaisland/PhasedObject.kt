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
 * A basic object that adds an execution phase.  When PhasedObjects are combined with
 * PhasedObjectManagers, objects within the manager will be updated by phase.
 */
open class PhasedObject  // so that the function overhead of an getter is non-trivial.
    : BaseObject() {
    @JvmField
    var phase // This is public because the phased is accessed extremely often, so much
            = 0

    override fun reset() {}
    // refactored to not override the built-in set function
    fun setPhaseToThis(phaseValue: Int) {
        phase = phaseValue
    }
}
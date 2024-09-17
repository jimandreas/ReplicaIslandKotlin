/*
 * Copyright (C) 2010 The Android Open Source Project
 * Copyright (C) 2024 Jim Andreas kotlin conversion and updating
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

@file:Suppress("unused")
package com.replica.replicaisland

/**
 * A game component that implements gravity.  Adding this component to a game object will cause
 * it to be pulled down towards the ground.
 */
class GravityComponent : GameComponent() {
    val gravity: Vector2
    private val scaledGravity: Vector2
    override fun reset() {
        gravity.set(sDefaultGravity)
    }

    override fun update(timeDelta: Float, parent: BaseObject?) {
        scaledGravity.set(gravity)
        scaledGravity.multiply(timeDelta)
        (parent as GameObject).velocity.add(scaledGravity)
    }

    fun setGravityMultiplier(multiplier: Float) {
        gravity.set(sDefaultGravity)
        gravity.multiply(multiplier)
    }

    companion object {
        private val sDefaultGravity = Vector2(0.0f, -400.0f)
    }

    init {
        gravity = Vector2(sDefaultGravity)
        scaledGravity = Vector2()
        setPhaseToThis(ComponentPhases.PHYSICS.ordinal)
    }
}
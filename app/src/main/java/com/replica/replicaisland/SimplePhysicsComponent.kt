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

/** A light-weight physics implementation for use with non-complex characters (enemies, etc).  */
class SimplePhysicsComponent : GameComponent() {
    private var mBounciness = 0f
    override fun reset() {
        mBounciness = DEFAULT_BOUNCINESS
    }

    fun setBounciness(bounciness: Float) {
        mBounciness = bounciness
    }

    override fun update(timeDelta: Float, parent: BaseObject?) {
        val parentObject = parent as GameObject
        val impulse = parentObject!!.impulse
        var velocityX = parentObject.velocity.x + impulse.x
        var velocityY = parentObject.velocity.y + impulse.y
        if (parentObject.touchingCeiling() && velocityY > 0.0f
                || parentObject.touchingGround() && velocityY < 0.0f) {
            velocityY = -velocityY * mBounciness
            if (Utils.close(velocityY, 0.0f)) {
                velocityY = 0.0f
            }
        }
        if (parentObject.touchingRightWall() && velocityX > 0.0f
                || parentObject.touchingLeftWall() && velocityX < 0.0f) {
            velocityX = -velocityX * mBounciness
            if (Utils.close(velocityX, 0.0f)) {
                velocityX = 0.0f
            }
        }
        parentObject.velocity[velocityX] = velocityY
        impulse.zero()
    }

    companion object {
        private const val DEFAULT_BOUNCINESS = 0.1f
    }

    init {
        setPhaseToThis(ComponentPhases.POST_PHYSICS.ordinal)
        reset()
    }
}
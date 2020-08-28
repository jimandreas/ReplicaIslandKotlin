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
@file:Suppress("unused", "IfThenToSafeAccess")

package com.replica.replicaisland

class FixedAnimationComponent : GameComponent() {
    private var mAnimationIndex = 0
    override fun reset() {
        mAnimationIndex = 0
    }

    override fun update(timeDelta: Float, parent: BaseObject?) {
        // We look up the sprite component each frame so that this component can be shared.
        val parentObject = parent as GameObject
        val sprite = parentObject.findByClass(SpriteComponent::class.java)
        if (sprite != null) {
            sprite.playAnimation(mAnimationIndex)
        }
    }

    fun setAnimation(index: Int) {
        mAnimationIndex = index
    }

    init {
        setPhaseToThis(ComponentPhases.ANIMATION.ordinal)
        reset()
    }
}
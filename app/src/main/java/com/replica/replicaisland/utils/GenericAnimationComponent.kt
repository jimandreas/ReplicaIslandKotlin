/*
 * Copyright (C) 2010 The Android Open Source Project
 * Copyright (C) 2025 Jim Andreas kotlin conversion
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

package com.replica.replicaisland.utils

import com.replica.replicaisland.GameComponent
import com.replica.replicaisland.core.BaseObject
import com.replica.replicaisland.core.GameObject
import com.replica.replicaisland.rendering.SpriteComponent

class GenericAnimationComponent : GameComponent() {
    private var mSprite: SpriteComponent? = null
    override fun reset() {
        mSprite = null
    }

    override fun update(timeDelta: Float, parent: BaseObject?) {
        if (mSprite != null) {
            val parentObject = parent as GameObject
            if (parentObject.facingDirection.x != 0.0f && parentObject.velocity.x != 0.0f) {
                parentObject.facingDirection.x = Utils.sign(parentObject.velocity.x).toFloat()
            }
            when (parentObject.currentAction) {
                GameObject.ActionType.IDLE -> mSprite!!.playAnimation(Animation.IDLE)
                GameObject.ActionType.MOVE -> mSprite!!.playAnimation(Animation.MOVE)
                GameObject.ActionType.ATTACK -> mSprite!!.playAnimation(Animation.ATTACK)
                GameObject.ActionType.HIT_REACT -> mSprite!!.playAnimation(Animation.HIT_REACT)
                GameObject.ActionType.DEATH -> mSprite!!.playAnimation(Animation.DEATH)
                GameObject.ActionType.HIDE -> mSprite!!.playAnimation(Animation.HIDE)
                GameObject.ActionType.FROZEN -> mSprite!!.playAnimation(Animation.FROZEN)
                GameObject.ActionType.INVALID -> mSprite!!.playAnimation(-1)
                else -> mSprite!!.playAnimation(-1)
            }
        }
    }

    fun setSprite(sprite: SpriteComponent?) {
        mSprite = sprite
    }

    object Animation {
        const val IDLE = 0
        const val MOVE = 1
        const val ATTACK = 2
        const val HIT_REACT = 3
        const val DEATH = 4
        const val HIDE = 5
        const val FROZEN = 6
    }

    init {
        setPhaseToThis(ComponentPhases.ANIMATION.ordinal)
        reset()
    }
}
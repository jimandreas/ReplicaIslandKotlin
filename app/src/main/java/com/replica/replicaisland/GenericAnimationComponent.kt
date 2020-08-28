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

import com.replica.replicaisland.GameObject.ActionType

class GenericAnimationComponent : GameComponent() {
    private var mSprite: SpriteComponent? = null
    override fun reset() {
        mSprite = null
    }

    override fun update(timeDelta: Float, parent: BaseObject?) {
        if (mSprite != null) {
            val parentObject = parent as GameObject
            if (parentObject!!.facingDirection.x != 0.0f && parentObject.velocity.x != 0.0f) {
                parentObject.facingDirection.x = Utils.sign(parentObject.velocity.x).toFloat()
            }
            when (parentObject.currentAction) {
                ActionType.IDLE -> mSprite!!.playAnimation(Animation.IDLE)
                ActionType.MOVE -> mSprite!!.playAnimation(Animation.MOVE)
                ActionType.ATTACK -> mSprite!!.playAnimation(Animation.ATTACK)
                ActionType.HIT_REACT -> mSprite!!.playAnimation(Animation.HIT_REACT)
                ActionType.DEATH -> mSprite!!.playAnimation(Animation.DEATH)
                ActionType.HIDE -> mSprite!!.playAnimation(Animation.HIDE)
                ActionType.FROZEN -> mSprite!!.playAnimation(Animation.FROZEN)
                ActionType.INVALID -> mSprite!!.playAnimation(-1)
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
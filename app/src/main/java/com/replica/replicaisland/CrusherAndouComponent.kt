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
package com.replica.replicaisland

class CrusherAndouComponent : GameComponent() {
    private var mSwap: ChangeComponentsComponent? = null
    override fun reset() {
        mSwap = null
    }

    override fun update(timeDelta: Float, parent: BaseObject?) {
        val parentObject = parent as GameObject
        if (mSwap!!.currentlySwapped) {
            if (parentObject.touchingGround()) {
                parentObject.currentAction = GameObject.ActionType.IDLE
            }
        } else {
            val input = sSystemRegistry.inputSystem
            if (input!!.fetchTouchScreen().getTriggered(sSystemRegistry.timeSystem!!.gameTime)) {
                parentObject.currentAction = GameObject.ActionType.ATTACK
                mSwap!!.activate(parentObject)
            }
        }
    }

    fun setSwap(swap: ChangeComponentsComponent?) {
        mSwap = swap
    }

    init {
        setPhaseToThis(ComponentPhases.THINK.ordinal)
        reset()
    }
}
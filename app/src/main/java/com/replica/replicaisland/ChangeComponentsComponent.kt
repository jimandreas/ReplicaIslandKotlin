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

import com.replica.replicaisland.GameObject.ActionType

/**
 * A game component that can swap other components in and out of its parent game object.  The
 * purpose of the ChangeComponentsComponent is to allow game objects to have different "modes"
 * defined by different combinations of GameComponents.  ChangeComponentsComponent manages the
 * switching in and out of those modes by activating and deactivating specific game components.
 */
class ChangeComponentsComponent : GameComponent() {
    private var componentsToInsert: FixedSizeArray<GameComponent?>
    private var componentsToRemove: FixedSizeArray<GameComponent?>
    private var mPingPong = false
    private var mActivated = false
    var currentlySwapped = false
        private set
    private var swapOnAction: ActionType? = null
    private var lastAction: ActionType? = null
    override fun reset() {
        val factory = sSystemRegistry.gameObjectFactory
        // GameComponents hanging out in the componentsToInsert list are not part of the object
        // hierarchy, so we need to manually release them.
        if (factory != null) {
            var unrelasedComponents = componentsToInsert
            if (mActivated) {
                if (!mPingPong) {
                    // if we've activated and are not set to ping pong, the contents of
                    // componentsToInsert have already been inserted into the object and
                    // will be cleaned up with all the other of the object's components.
                    // In that case, componentsToRemove contains objects that need manual
                    // clean up.
                    unrelasedComponents = componentsToRemove
                }
            }
            val inactiveComponentCount = unrelasedComponents.count
            for (x in 0 until inactiveComponentCount) {
                val component = unrelasedComponents[x]
                if (!component!!.shared) {
                    factory.releaseComponent(component)
                }
            }
        }
        componentsToInsert.clear()
        componentsToRemove.clear()
        mPingPong = false
        mActivated = false
        currentlySwapped = false
        swapOnAction = ActionType.INVALID
        lastAction = ActionType.INVALID
    }

    override fun update(timeDelta: Float, parent: BaseObject?) {
        if (swapOnAction != ActionType.INVALID) {
            val parentObject = parent as GameObject
            val currentAction = parentObject.currentAction
            if (currentAction != lastAction) {
                lastAction = currentAction
                if (currentAction == swapOnAction) {
                    activate(parentObject)
                }
            }
        }
    }

    fun addSwapInComponent(component: GameComponent?) {
        componentsToInsert.add(component)
    }

    fun addSwapOutComponent(component: GameComponent?) {
        componentsToRemove.add(component)
    }

    fun setPingPongBehavior(pingPong: Boolean) {
        mPingPong = pingPong
    }

    fun setSwapAction(action: ActionType?) {
        swapOnAction = action
    }

    /** Inserts and removes components added to the swap-in and swap-out list, respectively.
     * Unless mPingPong is set, this may only be called once.
     * @param parent  The parent object to swap components on.
     */
    fun activate(parent: GameObject) {
        if (!mActivated || mPingPong) {
            val removeCount = componentsToRemove.count
            for (x in 0 until removeCount) {
                parent.remove(componentsToRemove[x])
            }
            val addCount = componentsToInsert.count
            for (x in 0 until addCount) {
                parent.add(componentsToInsert[x] as BaseObject)
            }
            mActivated = true
            currentlySwapped = !currentlySwapped
            if (mPingPong) {
                val swap = componentsToInsert
                componentsToInsert = componentsToRemove
                componentsToRemove = swap
            }
        }
    }

    companion object {
        private const val MAX_COMPONENT_SWAPS = 16
    }

    init {
        componentsToInsert = FixedSizeArray(MAX_COMPONENT_SWAPS)
        componentsToRemove = FixedSizeArray(MAX_COMPONENT_SWAPS)
        reset()
    }
}
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
@file:Suppress("UNCHECKED_CAST")

package com.replica.replicaisland

import java.util.*

/**
 * A node in the game graph that manages the activation status of its children.  The
 * GameObjectManager moves the objects it manages in and out of the active list (that is,
 * in and out of the game tree, causing them to be updated or ignored, respectively) each frame
 * based on the distance of that object to the camera.  Objects may specify an "activation radius"
 * to define an area around themselves so that the position of the camera can be used to determine
 * which objects should receive processing time and which should be ignored.  Objects that do not
 * move should have an activation radius that defines a sphere similar to the size of the screen;
 * they only need processing when they are visible.  Objects that move around will probably need
 * larger regions so that they can leave the visible area of the game world and not be immediately
 * deactivated.
 */
class GameObjectManager(private val mMaxActivationRadius: Float) : ObjectManager(MAX_GAME_OBJECTS) {
    private val mInactiveObjects: FixedSizeArray<BaseObject?>
    private val mMarkedForDeathObjects: FixedSizeArray<GameObject?>
    var player: GameObject? = null
    private var mVisitingGraph: Boolean
    private val mCameraFocus: Vector2
    override fun commitUpdates() {
        super.commitUpdates()
        val factory = sSystemRegistry.gameObjectFactory
        val objectsToKillCount = mMarkedForDeathObjects.count
        if (factory != null && objectsToKillCount > 0) {
            val deathArray: Array<Any?> = mMarkedForDeathObjects.array as Array<Any?>
            for (x in 0 until objectsToKillCount) {
                factory.destroy(deathArray[x] as GameObject)
            }
            mMarkedForDeathObjects.clear()
        }
    }

    override fun update(timeDelta: Float, parent: BaseObject?) {
        commitUpdates()
        val camera = sSystemRegistry.cameraSystem
        mCameraFocus[camera!!.fetchFocusPositionX()] = camera.fetchFocusPositionY()
        mVisitingGraph = true
        val objects = fetchObjects()
        val count = objects.count
        if (count > 0) {
            val objectArray: Array<Any?> = objects.array as Array<Any?>
            for (i in count - 1 downTo 0) {
                val gameObject = objectArray[i] as GameObject?
                val distance2 = mCameraFocus.distance2(gameObject!!.position)
                if (distance2 < gameObject.activationRadius * gameObject.activationRadius
                        || gameObject.activationRadius == -1f) {
                    gameObject.update(timeDelta, this)
                } else {
                    // Remove the object from the list.
                    // It's safe to just swap the current object with the last
                    // object because this list is being iterated backwards, so
                    // the last object in the list has already been processed.
                    objects.swapWithLast(i)
                    objects.removeLast()
                    if (gameObject.destroyOnDeactivation) {
                        mMarkedForDeathObjects.add(gameObject)
                    } else {
                        mInactiveObjects.add(gameObject as BaseObject?)
                    }
                }
            }
        }
        mInactiveObjects.sort(false)
        val inactiveCount = mInactiveObjects.count
        if (inactiveCount > 0) {
            val inactiveArray: Array<Any?> = mInactiveObjects.array as Array<Any?>
            for (i in inactiveCount - 1 downTo 0) {
                val gameObject = inactiveArray[i] as GameObject?
                val position = gameObject!!.position
                val distance2 = mCameraFocus.distance2(position)
                val xDistance = position.x - mCameraFocus.x
                if (distance2 < gameObject.activationRadius * gameObject.activationRadius
                        || gameObject.activationRadius == -1f) {
                    gameObject.update(timeDelta, this)
                    mInactiveObjects.swapWithLast(i)
                    mInactiveObjects.removeLast()
                    objects.add(gameObject)
                } else if (xDistance < -mMaxActivationRadius) {
                    // We've passed the focus, we can stop processing now
                    break
                }
            }
        }
        mVisitingGraph = false
    }

    override fun add(thing: BaseObject) {
        if (thing is GameObject) {
            super.add(thing)
        }
    }

    override fun remove(thing: BaseObject?) {
        super.remove(thing)
        if (thing === player) {
            player = null
        }
    }

    fun destroy(thing: GameObject?) {
        mMarkedForDeathObjects.add(thing)
        remove(thing)
    }

    fun destroyAll() {
        //TODO 2 fix: assert(mVisitingGraph == false)
        commitUpdates()
        val objects = fetchObjects()
        val count = objects.count
        for (i in count - 1 downTo 0) {
            mMarkedForDeathObjects.add(objects[i] as GameObject?)
            objects.remove(i)
        }
        val inactiveObjectCount = mInactiveObjects.count
        for (j in inactiveObjectCount - 1 downTo 0) {
            mMarkedForDeathObjects.add(mInactiveObjects[j] as GameObject?)
            mInactiveObjects.remove(j)
        }
        player = null
    }

    /** Comparator for game objects objects.  */
    private class HorizontalPositionComparator : Comparator<BaseObject?> {
        override fun compare(object1: BaseObject?, object2: BaseObject?): Int {
            var result = 0
            if (object1 == null && object2 != null) {
                result = 1
            } else if (object1 != null && object2 == null) {
                result = -1
            } else if (object1 != null && object2 != null) {
                val delta = ((object1 as GameObject).position.x
                        - (object2 as GameObject).position.x)
                if (delta < 0) {
                    result = -1
                } else if (delta > 0) {
                    result = 1
                }
            }
            return result
        }
    }

    companion object {
        private const val MAX_GAME_OBJECTS = 384
        private val sGameObjectComparator = HorizontalPositionComparator()
    }

    init {
        mInactiveObjects = FixedSizeArray(MAX_GAME_OBJECTS)
        mInactiveObjects.setComparator(sGameObjectComparator)
        mMarkedForDeathObjects = FixedSizeArray(MAX_GAME_OBJECTS)
        mVisitingGraph = false
        mCameraFocus = Vector2()
    }
}
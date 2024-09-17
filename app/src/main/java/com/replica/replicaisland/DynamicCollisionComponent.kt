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

/**
 * A component to include dynamic collision volumes (such as those produced every frame from
 * animating sprites) in the dynamic collision world.  Given a set of "attack" volumes and
 * "vulnerability" volumes (organized such that only attack vs vulnerability intersections result
 * in valid "hits"), this component creates a bounding volume that encompasses the set and submits
 * it to the dynamic collision system.  Including this component in a game object will allow it to
 * send and receive hits to other game objects.
 */
class DynamicCollisionComponent : GameComponent() {
    private var mAttackVolumes: FixedSizeArray<CollisionVolume>? = null
    private var vulnerabilityVolumes: FixedSizeArray<CollisionVolume>? = null
    private val boundingVolume: SphereCollisionVolume = SphereCollisionVolume(0.0f, 0.0f, 0.0f)
    private var hitReactionComponent: HitReactionComponent? = null
    override fun reset() {
        mAttackVolumes = null
        vulnerabilityVolumes = null
        boundingVolume.center = Vector2.ZERO
        boundingVolume.radius = 0.0f
        hitReactionComponent = null
    }

    override fun update(timeDelta: Float, parent: BaseObject?) {
        val collision = sSystemRegistry.gameObjectCollisionSystem
        if (collision != null && boundingVolume.radius > 0.0f) {
            collision.registerForCollisions(parent as GameObject, hitReactionComponent, boundingVolume,
                    mAttackVolumes, vulnerabilityVolumes)
        }
    }

    fun setHitReactionComponent(component: HitReactionComponent?) {
        hitReactionComponent = component
    }

    fun setCollisionVolumes(attackVolumes: FixedSizeArray<CollisionVolume>?,
                            vulnerableVolumes: FixedSizeArray<CollisionVolume>?) {
        if (vulnerabilityVolumes != vulnerableVolumes || mAttackVolumes != attackVolumes) {
            mAttackVolumes = attackVolumes
            vulnerabilityVolumes = vulnerableVolumes
            boundingVolume.reset()
            if (mAttackVolumes != null) {
                val count = mAttackVolumes!!.count
                for (x in 0 until count) {
                    boundingVolume.growBy(mAttackVolumes!![x]!!)
                }
            }
            if (vulnerabilityVolumes != null) {
                val count = vulnerabilityVolumes!!.count
                for (x in 0 until count) {
                    boundingVolume.growBy(vulnerabilityVolumes!![x]!!)
                }
            }
        }
    }

    init {
        setPhaseToThis(ComponentPhases.FRAME_END.ordinal)
        reset()
    }
}
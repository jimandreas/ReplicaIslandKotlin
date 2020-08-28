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
@file:Suppress("unused", "SENSELESS_COMPARISON")

package com.replica.replicaisland

import com.replica.replicaisland.CollisionParameters.HitType
import com.replica.replicaisland.CollisionVolume.FlipInfo
import java.util.*

/**
 * A system for calculating collisions between moving game objects.  This system accepts collision
 * volumes from game objects each frame and performs a series of tests to see which of them
 * overlap.  Collisions are only considered between offending "attack" volumes and receiving
 * "vulnerability" volumes.  This implementation works by using a sweep-and-prune algorithm:
 * objects to be considered are sorted in the x axis and then compared in one dimension for
 * overlaps.  A bounding volume that encompasses all attack and vulnerability volumes is used for
 * this test, and when an intersection is found the actual offending and receiving volumes are
 * compared.  If an intersection is detected both objects receive notification via a
 * HitReactionComponent, if one has been specified.
 */
class GameObjectCollisionSystem : BaseObject() {
    private var mObjects: FixedSizeArray<CollisionVolumeRecord>
    private var mRecordPool: CollisionVolumeRecordPool
    private var mDrawDebugBoundingVolume = false
    private var mDrawDebugCollisionVolumes = false
    override fun reset() {
        val count = mObjects.count
        for (x in 0 until count) {
            mRecordPool.release(mObjects[x]!!)
        }
        mObjects.clear()
        mDrawDebugBoundingVolume = false
        mDrawDebugCollisionVolumes = false
    }

    /**
     * Adds a game object, and its related volumes, to the dynamic collision world for one frame.
     * Once registered for collisions the object may damage other objects via attack volumes or
     * receive damage from other volumes via vulnerability volumes.
     * @param gameObject  The object to consider for collision.
     * @param reactionComponent  A HitReactionComponent to notify when an intersection is calculated.
     * If null, the intersection will still occur and no notification will be sent.
     * @param boundingVolume  A volume that describes the game object in space.  It should encompass
     * all of the attack and vulnerability volumes.
     * @param attackVolumes  A list of volumes that can hit other game objects.  May be null.
     * @param vulnerabilityVolumes  A list of volumes that can receive hits from other game objects.
     * May be null.
     */
    fun registerForCollisions(gameObject: GameObject,
                              reactionComponent: HitReactionComponent?,
                              boundingVolume: CollisionVolume?,
                              attackVolumes: FixedSizeArray<CollisionVolume>?,
                              vulnerabilityVolumes: FixedSizeArray<CollisionVolume>?) {
        val record = mRecordPool.allocate()
        if (record != null && gameObject != null && boundingVolume != null && (attackVolumes != null || vulnerabilityVolumes != null)) {
            record.gameObject = gameObject
            record.boundingVolume = boundingVolume
            record.attackVolumes = attackVolumes
            record.vulnerabilityVolumes = vulnerabilityVolumes
            record.reactionComponent = reactionComponent
            mObjects.add(record)
        }
    }

    override fun update(timeDelta: Float, parent: BaseObject?) {
        // Sort the objects by their x position.
        mObjects.sort(true)
        val count = mObjects.count
        for (x in 0 until count) {
            val record = mObjects[x]
            val position = record!!.gameObject!!.position
            sFlip.flipX = record.gameObject!!.facingDirection.x < 0.0f
            sFlip.flipY = record.gameObject!!.facingDirection.y < 0.0f
            sFlip.parentWidth = record.gameObject!!.width
            sFlip.parentHeight = record.gameObject!!.height
            if (sSystemRegistry.debugSystem != null) {
                drawDebugVolumes(record)
            }
            val maxX = record.boundingVolume!!.maxXPosition(sFlip) + position.x
            for (y in x + 1 until count) {
                val other = mObjects[y]
                val otherPosition = other!!.gameObject!!.position
                sOtherFlip.flipX = other.gameObject!!.facingDirection.x < 0.0f
                sOtherFlip.flipY = other.gameObject!!.facingDirection.y < 0.0f
                sOtherFlip.parentWidth = other.gameObject!!.width
                sOtherFlip.parentHeight = other.gameObject!!.height
                if (otherPosition.x + other.boundingVolume!!.minXPosition(sOtherFlip) > maxX) {
                    // These objects can't possibly be colliding.  And since the list is sorted,
                    // there are no potentially colliding objects after this object
                    // either, so we're done!
                    break
                } else {
                    val testRequired = record.attackVolumes != null && other.vulnerabilityVolumes != null ||
                            record.vulnerabilityVolumes != null && other.attackVolumes != null
                    if (testRequired && record.boundingVolume!!.intersects(position, sFlip,
                                    other.boundingVolume, otherPosition, sOtherFlip)) {
                        // These two objects are potentially colliding.
                        // Now we must test all attack vs vulnerability boxes.
                        val hit = testAttackAgainstVulnerability(
                                record.attackVolumes,
                                other.vulnerabilityVolumes,
                                position,
                                otherPosition,
                                sFlip,
                                sOtherFlip)
                        if (hit != HitType.INVALID) {
                            var hitAccepted = false
                            if (other.reactionComponent != null) {
                                hitAccepted = other.reactionComponent!!.receivedHit(
                                        other.gameObject!!, record.gameObject!!, hit)
                            }
                            if (record.reactionComponent != null) {
                                record.reactionComponent!!.hitVictim(
                                        record.gameObject!!, other.gameObject!!, hit, hitAccepted)
                            }
                        }
                        val hit2 = testAttackAgainstVulnerability(
                                other.attackVolumes,
                                record.vulnerabilityVolumes,
                                otherPosition,
                                position,
                                sOtherFlip,
                                sFlip)
                        if (hit2 != HitType.INVALID) {
                            var hitAccepted = false
                            if (record.reactionComponent != null) {
                                hitAccepted = record.reactionComponent!!.receivedHit(
                                        record.gameObject!!, other.gameObject!!, hit2)
                            }
                            if (other.reactionComponent != null) {
                                other.reactionComponent!!.hitVictim(
                                        other.gameObject!!, record.gameObject!!, hit2, hitAccepted)
                            }
                        }
                    }
                }
            }
            // This is a little tricky.  Since we always sweep forward in the list it's safe
            // to invalidate the current record after we've tested it.  This way we don't have to
            // iterate over the object list twice.
            mRecordPool.release(record)
        }
        mObjects.clear()
    }

    /** Compares the passed list of attack volumes against the passed list of vulnerability volumes
     * and returns a hit type if an intersection is found.
     * @param attackVolumes  Offensive collision volumes.
     * @param vulnerabilityVolumes  Receiving collision volumes.
     * @param attackPosition  The world position of the attacking object.
     * @param vulnerabilityPosition  The world position of the receiving object.
     * @return  The hit type of the first attacking volume that intersects a vulnerability volume,
     * or HitType.INVALID if no intersections are found.
     */
    private fun testAttackAgainstVulnerability(
            attackVolumes: FixedSizeArray<CollisionVolume>?,
            vulnerabilityVolumes: FixedSizeArray<CollisionVolume>?,
            attackPosition: Vector2?,
            vulnerabilityPosition: Vector2?,
            attackFlip: FlipInfo,
            vulnerabilityFlip: FlipInfo): Int {
        var intersectionType = HitType.INVALID
        if (attackVolumes != null && vulnerabilityVolumes != null) {
            val attackCount = attackVolumes.count
            var x = 0
            while (x < attackCount && intersectionType == HitType.INVALID) {
                val attackVolume = attackVolumes[x]
                val hitType = attackVolume!!.hitType
                if (hitType != HitType.INVALID) {
                    val vulnerabilityCount = vulnerabilityVolumes.count
                    for (y in 0 until vulnerabilityCount) {
                        val vulnerabilityVolume = vulnerabilityVolumes[y]
                        val vulnerableType = vulnerabilityVolume!!.hitType
                        if (vulnerableType == HitType.INVALID || vulnerableType == hitType) {
                            if (attackVolume.intersects(attackPosition, attackFlip,
                                            vulnerabilityVolume, vulnerabilityPosition,
                                            vulnerabilityFlip)) {
                                intersectionType = hitType
                                break
                            }
                        }
                    }
                }
                x++
            }
        }
        return intersectionType
    }

    private fun drawDebugVolumes(record: CollisionVolumeRecord?) {
        val position = record!!.gameObject!!.position
        if (mDrawDebugBoundingVolume) {
            val boundingVolume = record.boundingVolume
            sSystemRegistry.debugSystem!!.drawShape(
                    position.x + boundingVolume!!.minXPosition(sFlip), position.y + boundingVolume.minYPosition(sFlip),
                    boundingVolume.fetchMaxX() - boundingVolume.fetchMinX(),
                    boundingVolume.fetchMaxY() - boundingVolume.fetchMinY(),
                    DebugSystem.SHAPE_CIRCLE,
                    DebugSystem.COLOR_OUTLINE)
        }
        if (mDrawDebugCollisionVolumes) {
            if (record.attackVolumes != null) {
                val attackVolumeCount = record.attackVolumes!!.count
                for (y in 0 until attackVolumeCount) {
                    val volume = record.attackVolumes!![y]
                    sSystemRegistry.debugSystem!!.drawShape(
                            position.x + volume!!.minXPosition(sFlip), position.y + volume.minYPosition(sFlip),
                            volume.fetchMaxX() - volume.fetchMinX(),
                            volume.fetchMaxY() - volume.fetchMinY(),
                            if (volume.javaClass == AABoxCollisionVolume::class.java) DebugSystem.SHAPE_BOX else DebugSystem.SHAPE_CIRCLE,
                            DebugSystem.COLOR_RED)
                }
            }
            if (record.vulnerabilityVolumes != null) {
                val vulnVolumeCount = record.vulnerabilityVolumes!!.count
                for (y in 0 until vulnVolumeCount) {
                    val volume = record.vulnerabilityVolumes!![y]
                    sSystemRegistry.debugSystem!!.drawShape(
                            position.x + volume!!.minXPosition(sFlip), position.y + volume.minYPosition(sFlip),
                            volume.fetchMaxX() - volume.fetchMinX(),
                            volume.fetchMaxY() - volume.fetchMinY(),
                            if (volume.javaClass == AABoxCollisionVolume::class.java) DebugSystem.SHAPE_BOX else DebugSystem.SHAPE_CIRCLE,
                            DebugSystem.COLOR_BLUE)
                }
            }
        }
    }

    fun setDebugPrefs(drawBoundingVolumes: Boolean, drawCollisionVolumes: Boolean) {
        mDrawDebugBoundingVolume = drawBoundingVolumes
        mDrawDebugCollisionVolumes = drawCollisionVolumes
    }

    /** A record of a single game object and its associated collision info.   */
    class CollisionVolumeRecord : AllocationGuard() {
        var gameObject: GameObject? = null
        var reactionComponent: HitReactionComponent? = null
        var boundingVolume: CollisionVolume? = null
        var attackVolumes: FixedSizeArray<CollisionVolume>? = null
        var vulnerabilityVolumes: FixedSizeArray<CollisionVolume>? = null
        fun reset() {
            gameObject = null
            attackVolumes = null
            vulnerabilityVolumes = null
            boundingVolume = null
            reactionComponent = null
        }
    }

    /** A pool of collision volume records.   */
    inner class CollisionVolumeRecordPool(count: Int) : TObjectPool<CollisionVolumeRecord?>(count) {
        override fun fill() {
            for (x in 0 until fetchSize()) {
                fetchAvailable()!!.add(CollisionVolumeRecord())
            }
        }

        override fun release(entry: Any) {
            (entry as CollisionVolumeRecord).reset()
            super.release(entry)
        }
    }

    /**
     * Comparator for game objects that considers the world position of the object's bounding
     * volume and sorts objects from left to right on the x axis.  */
    class CollisionVolumeComparator : Comparator<CollisionVolumeRecord?> {
        override fun compare(object1: CollisionVolumeRecord?, object2: CollisionVolumeRecord?): Int {
            var result = 0
            if (object1 == null && object2 != null) {
                result = 1
            } else if (object1 != null && object2 == null) {
                result = -1
            } else if (object1 != null && object2 != null) {
                sCompareFlip.flipX = object1.gameObject!!.facingDirection.x < 0.0f
                sCompareFlip.flipY = object1.gameObject!!.facingDirection.y < 0.0f
                sCompareFlip.parentWidth = object1.gameObject!!.width
                sCompareFlip.parentHeight = object1.gameObject!!.height
                val minX1 = (object1.gameObject!!.position.x
                        + object1.boundingVolume!!.minXPosition(sCompareFlip))
                sCompareFlip.flipX = object2.gameObject!!.facingDirection.x < 0.0f
                sCompareFlip.flipY = object2.gameObject!!.facingDirection.y < 0.0f
                sCompareFlip.parentWidth = object2.gameObject!!.width
                sCompareFlip.parentHeight = object2.gameObject!!.height
                val minX2 = (object2.gameObject!!.position.x
                        + object2.boundingVolume!!.minXPosition(sCompareFlip))
                val delta = minX1 - minX2
                if (delta < 0.0f) {
                    result = -1
                } else if (delta > 0.0f) {
                    result = 1
                }
            }
            return result
        }

        companion object {
            private val sCompareFlip = FlipInfo()
        }
    }

    companion object {
        private const val MAX_COLLIDING_OBJECTS = 256
        private const val COLLISION_RECORD_POOL_SIZE = 256
        private val sCollisionVolumeComparator = CollisionVolumeComparator()
        private val sFlip = FlipInfo()
        private val sOtherFlip = FlipInfo()
    }

    init {
        mObjects = FixedSizeArray(MAX_COLLIDING_OBJECTS)
        mObjects.setComparator(sCollisionVolumeComparator)
        //mObjects.setSorter(new ShellSorter<CollisionVolumeRecord>());
        mRecordPool = CollisionVolumeRecordPool(COLLISION_RECORD_POOL_SIZE)
    }
}
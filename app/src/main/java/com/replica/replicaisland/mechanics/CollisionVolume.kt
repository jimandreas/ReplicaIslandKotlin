package com.replica.replicaisland.mechanics

import com.replica.replicaisland.utils.AllocationGuard
import com.replica.replicaisland.utils.Vector2

/**
 * CollisionVolume describes a volume (rectangle, sphere, etc) used for dynamic collision detection.
 * Volumes can be tested for intersection against other volumes, and can be grown to contain a set
 * of other volumes.  The volume itself is stored in object-relative space (in terms of offsets from
 * some origin); when used with game objects the position of the parent object must be passed to
 * a parameter of the intersection test.  This means that a single instance of a CollisionVolume and
 * its derivatives is safe to share amongst many game object instances.
 */
abstract class CollisionVolume : AllocationGuard {
    // TODO: does this really belong here?
    // When used as an attack volume, mHitType specifies the type of hit that the volume deals.
    // When used as a vulnerability volume, it specifies which type the volume is vulernable to
    // (invalid = all types).
    var hitType: Int

    constructor() : super() {
        hitType = CollisionParameters.HitType.INVALID
    }

    constructor(type: Int) : super() {
        hitType = type
    }

    abstract fun intersects(position: Vector2?, flip: FlipInfo?, other: CollisionVolume?,
                            otherPosition: Vector2?, otherFlip: FlipInfo?): Boolean

    fun minXPosition(flip: FlipInfo?): Float {
        val value = if (flip != null && flip.flipX) {
            val maxX = fetchMaxX()
            flip.parentWidth - maxX
        } else {
            fetchMinX()
        }
        return value
    }

    fun maxXPosition(flip: FlipInfo?): Float {
        val value = if (flip != null && flip.flipX) {
            val minX = fetchMinX()
            flip.parentWidth - minX
        } else {
            fetchMaxX()
        }
        return value
    }

    fun minYPosition(flip: FlipInfo?): Float {
        val value = if (flip != null && flip.flipY) {
            val maxY = fetchMaxY()
            flip.parentHeight - maxY
        } else {
            fetchMinY()
        }
        return value
    }

    fun maxYPosition(flip: FlipInfo?): Float {
        val value = if (flip != null && flip.flipY) {
            val minY = fetchMinY()
            flip.parentHeight - minY
        } else {
            fetchMaxY()
        }
        return value
    }

    // Note: renamed from "get" form for Kotlin conversion, otherwise things get confused
    abstract fun fetchMinX(): Float
    abstract fun fetchMaxX(): Float
    abstract fun fetchMinY(): Float
    abstract fun fetchMaxY(): Float
    class FlipInfo {
        
        var flipX = false
        
        var flipY = false
        
        var parentWidth = 0f
        
        var parentHeight = 0f
    }
}
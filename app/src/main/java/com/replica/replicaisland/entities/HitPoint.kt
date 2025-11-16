package com.replica.replicaisland.entities

import com.replica.replicaisland.utils.AllocationGuard
import com.replica.replicaisland.utils.Vector2

class HitPoint : AllocationGuard() {
    @JvmField
    var hitPoint: Vector2? = null
    @JvmField
    var hitNormal: Vector2? = null
    fun reset() {
        hitPoint = null
        hitNormal = null
    }
}
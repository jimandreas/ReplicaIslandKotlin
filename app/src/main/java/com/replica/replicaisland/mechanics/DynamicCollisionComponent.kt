package com.replica.replicaisland.mechanics

import com.replica.replicaisland.utils.FixedSizeArray
import com.replica.replicaisland.GameComponent
import com.replica.replicaisland.HitReactionComponent
import com.replica.replicaisland.utils.Vector2
import com.replica.replicaisland.core.BaseObject
import com.replica.replicaisland.core.GameObject

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
        boundingVolume.center = Vector2.Companion.ZERO
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
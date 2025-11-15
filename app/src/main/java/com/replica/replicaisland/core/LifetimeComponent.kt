package com.replica.replicaisland.core

import com.replica.replicaisland.GameComponent
import com.replica.replicaisland.GhostComponent
import com.replica.replicaisland.mechanics.HotSpotSystem
import com.replica.replicaisland.LaunchProjectileComponent
import com.replica.replicaisland.utils.Vector2
import com.replica.replicaisland.sound.SoundSystem
import kotlin.math.abs

/**
 * This component allows objects to die and be deleted when their life is reduced to zero or they
 * meet other configurable criteria.
 */
class LifetimeComponent : GameComponent() {
    private var dieWhenInvisible = false
    private var timeUntilDeath = 0f
    private var spawnOnDeathType: GameObjectFactory.GameObjectType? = null
    private var trackingSpawner: LaunchProjectileComponent? = null
    private val hotSpotTestPoint: Vector2 = Vector2()
    private var releaseGhostOnDeath = false
    private var vulnerableToDeathTiles = false
    private var dieOnHitBackground = false
    private var mDeathSound: SoundSystem.Sound? = null
    private var mIncrementEventCounter = false
    private var eventCounter = 0
    override fun reset() {
        dieWhenInvisible = false
        timeUntilDeath = -1f
        spawnOnDeathType = GameObjectFactory.GameObjectType.INVALID
        trackingSpawner = null
        hotSpotTestPoint.zero()
        releaseGhostOnDeath = true
        vulnerableToDeathTiles = false
        dieOnHitBackground = false
        mDeathSound = null
        mIncrementEventCounter = false
        eventCounter = -1
    }

    fun setDieWhenInvisible(die: Boolean) {
        dieWhenInvisible = die
    }

    fun setTimeUntilDeath(time: Float) {
        timeUntilDeath = time
    }

    fun setObjectToSpawnOnDeath(type: GameObjectFactory.GameObjectType?) {
        spawnOnDeathType = type
    }

    fun setIncrementEventCounter(event: Int) {
        mIncrementEventCounter = true
        eventCounter = event
    }

    override fun update(timeDelta: Float, parent: BaseObject?) {
        val parentObject = parent as GameObject
        if (timeUntilDeath > 0) {
            timeUntilDeath -= timeDelta
            if (timeUntilDeath <= 0) {
                die(parentObject)
                return
            }
        }
        if (dieWhenInvisible) {
            val camera = sSystemRegistry.cameraSystem
            val context = sSystemRegistry.contextParameters
            val dx = abs(parentObject.position.x - camera!!.fetchFocusPositionX())
            val dy = abs(parentObject.position.y - camera.fetchFocusPositionY())
            if (dx > context!!.gameWidth || dy > context.gameHeight) {
                // the position of this object is off the screen, destroy!
                // TODO: this is a pretty dumb test.  We should have a bounding volume instead.
                die(parentObject)
                return
            }
        }
        if (parentObject.life > 0 && vulnerableToDeathTiles) {
            val hotSpot = sSystemRegistry.hotSpotSystem
            if (hotSpot != null) {
                // TODO: HACK!  Unify all this code.
                if (hotSpot.getHotSpot(parentObject.centeredPositionX,
                                parentObject.position.y + 10.0f) == HotSpotSystem.HotSpotType.DIE) {
                    parentObject.life = 0
                }
            }
        }
        if (parentObject.life > 0 && dieOnHitBackground) {
            if (parentObject.backgroundCollisionNormal.length2() > 0.0f) {
                parentObject.life = 0
            }
        }
        if (parentObject.life <= 0) {
            die(parentObject)
            return
        }
    }

    private fun die(parentObject: GameObject) {
        val factory = sSystemRegistry.gameObjectFactory
        val manager = sSystemRegistry.gameObjectManager
        if (releaseGhostOnDeath) {
            // TODO: This is sort of a hack.  Find a better way to do this without introducing a
            // dependency between these two.  Generic on-death event or something.
            val ghost = parentObject.findByClass(GhostComponent::class.java)
            if (ghost != null) {
                ghost.releaseControl(parentObject)
            }
        }
        if (mIncrementEventCounter) {
            val recorder = sSystemRegistry.eventRecorder
            recorder!!.incrementEventCounter(eventCounter)
        }
        if (spawnOnDeathType != GameObjectFactory.GameObjectType.INVALID) {
            val `object` = factory!!.spawn(spawnOnDeathType!!, parentObject.position.x,
                    parentObject.position.y, parentObject.facingDirection.x < 0.0f)
            if (`object` != null && manager != null) {
                manager.add(`object`)
            }
        }
        if (trackingSpawner != null) {
            trackingSpawner!!.trackedProjectileDestroyed()
        }
        manager?.destroy(parentObject)
        if (mDeathSound != null) {
            val sound = sSystemRegistry.soundSystem
            sound?.play(mDeathSound!!, false, SoundSystem.Companion.PRIORITY_NORMAL)
        }
    }

    fun setTrackingSpawner(spawner: LaunchProjectileComponent?) {
        trackingSpawner = spawner
    }

    fun setReleaseGhostOnDeath(release: Boolean) {
        releaseGhostOnDeath = release
    }

    fun setVulnerableToDeathTiles(vulnerable: Boolean) {
        vulnerableToDeathTiles = vulnerable
    }

    fun setDieOnHitBackground(die: Boolean) {
        dieOnHitBackground = die
    }

    fun setDeathSound(deathSound: SoundSystem.Sound?) {
        mDeathSound = deathSound
    }

    init {
        reset()
        setPhaseToThis(ComponentPhases.THINK.ordinal)
    }
}
package com.replica.replicaisland.entities

import com.replica.replicaisland.GameComponent
import com.replica.replicaisland.core.BaseObject
import com.replica.replicaisland.core.GameObject
import com.replica.replicaisland.core.GameObjectFactory
import com.replica.replicaisland.core.LifetimeComponent
import com.replica.replicaisland.sound.SoundSystem
import com.replica.replicaisland.utils.Utils
import com.replica.replicaisland.utils.Vector2
import kotlin.math.cos
import kotlin.math.sin

/**
 * A component that allows an object to spawn other objects and apply velocity to them at
 * specific intervals.  Can be used to launch projectiles, particle effects, or any other type
 * of game object.
 */
class LaunchProjectileComponent : GameComponent() {
    private var mObjectTypeToSpawn: GameObjectFactory.GameObjectType? = null
    private var mOffsetX = 0f
    private var mOffsetY = 0f
    private var mVelocityX = 0f
    private var mVelocityY = 0f
    private var thetaError = 0f
    private var mRequiredAction: GameObject.ActionType? = null
    private var delayBetweenShots = 0f
    private var projectilesInSet = 0
    private var mDelayBetweenSets = 0f
    private var setsPerActivation = 0
    private var mDelayBeforeFirstSet = 0f
    private var lastProjectileTime = 0f
    private var setStartedTime = 0f
    private var launchedCount = 0
    private var mSetCount = 0
    private var trackProjectiles = false
    private var maxTrackedProjectiles = 0
    private var trackedProjectileCount = 0
    private val workingVector: Vector2
    private var shootSound: SoundSystem.Sound? = null
    override fun reset() {
        mRequiredAction = GameObject.ActionType.INVALID
        mObjectTypeToSpawn = GameObjectFactory.GameObjectType.INVALID
        mOffsetX = 0.0f
        mOffsetY = 0.0f
        mVelocityX = 0.0f
        mVelocityY = 0.0f
        delayBetweenShots = 0.0f
        projectilesInSet = 0
        mDelayBetweenSets = 0.0f
        lastProjectileTime = 0.0f
        setStartedTime = -1.0f
        launchedCount = 0
        mSetCount = 0
        setsPerActivation = -1
        projectilesInSet = 0
        mDelayBeforeFirstSet = 0.0f
        trackProjectiles = false
        maxTrackedProjectiles = 0
        trackedProjectileCount = 0
        thetaError = 0.0f
        shootSound = null
    }

    override fun update(timeDelta: Float, parent: BaseObject?) {
        val parentObject = parent as GameObject
        val time = sSystemRegistry.timeSystem
        val gameTime = time!!.gameTime
        if (trackedProjectileCount < maxTrackedProjectiles || !trackProjectiles) {
            if (parentObject.currentAction == mRequiredAction
                    || mRequiredAction == GameObject.ActionType.INVALID) {
                if (setStartedTime == -1.0f) {
                    launchedCount = 0
                    lastProjectileTime = 0.0f
                    setStartedTime = gameTime
                }
                val setDelay = if (mSetCount > 0) mDelayBetweenSets else mDelayBeforeFirstSet
                if (gameTime - setStartedTime >= setDelay &&
                        (mSetCount < setsPerActivation || setsPerActivation == -1)) {
                    // We can start shooting.
                    val timeSinceLastShot = gameTime - lastProjectileTime
                    if (timeSinceLastShot >= delayBetweenShots) {
                        launch(parentObject)
                        lastProjectileTime = gameTime
                        if (launchedCount >= projectilesInSet && projectilesInSet > 0) {
                            setStartedTime = -1.0f
                            mSetCount++
                        }
                    }
                }
            } else {
                // Force the timer to start counting when the right action is activated.
                setStartedTime = -1.0f
                mSetCount = 0
            }
        }
    }

    private fun launch(parentObject: GameObject) {
        launchedCount++
        val factory = sSystemRegistry.gameObjectFactory
        val manager = sSystemRegistry.gameObjectManager
        if (factory != null && manager != null) {
            var offsetX = mOffsetX
            var offsetY = mOffsetY
            var flip = false
            if (parentObject.facingDirection.x < 0.0f) {
                offsetX = parentObject.width - mOffsetX
                flip = true
            }
            if (parentObject.facingDirection.y < 0.0f) {
                offsetY = parentObject.height - mOffsetY
            }
            val x = parentObject.position.x + offsetX
            val y = parentObject.position.y + offsetY
            val thing = factory.spawn(mObjectTypeToSpawn!!, x, y, flip)
            if (thing != null) {
                workingVector[1.0f] = 1.0f
                if (thetaError > 0.0f) {
                    val angle = (Math.random() * thetaError * Math.PI * 2.0f).toFloat()
                    workingVector.x = sin(angle.toDouble()).toFloat()
                    workingVector.y = cos(angle.toDouble()).toFloat()
                    if (Utils.Companion.close(workingVector.length2(), 0.0f)) {
                        workingVector[1.0f] = 1.0f
                    }
                }
                workingVector.x *= if (flip) -mVelocityX else mVelocityX
                workingVector.y *= mVelocityY
                thing.velocity.set(workingVector)
                thing.targetVelocity.set(workingVector)
                // Center the projectile on the spawn point.
                thing.position.x -= thing.width / 2.0f
                thing.position.y -= thing.height / 2.0f
                if (trackProjectiles) {
                    thing.commitUpdates()
                    val projectileLife = thing.findByClass(LifetimeComponent::class.java)
                    if (projectileLife != null) {
                        projectileLife.setTrackingSpawner(this)
                        trackedProjectileCount++
                    }
                }
                manager.add(thing)
                if (shootSound != null) {
                    val sound = sSystemRegistry.soundSystem
                    sound?.play(shootSound!!, false, SoundSystem.Companion.PRIORITY_NORMAL)
                }
            }
        }
    }

    fun setObjectTypeToSpawn(objectTypeToSpawn: GameObjectFactory.GameObjectType?) {
        mObjectTypeToSpawn = objectTypeToSpawn
    }

    fun setOffsetX(offsetX: Float) {
        mOffsetX = offsetX
    }

    fun setOffsetY(offsetY: Float) {
        mOffsetY = offsetY
    }

    fun setVelocityX(velocityX: Float) {
        mVelocityX = velocityX
    }

    fun setVelocityY(velocityY: Float) {
        mVelocityY = velocityY
    }

    fun setRequiredAction(requiredAction: GameObject.ActionType?) {
        mRequiredAction = requiredAction
    }

    fun setDelayBetweenShots(launchDelay: Float) {
        delayBetweenShots = launchDelay
    }

    fun setDelayBetweenSets(delayBetweenSets: Float) {
        mDelayBetweenSets = delayBetweenSets
    }

    fun setDelayBeforeFirstSet(delayBeforeFirstSet: Float) {
        mDelayBeforeFirstSet = delayBeforeFirstSet
    }

    fun setShotsPerSet(shotCount: Int) {
        projectilesInSet = shotCount
    }

    fun setSetsPerActivation(setCount: Int) {
        setsPerActivation = setCount
    }

    fun enableProjectileTracking(max: Int) {
        maxTrackedProjectiles = max
        trackProjectiles = true
    }

    fun disableProjectileTracking() {
        maxTrackedProjectiles = 0
        trackProjectiles = false
    }

    fun trackedProjectileDestroyed() {
        // TODO 2: fix assert(trackProjectiles)
        if (trackedProjectileCount == maxTrackedProjectiles) {
            // Let's restart the set.
            setStartedTime = -1.0f
            mSetCount = 0
        }
        trackedProjectileCount--
    }

    fun setThetaError(error: Float) {
        thetaError = error
    }

    fun setShootSound(shoot: SoundSystem.Sound?) {
        shootSound = shoot
    }

    init {
        setPhaseToThis(ComponentPhases.POST_COLLISION.ordinal)
        workingVector = Vector2()
        reset()
    }
}
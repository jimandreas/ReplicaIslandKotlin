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
@file:Suppress("ConvertTwoComparisonsToRangeCheck")

package com.replica.replicaisland

import com.replica.replicaisland.GameObject.ActionType
import com.replica.replicaisland.GameObjectFactory.GameObjectType
import com.replica.replicaisland.SoundSystem.Sound
import kotlin.math.cos
import kotlin.math.sin

/**
 * A component that allows an object to spawn other objects and apply velocity to them at
 * specific intervals.  Can be used to launch projectiles, particle effects, or any other type
 * of game object.
 */
class LaunchProjectileComponent : GameComponent() {
    private var mObjectTypeToSpawn: GameObjectType? = null
    private var mOffsetX = 0f
    private var mOffsetY = 0f
    private var mVelocityX = 0f
    private var mVelocityY = 0f
    private var mThetaError = 0f
    private var mRequiredAction: ActionType? = null
    private var mDelayBetweenShots = 0f
    private var mProjectilesInSet = 0
    private var mDelayBetweenSets = 0f
    private var mSetsPerActivation = 0
    private var mDelayBeforeFirstSet = 0f
    private var mLastProjectileTime = 0f
    private var mSetStartedTime = 0f
    private var mLaunchedCount = 0
    private var mSetCount = 0
    private var mTrackProjectiles = false
    private var mMaxTrackedProjectiles = 0
    private var mTrackedProjectileCount = 0
    private val mWorkingVector: Vector2
    private var mShootSound: Sound? = null
    override fun reset() {
        mRequiredAction = ActionType.INVALID
        mObjectTypeToSpawn = GameObjectType.INVALID
        mOffsetX = 0.0f
        mOffsetY = 0.0f
        mVelocityX = 0.0f
        mVelocityY = 0.0f
        mDelayBetweenShots = 0.0f
        mProjectilesInSet = 0
        mDelayBetweenSets = 0.0f
        mLastProjectileTime = 0.0f
        mSetStartedTime = -1.0f
        mLaunchedCount = 0
        mSetCount = 0
        mSetsPerActivation = -1
        mProjectilesInSet = 0
        mDelayBeforeFirstSet = 0.0f
        mTrackProjectiles = false
        mMaxTrackedProjectiles = 0
        mTrackedProjectileCount = 0
        mThetaError = 0.0f
        mShootSound = null
    }

    override fun update(timeDelta: Float, parent: BaseObject?) {
        val parentObject = parent as GameObject
        val time = sSystemRegistry.timeSystem
        val gameTime = time!!.gameTime
        if (mTrackedProjectileCount < mMaxTrackedProjectiles || !mTrackProjectiles) {
            if (parentObject.currentAction == mRequiredAction
                    || mRequiredAction == ActionType.INVALID) {
                if (mSetStartedTime == -1.0f) {
                    mLaunchedCount = 0
                    mLastProjectileTime = 0.0f
                    mSetStartedTime = gameTime
                }
                val setDelay = if (mSetCount > 0) mDelayBetweenSets else mDelayBeforeFirstSet
                if (gameTime - mSetStartedTime >= setDelay &&
                        (mSetCount < mSetsPerActivation || mSetsPerActivation == -1)) {
                    // We can start shooting.
                    val timeSinceLastShot = gameTime - mLastProjectileTime
                    if (timeSinceLastShot >= mDelayBetweenShots) {
                        launch(parentObject)
                        mLastProjectileTime = gameTime
                        if (mLaunchedCount >= mProjectilesInSet && mProjectilesInSet > 0) {
                            mSetStartedTime = -1.0f
                            mSetCount++
                        }
                    }
                }
            } else {
                // Force the timer to start counting when the right action is activated.
                mSetStartedTime = -1.0f
                mSetCount = 0
            }
        }
    }

    private fun launch(parentObject: GameObject) {
        mLaunchedCount++
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
                mWorkingVector[1.0f] = 1.0f
                if (mThetaError > 0.0f) {
                    val angle = (Math.random() * mThetaError * Math.PI * 2.0f).toFloat()
                    mWorkingVector.x = sin(angle.toDouble()).toFloat()
                    mWorkingVector.y = cos(angle.toDouble()).toFloat()
                    if (Utils.close(mWorkingVector.length2(), 0.0f)) {
                        mWorkingVector[1.0f] = 1.0f
                    }
                }
                mWorkingVector.x *= if (flip) -mVelocityX else mVelocityX
                mWorkingVector.y *= mVelocityY
                thing.velocity.set(mWorkingVector)
                thing.targetVelocity.set(mWorkingVector)
                // Center the projectile on the spawn point.
                thing.position.x -= thing.width / 2.0f
                thing.position.y -= thing.height / 2.0f
                if (mTrackProjectiles) {
                    thing.commitUpdates()
                    val projectileLife = thing.findByClass(LifetimeComponent::class.java)
                    if (projectileLife != null) {
                        projectileLife.setTrackingSpawner(this)
                        mTrackedProjectileCount++
                    }
                }
                manager.add(thing)
                if (mShootSound != null) {
                    val sound = sSystemRegistry.soundSystem
                    sound?.play(mShootSound!!, false, SoundSystem.PRIORITY_NORMAL)
                }
            }
        }
    }

    fun setObjectTypeToSpawn(objectTypeToSpawn: GameObjectType?) {
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

    fun setRequiredAction(requiredAction: ActionType?) {
        mRequiredAction = requiredAction
    }

    fun setDelayBetweenShots(launchDelay: Float) {
        mDelayBetweenShots = launchDelay
    }

    fun setDelayBetweenSets(delayBetweenSets: Float) {
        mDelayBetweenSets = delayBetweenSets
    }

    fun setDelayBeforeFirstSet(delayBeforeFirstSet: Float) {
        mDelayBeforeFirstSet = delayBeforeFirstSet
    }

    fun setShotsPerSet(shotCount: Int) {
        mProjectilesInSet = shotCount
    }

    fun setSetsPerActivation(setCount: Int) {
        mSetsPerActivation = setCount
    }

    fun enableProjectileTracking(max: Int) {
        mMaxTrackedProjectiles = max
        mTrackProjectiles = true
    }

    fun disableProjectileTracking() {
        mMaxTrackedProjectiles = 0
        mTrackProjectiles = false
    }

    fun trackedProjectileDestroyed() {
        // TODO 2: fix assert(mTrackProjectiles)
        if (mTrackedProjectileCount == mMaxTrackedProjectiles) {
            // Let's restart the set.
            mSetStartedTime = -1.0f
            mSetCount = 0
        }
        mTrackedProjectileCount--
    }

    fun setThetaError(error: Float) {
        mThetaError = error
    }

    fun setShootSound(shoot: Sound?) {
        mShootSound = shoot
    }

    init {
        setPhaseToThis(ComponentPhases.POST_COLLISION.ordinal)
        mWorkingVector = Vector2()
        reset()
    }
}
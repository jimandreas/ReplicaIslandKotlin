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
@file:Suppress("UNUSED_PARAMETER", "unused", "IfThenToSafeAccess")

package com.replica.replicaisland

import com.replica.replicaisland.CollisionParameters.HitType
import com.replica.replicaisland.GameObject.ActionType
import com.replica.replicaisland.GameObject.Team
import com.replica.replicaisland.GameObjectFactory.GameObjectType
import com.replica.replicaisland.InventoryComponent.UpdateRecord
import com.replica.replicaisland.SoundSystem.Sound

/**
 * A general-purpose component that responds to dynamic collision notifications.  This component
 * may be configured to produce common responses to hit (taking damage, being knocked back, etc), or
 * it can be derived for entirely different responses.  This component must exist on an object for
 * that object to respond to dynamic collisions.
 */
class HitReactionComponent : GameComponent() {
    private var mPauseOnAttack = false
    private var mPauseOnAttackTime = 0f
    private var mBounceOnHit = false
    private var mBounceMagnitude = 0f
    private var mInvincibleAfterHitTime = 0f
    private var mLastHitTime = 0f
    private var mInvincible = false
    private var mDieOnCollect = false
    private var mDieOnAttack = false
    private var mPossessionComponent: ChangeComponentsComponent? = null
    private var mInventoryUpdate: UpdateRecord? = null
    private var mLauncherComponent: LauncherComponent? = null
    private var mLauncherHitType = 0
    private var mInvincibleTime = 0f
    private var mGameEventHitType = 0
    private var mGameEventOnHit = 0
    private var mGameEventIndexData = 0
    private var mLastGameEventTime = 0f
    private var mForceInvincibility = false
    private var mTakeHitSound: Sound? = null
    private var mDealHitSound: Sound? = null
    private var mDealHitSoundHitType = 0
    private var mTakeHitSoundHitType = 0
    private var mSpawnOnDealHitObjectType: GameObjectType? = null
    private var mSpawnOnDealHitHitType = 0
    private var mAlignDealHitObjectToVictimX = false
    private var mAlignDealHitObjectToVictimY = false
    override fun reset() {
        mPauseOnAttack = false
        mPauseOnAttackTime = ATTACK_PAUSE_DELAY
        mBounceOnHit = false
        mBounceMagnitude = DEFAULT_BOUNCE_MAGNITUDE
        mInvincibleAfterHitTime = 0.0f
        mInvincible = false
        mDieOnCollect = false
        mDieOnAttack = false
        mPossessionComponent = null
        mInventoryUpdate = null
        mLauncherComponent = null
        mLauncherHitType = HitType.LAUNCH
        mInvincibleTime = 0.0f
        mGameEventOnHit = -1
        mGameEventIndexData = 0
        mLastGameEventTime = -1.0f
        mGameEventHitType = HitType.INVALID
        mForceInvincibility = false
        mTakeHitSound = null
        mDealHitSound = null
        mSpawnOnDealHitObjectType = GameObjectType.INVALID
        mSpawnOnDealHitHitType = HitType.INVALID
        mDealHitSoundHitType = HitType.INVALID
        mAlignDealHitObjectToVictimX = false
        mAlignDealHitObjectToVictimY = false
    }

    /** Called when this object attacks another object.  */
    fun hitVictim(parent: GameObject, victim: GameObject, hitType: Int,
                  hitAccepted: Boolean) {
        if (hitAccepted) {
            if (mPauseOnAttack && hitType == HitType.HIT) {
                val time = sSystemRegistry.timeSystem
                time!!.freeze(mPauseOnAttackTime)
            }
            if (mDieOnAttack) {
                parent.life = 0
            }
            if (hitType == mLauncherHitType && mLauncherComponent != null) {
                mLauncherComponent!!.prepareToLaunch(victim, parent)
            }
            if (mDealHitSound != null &&
                    (hitType == mDealHitSoundHitType ||
                            mDealHitSoundHitType == HitType.INVALID)) {
                val sound = sSystemRegistry.soundSystem
                sound?.play(mDealHitSound!!, false, SoundSystem.PRIORITY_NORMAL)
            }
            if (mSpawnOnDealHitObjectType != GameObjectType.INVALID &&
                    hitType == mSpawnOnDealHitHitType) {
                val x = if (mAlignDealHitObjectToVictimX) victim.position.x else parent.position.x
                val y = if (mAlignDealHitObjectToVictimY) victim.position.y else parent.position.y
                val factory = sSystemRegistry.gameObjectFactory
                val manager = sSystemRegistry.gameObjectManager
                if (factory != null) {
                    val thing = factory.spawn(mSpawnOnDealHitObjectType!!, x,
                            y, parent.facingDirection.x < 0.0f)
                    if (thing != null && manager != null) {
                        manager.add(thing)
                    }
                }
            }
        }
    }

    /** Called when this object is hit by another object.  */
    fun receivedHit(parent: GameObject, attacker: GameObject, hitTypeIn: Int): Boolean {
        var hitType = hitTypeIn
        val time = sSystemRegistry.timeSystem
        val gameTime = time!!.gameTime
        if (mGameEventHitType == hitType &&
                mGameEventHitType != HitType.INVALID) {
            if (mLastGameEventTime < 0.0f || gameTime > mLastGameEventTime + EVENT_SEND_DELAY) {
                val level = sSystemRegistry.levelSystem
                level!!.sendGameEvent(mGameEventOnHit, mGameEventIndexData, true)
            } else {
                // special case.  If we're waiting for a hit type to spawn an event and
                // another event has just happened, eat this hit so we don't miss
                // the chance to send the event.
                hitType = HitType.INVALID
            }
            mLastGameEventTime = gameTime
        }
        when (hitType) {
            HitType.INVALID -> {
            }
            HitType.HIT -> {
                // don't hit our friends, if we have friends.
                val sameTeam = parent.team == attacker.team && parent.team != Team.NONE
                if (!mForceInvincibility && !mInvincible && parent.life > 0 && !sameTeam) {
                    parent.life -= 1
                    if (mBounceOnHit && parent.life > 0) {
                        val pool = sSystemRegistry.vectorPool
                        val newVelocity = pool!!.allocate(parent.position)
                        newVelocity.subtract(attacker.position)
                        newVelocity[0.5f * Utils.sign(newVelocity.x)] = 0.5f * Utils.sign(newVelocity.y)
                        newVelocity.multiply(mBounceMagnitude)
                        parent.velocity = newVelocity
                        parent.targetVelocity.zero()
                        pool.release(newVelocity)
                    }
                    if (mInvincibleAfterHitTime > 0.0f) {
                        mInvincible = true
                        mInvincibleTime = mInvincibleAfterHitTime
                    }
                } else {
                    // Ignore this hit.
                    hitType = HitType.INVALID
                }
            }
            HitType.DEATH ->                 // respect teams?
                parent.life = 0
            HitType.COLLECT -> {
                if (mInventoryUpdate != null && parent.life > 0) {
                    val attackerInventory = attacker.findByClass(InventoryComponent::class.java)
                    if (attackerInventory != null) {
                        (attackerInventory).applyUpdate(mInventoryUpdate!!)
                    }

                }
                if (mDieOnCollect && parent.life > 0) {
                    parent.life = 0
                }
            }
            HitType.POSSESS -> if (mPossessionComponent != null && parent.life > 0 && attacker.life > 0) {
                mPossessionComponent!!.activate(parent)
            } else {
                hitType = HitType.INVALID
            }
            HitType.LAUNCH -> {
            }
            else -> {
            }
        }
        if (hitType != HitType.INVALID) {
            if (mTakeHitSound != null && hitType == mTakeHitSoundHitType) {
                val sound = sSystemRegistry.soundSystem
                sound?.play(mTakeHitSound!!, false, SoundSystem.PRIORITY_NORMAL)
            }
            mLastHitTime = gameTime
            parent.currentAction = ActionType.HIT_REACT
            parent.lastReceivedHitType = hitType
        }
        return hitType != HitType.INVALID
    }

    override fun update(timeDelta: Float, parent: BaseObject?) {
        val parentObject = parent as GameObject
        val time = sSystemRegistry.timeSystem
        val gameTime = time!!.gameTime
        if (mInvincible && mInvincibleTime > 0) {
            if (time.gameTime > mLastHitTime + mInvincibleTime) {
                mInvincible = false
            }
        }

        // This means that the lastReceivedHitType will persist for two frames, giving all systems
        // a chance to react.
        if (gameTime - mLastHitTime > timeDelta) {
            parentObject.lastReceivedHitType = HitType.INVALID
        }
    }

    fun setPauseOnAttack(pause: Boolean) {
        mPauseOnAttack = pause
    }

    fun setPauseOnAttackTime(seconds: Float) {
        mPauseOnAttackTime = seconds
    }

    fun setBounceOnHit(bounce: Boolean) {
        mBounceOnHit = bounce
    }

    fun setBounceMagnitude(magnitude: Float) {
        mBounceMagnitude = magnitude
    }

    fun setInvincibleTime(time: Float) {
        mInvincibleAfterHitTime = time
    }

    fun setDieWhenCollected(die: Boolean) {
        mDieOnCollect = true
    }

    fun setDieOnAttack(die: Boolean) {
        mDieOnAttack = die
    }

    fun setInvincible(invincible: Boolean) {
        mInvincible = invincible
    }

    fun setPossessionComponent(component: ChangeComponentsComponent?) {
        mPossessionComponent = component
    }

    fun setInventoryUpdate(update: UpdateRecord?) {
        mInventoryUpdate = update
    }

    fun setLauncherComponent(component: LauncherComponent?, launchHitType: Int) {
        mLauncherComponent = component
        mLauncherHitType = launchHitType
    }

    fun setSpawnGameEventOnHit(hitType: Int, gameFlowEventType: Int, indexData: Int) {
        mGameEventHitType = hitType
        mGameEventOnHit = gameFlowEventType
        mGameEventIndexData = indexData
        if (hitType == HitType.INVALID) {
            // The game event has been cleared, so reset the timer blocking a
            // subsequent event.
            mLastGameEventTime = -1.0f
        }
    }

    fun setForceInvincible(force: Boolean) {
        mForceInvincibility = force
    }

    fun setTakeHitSound(hitType: Int, sound: Sound?) {
        mTakeHitSoundHitType = hitType
        mTakeHitSound = sound
    }

    fun setDealHitSound(hitType: Int, sound: Sound?) {
        mDealHitSound = sound
        mDealHitSoundHitType = hitType
    }

    fun setSpawnOnDealHit(hitType: Int, objectType: GameObjectType?, alignToVictimX: Boolean,
                          alignToVicitmY: Boolean) {
        mSpawnOnDealHitObjectType = objectType
        mSpawnOnDealHitHitType = hitType
        mAlignDealHitObjectToVictimX = alignToVictimX
        mAlignDealHitObjectToVictimY = alignToVicitmY
    }

    companion object {
        private const val ATTACK_PAUSE_DELAY = 1.0f / 60 * 4
        private const val DEFAULT_BOUNCE_MAGNITUDE = 200.0f
        private const val EVENT_SEND_DELAY = 5.0f
    }

    init {
        reset()
        setPhaseToThis(ComponentPhases.PRE_DRAW.ordinal)
    }
}
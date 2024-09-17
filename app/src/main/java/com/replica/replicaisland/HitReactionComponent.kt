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
    private var pauseOnAttack = false
    private var pauseOnAttackTime = 0f
    private var bounceOnHit = false
    private var bounceMagnitude = 0f
    private var invincibleAfterHitTime = 0f
    private var lastHitTime = 0f
    private var mInvincible = false
    private var dieOnCollect = false
    private var dieOnAttack = false
    private var possessionComponent: ChangeComponentsComponent? = null
    private var inventoryUpdate: UpdateRecord? = null
    private var launcherComponent: LauncherComponent? = null
    private var launcherHitType = 0
    private var invincibleTime = 0f
    private var gameEventHitType = 0
    private var gameEventOnHit = 0
    private var gameEventIndexData = 0
    private var lastGameEventTime = 0f
    private var forceInvincibility = false
    private var takeHitSound: Sound? = null
    private var dealHitSound: Sound? = null
    private var dealHitSoundHitType = 0
    private var takeHitSoundHitType = 0
    private var spawnOnDealHitObjectType: GameObjectType? = null
    private var spawnOnDealHitHitType = 0
    private var alignDealHitObjectToVictimX = false
    private var alignDealHitObjectToVictimY = false
    override fun reset() {
        pauseOnAttack = false
        pauseOnAttackTime = ATTACK_PAUSE_DELAY
        bounceOnHit = false
        bounceMagnitude = DEFAULT_BOUNCE_MAGNITUDE
        invincibleAfterHitTime = 0.0f
        mInvincible = false
        dieOnCollect = false
        dieOnAttack = false
        possessionComponent = null
        inventoryUpdate = null
        launcherComponent = null
        launcherHitType = HitType.LAUNCH
        invincibleTime = 0.0f
        gameEventOnHit = -1
        gameEventIndexData = 0
        lastGameEventTime = -1.0f
        gameEventHitType = HitType.INVALID
        forceInvincibility = false
        takeHitSound = null
        dealHitSound = null
        spawnOnDealHitObjectType = GameObjectType.INVALID
        spawnOnDealHitHitType = HitType.INVALID
        dealHitSoundHitType = HitType.INVALID
        alignDealHitObjectToVictimX = false
        alignDealHitObjectToVictimY = false
    }

    /** Called when this object attacks another object.  */
    fun hitVictim(parent: GameObject, victim: GameObject, hitType: Int,
                  hitAccepted: Boolean) {
        if (hitAccepted) {
            if (pauseOnAttack && hitType == HitType.HIT) {
                val time = sSystemRegistry.timeSystem
                time!!.freeze(pauseOnAttackTime)
            }
            if (dieOnAttack) {
                parent.life = 0
            }
            if (hitType == launcherHitType && launcherComponent != null) {
                launcherComponent!!.prepareToLaunch(victim, parent)
            }
            if (dealHitSound != null &&
                    (hitType == dealHitSoundHitType ||
                            dealHitSoundHitType == HitType.INVALID)) {
                val sound = sSystemRegistry.soundSystem
                sound?.play(dealHitSound!!, false, SoundSystem.PRIORITY_NORMAL)
            }
            if (spawnOnDealHitObjectType != GameObjectType.INVALID &&
                    hitType == spawnOnDealHitHitType) {
                val x = if (alignDealHitObjectToVictimX) victim.position.x else parent.position.x
                val y = if (alignDealHitObjectToVictimY) victim.position.y else parent.position.y
                val factory = sSystemRegistry.gameObjectFactory
                val manager = sSystemRegistry.gameObjectManager
                if (factory != null) {
                    val thing = factory.spawn(spawnOnDealHitObjectType!!, x,
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
        if (gameEventHitType == hitType &&
                gameEventHitType != HitType.INVALID) {
            if (lastGameEventTime < 0.0f || gameTime > lastGameEventTime + EVENT_SEND_DELAY) {
                val level = sSystemRegistry.levelSystem
                level!!.sendGameEvent(gameEventOnHit, gameEventIndexData, true)
            } else {
                // special case.  If we're waiting for a hit type to spawn an event and
                // another event has just happened, eat this hit so we don't miss
                // the chance to send the event.
                hitType = HitType.INVALID
            }
            lastGameEventTime = gameTime
        }
        when (hitType) {
            HitType.INVALID -> {
            }
            HitType.HIT -> {
                // don't hit our friends, if we have friends.
                val sameTeam = parent.team == attacker.team && parent.team != Team.NONE
                if (!forceInvincibility && !mInvincible && parent.life > 0 && !sameTeam) {
                    parent.life -= 1
                    if (bounceOnHit && parent.life > 0) {
                        val pool = sSystemRegistry.vectorPool
                        val newVelocity = pool!!.allocate(parent.position)
                        newVelocity.subtract(attacker.position)
                        newVelocity[0.5f * Utils.sign(newVelocity.x)] = 0.5f * Utils.sign(newVelocity.y)
                        newVelocity.multiply(bounceMagnitude)
                        parent.velocity = newVelocity
                        parent.targetVelocity.zero()
                        pool.release(newVelocity)
                    }
                    if (invincibleAfterHitTime > 0.0f) {
                        mInvincible = true
                        invincibleTime = invincibleAfterHitTime
                    }
                } else {
                    // Ignore this hit.
                    hitType = HitType.INVALID
                }
            }
            HitType.DEATH ->                 // respect teams?
                parent.life = 0
            HitType.COLLECT -> {
                if (inventoryUpdate != null && parent.life > 0) {
                    val attackerInventory = attacker.findByClass(InventoryComponent::class.java)
                    if (attackerInventory != null) {
                        (attackerInventory).applyUpdate(inventoryUpdate!!)
                    }

                }
                if (dieOnCollect && parent.life > 0) {
                    parent.life = 0
                }
            }
            HitType.POSSESS -> if (possessionComponent != null && parent.life > 0 && attacker.life > 0) {
                possessionComponent!!.activate(parent)
            } else {
                hitType = HitType.INVALID
            }
            HitType.LAUNCH -> {
            }
            else -> {
            }
        }
        if (hitType != HitType.INVALID) {
            if (takeHitSound != null && hitType == takeHitSoundHitType) {
                val sound = sSystemRegistry.soundSystem
                sound?.play(takeHitSound!!, false, SoundSystem.PRIORITY_NORMAL)
            }
            lastHitTime = gameTime
            parent.currentAction = ActionType.HIT_REACT
            parent.lastReceivedHitType = hitType
        }
        return hitType != HitType.INVALID
    }

    override fun update(timeDelta: Float, parent: BaseObject?) {
        val parentObject = parent as GameObject
        val time = sSystemRegistry.timeSystem
        val gameTime = time!!.gameTime
        if (mInvincible && invincibleTime > 0) {
            if (time.gameTime > lastHitTime + invincibleTime) {
                mInvincible = false
            }
        }

        // This means that the lastReceivedHitType will persist for two frames, giving all systems
        // a chance to react.
        if (gameTime - lastHitTime > timeDelta) {
            parentObject.lastReceivedHitType = HitType.INVALID
        }
    }

    fun setPauseOnAttack(pause: Boolean) {
        pauseOnAttack = pause
    }

    fun setPauseOnAttackTime(seconds: Float) {
        pauseOnAttackTime = seconds
    }

    fun setBounceOnHit(bounce: Boolean) {
        bounceOnHit = bounce
    }

    fun setBounceMagnitude(magnitude: Float) {
        bounceMagnitude = magnitude
    }

    fun setInvincibleTime(time: Float) {
        invincibleAfterHitTime = time
    }

    fun setDieWhenCollected(die: Boolean) {
        dieOnCollect = true
    }

    fun setDieOnAttack(die: Boolean) {
        dieOnAttack = die
    }

    fun setInvincible(invincible: Boolean) {
        mInvincible = invincible
    }

    fun setPossessionComponent(component: ChangeComponentsComponent?) {
        possessionComponent = component
    }

    fun setInventoryUpdate(update: UpdateRecord?) {
        inventoryUpdate = update
    }

    fun setLauncherComponent(component: LauncherComponent?, launchHitType: Int) {
        launcherComponent = component
        launcherHitType = launchHitType
    }

    fun setSpawnGameEventOnHit(hitType: Int, gameFlowEventType: Int, indexData: Int) {
        gameEventHitType = hitType
        gameEventOnHit = gameFlowEventType
        gameEventIndexData = indexData
        if (hitType == HitType.INVALID) {
            // The game event has been cleared, so reset the timer blocking a
            // subsequent event.
            lastGameEventTime = -1.0f
        }
    }

    fun setForceInvincible(force: Boolean) {
        forceInvincibility = force
    }

    fun setTakeHitSound(hitType: Int, sound: Sound?) {
        takeHitSoundHitType = hitType
        takeHitSound = sound
    }

    fun setDealHitSound(hitType: Int, sound: Sound?) {
        dealHitSound = sound
        dealHitSoundHitType = hitType
    }

    fun setSpawnOnDealHit(hitType: Int, objectType: GameObjectType?, alignToVictimX: Boolean,
                          alignToVicitmY: Boolean) {
        spawnOnDealHitObjectType = objectType
        spawnOnDealHitHitType = hitType
        alignDealHitObjectToVictimX = alignToVictimX
        alignDealHitObjectToVictimY = alignToVicitmY
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
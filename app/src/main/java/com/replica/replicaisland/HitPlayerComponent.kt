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
package com.replica.replicaisland

import com.replica.replicaisland.HitReactionComponent

class HitPlayerComponent : GameComponent() {
    private var mDistance2 = 0f
    private var mPlayerPosition: Vector2 = Vector2()
    private var mMyPosition: Vector2 = Vector2()
    private var mHitReact: HitReactionComponent? = null
    private var mHitType = 0
    private var mHitDirection = false
    override fun reset() {
        mDistance2 = 0.0f
        mPlayerPosition.zero()
        mMyPosition.zero()
        mHitReact = null
        mHitType = CollisionParameters.HitType.INVALID
        mHitDirection = false // by default, hit myself
    }

    override fun update(timeDelta: Float, parent: BaseObject?) {
        val manager = sSystemRegistry.gameObjectManager
        if (manager != null && mHitReact != null) {
            val player = manager.player
            if (player != null && player.life > 0) {
                mPlayerPosition[player.centeredPositionX] = player.centeredPositionY
                val parentObject = parent as GameObject
                mMyPosition[parentObject.centeredPositionX] = parentObject.centeredPositionY
                if (mMyPosition.distance2(mPlayerPosition) <= mDistance2) {
                    val playerHitReact = player.findByClass(HitReactionComponent::class.java)
                    if (playerHitReact != null) {
                        if (!mHitDirection) {
                            // hit myself
                            val accepted = mHitReact!!.receivedHit(parentObject, player, mHitType)
                            playerHitReact.hitVictim(player, parentObject, mHitType, accepted)
                        } else {
                            // hit the player
                            val accepted =
                                    playerHitReact.receivedHit(player, parentObject, mHitType)
                            mHitReact!!.hitVictim(parentObject, player, mHitType, accepted)
                        }
                    }
                }
            }
        }
    }

    fun setup(distance: Float, hitReact: HitReactionComponent?, hitType: Int, hitPlayer: Boolean) {
        mDistance2 = distance * distance
        mHitReact = hitReact
        mHitType = hitType
        mHitDirection = hitPlayer
    }

    init {
        reset()
        setPhaseToThis(ComponentPhases.THINK.ordinal)
    }
}
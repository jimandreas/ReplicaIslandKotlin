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
@file:Suppress("MoveVariableDeclarationIntoWhen")

package com.replica.replicaisland

import com.replica.replicaisland.CollisionParameters.HitType

class SelectDialogComponent : GameComponent() {
    private var mHitReact: HitReactionComponent? = null
    private val mLastPosition: Vector2
    override fun reset() {
        mHitReact = null
        mLastPosition.zero()
    }

    override fun update(timeDelta: Float, parent: BaseObject?) {
        val hotSpot = sSystemRegistry.hotSpotSystem
        if (hotSpot != null && mHitReact != null) {
            val parentObject = parent as GameObject
            val currentPosition = parentObject.position
            if (mLastPosition.distance2(parentObject.position) > 0.0f) {
                mLastPosition.set(currentPosition)
                val hitSpot = hotSpot.getHotSpot(parentObject.centeredPositionX, currentPosition.y + 10)
                when (hitSpot) {
                    HotSpotSystem.HotSpotType.NPC_SELECT_DIALOG_1_1,
                    HotSpotSystem.HotSpotType.NPC_SELECT_DIALOG_1_2,
                    HotSpotSystem.HotSpotType.NPC_SELECT_DIALOG_1_3,
                    HotSpotSystem.HotSpotType.NPC_SELECT_DIALOG_1_4,
                    HotSpotSystem.HotSpotType.NPC_SELECT_DIALOG_1_5,
                    HotSpotSystem.HotSpotType.NPC_SELECT_DIALOG_2_1,
                    HotSpotSystem.HotSpotType.NPC_SELECT_DIALOG_2_2,
                    HotSpotSystem.HotSpotType.NPC_SELECT_DIALOG_2_3,
                    HotSpotSystem.HotSpotType.NPC_SELECT_DIALOG_2_4,
                    HotSpotSystem.HotSpotType.NPC_SELECT_DIALOG_2_5 -> {

                        var event = GameFlowEvent.EVENT_SHOW_DIALOG_CHARACTER1
                        var index = hitSpot - HotSpotSystem.HotSpotType.NPC_SELECT_DIALOG_1_1
                        if (hitSpot >= HotSpotSystem.HotSpotType.NPC_SELECT_DIALOG_2_1) {
                            event = GameFlowEvent.EVENT_SHOW_DIALOG_CHARACTER2
                            index = hitSpot - HotSpotSystem.HotSpotType.NPC_SELECT_DIALOG_2_1
                        }
                        mHitReact!!.setSpawnGameEventOnHit(HitType.COLLECT, event, index)
                    }
                }
            }
        }
    }

    fun setHitReact(hit: HitReactionComponent?) {
        mHitReact = hit
    }

    init {
        setPhaseToThis(ComponentPhases.THINK.ordinal)
        mLastPosition = Vector2()
    }
}
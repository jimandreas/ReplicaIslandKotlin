package com.replica.replicaisland.entities

import com.replica.replicaisland.GameComponent
import com.replica.replicaisland.core.BaseObject
import com.replica.replicaisland.core.GameObject
import com.replica.replicaisland.mechanics.CollisionParameters
import com.replica.replicaisland.mechanics.GameFlowEvent
import com.replica.replicaisland.mechanics.HotSpotSystem
import com.replica.replicaisland.utils.Vector2

class SelectDialogComponent : GameComponent() {
    private var hitReact: HitReactionComponent? = null
    private val lastPosition: Vector2
    override fun reset() {
        hitReact = null
        lastPosition.zero()
    }

    override fun update(timeDelta: Float, parent: BaseObject?) {
        val hotSpot = sSystemRegistry.hotSpotSystem
        if (hotSpot != null && hitReact != null) {
            val parentObject = parent as GameObject
            val currentPosition = parentObject.position
            if (lastPosition.distance2(parentObject.position) > 0.0f) {
                lastPosition.set(currentPosition)
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
                        hitReact!!.setSpawnGameEventOnHit(CollisionParameters.HitType.COLLECT, event, index)
                    }
                }
            }
        }
    }

    fun setHitReact(hit: HitReactionComponent?) {
        hitReact = hit
    }

    init {
        setPhaseToThis(ComponentPhases.THINK.ordinal)
        lastPosition = Vector2()
    }
}
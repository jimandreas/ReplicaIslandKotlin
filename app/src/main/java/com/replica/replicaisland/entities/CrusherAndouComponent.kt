package com.replica.replicaisland.entities

import com.replica.replicaisland.GameComponent
import com.replica.replicaisland.core.BaseObject
import com.replica.replicaisland.core.GameObject

class CrusherAndouComponent : GameComponent() {
    private var mSwap: ChangeComponentsComponent? = null
    override fun reset() {
        mSwap = null
    }

    override fun update(timeDelta: Float, parent: BaseObject?) {
        val parentObject = parent as GameObject
        if (mSwap!!.currentlySwapped) {
            if (parentObject.touchingGround()) {
                parentObject.currentAction = GameObject.ActionType.IDLE
            }
        } else {
            val input = sSystemRegistry.inputSystem
            if (input!!.fetchTouchScreen().getTriggered(sSystemRegistry.timeSystem!!.gameTime)) {
                parentObject.currentAction = GameObject.ActionType.ATTACK
                mSwap!!.activate(parentObject)
            }
        }
    }

    fun setSwap(swap: ChangeComponentsComponent?) {
        mSwap = swap
    }

    init {
        setPhaseToThis(ComponentPhases.THINK.ordinal)
        reset()
    }
}
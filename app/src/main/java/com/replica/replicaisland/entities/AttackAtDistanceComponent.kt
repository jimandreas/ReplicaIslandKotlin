package com.replica.replicaisland.entities

import com.replica.replicaisland.GameComponent
import com.replica.replicaisland.core.BaseObject
import com.replica.replicaisland.core.GameObject
import com.replica.replicaisland.utils.Utils
import com.replica.replicaisland.utils.Vector2

class AttackAtDistanceComponent : GameComponent() {
    private var attackDistance = 0f
    private var attackDelay = 0f
    private var attackLength = 0f
    private var attackStartTime = 0f
    private var mRequireFacing = false
    private val mDistance: Vector2
    override fun reset() {
        attackDelay = 0f
        attackLength = 0f
        attackDistance = DEFAULT_ATTACK_DISTANCE.toFloat()
        mRequireFacing = false
    }

    override fun update(timeDelta: Float, parent: BaseObject?) {
        val parentObject = parent as GameObject
        val manager = sSystemRegistry.gameObjectManager
        if (manager != null) {
            val player = manager.player
            if (player != null) {
                mDistance.set(player.position)
                mDistance.subtract(parentObject.position)
                val time = sSystemRegistry.timeSystem
                val currentTime = time!!.gameTime
                val facingPlayer = (Utils.sign(player.position.x - parentObject.position.x)
                        == Utils.sign(parentObject.facingDirection.x))
                val facingDirectionCorrect = (mRequireFacing && facingPlayer
                        || !mRequireFacing)
                if (parentObject.currentAction == GameObject.ActionType.ATTACK) {
                    if (currentTime > attackStartTime + attackLength) {
                        parentObject.currentAction = GameObject.ActionType.IDLE
                    }
                } else if (mDistance.length2() < attackDistance * attackDistance
                        && currentTime > attackStartTime + attackLength + attackDelay
                        && facingDirectionCorrect) {
                    attackStartTime = currentTime
                    parentObject.currentAction = GameObject.ActionType.ATTACK
                } else {
                    parentObject.currentAction = GameObject.ActionType.IDLE
                }
            }
        }
    }

    fun setupAttack(distance: Float, delay: Float, duration: Float, requireFacing: Boolean) {
        attackDistance = distance
        attackDelay = delay
        attackLength = duration
        mRequireFacing = requireFacing
    }

    companion object {
        private const val DEFAULT_ATTACK_DISTANCE = 100
    }

    init {
        setPhaseToThis(ComponentPhases.THINK.ordinal)
        mDistance = Vector2()
        reset()
    }
}
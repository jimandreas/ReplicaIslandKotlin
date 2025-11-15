package com.replica.replicaisland.core

import com.replica.replicaisland.mechanics.CollisionParameters
import com.replica.replicaisland.core.PhasedObjectManager
import com.replica.replicaisland.Utils
import com.replica.replicaisland.Vector2

/**
 * GameObject defines any object that resides in the game world (character, background, special
 * effect, enemy, etc).  It is a collection of GameComponents which implement its behavior;
 * GameObjects themselves have no intrinsic behavior.  GameObjects are also "bags of data" that
 * components can use to share state (direct component-to-component communication is discouraged).
 */
class GameObject : PhasedObjectManager() {
    // These fields are managed by components.
    private val mPosition: Vector2 = Vector2()
    private val mVelocity: Vector2 = Vector2()
    private val mTargetVelocity: Vector2 = Vector2()
    private val mAcceleration: Vector2 = Vector2()
    private val mImpulse: Vector2 = Vector2()
    private val mBackgroundCollisionNormal: Vector2 = Vector2()
    var lastTouchedFloorTime = 0f
    var lastTouchedCeilingTime = 0f
    var lastTouchedLeftWallTime = 0f
    var lastTouchedRightWallTime = 0f
    @JvmField
    var positionLocked = false
    @JvmField
    var activationRadius = 0f
    @JvmField
    var destroyOnDeactivation = false
    @JvmField
    var life = 0
    @JvmField
    var lastReceivedHitType = 0
    @JvmField
    var facingDirection: Vector2 = Vector2(1f, 0f)

    @JvmField
    var width = 0f
    @JvmField
    var height = 0f

    enum class ActionType {
        INVALID, IDLE, MOVE, ATTACK, HIT_REACT, DEATH, HIDE, FROZEN
    }

    var currentAction: ActionType? = null

    enum class Team {
        NONE, PLAYER, ENEMY
    }

    @JvmField
    var team: Team? = null
    override fun reset() {
        removeAll()
        commitUpdates()
        mPosition.zero()
        mVelocity.zero()
        mTargetVelocity.zero()
        mAcceleration.zero()
        mImpulse.zero()
        mBackgroundCollisionNormal.zero()
        facingDirection[1.0f] = 1.0f
        currentAction = ActionType.INVALID
        positionLocked = false
        activationRadius = 0f
        destroyOnDeactivation = false
        life = DEFAULT_LIFE
        team = Team.NONE
        width = 0.0f
        height = 0.0f
        lastReceivedHitType = CollisionParameters.HitType.INVALID
    }

    // Utility functions
    fun touchingGround(): Boolean {
        val time = sSystemRegistry.timeSystem
        val gameTime = time!!.gameTime
        return gameTime > 0.1f &&
                Utils.Companion.close(lastTouchedFloorTime, time.gameTime, COLLISION_SURFACE_DECAY_TIME)
    }

    fun touchingCeiling(): Boolean {
        val time = sSystemRegistry.timeSystem
        val gameTime = time!!.gameTime
        return gameTime > 0.1f &&
                Utils.Companion.close(lastTouchedCeilingTime, time.gameTime, COLLISION_SURFACE_DECAY_TIME)
    }

    fun touchingLeftWall(): Boolean {
        val time = sSystemRegistry.timeSystem
        val gameTime = time!!.gameTime
        return gameTime > 0.1f &&
                Utils.Companion.close(lastTouchedLeftWallTime, time.gameTime, COLLISION_SURFACE_DECAY_TIME)
    }

    fun touchingRightWall(): Boolean {
        val time = sSystemRegistry.timeSystem
        val gameTime = time!!.gameTime
        return gameTime > 0.1f &&
                Utils.Companion.close(lastTouchedRightWallTime, time.gameTime, COLLISION_SURFACE_DECAY_TIME)
    }

    var position: Vector2
        get() = mPosition
        set(position) {
            mPosition.set(position)
        }
    val centeredPositionX: Float
        get() = mPosition.x + width / 2.0f
    val centeredPositionY: Float
        get() = mPosition.y + height / 2.0f
    var velocity: Vector2
        get() = mVelocity
        set(velocity) {
            mVelocity.set(velocity)
        }
    var targetVelocity: Vector2
        get() = mTargetVelocity
        set(targetVelocity) {
            mTargetVelocity.set(targetVelocity)
        }
    var acceleration: Vector2
        get() = mAcceleration
        set(acceleration) {
            mAcceleration.set(acceleration)
        }
    var impulse: Vector2
        get() = mImpulse
        set(impulse) {
            mImpulse.set(impulse)
        }
    var backgroundCollisionNormal: Vector2
        get() = mBackgroundCollisionNormal
        set(normal) {
            mBackgroundCollisionNormal.set(normal)
        }

    companion object {
        private const val COLLISION_SURFACE_DECAY_TIME = 0.3f
        private const val DEFAULT_LIFE = 1
    }

    init {
        reset()
    }
}
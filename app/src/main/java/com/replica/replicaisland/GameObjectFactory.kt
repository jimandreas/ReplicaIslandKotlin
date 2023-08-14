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
@file:Suppress("RemoveEmptySecondaryConstructorBody", "MemberVisibilityCanBePrivate", "BooleanLiteralArgument", "NON_EXHAUSTIVE_WHEN", "UNUSED_PARAMETER", "SENSELESS_COMPARISON", "VARIABLE_WITH_REDUNDANT_INITIALIZER", "LocalVariableName", "unused")

package com.replica.replicaisland


import com.replica.replicaisland.AnimationComponent.PlayerAnimations
import com.replica.replicaisland.CollisionParameters.HitType
import com.replica.replicaisland.EnemyAnimationComponent.EnemyAnimations
import com.replica.replicaisland.GameObject.ActionType
import com.replica.replicaisland.GameObject.Team
import com.replica.replicaisland.InventoryComponent.UpdateRecord
import com.replica.replicaisland.PlayerComponent.Companion.difficultyConstants

import java.util.*
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/** A class for generating game objects at runtime.
 * This should really be replaced with something that is data-driven, but it is hard to do data
 * parsing quickly at runtime.  For the moment this class is full of large functions that just
 * patch pointers between objects, but in the future those functions should either be
 * a) generated from data at compile time, or b) described by data at runtime.
 */
class GameObjectFactory : BaseObject() {
    private val staticBaseObjectArray: FixedSizeArray<FixedSizeArray<BaseObject?>?>
    private val componentPools: FixedSizeArray<GameComponentPool>
    private val poolSearchDummy: GameComponentPool
    private val gameObjectPool: GameObjectPool
    private val tightActivationRadius: Float
    private val normalActivationRadius: Float
    private val wideActivationRadius: Float
    private val alwaysActive: Float

    // A list of game objects that can be spawned at runtime.  Note that the indicies of these
    // objects must match the order of the object tileset in the level editor in order for the
    // level content to make sense.
    enum class GameObjectType(private val mIndex: Int) {
        INVALID(-1),
        PLAYER(0),

        // Collectables
        COIN(1),
        RUBY(2),
        DIARY(3),

        // Characters
        WANDA(10),
        KYLE(11),
        KYLE_DEAD(12),
        ANDOU_DEAD(13),
        KABOCHA(26),
        ROKUDOU_TERMINAL(27),
        KABOCHA_TERMINAL(28),
        EVIL_KABOCHA(29),
        ROKUDOU(30),

        // AI
        BROBOT(16),
        SNAILBOMB(17),
        SHADOWSLIME(18),
        MUDMAN(19),
        SKELETON(20),
        KARAGUIN(21),
        PINK_NAMAZU(22),
        TURRET(23),
        TURRET_LEFT(24),
        BAT(6),
        STING(7),
        ONION(8),

        // Objects
        DOOR_RED(32),
        DOOR_BLUE(33),
        DOOR_GREEN(34),
        BUTTON_RED(35),
        BUTTON_BLUE(36),
        BUTTON_GREEN(37),
        CANNON(38),
        BROBOT_SPAWNER(39),
        BROBOT_SPAWNER_LEFT(40),
        BREAKABLE_BLOCK(41),
        THE_SOURCE(42),
        HINT_SIGN(43),

        // Effects
        DUST(48),
        EXPLOSION_SMALL(49),
        EXPLOSION_LARGE(50),
        EXPLOSION_GIANT(51),

        // Special Spawnable
        DOOR_RED_NONBLOCKING(52),
        DOOR_BLUE_NONBLOCKING(53),
        DOOR_GREEN_NONBLOCKING(54),
        GHOST_NPC(55),
        CAMERA_BIAS(56),
        FRAMERATE_WATCHER(57),
        INFINITE_SPAWNER(58),
        CRUSHER_ANDOU(59),

        // Projectiles
        ENERGY_BALL(68),
        CANNON_BALL(65),
        TURRET_BULLET(66),
        BROBOT_BULLET(67),
        BREAKABLE_BLOCK_PIECE(68),
        BREAKABLE_BLOCK_PIECE_SPAWNER(69),
        WANDA_SHOT(70),

        // Special Objects -- Not spawnable normally
        SMOKE_BIG(-1),
        SMOKE_SMALL(-1),
        CRUSH_FLASH(-1),
        FLASH(-1),
        PLAYER_JETS(-1),
        PLAYER_SPARKS(-1),
        PLAYER_GLOW(-1),
        ENEMY_SPARKS(-1),
        GHOST(-1),
        SMOKE_POOF(-1),
        GEM_EFFECT(-1),
        GEM_EFFECT_SPAWNER(-1),

        // End
        OBJECT_COUNT(-1);

        fun index(): Int {
            return mIndex
        }

        companion object {
            // TODO: Is there any better way to do this?
            fun indexToType(index: Int): GameObjectType {
                val valuesArray = values()
                var foundType = INVALID
                for (x in valuesArray.indices) {
                    val type = valuesArray[x]
                    if (type.mIndex == index) {
                        foundType = type
                        break
                    }
                }
                return foundType
            }
        }
    }

    override fun reset() {}
    private fun getComponentPool(componentType: Class<*>?): GameComponentPool? {
        var pool: GameComponentPool? = null
        poolSearchDummy.objectClass = componentType
        val index = componentPools.find(poolSearchDummy, false)
        if (index != -1) {
            pool = componentPools[index]
        }
        return pool
    }

    private fun allocateComponent(componentType: Class<*>?): GameComponent? {
        val pool = getComponentPool(componentType)!!
        var component: GameComponent? = null
        if (pool != null) {
            component = pool.allocate()
        }
        return component
    }

    fun releaseComponent(component: GameComponent?) {
        val pool = getComponentPool(component!!.javaClass)!!
        if (pool != null) {
            component.reset()
            component.shared = false
            pool.release(component)
        }
    }

    private fun componentAvailable(componentType: Class<*>?, count: Int): Boolean {
        var canAllocate = false
        val pool = getComponentPool(componentType)!!
        if (pool != null) {
            canAllocate = pool.fetchAllocatedCount() + count < pool.fetchSize()
        }
        return canAllocate
    }

    fun preloadEffects() {
        // These textures appear in every level, so they are long-term.
        val textureLibrary = sSystemRegistry.longTermTextureLibrary
        textureLibrary!!.allocateTexture(R.drawable.dust01)
        textureLibrary.allocateTexture(R.drawable.dust02)
        textureLibrary.allocateTexture(R.drawable.dust03)
        textureLibrary.allocateTexture(R.drawable.dust04)
        textureLibrary.allocateTexture(R.drawable.dust05)
        textureLibrary.allocateTexture(R.drawable.effect_energyball01)
        textureLibrary.allocateTexture(R.drawable.effect_energyball02)
        textureLibrary.allocateTexture(R.drawable.effect_energyball03)
        textureLibrary.allocateTexture(R.drawable.effect_energyball04)
        textureLibrary.allocateTexture(R.drawable.effect_explosion_small01)
        textureLibrary.allocateTexture(R.drawable.effect_explosion_small02)
        textureLibrary.allocateTexture(R.drawable.effect_explosion_small03)
        textureLibrary.allocateTexture(R.drawable.effect_explosion_small04)
        textureLibrary.allocateTexture(R.drawable.effect_explosion_small05)
        textureLibrary.allocateTexture(R.drawable.effect_explosion_small06)
        textureLibrary.allocateTexture(R.drawable.effect_explosion_small07)
        textureLibrary.allocateTexture(R.drawable.effect_explosion_big01)
        textureLibrary.allocateTexture(R.drawable.effect_explosion_big02)
        textureLibrary.allocateTexture(R.drawable.effect_explosion_big03)
        textureLibrary.allocateTexture(R.drawable.effect_explosion_big04)
        textureLibrary.allocateTexture(R.drawable.effect_explosion_big05)
        textureLibrary.allocateTexture(R.drawable.effect_explosion_big06)
        textureLibrary.allocateTexture(R.drawable.effect_explosion_big07)
        textureLibrary.allocateTexture(R.drawable.effect_explosion_big08)
        textureLibrary.allocateTexture(R.drawable.effect_explosion_big09)
        textureLibrary.allocateTexture(R.drawable.effect_smoke_big01)
        textureLibrary.allocateTexture(R.drawable.effect_smoke_big02)
        textureLibrary.allocateTexture(R.drawable.effect_smoke_big03)
        textureLibrary.allocateTexture(R.drawable.effect_smoke_big04)
        textureLibrary.allocateTexture(R.drawable.effect_smoke_big05)
        textureLibrary.allocateTexture(R.drawable.effect_smoke_small01)
        textureLibrary.allocateTexture(R.drawable.effect_smoke_small02)
        textureLibrary.allocateTexture(R.drawable.effect_smoke_small03)
        textureLibrary.allocateTexture(R.drawable.effect_smoke_small04)
        textureLibrary.allocateTexture(R.drawable.effect_smoke_small05)
        textureLibrary.allocateTexture(R.drawable.effect_crush_back01)
        textureLibrary.allocateTexture(R.drawable.effect_crush_back02)
        textureLibrary.allocateTexture(R.drawable.effect_crush_back03)
        textureLibrary.allocateTexture(R.drawable.effect_crush_front01)
        textureLibrary.allocateTexture(R.drawable.effect_crush_front02)
        textureLibrary.allocateTexture(R.drawable.effect_crush_front03)
        textureLibrary.allocateTexture(R.drawable.effect_crush_front04)
        textureLibrary.allocateTexture(R.drawable.effect_crush_front05)
        textureLibrary.allocateTexture(R.drawable.effect_crush_front06)
        textureLibrary.allocateTexture(R.drawable.effect_crush_front07)
    }

    fun destroy(thing: GameObject) {
        thing.commitUpdates()
        val componentCount = thing.fetchCount()
        for (x in 0 until componentCount) {
            val component = thing.fetch(x) as GameComponent?
            if (!component!!.shared) {
                releaseComponent(component)
            }
        }
        thing.removeAll()
        thing.commitUpdates()
        gameObjectPool.release(thing)
    }

    fun spawn(type: GameObjectType, x: Float, y: Float, horzFlip: Boolean): GameObject? {
        var newObject: GameObject? = null
        when (type) {
            GameObjectType.PLAYER -> newObject = spawnPlayer(x, y)
            GameObjectType.COIN -> newObject = spawnCoin(x, y)
            GameObjectType.RUBY -> newObject = spawnRuby(x, y)
            GameObjectType.DIARY -> newObject = spawnDiary(x, y)
            GameObjectType.WANDA -> newObject = spawnEnemyWanda(x, y, true)
            GameObjectType.KYLE -> newObject = spawnEnemyKyle(x, y, true)
            GameObjectType.KYLE_DEAD -> newObject = spawnEnemyKyleDead(x, y)
            GameObjectType.ANDOU_DEAD -> newObject = spawnEnemyAndouDead(x, y)
            GameObjectType.KABOCHA -> newObject = spawnEnemyKabocha(x, y, true)
            GameObjectType.ROKUDOU_TERMINAL -> newObject = spawnRokudouTerminal(x, y)
            GameObjectType.KABOCHA_TERMINAL -> newObject = spawnKabochaTerminal(x, y)
            GameObjectType.EVIL_KABOCHA -> newObject = spawnEnemyEvilKabocha(x, y, true)
            GameObjectType.ROKUDOU -> newObject = spawnEnemyRokudou(x, y, true)
            GameObjectType.BROBOT -> newObject = spawnEnemyBrobot(x, y, horzFlip)
            GameObjectType.SNAILBOMB -> newObject = spawnEnemySnailBomb(x, y, horzFlip)
            GameObjectType.SHADOWSLIME -> newObject = spawnEnemyShadowSlime(x, y, horzFlip)
            GameObjectType.MUDMAN -> newObject = spawnEnemyMudman(x, y, horzFlip)
            GameObjectType.SKELETON -> newObject = spawnEnemySkeleton(x, y, horzFlip)
            GameObjectType.KARAGUIN -> newObject = spawnEnemyKaraguin(x, y, horzFlip)
            GameObjectType.PINK_NAMAZU -> newObject = spawnEnemyPinkNamazu(x, y, horzFlip)
            GameObjectType.BAT -> newObject = spawnEnemyBat(x, y, horzFlip)
            GameObjectType.STING -> newObject = spawnEnemySting(x, y, horzFlip)
            GameObjectType.ONION -> newObject = spawnEnemyOnion(x, y, horzFlip)
            GameObjectType.TURRET, GameObjectType.TURRET_LEFT -> newObject = spawnObjectTurret(x, y, type == GameObjectType.TURRET_LEFT)
            GameObjectType.DOOR_RED, GameObjectType.DOOR_RED_NONBLOCKING -> newObject = spawnObjectDoor(x, y, GameObjectType.DOOR_RED, type == GameObjectType.DOOR_RED)
            GameObjectType.DOOR_BLUE, GameObjectType.DOOR_BLUE_NONBLOCKING -> newObject = spawnObjectDoor(x, y, GameObjectType.DOOR_BLUE, type == GameObjectType.DOOR_BLUE)
            GameObjectType.DOOR_GREEN, GameObjectType.DOOR_GREEN_NONBLOCKING -> newObject = spawnObjectDoor(x, y, GameObjectType.DOOR_GREEN, type == GameObjectType.DOOR_GREEN)
            GameObjectType.BUTTON_RED -> newObject = spawnObjectButton(x, y, GameObjectType.BUTTON_RED)
            GameObjectType.BUTTON_BLUE -> newObject = spawnObjectButton(x, y, GameObjectType.BUTTON_BLUE)
            GameObjectType.BUTTON_GREEN -> newObject = spawnObjectButton(x, y, GameObjectType.BUTTON_GREEN)
            GameObjectType.CANNON -> newObject = spawnObjectCannon(x, y)
            GameObjectType.BROBOT_SPAWNER, GameObjectType.BROBOT_SPAWNER_LEFT -> newObject = spawnObjectBrobotSpawner(x, y, type == GameObjectType.BROBOT_SPAWNER_LEFT)
            GameObjectType.BREAKABLE_BLOCK -> newObject = spawnObjectBreakableBlock(x, y)
            GameObjectType.THE_SOURCE -> newObject = spawnObjectTheSource(x, y)
            GameObjectType.HINT_SIGN -> newObject = spawnObjectSign(x, y)
            GameObjectType.DUST -> newObject = spawnDust(x, y, horzFlip)
            GameObjectType.EXPLOSION_SMALL -> newObject = spawnEffectExplosionSmall(x, y)
            GameObjectType.EXPLOSION_LARGE -> newObject = spawnEffectExplosionLarge(x, y)
            GameObjectType.EXPLOSION_GIANT -> newObject = spawnEffectExplosionGiant(x, y)
            GameObjectType.GHOST_NPC -> newObject = spawnGhostNPC(x, y)
            GameObjectType.CAMERA_BIAS -> newObject = spawnCameraBias(x, y)
            GameObjectType.FRAMERATE_WATCHER -> newObject = spawnFrameRateWatcher(x, y)
            GameObjectType.INFINITE_SPAWNER -> newObject = spawnObjectInfiniteSpawner(x, y)
            GameObjectType.CRUSHER_ANDOU -> newObject = spawnObjectCrusherAndou(x, y)
            GameObjectType.SMOKE_BIG -> newObject = spawnEffectSmokeBig(x, y)
            GameObjectType.SMOKE_SMALL -> newObject = spawnEffectSmokeSmall(x, y)
            GameObjectType.CRUSH_FLASH -> newObject = spawnEffectCrushFlash(x, y)
            GameObjectType.FLASH -> newObject = spawnEffectFlash(x, y)
            GameObjectType.ENERGY_BALL -> newObject = spawnEnergyBall(x, y, horzFlip)
            GameObjectType.CANNON_BALL -> newObject = spawnCannonBall(x, y, horzFlip)
            GameObjectType.TURRET_BULLET -> newObject = spawnTurretBullet(x, y, horzFlip)
            GameObjectType.BROBOT_BULLET -> newObject = spawnBrobotBullet(x, y, horzFlip)
            GameObjectType.BREAKABLE_BLOCK_PIECE -> newObject = spawnBreakableBlockPiece(x, y)
            GameObjectType.BREAKABLE_BLOCK_PIECE_SPAWNER -> newObject = spawnBreakableBlockPieceSpawner(x, y)
            GameObjectType.WANDA_SHOT -> newObject = spawnWandaShot(x, y, horzFlip)
            GameObjectType.SMOKE_POOF -> newObject = spawnSmokePoof(x, y)
            GameObjectType.GEM_EFFECT -> newObject = spawnGemEffect(x, y)
            GameObjectType.GEM_EFFECT_SPAWNER -> newObject = spawnGemEffectSpawner(x, y)
            else -> {}
        }
        return newObject
    }

    fun spawnFromWorld(world: TiledWorld, tileWidth: Int, tileHeight: Int) {
        // Walk the world and spawn objects based on tile indexes.
        val worldHeight = world.fetchHeight() * tileHeight.toFloat()
        val manager = sSystemRegistry.gameObjectManager
        if (manager != null) {
            for (y in 0 until world.fetchHeight()) {
                for (x in 0 until world.fetchWidth()) {
                    val index = world.getTile(x, y)
                    if (index != -1) {
                        val type = GameObjectType.indexToType(index)
                        if (type != GameObjectType.INVALID) {
                            val worldX = x * tileWidth.toFloat()
                            val worldY = worldHeight - (y + 1) * tileHeight
                            val thing = spawn(type, worldX, worldY, false)
                            if (thing != null) {
                                if (thing.height < tileHeight) {
                                    // make sure small objects are vertically centered in their
                                    // tile.
                                    thing.position.y += (tileHeight - thing.height) / 2.0f
                                }
                                if (thing.width < tileWidth) {
                                    thing.position.x += (tileWidth - thing.width) / 2.0f
                                } else if (thing.width > tileWidth) {
                                    thing.position.x -= (thing.width - tileWidth) / 2.0f
                                }
                                manager.add(thing)
                                if (type == GameObjectType.PLAYER) {
                                    manager.player = thing
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun getStaticData(type: GameObjectType): FixedSizeArray<BaseObject?>? {
        return staticBaseObjectArray[type.ordinal]
    }

    private fun setStaticData(type: GameObjectType, data: FixedSizeArray<BaseObject?>) {
        val index = type.ordinal
        //TODO 2 fix: assert(mStaticData[index] == null)
        val staticDataCount = data.count
        for (x in 0 until staticDataCount) {
            val entry = data[x]
            if (entry is GameComponent) {
                entry.shared = true
            }
        }
        staticBaseObjectArray[index] = data
    }

    private fun addStaticData(type: GameObjectType, thing: GameObject?, sprite: SpriteComponent?) {
        val staticData = getStaticData(type)!!
        if (staticData != null) {
            val staticDataCount = staticData.count
            for (x in 0 until staticDataCount) {
                val entry = staticData[x]
                if (entry is GameComponent && thing != null) {
                    thing.add((entry as GameComponent?)!!)
                } else if (entry is SpriteAnimation && sprite != null) {
                    sprite.addAnimation(entry as SpriteAnimation?)
                }
            }
        }
    }

    fun clearStaticData() {
        val typeCount = staticBaseObjectArray.count
        for (x in 0 until typeCount) {
            val staticData = staticBaseObjectArray[x]
            if (staticData != null) {
                val count = staticData.count
                for (y in 0 until count) {
                    val entry = staticData[y]
                    if (entry != null) {
                        if (entry is GameComponent) {
                            releaseComponent(entry as GameComponent?)
                        }
                    }
                }
                staticData.clear()
                staticBaseObjectArray[x] = null
            }
        }
    }

    fun sanityCheckPools() {
        val outstandingObjects = gameObjectPool.fetchAllocatedCount()
        if (outstandingObjects != 0) {
            DebugLog.d("Sanity Check", "Outstanding game thing allocations! ("
                    + outstandingObjects + ")")
            //TODO 2 fix: assert(false)
        }
        val componentPoolCount = componentPools.count
        for (x in 0 until componentPoolCount) {
            val outstandingComponents = componentPools[x]!!.fetchAllocatedCount()
            if (outstandingComponents != 0) {
                DebugLog.d("Sanity Check", "Outstanding "
                        + componentPools[x]!!.objectClass!!.simpleName
                        + " allocations! (" + outstandingComponents + ")")
                ////TODO 2 fix: assert false;
            }
        }
    }

    fun spawnPlayer(positionX: Float, positionY: Float): GameObject? {
        val textureLibrary = sSystemRegistry.shortTermTextureLibrary
        val thing = gameObjectPool.allocate()
        thing!!.position[positionX] = positionY
        thing.activationRadius = alwaysActive
        thing.width = 64f
        thing.height = 64f
        var staticData = getStaticData(GameObjectType.PLAYER)
        if (staticData == null) {
            val staticObjectCount = 13
            staticData = FixedSizeArray(staticObjectCount)
            val gravity = allocateComponent(GravityComponent::class.java)
            val movement = allocateComponent(MovementComponent::class.java)
            val physics = allocateComponent(PhysicsComponent::class.java) as PhysicsComponent?
            physics!!.mass = 9.1f // ~90kg w/ earth gravity
            physics.dynamicFrictionCoeffecient = 0.2f
            physics.staticFrictionCoeffecient = 0.01f

            // Animation Data
            val basicVulnerabilityVolume = FixedSizeArray<CollisionVolume>(1)
            basicVulnerabilityVolume.add(SphereCollisionVolume(16, 32, 32))
            val pressAndCollectVolume = FixedSizeArray<CollisionVolume>(2)
            val collectionVolume = AABoxCollisionVolume(16, 0, 32, 48)
            collectionVolume.hitType = HitType.COLLECT
            pressAndCollectVolume.add(collectionVolume)
            val pressCollisionVolume = AABoxCollisionVolume(16, 0, 32, 16)
            pressCollisionVolume.hitType = HitType.DEPRESS
            pressAndCollectVolume.add(pressCollisionVolume)
            val idle = SpriteAnimation(PlayerAnimations.IDLE.ordinal, 1)
            idle.addFrame(AnimationFrame(textureLibrary!!.allocateTexture(R.drawable.andou_stand),
                    1.0f, pressAndCollectVolume, basicVulnerabilityVolume))
            val angle = SpriteAnimation(PlayerAnimations.MOVE.ordinal, 1)
            angle.addFrame(
                    AnimationFrame(textureLibrary.allocateTexture(R.drawable.andou_diag01),
                            0.0416f, pressAndCollectVolume, basicVulnerabilityVolume))
            val extremeAngle = SpriteAnimation(
                    PlayerAnimations.MOVE_FAST.ordinal, 1)
            extremeAngle.addFrame(
                    AnimationFrame(textureLibrary.allocateTexture(R.drawable.andou_diagmore01),
                            0.0416f, pressAndCollectVolume, basicVulnerabilityVolume))
            val up = SpriteAnimation(PlayerAnimations.BOOST_UP.ordinal, 2)
            up.addFrame(AnimationFrame(textureLibrary.allocateTexture(R.drawable.andou_flyup02),
                    Utils.framesToTime(24, 1), pressAndCollectVolume, basicVulnerabilityVolume))
            up.addFrame(AnimationFrame(textureLibrary.allocateTexture(R.drawable.andou_flyup03),
                    Utils.framesToTime(24, 1), pressAndCollectVolume, basicVulnerabilityVolume))
            up.loop = true
            val boostAngle = SpriteAnimation(PlayerAnimations.BOOST_MOVE.ordinal, 2)
            boostAngle.addFrame(
                    AnimationFrame(textureLibrary.allocateTexture(R.drawable.andou_diag02),
                            Utils.framesToTime(24, 1), pressAndCollectVolume, basicVulnerabilityVolume))
            boostAngle.addFrame(
                    AnimationFrame(textureLibrary.allocateTexture(R.drawable.andou_diag03),
                            Utils.framesToTime(24, 1), pressAndCollectVolume, basicVulnerabilityVolume))
            boostAngle.loop = true
            val boostExtremeAngle = SpriteAnimation(
                    PlayerAnimations.BOOST_MOVE_FAST.ordinal, 2)
            boostExtremeAngle.addFrame(
                    AnimationFrame(textureLibrary.allocateTexture(R.drawable.andou_diagmore02),
                            Utils.framesToTime(24, 1), pressAndCollectVolume, basicVulnerabilityVolume))
            boostExtremeAngle.addFrame(
                    AnimationFrame(textureLibrary.allocateTexture(R.drawable.andou_diagmore03),
                            Utils.framesToTime(24, 1), pressAndCollectVolume, basicVulnerabilityVolume))
            boostExtremeAngle.loop = true
            val stompAttackVolume = FixedSizeArray<CollisionVolume>(3)
            stompAttackVolume.add(AABoxCollisionVolume(16f, -5.0f, 32f, 37f, HitType.HIT))
            stompAttackVolume.add(pressCollisionVolume)
            stompAttackVolume.add(collectionVolume)
            val stomp = SpriteAnimation(PlayerAnimations.STOMP.ordinal, 4)
            stomp.addFrame(
                    AnimationFrame(textureLibrary.allocateTexture(R.drawable.andou_stomp01),
                            Utils.framesToTime(24, 1), stompAttackVolume, null))
            stomp.addFrame(
                    AnimationFrame(textureLibrary.allocateTexture(R.drawable.andou_stomp02),
                            Utils.framesToTime(24, 1), stompAttackVolume, null))
            stomp.addFrame(
                    AnimationFrame(textureLibrary.allocateTexture(R.drawable.andou_stomp03),
                            Utils.framesToTime(24, 1), stompAttackVolume, null))
            stomp.addFrame(
                    AnimationFrame(textureLibrary.allocateTexture(R.drawable.andou_stomp04),
                            Utils.framesToTime(24, 1), stompAttackVolume, null))
            val hitReactAnim = SpriteAnimation(PlayerAnimations.HIT_REACT.ordinal, 1)
            hitReactAnim.addFrame(
                    AnimationFrame(textureLibrary.allocateTexture(R.drawable.andou_hit),
                            0.1f, pressAndCollectVolume, null))
            val deathAnim = SpriteAnimation(PlayerAnimations.DEATH.ordinal, 16)
            val death1 = AnimationFrame(textureLibrary.allocateTexture(R.drawable.andou_die01),
                    Utils.framesToTime(24, 1), null, null)
            val death2 = AnimationFrame(textureLibrary.allocateTexture(R.drawable.andou_die02),
                    Utils.framesToTime(24, 1), null, null)
            deathAnim.addFrame(death1)
            deathAnim.addFrame(death2)
            deathAnim.addFrame(death1)
            deathAnim.addFrame(death2)
            deathAnim.addFrame(
                    AnimationFrame(textureLibrary.allocateTexture(R.drawable.andou_explode01),
                            Utils.framesToTime(24, 1), null, null))
            deathAnim.addFrame(
                    AnimationFrame(textureLibrary.allocateTexture(R.drawable.andou_explode02),
                            Utils.framesToTime(24, 1), null, null))
            deathAnim.addFrame(
                    AnimationFrame(textureLibrary.allocateTexture(R.drawable.andou_explode03),
                            Utils.framesToTime(24, 1), null, null))
            deathAnim.addFrame(
                    AnimationFrame(textureLibrary.allocateTexture(R.drawable.andou_explode04),
                            Utils.framesToTime(24, 1), null, null))
            deathAnim.addFrame(
                    AnimationFrame(textureLibrary.allocateTexture(R.drawable.andou_explode05),
                            Utils.framesToTime(24, 2), null, null))
            deathAnim.addFrame(
                    AnimationFrame(textureLibrary.allocateTexture(R.drawable.andou_explode06),
                            Utils.framesToTime(24, 2), null, null))
            deathAnim.addFrame(
                    AnimationFrame(textureLibrary.allocateTexture(R.drawable.andou_explode07),
                            Utils.framesToTime(24, 2), null, null))
            deathAnim.addFrame(
                    AnimationFrame(textureLibrary.allocateTexture(R.drawable.andou_explode08),
                            Utils.framesToTime(24, 2), null, null))
            deathAnim.addFrame(
                    AnimationFrame(textureLibrary.allocateTexture(R.drawable.andou_explode09),
                            Utils.framesToTime(24, 2), null, null))
            deathAnim.addFrame(
                    AnimationFrame(textureLibrary.allocateTexture(R.drawable.andou_explode10),
                            Utils.framesToTime(24, 2), null, null))
            deathAnim.addFrame(
                    AnimationFrame(textureLibrary.allocateTexture(R.drawable.andou_explode11),
                            Utils.framesToTime(24, 2), null, null))
            deathAnim.addFrame(
                    AnimationFrame(textureLibrary.allocateTexture(R.drawable.andou_explode12),
                            Utils.framesToTime(24, 2), null, null))
            val frozenAnim = SpriteAnimation(PlayerAnimations.FROZEN.ordinal, 1)
            // Frozen has no frames!


            // Save static data
            staticData.add(gravity)
            staticData.add(movement)
            staticData.add(physics)
            staticData.add(idle)
            staticData.add(angle)
            staticData.add(extremeAngle)
            staticData.add(up)
            staticData.add(boostAngle)
            staticData.add(boostExtremeAngle)
            staticData.add(stomp)
            staticData.add(hitReactAnim)
            staticData.add(deathAnim)
            staticData.add(frozenAnim)
            setStaticData(GameObjectType.PLAYER, staticData)
        }
        val render = allocateComponent(RenderComponent::class.java) as RenderComponent?
        render!!.priority = SortConstants.PLAYER
        val bgcollision = allocateComponent(BackgroundCollisionComponent::class.java) as BackgroundCollisionComponent?
        bgcollision!!.setSize(32, 48)
        bgcollision.setOffset(16, 0)
        val player = allocateComponent(PlayerComponent::class.java) as PlayerComponent?
        val animation = allocateComponent(AnimationComponent::class.java) as AnimationComponent?
        animation!!.setPlayer(player)
        val sound = sSystemRegistry.soundSystem
        if (sound != null) {
            animation.setLandThump(sound.load(R.raw.thump))
            animation.setRocketSound(sound.load(R.raw.rockets))
            animation.setRubySounds(sound.load(R.raw.gem1), sound.load(R.raw.gem2), sound.load(R.raw.gem3))
            animation.setExplosionSound(sound.load(R.raw.sound_explode))
        }
        val sprite = allocateComponent(SpriteComponent::class.java) as SpriteComponent?
        sprite!!.setSize(thing.width.toInt(), thing.height.toInt())
        sprite.setRenderComponent(render)
        animation.setSprite(sprite)
        val dynamicCollision = allocateComponent(DynamicCollisionComponent::class.java) as DynamicCollisionComponent?
        sprite.setCollisionComponent(dynamicCollision)
        val hitReact = allocateComponent(HitReactionComponent::class.java) as HitReactionComponent?
        hitReact!!.setBounceOnHit(true)
        hitReact.setPauseOnAttack(true)
        hitReact.setInvincibleTime(3.0f)
        hitReact.setSpawnOnDealHit(HitType.HIT, GameObjectType.CRUSH_FLASH, false, true)
        if (sound != null) {
            hitReact.setTakeHitSound(HitType.HIT, sound.load(R.raw.deep_clang))
        }
        dynamicCollision!!.setHitReactionComponent(hitReact)
        player!!.setHitReactionComponent(hitReact)
        val inventory = allocateComponent(InventoryComponent::class.java) as InventoryComponent?
        player.setInventory(inventory)
        animation.setInventory(inventory)
        val damageSwap = allocateComponent(ChangeComponentsComponent::class.java) as ChangeComponentsComponent?
        animation.setDamageSwap(damageSwap)
        val smokeGun = allocateComponent(LaunchProjectileComponent::class.java) as LaunchProjectileComponent?
        smokeGun!!.setDelayBetweenShots(0.25f)
        smokeGun.setObjectTypeToSpawn(GameObjectType.SMOKE_BIG)
        smokeGun.setOffsetX(32f)
        smokeGun.setOffsetY(15f)
        smokeGun.setVelocityX(-150.0f)
        smokeGun.setVelocityY(100.0f)
        smokeGun.setThetaError(0.1f)
        val smokeGun2 = allocateComponent(LaunchProjectileComponent::class.java) as LaunchProjectileComponent?
        smokeGun2!!.setDelayBetweenShots(0.35f)
        smokeGun2.setObjectTypeToSpawn(GameObjectType.SMOKE_SMALL)
        smokeGun2.setOffsetX(16f)
        smokeGun2.setOffsetY(15f)
        smokeGun2.setVelocityX(-150.0f)
        smokeGun2.setVelocityY(150.0f)
        smokeGun2.setThetaError(0.1f)
        damageSwap!!.addSwapInComponent(smokeGun)
        damageSwap.addSwapInComponent(smokeGun2)
        damageSwap.setPingPongBehavior(true)
        val invincibleSwap = allocateComponent(ChangeComponentsComponent::class.java) as ChangeComponentsComponent?
        invincibleSwap!!.setPingPongBehavior(true)
        player.setInvincibleSwap(invincibleSwap)
        thing.life = difficultyConstants.whatIsMaxPlayerLife()
        thing.team = Team.PLAYER

        // Very very basic DDA.  Make the game easier if we've died on this level too much.
        val level = sSystemRegistry.levelSystem
        if (level != null) {
            player.adjustDifficulty(thing, level.attemptsCount)
        }
        thing.add(player)
        thing.add(inventory!!)
        thing.add(bgcollision)
        thing.add(render)
        thing.add(animation)
        thing.add(sprite)
        thing.add(dynamicCollision)
        thing.add(hitReact)
        thing.add(damageSwap)
        thing.add(invincibleSwap)
        addStaticData(GameObjectType.PLAYER, thing, sprite)
        sprite.playAnimation(PlayerAnimations.IDLE.ordinal)


        // Jets
        run {
            var jetStaticData = getStaticData(GameObjectType.PLAYER_JETS)
            if (jetStaticData == null) {
                jetStaticData = FixedSizeArray(1)
                val jetAnim = SpriteAnimation(0, 2)
                jetAnim.addFrame(
                        AnimationFrame(textureLibrary!!.allocateTexture(R.drawable.jetfire01),
                                Utils.framesToTime(24, 1)))
                jetAnim.addFrame(
                        AnimationFrame(textureLibrary.allocateTexture(R.drawable.jetfire02),
                                Utils.framesToTime(24, 1)))
                jetAnim.loop = true
                jetStaticData.add(jetAnim)
                setStaticData(GameObjectType.PLAYER_JETS, jetStaticData)
            }
            val jetRender = allocateComponent(RenderComponent::class.java) as RenderComponent?
            jetRender!!.priority = SortConstants.PLAYER - 1
            jetRender.setDrawOffset(0.0f, -16.0f)
            val jetSprite = allocateComponent(SpriteComponent::class.java) as SpriteComponent?
            jetSprite!!.setSize(64, 64)
            jetSprite.setRenderComponent(jetRender)
            thing.add(jetRender)
            thing.add(jetSprite)
            addStaticData(GameObjectType.PLAYER_JETS, thing, jetSprite)
            jetSprite.playAnimation(0)
            animation.setJetSprite(jetSprite)
        }
        // Sparks
        run {
            var sparksStaticData = getStaticData(GameObjectType.PLAYER_SPARKS)
            if (sparksStaticData == null) {
                sparksStaticData = FixedSizeArray(1)
                val sparksAnim = SpriteAnimation(0, 3)
                sparksAnim.addFrame(
                        AnimationFrame(textureLibrary!!.allocateTexture(R.drawable.spark01),
                                Utils.framesToTime(24, 1)))
                sparksAnim.addFrame(
                        AnimationFrame(textureLibrary.allocateTexture(R.drawable.spark02),
                                Utils.framesToTime(24, 1)))
                sparksAnim.addFrame(
                        AnimationFrame(textureLibrary.allocateTexture(R.drawable.spark03),
                                Utils.framesToTime(24, 1)))
                sparksAnim.loop = true
                sparksStaticData.add(sparksAnim)
                setStaticData(GameObjectType.PLAYER_SPARKS, sparksStaticData)
            }
            val sparksRender = allocateComponent(RenderComponent::class.java) as RenderComponent?
            sparksRender!!.priority = SortConstants.PLAYER + 1
            val sparksSprite = allocateComponent(SpriteComponent::class.java) as SpriteComponent?
            sparksSprite!!.setSize(64, 64)
            sparksSprite.setRenderComponent(sparksRender)
            thing.add(sparksRender)
            thing.add(sparksSprite)
            addStaticData(GameObjectType.PLAYER_SPARKS, thing, sparksSprite)
            sparksSprite.playAnimation(0)
            animation.setSparksSprite(sparksSprite)
        }

        // Glow
        run {
            var glowStaticData = getStaticData(GameObjectType.PLAYER_GLOW)
            if (glowStaticData == null) {
                glowStaticData = FixedSizeArray(1)
                val glowAttackVolume = FixedSizeArray<CollisionVolume>(1)
                glowAttackVolume.add(SphereCollisionVolume(40, 40, 40, HitType.HIT))
                val glowAnim = SpriteAnimation(0, 3)
                glowAnim.addFrame(
                        AnimationFrame(textureLibrary!!.allocateTexture(R.drawable.effect_glow01),
                                Utils.framesToTime(24, 1), glowAttackVolume, null))
                glowAnim.addFrame(
                        AnimationFrame(textureLibrary.allocateTexture(R.drawable.effect_glow02),
                                Utils.framesToTime(24, 1), glowAttackVolume, null))
                glowAnim.addFrame(
                        AnimationFrame(textureLibrary.allocateTexture(R.drawable.effect_glow03),
                                Utils.framesToTime(24, 1), glowAttackVolume, null))
                glowAnim.loop = true
                glowStaticData.add(glowAnim)
                setStaticData(GameObjectType.PLAYER_GLOW, glowStaticData)
            }
            val glowRender = allocateComponent(RenderComponent::class.java) as RenderComponent?
            glowRender!!.priority = SortConstants.PLAYER + 1
            glowRender.setDrawOffset(0f, -5.0f)
            val glowSprite = allocateComponent(SpriteComponent::class.java) as SpriteComponent?
            glowSprite!!.setSize(64, 64)
            glowSprite.setRenderComponent(glowRender)
            val glowCollision = allocateComponent(DynamicCollisionComponent::class.java) as DynamicCollisionComponent?
            glowSprite.setCollisionComponent(glowCollision)
            val glowFade = allocateComponent(FadeDrawableComponent::class.java) as FadeDrawableComponent?
            val glowDuration = difficultyConstants.whatIsGlowDuration()
            glowFade!!.setupFade(1.0f, 0.0f, 0.15f,
                    FadeDrawableComponent.LOOP_TYPE_PING_PONG,
                    FadeDrawableComponent.FADE_EASE,
                    glowDuration - 4.0f) // 4 seconds before the glow ends, start flashing
            glowFade.setPhaseDuration(glowDuration)
            glowFade.setRenderComponent(glowRender)

            // HACK
            player.setInvincibleFader(glowFade)
            invincibleSwap.addSwapInComponent(glowRender)
            invincibleSwap.addSwapInComponent(glowSprite)
            invincibleSwap.addSwapInComponent(glowCollision)
            invincibleSwap.addSwapInComponent(glowFade)
            addStaticData(GameObjectType.PLAYER_GLOW, thing, glowSprite)
            glowSprite.playAnimation(0)
        }
        val camera = sSystemRegistry.cameraSystem
        if (camera != null) {
            camera.target = thing
        }
        return thing
    }

    // Sparks are used by more than one enemy type, so the setup for them is abstracted.
    private fun setupEnemySparks() {
        var staticData = getStaticData(GameObjectType.ENEMY_SPARKS)
        if (staticData == null) {
            staticData = FixedSizeArray(1)
            val textureLibrary = sSystemRegistry.shortTermTextureLibrary
            val sparksAnim = SpriteAnimation(0, 13)
            val frame1 = AnimationFrame(textureLibrary!!.allocateTexture(R.drawable.spark01),
                    Utils.framesToTime(24, 1))
            val frame2 = AnimationFrame(textureLibrary.allocateTexture(R.drawable.spark02),
                    Utils.framesToTime(24, 1))
            val frame3 = AnimationFrame(textureLibrary.allocateTexture(R.drawable.spark03),
                    Utils.framesToTime(24, 1))
            sparksAnim.addFrame(frame1)
            sparksAnim.addFrame(frame2)
            sparksAnim.addFrame(frame3)
            sparksAnim.addFrame(frame1)
            sparksAnim.addFrame(frame2)
            sparksAnim.addFrame(frame3)
            sparksAnim.addFrame(frame1)
            sparksAnim.addFrame(frame2)
            sparksAnim.addFrame(frame3)
            sparksAnim.addFrame(frame1)
            sparksAnim.addFrame(frame2)
            sparksAnim.addFrame(frame3)
            sparksAnim.addFrame(AnimationFrame(null, 3.0f))
            sparksAnim.loop = true
            staticData.add(sparksAnim)
            setStaticData(GameObjectType.ENEMY_SPARKS, staticData)
        }
    }

    fun spawnEnemyBrobot(positionX: Float, positionY: Float, flipHorizontal: Boolean): GameObject? {
        val textureLibrary = sSystemRegistry.shortTermTextureLibrary
        val thing = gameObjectPool.allocate()
        thing!!.position[positionX] = positionY
        thing.activationRadius = normalActivationRadius
        thing.width = 64f
        thing.height = 64f
        var staticData = getStaticData(GameObjectType.BROBOT)
        if (staticData == null) {
            val staticObjectCount = 5
            staticData = FixedSizeArray(staticObjectCount)
            val gravity = allocateComponent(GravityComponent::class.java)
            val movement = allocateComponent(MovementComponent::class.java)
            val physics = allocateComponent(SimplePhysicsComponent::class.java) as SimplePhysicsComponent?
            physics!!.setBounciness(0.4f)


            // Animations
            val basicVulnerabilityVolume = FixedSizeArray<CollisionVolume>(1)
            basicVulnerabilityVolume.add(SphereCollisionVolume(16, 32, 32))
            val basicAttackVolume = FixedSizeArray<CollisionVolume>(2)
            basicAttackVolume.add(SphereCollisionVolume(16, 32, 32, HitType.HIT))
            basicAttackVolume.add(AABoxCollisionVolume(16, 0, 32, 16, HitType.DEPRESS))
            val idle = SpriteAnimation(EnemyAnimations.IDLE.ordinal, 4)
            idle.addFrame(AnimationFrame(
                    textureLibrary!!.allocateTexture(R.drawable.enemy_brobot_idle01),
                    Utils.framesToTime(24, 3), basicAttackVolume, basicVulnerabilityVolume))
            idle.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_brobot_idle02),
                    Utils.framesToTime(24, 1), basicAttackVolume, basicVulnerabilityVolume))
            idle.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_brobot_idle03),
                    Utils.framesToTime(24, 3), basicAttackVolume, basicVulnerabilityVolume))
            idle.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_brobot_idle02),
                    Utils.framesToTime(24, 3), basicAttackVolume, basicVulnerabilityVolume))
            idle.loop = true
            val walk = SpriteAnimation(EnemyAnimations.MOVE.ordinal, 3)
            walk.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_brobot_walk01),
                    Utils.framesToTime(24, 1), basicAttackVolume, basicVulnerabilityVolume))
            walk.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_brobot_walk02),
                    Utils.framesToTime(24, 1), basicAttackVolume, basicVulnerabilityVolume))
            walk.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_brobot_walk03),
                    Utils.framesToTime(24, 1), basicAttackVolume, basicVulnerabilityVolume))
            walk.loop = true
            staticData.add(gravity)
            staticData.add(movement)
            staticData.add(physics)
            staticData.add(idle)
            staticData.add(walk)
            setStaticData(GameObjectType.BROBOT, staticData)
        }
        val render = allocateComponent(RenderComponent::class.java) as RenderComponent?
        render!!.priority = SortConstants.GENERAL_ENEMY
        val bgcollision = allocateComponent(BackgroundCollisionComponent::class.java) as BackgroundCollisionComponent?
        bgcollision!!.setSize(32, 48)
        bgcollision.setOffset(16, 0)
        val sprite = allocateComponent(SpriteComponent::class.java) as SpriteComponent?
        sprite!!.setSize(thing.width.toInt(), thing.height.toInt())
        sprite.setRenderComponent(render)
        val animation = allocateComponent(EnemyAnimationComponent::class.java) as EnemyAnimationComponent?
        animation!!.setSprite(sprite)
        val patrol = allocateComponent(PatrolComponent::class.java) as PatrolComponent?
        patrol!!.setMovementSpeed(50.0f, 1000.0f)
        val collision = allocateComponent(DynamicCollisionComponent::class.java) as DynamicCollisionComponent?
        sprite.setCollisionComponent(collision)
        val hitReact = allocateComponent(HitReactionComponent::class.java) as HitReactionComponent?
        collision!!.setHitReactionComponent(hitReact)
        val lifetime = allocateComponent(LifetimeComponent::class.java) as LifetimeComponent?
        lifetime!!.setObjectToSpawnOnDeath(GameObjectType.EXPLOSION_GIANT)
        lifetime.setVulnerableToDeathTiles(true)
        lifetime.setIncrementEventCounter(EventRecorder.COUNTER_ROBOTS_DESTROYED)
        val ghost = allocateComponent(GhostComponent::class.java) as GhostComponent?
        ghost!!.setMovementSpeed(500.0f)
        ghost.setAcceleration(1000.0f)
        ghost.setJumpImpulse(300.0f)
        ghost.setKillOnRelease(true)
        ghost.setDelayOnRelease(1.5f)
        val sound = sSystemRegistry.soundSystem
        if (sound != null) {
            ghost.setAmbientSound(sound.load(R.raw.sound_possession))
        }
        val ghostSwap = allocateComponent(ChangeComponentsComponent::class.java) as ChangeComponentsComponent?
        ghostSwap!!.addSwapInComponent(ghost)
        ghostSwap.addSwapOutComponent(patrol)
        val ghostPhysics = allocateComponent(SimplePhysicsComponent::class.java) as SimplePhysicsComponent?
        ghostPhysics!!.setBounciness(0.0f)
        thing.add(render)
        thing.add(sprite)
        thing.add(bgcollision)
        thing.add(animation)
        thing.add(patrol)
        thing.add(collision)
        thing.add(hitReact!!)
        thing.add(lifetime)
        thing.add(ghostSwap)
        thing.team = Team.ENEMY
        if (flipHorizontal) {
            thing.facingDirection.x = -1.0f
        }
        addStaticData(GameObjectType.BROBOT, thing, sprite)
        thing.commitUpdates()
        val normalPhysics = thing.findByClass(SimplePhysicsComponent::class.java)
        if (normalPhysics != null) {
            ghostSwap.addSwapOutComponent(normalPhysics)
        }
        ghostSwap.addSwapInComponent(ghostPhysics)
        sprite.playAnimation(0)

        // Sparks
        setupEnemySparks()
        val sparksRender = allocateComponent(RenderComponent::class.java) as RenderComponent?
        sparksRender!!.priority = render.priority + 1
        val sparksSprite = allocateComponent(SpriteComponent::class.java) as SpriteComponent?
        sparksSprite!!.setSize(64, 64)
        sparksSprite.setRenderComponent(sparksRender)
        addStaticData(GameObjectType.ENEMY_SPARKS, thing, sparksSprite)
        sparksSprite.playAnimation(0)
        ghostSwap.addSwapInComponent(sparksSprite)
        ghostSwap.addSwapInComponent(sparksRender)
        hitReact.setPossessionComponent(ghostSwap)
        return thing
    }

    fun spawnEnemySnailBomb(positionX: Float, positionY: Float, flipHorizontal: Boolean): GameObject? {
        val textureLibrary = sSystemRegistry.shortTermTextureLibrary


        // Make sure related textures are loaded.
        textureLibrary!!.allocateTexture(R.drawable.snail_bomb)
        val thing = gameObjectPool.allocate()
        thing!!.position[positionX] = positionY
        thing.activationRadius = normalActivationRadius
        thing.width = 64f
        thing.height = 64f
        var staticData = getStaticData(GameObjectType.SNAILBOMB)
        if (staticData == null) {
            val staticObjectCount = 6
            staticData = FixedSizeArray(staticObjectCount)
            val gravity = allocateComponent(GravityComponent::class.java)
            val movement = allocateComponent(MovementComponent::class.java)
            val physics = allocateComponent(SimplePhysicsComponent::class.java)

            // Animations
            val basicVulnerabilityVolume = FixedSizeArray<CollisionVolume>(1)
            basicVulnerabilityVolume.add(AABoxCollisionVolume(12, 5, 42, 27, HitType.HIT))
            val basicAttackVolume = FixedSizeArray<CollisionVolume>(1)
            basicAttackVolume.add(AABoxCollisionVolume(12, 5, 42, 27, HitType.HIT))
            val idle = SpriteAnimation(EnemyAnimations.IDLE.ordinal, 1)
            idle.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.snailbomb_stand),
                    Utils.framesToTime(24, 3), basicAttackVolume, basicVulnerabilityVolume))
            val walk = SpriteAnimation(EnemyAnimations.MOVE.ordinal, 5)
            walk.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.snailbomb_stand),
                    Utils.framesToTime(24, 2), basicAttackVolume, basicVulnerabilityVolume))
            walk.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.snailbomb_walk01),
                    Utils.framesToTime(24, 2), basicAttackVolume, basicVulnerabilityVolume))
            walk.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.snailbomb_walk02),
                    Utils.framesToTime(24, 6), basicAttackVolume, basicVulnerabilityVolume))
            walk.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.snailbomb_walk01),
                    Utils.framesToTime(24, 2), basicAttackVolume, basicVulnerabilityVolume))
            walk.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.snailbomb_stand),
                    Utils.framesToTime(24, 2), basicAttackVolume, basicVulnerabilityVolume))
            walk.loop = true
            val attack = SpriteAnimation(EnemyAnimations.ATTACK.ordinal, 2)
            attack.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.snailbomb_shoot01),
                    Utils.framesToTime(24, 3), basicAttackVolume, basicVulnerabilityVolume))
            attack.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.snailbomb_shoot02),
                    Utils.framesToTime(24, 2), basicAttackVolume, basicVulnerabilityVolume))
            staticData.add(gravity)
            staticData.add(movement)
            staticData.add(physics)
            staticData.add(idle)
            staticData.add(walk)
            staticData.add(attack)
            setStaticData(GameObjectType.SNAILBOMB, staticData)
        }
        val render = allocateComponent(RenderComponent::class.java) as RenderComponent?
        render!!.priority = SortConstants.GENERAL_ENEMY
        val bgcollision = allocateComponent(BackgroundCollisionComponent::class.java) as BackgroundCollisionComponent?
        bgcollision!!.setSize(32, 48)
        bgcollision.setOffset(16, 5)
        val sprite = allocateComponent(SpriteComponent::class.java) as SpriteComponent?
        sprite!!.setSize(thing.width.toInt(), thing.height.toInt())
        sprite.setRenderComponent(render)
        val animation = allocateComponent(EnemyAnimationComponent::class.java) as EnemyAnimationComponent?
        animation!!.setSprite(sprite)
        val patrol = allocateComponent(PatrolComponent::class.java) as PatrolComponent?
        patrol!!.setMovementSpeed(20.0f, 1000.0f)
        patrol.setupAttack(300f, 1.0f, 4.0f, true)
        val collision = allocateComponent(DynamicCollisionComponent::class.java) as DynamicCollisionComponent?
        sprite.setCollisionComponent(collision)
        val hitReact = allocateComponent(HitReactionComponent::class.java) as HitReactionComponent?
        collision!!.setHitReactionComponent(hitReact)
        val lifetime = allocateComponent(LifetimeComponent::class.java) as LifetimeComponent?
        lifetime!!.setVulnerableToDeathTiles(true)
        lifetime.setObjectToSpawnOnDeath(GameObjectType.SMOKE_POOF)
        val sound = sSystemRegistry.soundSystem
        if (sound != null) {
            lifetime.setDeathSound(sound.load(R.raw.sound_stomp))
        }
        val gun = allocateComponent(LaunchProjectileComponent::class.java) as LaunchProjectileComponent?
        gun!!.setSetsPerActivation(1)
        gun.setShotsPerSet(3)
        gun.setDelayBeforeFirstSet(1.0f)
        gun.setDelayBetweenShots(0.25f)
        gun.setObjectTypeToSpawn(GameObjectType.CANNON_BALL)
        gun.setOffsetX(55f)
        gun.setOffsetY(21f)
        gun.setRequiredAction(ActionType.ATTACK)
        gun.setVelocityX(100.0f)
        thing.team = Team.ENEMY
        if (flipHorizontal) {
            thing.facingDirection.x = -1.0f
        }
        thing.add(render)
        thing.add(sprite)
        thing.add(bgcollision)
        thing.add(animation)
        thing.add(patrol)
        thing.add(collision)
        thing.add(hitReact!!)
        thing.add(lifetime)
        thing.add(gun)
        addStaticData(GameObjectType.SNAILBOMB, thing, sprite)
        val attack = sprite.findAnimation(EnemyAnimations.ATTACK.ordinal)
        if (attack != null) {
            gun.setDelayBeforeFirstSet(attack.length)
        }
        sprite.playAnimation(0)
        return thing
    }

    fun spawnEnemyShadowSlime(positionX: Float, positionY: Float, flipHorizontal: Boolean): GameObject? {
        val textureLibrary = sSystemRegistry.shortTermTextureLibrary


        // Make sure related textures are loaded.
        textureLibrary!!.allocateTexture(R.drawable.energy_ball01)
        textureLibrary.allocateTexture(R.drawable.energy_ball02)
        textureLibrary.allocateTexture(R.drawable.energy_ball03)
        textureLibrary.allocateTexture(R.drawable.energy_ball04)
        val thing = gameObjectPool.allocate()
        thing!!.position[positionX] = positionY
        thing.activationRadius = tightActivationRadius
        thing.width = 64f
        thing.height = 64f
        var staticData = getStaticData(GameObjectType.SHADOWSLIME)
        if (staticData == null) {
            val staticObjectCount = 5
            staticData = FixedSizeArray(staticObjectCount)
            val popOut = allocateComponent(PopOutComponent::class.java) as PopOutComponent?
            // edit: these guys turned out to be really annoying, so I'm changing the values
            // here to force them to always be out.
            popOut!!.setAppearDistance(2000f)
            popOut.setHideDistance(4000f)
            val basicVulnerabilityVolume = FixedSizeArray<CollisionVolume>(1)
            basicVulnerabilityVolume.add(SphereCollisionVolume(16, 32, 32))
            basicVulnerabilityVolume[0]!!.hitType = HitType.HIT
            val basicAttackVolume = FixedSizeArray<CollisionVolume>(1)
            basicAttackVolume.add(SphereCollisionVolume(16, 32, 32, HitType.HIT))
            val idle = SpriteAnimation(EnemyAnimations.IDLE.ordinal, 2)
            val idle1 = AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_shadowslime_idle01),
                    Utils.framesToTime(24, 3), basicAttackVolume, basicVulnerabilityVolume)
            val idle2 = AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_shadowslime_idle02),
                    Utils.framesToTime(24, 3), basicAttackVolume, basicVulnerabilityVolume)
            idle.addFrame(idle1)
            idle.addFrame(idle2)
            idle.loop = true
            val appear = SpriteAnimation(EnemyAnimations.APPEAR.ordinal, 6)
            val appear1 = AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_shadowslime_activate01),
                    Utils.framesToTime(24, 2), basicAttackVolume, basicVulnerabilityVolume)
            val appear2 = AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_shadowslime_activate02),
                    Utils.framesToTime(24, 2), basicAttackVolume, basicVulnerabilityVolume)
            val appear3 = AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_shadowslime_activate03),
                    Utils.framesToTime(24, 1), basicAttackVolume, basicVulnerabilityVolume)
            val appear4 = AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_shadowslime_activate04),
                    Utils.framesToTime(24, 1), basicAttackVolume, basicVulnerabilityVolume)
            val appear5 = AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_shadowslime_activate05),
                    Utils.framesToTime(24, 2), basicAttackVolume, basicVulnerabilityVolume)
            val appear6 = AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_shadowslime_activate06),
                    Utils.framesToTime(24, 1), basicAttackVolume, basicVulnerabilityVolume)
            appear.addFrame(appear1)
            appear.addFrame(appear2)
            appear.addFrame(appear3)
            appear.addFrame(appear4)
            appear.addFrame(appear5)
            appear.addFrame(appear6)
            val hidden = SpriteAnimation(EnemyAnimations.HIDDEN.ordinal, 6)
            hidden.addFrame(appear6)
            hidden.addFrame(appear5)
            hidden.addFrame(appear4)
            hidden.addFrame(appear3)
            hidden.addFrame(appear2)
            hidden.addFrame(appear1)
            /*hidden.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_shadowslime_stand),
                    Utils.framesToTime(24, 3), basicAttackVolume, basicVulnerabilityVolume));*/
            val attack = SpriteAnimation(EnemyAnimations.ATTACK.ordinal, 10)
            val attack1 = AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_shadowslime_attack01),
                    Utils.framesToTime(24, 2), basicAttackVolume, basicVulnerabilityVolume)
            val attack2 = AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_shadowslime_attack02),
                    Utils.framesToTime(24, 2), basicAttackVolume, basicVulnerabilityVolume)
            val attack3 = AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_shadowslime_attack03),
                    Utils.framesToTime(24, 2), basicAttackVolume, basicVulnerabilityVolume)
            val attack4 = AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_shadowslime_attack04),
                    Utils.framesToTime(24, 6), basicAttackVolume, basicVulnerabilityVolume)
            val attackFlash = AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_shadowslime_flash),
                    Utils.framesToTime(24, 1), basicAttackVolume, basicVulnerabilityVolume)
            val attack5 = AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_shadowslime_attack03),
                    Utils.framesToTime(24, 3), basicAttackVolume, basicVulnerabilityVolume)
            val attack6 = AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_shadowslime_attack02),
                    Utils.framesToTime(24, 3), basicAttackVolume, basicVulnerabilityVolume)
            val attack7 = AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_shadowslime_attack04),
                    Utils.framesToTime(24, 1), basicAttackVolume, basicVulnerabilityVolume)
            attack.addFrame(attack1)
            attack.addFrame(attack2)
            attack.addFrame(attack3)
            attack.addFrame(attack4)
            attack.addFrame(attackFlash)
            attack.addFrame(attack7)
            attack.addFrame(attackFlash)
            attack.addFrame(attack5)
            attack.addFrame(attack6)
            attack.addFrame(attack1)
            popOut.setupAttack(200f, 2.0f, attack.length)
            staticData.add(popOut)
            staticData.add(idle)
            staticData.add(hidden)
            staticData.add(appear)
            staticData.add(attack)
            setStaticData(GameObjectType.SHADOWSLIME, staticData)
        }
        val render = allocateComponent(RenderComponent::class.java) as RenderComponent?
        render!!.priority = SortConstants.GENERAL_ENEMY
        val bgcollision = allocateComponent(BackgroundCollisionComponent::class.java) as BackgroundCollisionComponent?
        bgcollision!!.setSize(32, 48)
        bgcollision.setOffset(16, 5)
        val sprite = allocateComponent(SpriteComponent::class.java) as SpriteComponent?
        sprite!!.setSize(thing.width.toInt(), thing.height.toInt())
        sprite.setRenderComponent(render)
        sprite.playAnimation(0)
        val animation = allocateComponent(EnemyAnimationComponent::class.java) as EnemyAnimationComponent?
        animation!!.setSprite(sprite)
        animation.setFacePlayer(true)
        val collision = allocateComponent(DynamicCollisionComponent::class.java) as DynamicCollisionComponent?
        sprite.setCollisionComponent(collision)
        val hitReact = allocateComponent(HitReactionComponent::class.java) as HitReactionComponent?
        collision!!.setHitReactionComponent(hitReact)
        val lifetime = allocateComponent(LifetimeComponent::class.java) as LifetimeComponent?
        lifetime!!.setObjectToSpawnOnDeath(GameObjectType.SMOKE_POOF)
        val sound = sSystemRegistry.soundSystem
        if (sound != null) {
            lifetime.setDeathSound(sound.load(R.raw.sound_stomp))
        }
        val gun = allocateComponent(LaunchProjectileComponent::class.java) as LaunchProjectileComponent?
        gun!!.setShotsPerSet(1)
        gun.setSetsPerActivation(1)
        gun.setObjectTypeToSpawn(GameObjectType.ENERGY_BALL)
        gun.setOffsetX(44f)
        gun.setOffsetY(22f)
        gun.setRequiredAction(ActionType.ATTACK)
        gun.setVelocityX(30.0f)
        thing.team = Team.ENEMY
        if (flipHorizontal) {
            thing.facingDirection.x = -1.0f
        }

        // Hack.  Adjusting position lets us avoid giving this character gravity, physics, and
        // collision.
        thing.position.y -= 5
        thing.add(render)
        thing.add(sprite)
        thing.add(bgcollision)
        thing.add(animation)
        thing.add(collision)
        thing.add(hitReact!!)
        thing.add(lifetime)
        thing.add(gun)
        addStaticData(GameObjectType.SHADOWSLIME, thing, sprite)
        val attack = sprite.findAnimation(EnemyAnimations.ATTACK.ordinal)
        val appear = sprite.findAnimation(EnemyAnimations.APPEAR.ordinal)
        if (attack != null && appear != null) {
            gun.setDelayBeforeFirstSet(attack.length / 2.0f)
        } else {
            gun.setDelayBeforeFirstSet(Utils.framesToTime(24, 12))
        }
        return thing
    }

    fun spawnEnemyMudman(positionX: Float, positionY: Float, flipHorizontal: Boolean): GameObject? {
        val textureLibrary = sSystemRegistry.shortTermTextureLibrary
        val thing = gameObjectPool.allocate()
        thing!!.position[positionX] = positionY
        thing.activationRadius = normalActivationRadius
        thing.width = 128f
        thing.height = 128f
        var staticData = getStaticData(GameObjectType.MUDMAN)
        if (staticData == null) {
            val staticObjectCount = 7
            staticData = FixedSizeArray(staticObjectCount)
            val gravity = allocateComponent(GravityComponent::class.java)
            val movement = allocateComponent(MovementComponent::class.java)
            val physics = allocateComponent(SimplePhysicsComponent::class.java)
            val solidSurface = allocateComponent(SolidSurfaceComponent::class.java) as SolidSurfaceComponent?
            solidSurface!!.inititalize(4)
            // house shape:
            // / \      1/ \2
            // | |      3| |4
            val surface1Start = Vector2(32, 64)
            val surface1End = Vector2(64, 96)
            val surface1Normal = Vector2(-0.707f, 0.707f)
            surface1Normal.normalize()
            val surface2Start = Vector2(64, 96)
            val surface2End = Vector2(75, 64)
            val surface2Normal = Vector2(0.9456f, 0.3250f)
            surface2Normal.normalize()
            val surface3Start = Vector2(32, 0)
            val surface3End = Vector2(32, 64)
            val surface3Normal = Vector2(-1, 0)
            val surface4Start = Vector2(75, 0)
            val surface4End = Vector2(75, 64)
            val surface4Normal = Vector2(1, 0)
            solidSurface.addSurface(surface1Start, surface1End, surface1Normal)
            solidSurface.addSurface(surface2Start, surface2End, surface2Normal)
            solidSurface.addSurface(surface3Start, surface3End, surface3Normal)
            solidSurface.addSurface(surface4Start, surface4End, surface4Normal)
            val idle = SpriteAnimation(EnemyAnimations.IDLE.ordinal, 4)
            idle.addFrame(AnimationFrame(
                    textureLibrary!!.allocateTexture(R.drawable.enemy_mud_stand),
                    Utils.framesToTime(24, 12), null, null))
            val idle1 = AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_mud_idle01),
                    Utils.framesToTime(24, 2), null, null)
            val idle2 = AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_mud_idle01),
                    Utils.framesToTime(24, 7), null, null)
            idle.addFrame(idle1)
            idle.addFrame(idle2)
            idle.addFrame(idle1)
            idle.loop = true
            val walk = SpriteAnimation(EnemyAnimations.MOVE.ordinal, 6)
            walk.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_mud_walk01),
                    Utils.framesToTime(24, 4), null, null))
            walk.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_mud_walk02),
                    Utils.framesToTime(24, 4), null, null))
            walk.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_mud_walk03),
                    Utils.framesToTime(24, 5), null, null))
            walk.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_mud_walk04),
                    Utils.framesToTime(24, 4), null, null))
            walk.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_mud_walk05),
                    Utils.framesToTime(24, 4), null, null))
            walk.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_mud_walk06),
                    Utils.framesToTime(24, 5), null, null))
            walk.loop = true
            val crushAttackVolume = FixedSizeArray<CollisionVolume>(1)
            crushAttackVolume.add(AABoxCollisionVolume(64, 0, 64, 96, HitType.HIT))
            val attack = SpriteAnimation(EnemyAnimations.ATTACK.ordinal, 8)
            attack.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_mud_stand),
                    Utils.framesToTime(24, 2), null, null))
            attack.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_mud_attack01),
                    Utils.framesToTime(24, 2), null, null))
            attack.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_mud_attack02),
                    Utils.framesToTime(24, 2), null, null))
            attack.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_mud_attack03),
                    Utils.framesToTime(24, 2), null, null))
            attack.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_mud_attack04),
                    Utils.framesToTime(24, 1), crushAttackVolume, null))
            attack.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_mud_attack05),
                    Utils.framesToTime(24, 1), crushAttackVolume, null))
            attack.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_mud_attack06),
                    Utils.framesToTime(24, 8), crushAttackVolume, null))
            attack.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_mud_attack07),
                    Utils.framesToTime(24, 5), null, null))
            staticData.add(gravity)
            staticData.add(movement)
            staticData.add(physics)
            staticData.add(solidSurface)
            staticData.add(idle)
            staticData.add(walk)
            staticData.add(attack)
            setStaticData(GameObjectType.MUDMAN, staticData)
        }
        val render = allocateComponent(RenderComponent::class.java) as RenderComponent?
        render!!.priority = SortConstants.GENERAL_ENEMY
        val bgcollision = allocateComponent(BackgroundCollisionComponent::class.java) as BackgroundCollisionComponent?
        bgcollision!!.setSize(80, 90)
        bgcollision.setOffset(32, 5)
        val sprite = allocateComponent(SpriteComponent::class.java) as SpriteComponent?
        sprite!!.setSize(thing.width.toInt(), thing.height.toInt())
        sprite.setRenderComponent(render)
        sprite.playAnimation(0)
        val animation = allocateComponent(EnemyAnimationComponent::class.java) as EnemyAnimationComponent?
        animation!!.setSprite(sprite)
        val patrol = allocateComponent(PatrolComponent::class.java) as PatrolComponent?
        patrol!!.setMovementSpeed(20.0f, 400.0f)
        val collision = allocateComponent(DynamicCollisionComponent::class.java) as DynamicCollisionComponent?
        sprite.setCollisionComponent(collision)
        val hitReact = allocateComponent(HitReactionComponent::class.java) as HitReactionComponent?
        collision!!.setHitReactionComponent(hitReact)
        val lifetime = allocateComponent(LifetimeComponent::class.java) as LifetimeComponent?
        thing.team = Team.ENEMY
        thing.life = 1
        if (flipHorizontal) {
            thing.facingDirection.x = -1.0f
        }
        thing.add(render)
        thing.add(sprite)
        thing.add(bgcollision)
        thing.add(animation)
        thing.add(patrol)
        thing.add(collision)
        thing.add(hitReact!!)
        thing.add(lifetime!!)
        addStaticData(GameObjectType.MUDMAN, thing, sprite)
        val attack = sprite.findAnimation(EnemyAnimations.ATTACK.ordinal)
        if (attack != null) {
            patrol.setupAttack(70.0f, attack.length, 0.0f, true)
        }
        sprite.playAnimation(0)
        return thing
    }

    fun spawnEnemySkeleton(positionX: Float, positionY: Float, flipHorizontal: Boolean): GameObject? {
        val textureLibrary = sSystemRegistry.shortTermTextureLibrary
        val thing = gameObjectPool.allocate()
        thing!!.position[positionX] = positionY
        thing.activationRadius = normalActivationRadius
        thing.width = 64f
        thing.height = 64f
        var staticData = getStaticData(GameObjectType.SKELETON)
        if (staticData == null) {
            val staticObjectCount = 7
            staticData = FixedSizeArray(staticObjectCount)
            val gravity = allocateComponent(GravityComponent::class.java)
            val movement = allocateComponent(MovementComponent::class.java)
            val physics = allocateComponent(SimplePhysicsComponent::class.java)
            val solidSurface = allocateComponent(SolidSurfaceComponent::class.java) as SolidSurfaceComponent?
            solidSurface!!.inititalize(4)
            val surface1Start = Vector2(25, 0)
            val surface1End = Vector2(25, 64)
            val surface1Normal = Vector2(-1, 0)
            val surface2Start = Vector2(40, 0)
            val surface2End = Vector2(40, 64)
            val surface2Normal = Vector2(1, 0)
            solidSurface.addSurface(surface1Start, surface1End, surface1Normal)
            solidSurface.addSurface(surface2Start, surface2End, surface2Normal)
            val basicVulnerabilityVolume = FixedSizeArray<CollisionVolume>(1)
            basicVulnerabilityVolume.add(SphereCollisionVolume(16, 32, 32))
            basicVulnerabilityVolume[0]!!.hitType = HitType.HIT
            val basicAttackVolume = FixedSizeArray<CollisionVolume>(1)
            basicAttackVolume.add(SphereCollisionVolume(16, 48, 32, HitType.HIT))
            val idle = SpriteAnimation(EnemyAnimations.IDLE.ordinal, 1)
            idle.addFrame(AnimationFrame(
                    textureLibrary!!.allocateTexture(R.drawable.enemy_skeleton_stand),
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume))
            val walk = SpriteAnimation(EnemyAnimations.MOVE.ordinal, 6)
            walk.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_skeleton_walk01),
                    Utils.framesToTime(24, 3), null, basicVulnerabilityVolume))
            walk.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_skeleton_walk02),
                    Utils.framesToTime(24, 4), null, basicVulnerabilityVolume))
            walk.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_skeleton_walk03),
                    Utils.framesToTime(24, 3), null, basicVulnerabilityVolume))
            walk.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_skeleton_walk04),
                    Utils.framesToTime(24, 3), null, basicVulnerabilityVolume))
            walk.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_skeleton_walk05),
                    Utils.framesToTime(24, 4), null, basicVulnerabilityVolume))
            walk.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_skeleton_walk03),
                    Utils.framesToTime(24, 3), null, basicVulnerabilityVolume))
            walk.loop = true
            val attack = SpriteAnimation(EnemyAnimations.ATTACK.ordinal, 3)
            attack.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_skeleton_attack01),
                    Utils.framesToTime(24, 5), null, basicVulnerabilityVolume))
            attack.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_skeleton_attack03),
                    Utils.framesToTime(24, 1), basicAttackVolume, basicVulnerabilityVolume))
            attack.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_skeleton_attack04),
                    Utils.framesToTime(24, 1), basicAttackVolume, basicVulnerabilityVolume))
            staticData.add(gravity)
            staticData.add(movement)
            staticData.add(physics)
            staticData.add(solidSurface)
            staticData.add(idle)
            staticData.add(walk)
            staticData.add(attack)
            setStaticData(GameObjectType.SKELETON, staticData)
        }
        val render = allocateComponent(RenderComponent::class.java) as RenderComponent?
        render!!.priority = SortConstants.GENERAL_ENEMY
        val bgcollision = allocateComponent(BackgroundCollisionComponent::class.java) as BackgroundCollisionComponent?
        bgcollision!!.setSize(32, 48)
        bgcollision.setOffset(16, 5)
        val sprite = allocateComponent(SpriteComponent::class.java) as SpriteComponent?
        sprite!!.setSize(thing.width.toInt(), thing.height.toInt())
        sprite.setRenderComponent(render)
        val animation = allocateComponent(EnemyAnimationComponent::class.java) as EnemyAnimationComponent?
        animation!!.setSprite(sprite)
        val patrol = allocateComponent(PatrolComponent::class.java) as PatrolComponent?
        patrol!!.setMovementSpeed(20.0f, 1000.0f)
        patrol.setTurnToFacePlayer(true)
        val collision = allocateComponent(DynamicCollisionComponent::class.java) as DynamicCollisionComponent?
        sprite.setCollisionComponent(collision)
        val hitReact = allocateComponent(HitReactionComponent::class.java) as HitReactionComponent?
        collision!!.setHitReactionComponent(hitReact)
        val lifetime = allocateComponent(LifetimeComponent::class.java) as LifetimeComponent?
        lifetime!!.setVulnerableToDeathTiles(true)
        lifetime.setObjectToSpawnOnDeath(GameObjectType.SMOKE_POOF)
        val sound = sSystemRegistry.soundSystem
        if (sound != null) {
            lifetime.setDeathSound(sound.load(R.raw.sound_stomp))
        }
        thing.team = Team.ENEMY
        if (flipHorizontal) {
            thing.facingDirection.x = -1.0f
        }
        thing.add(render)
        thing.add(sprite)
        thing.add(bgcollision)
        thing.add(animation)
        thing.add(patrol)
        thing.add(collision)
        thing.add(hitReact!!)
        thing.add(lifetime)
        addStaticData(GameObjectType.SKELETON, thing, sprite)
        val attack = sprite.findAnimation(EnemyAnimations.ATTACK.ordinal)
        if (attack != null) {
            patrol.setupAttack(75.0f, attack.length, 2.0f, true)
        }
        sprite.playAnimation(0)
        return thing
    }

    fun spawnEnemyKaraguin(positionX: Float, positionY: Float, flipHorizontal: Boolean): GameObject? {
        val textureLibrary = sSystemRegistry.shortTermTextureLibrary
        val thing = gameObjectPool.allocate()
        thing!!.position[positionX] = positionY
        thing.activationRadius = normalActivationRadius
        thing.width = 32f
        thing.height = 32f
        var staticData = getStaticData(GameObjectType.KARAGUIN)
        if (staticData == null) {
            val staticObjectCount = 2
            staticData = FixedSizeArray(staticObjectCount)
            val movement = allocateComponent(MovementComponent::class.java)
            val basicVulnerabilityVolume = FixedSizeArray<CollisionVolume>(1)
            basicVulnerabilityVolume.add(SphereCollisionVolume(8, 16, 16))
            basicVulnerabilityVolume[0]!!.hitType = HitType.HIT
            val basicAttackVolume = FixedSizeArray<CollisionVolume>(1)
            basicAttackVolume.add(SphereCollisionVolume(8, 16, 16, HitType.HIT))
            val idle = SpriteAnimation(0, 3)
            idle.addFrame(AnimationFrame(
                    textureLibrary!!.allocateTexture(R.drawable.enemy_karaguin01),
                    Utils.framesToTime(24, 1), basicAttackVolume, basicVulnerabilityVolume))
            idle.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_karaguin02),
                    Utils.framesToTime(24, 1), basicAttackVolume, basicVulnerabilityVolume))
            idle.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_karaguin03),
                    Utils.framesToTime(24, 1), basicAttackVolume, basicVulnerabilityVolume))
            idle.loop = true
            staticData.add(movement)
            staticData.add(idle)
            setStaticData(GameObjectType.KARAGUIN, staticData)
        }
        val render = allocateComponent(RenderComponent::class.java) as RenderComponent?
        render!!.priority = SortConstants.GENERAL_ENEMY
        val sprite = allocateComponent(SpriteComponent::class.java) as SpriteComponent?
        sprite!!.setSize(thing.width.toInt(), thing.height.toInt())
        sprite.setRenderComponent(render)
        val patrol = allocateComponent(PatrolComponent::class.java) as PatrolComponent?
        patrol!!.setMovementSpeed(50.0f, 1000.0f)
        patrol.setTurnToFacePlayer(false)
        patrol.setFlying(true)
        val collision = allocateComponent(DynamicCollisionComponent::class.java) as DynamicCollisionComponent?
        sprite.setCollisionComponent(collision)
        val hitReact = allocateComponent(HitReactionComponent::class.java) as HitReactionComponent?
        collision!!.setHitReactionComponent(hitReact)
        val lifetime = allocateComponent(LifetimeComponent::class.java) as LifetimeComponent?
        lifetime!!.setObjectToSpawnOnDeath(GameObjectType.SMOKE_POOF)
        val sound = sSystemRegistry.soundSystem
        if (sound != null) {
            lifetime.setDeathSound(sound.load(R.raw.sound_stomp))
        }
        val animation = allocateComponent(EnemyAnimationComponent::class.java) as EnemyAnimationComponent?
        animation!!.setSprite(sprite)
        thing.team = Team.ENEMY
        if (flipHorizontal) {
            thing.facingDirection.x = -1.0f
        }
        // HACK.  These guys originally moved on their own, so let's keep them that way.
        thing.velocity.x = 50.0f * thing.facingDirection.x
        thing.targetVelocity.x = 50.0f * thing.facingDirection.x
        thing.add(render)
        thing.add(animation)
        thing.add(sprite)
        thing.add(patrol)
        thing.add(collision)
        thing.add(hitReact!!)
        thing.add(lifetime)
        addStaticData(GameObjectType.KARAGUIN, thing, sprite)
        sprite.playAnimation(0)
        return thing
    }

    fun spawnEnemyPinkNamazu(positionX: Float, positionY: Float, flipHorizontal: Boolean): GameObject? {
        val textureLibrary = sSystemRegistry.shortTermTextureLibrary
        val thing = gameObjectPool.allocate()
        thing!!.position[positionX] = positionY
        thing.activationRadius = tightActivationRadius
        thing.width = 128f
        thing.height = 128f
        var staticData = getStaticData(GameObjectType.PINK_NAMAZU)
        if (staticData == null) {
            val staticObjectCount = 7
            staticData = FixedSizeArray(staticObjectCount)
            val gravity = allocateComponent(GravityComponent::class.java)
            val movement = allocateComponent(MovementComponent::class.java)
            val physics = allocateComponent(SimplePhysicsComponent::class.java)
            val solidSurface = allocateComponent(SolidSurfaceComponent::class.java) as SolidSurfaceComponent?
            solidSurface!!.inititalize(5)
            // circle shape:
            //  __        __3
            // /  \      2/ \4
            // |   |     1|  |5
            /*
                0:12,6:22,52:0.98058067569092,-0.19611613513818
                0:22,52:50,75:-0.62580046626293,0.77998318983495
                0:50,75:81,75:0,1
                0:81,75:104,49:0.74038072228541,0.67218776102228
                0:104,49:104,6:-0.99997086544204,-0.00763336538505
             */
            val surface1Start = Vector2(12, 3)
            val surface1End = Vector2(22, 52)
            val surface1Normal = Vector2(-0.98058067569092f, -0.19611613513818f)
            surface1Normal.normalize()
            val surface2Start = Vector2(22, 52)
            val surface2End = Vector2(50, 75)
            val surface2Normal = Vector2(-0.62580046626293f, 0.77998318983495f)
            surface2Normal.normalize()
            val surface3Start = Vector2(50, 75)
            val surface3End = Vector2(81, 75)
            val surface3Normal = Vector2(0, 1)
            val surface4Start = Vector2(81, 75)
            val surface4End = Vector2(104, 49)
            val surface4Normal = Vector2(0.74038072228541f, 0.67218776102228f)
            val surface5Start = Vector2(104, 49)
            val surface5End = Vector2(104, 3)
            val surface5Normal = Vector2(1.0f, 0.0f)
            solidSurface.addSurface(surface1Start, surface1End, surface1Normal)
            solidSurface.addSurface(surface2Start, surface2End, surface2Normal)
            solidSurface.addSurface(surface3Start, surface3End, surface3Normal)
            solidSurface.addSurface(surface4Start, surface4End, surface4Normal)
            solidSurface.addSurface(surface5Start, surface5End, surface5Normal)
            val idle = SpriteAnimation(GenericAnimationComponent.Animation.IDLE, 4)
            idle.addFrame(AnimationFrame(
                    textureLibrary!!.allocateTexture(R.drawable.enemy_pinkdude_stand),
                    Utils.framesToTime(24, 8), null, null))
            val idle1 = AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_pinkdude_sleep01),
                    Utils.framesToTime(24, 3), null, null)
            val idle2 = AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_pinkdude_sleep02),
                    Utils.framesToTime(24, 8), null, null)
            idle.addFrame(idle1)
            idle.addFrame(idle2)
            idle.addFrame(idle1)
            idle.loop = true
            val wake = SpriteAnimation(GenericAnimationComponent.Animation.MOVE, 4)
            val wake1 = AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_pinkdude_eyeopen),
                    Utils.framesToTime(24, 3), null, null)
            val wake2 = AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_pinkdude_stand),
                    Utils.framesToTime(24, 3), null, null)
            wake.addFrame(wake1)
            wake.addFrame(wake2)
            wake.addFrame(wake1)
            wake.addFrame(wake2)
            val crushAttackVolume = FixedSizeArray<CollisionVolume>(1)
            crushAttackVolume.add(AABoxCollisionVolume(32, 0, 64, 32, HitType.HIT))
            val attack = SpriteAnimation(GenericAnimationComponent.Animation.ATTACK, 1)
            attack.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_pinkdude_jump),
                    Utils.framesToTime(24, 2), crushAttackVolume, null))
            staticData.add(gravity)
            staticData.add(movement)
            staticData.add(physics)
            staticData.add(solidSurface)
            staticData.add(idle)
            staticData.add(wake)
            staticData.add(attack)
            setStaticData(GameObjectType.PINK_NAMAZU, staticData)
        }
        val render = allocateComponent(RenderComponent::class.java) as RenderComponent?
        render!!.priority = SortConstants.GENERAL_ENEMY
        val bgcollision = allocateComponent(BackgroundCollisionComponent::class.java) as BackgroundCollisionComponent?
        bgcollision!!.setSize(100, 75)
        bgcollision.setOffset(12, 5)
        val sprite = allocateComponent(SpriteComponent::class.java) as SpriteComponent?
        sprite!!.setSize(thing.width.toInt(), thing.height.toInt())
        sprite.setRenderComponent(render)
        val animation = allocateComponent(GenericAnimationComponent::class.java) as GenericAnimationComponent?
        animation!!.setSprite(sprite)
        val sleeper = allocateComponent(SleeperComponent::class.java) as SleeperComponent?
        sleeper!!.setAttackImpulse(100.0f, 170.0f)
        sleeper.setSlam(0.3f, 25.0f)
        val collision = allocateComponent(DynamicCollisionComponent::class.java) as DynamicCollisionComponent?
        sprite.setCollisionComponent(collision)
        val hitReact = allocateComponent(HitReactionComponent::class.java) as HitReactionComponent?
        collision!!.setHitReactionComponent(hitReact)
        thing.team = Team.ENEMY
        thing.life = 1
        if (flipHorizontal) {
            thing.facingDirection.x = -1.0f
        }
        thing.add(render)
        thing.add(sprite)
        thing.add(bgcollision)
        thing.add(animation)
        thing.add(collision)
        thing.add(hitReact!!)
        thing.add(sleeper)
        addStaticData(GameObjectType.PINK_NAMAZU, thing, sprite)
        val wakeUp = sprite.findAnimation(GenericAnimationComponent.Animation.MOVE)
        if (wakeUp != null) {
            sleeper.setWakeUpDuration(wakeUp.length + 1.0f)
        }
        sprite.playAnimation(GenericAnimationComponent.Animation.IDLE)
        return thing
    }

    fun spawnEnemyBat(positionX: Float, positionY: Float, flipHorizontal: Boolean): GameObject? {
        val textureLibrary = sSystemRegistry.shortTermTextureLibrary
        val thing = gameObjectPool.allocate()
        thing!!.position[positionX] = positionY
        thing.activationRadius = normalActivationRadius
        thing.width = 64f
        thing.height = 32f
        var staticData = getStaticData(GameObjectType.BAT)
        if (staticData == null) {
            val staticObjectCount = 2
            staticData = FixedSizeArray(staticObjectCount)
            val movement = allocateComponent(MovementComponent::class.java)
            val basicVulnerabilityVolume = FixedSizeArray<CollisionVolume>(1)
            basicVulnerabilityVolume.add(SphereCollisionVolume(16, 32, 16))
            basicVulnerabilityVolume[0]!!.hitType = HitType.HIT
            val basicAttackVolume = FixedSizeArray<CollisionVolume>(1)
            basicAttackVolume.add(SphereCollisionVolume(16, 32, 16, HitType.HIT))
            val idle = SpriteAnimation(0, 4)
            idle.addFrame(AnimationFrame(
                    textureLibrary!!.allocateTexture(R.drawable.enemy_bat01),
                    Utils.framesToTime(24, 1), basicAttackVolume, basicVulnerabilityVolume))
            idle.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_bat02),
                    Utils.framesToTime(24, 1), basicAttackVolume, basicVulnerabilityVolume))
            idle.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_bat03),
                    Utils.framesToTime(24, 1), basicAttackVolume, basicVulnerabilityVolume))
            idle.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_bat04),
                    Utils.framesToTime(24, 1), basicAttackVolume, basicVulnerabilityVolume))
            idle.loop = true
            staticData.add(movement)
            staticData.add(idle)
            setStaticData(GameObjectType.BAT, staticData)
        }
        val render = allocateComponent(RenderComponent::class.java) as RenderComponent?
        render!!.priority = SortConstants.GENERAL_ENEMY
        val sprite = allocateComponent(SpriteComponent::class.java) as SpriteComponent?
        sprite!!.setSize(thing.width.toInt(), thing.height.toInt())
        sprite.setRenderComponent(render)
        val patrol = allocateComponent(PatrolComponent::class.java) as PatrolComponent?
        patrol!!.setMovementSpeed(75.0f, 1000.0f)
        patrol.setTurnToFacePlayer(false)
        patrol.setFlying(true)
        val collision = allocateComponent(DynamicCollisionComponent::class.java) as DynamicCollisionComponent?
        sprite.setCollisionComponent(collision)
        val hitReact = allocateComponent(HitReactionComponent::class.java) as HitReactionComponent?
        collision!!.setHitReactionComponent(hitReact)
        val lifetime = allocateComponent(LifetimeComponent::class.java) as LifetimeComponent?
        lifetime!!.setObjectToSpawnOnDeath(GameObjectType.SMOKE_POOF)
        val sound = sSystemRegistry.soundSystem
        if (sound != null) {
            lifetime.setDeathSound(sound.load(R.raw.sound_stomp))
        }
        val animation = allocateComponent(EnemyAnimationComponent::class.java) as EnemyAnimationComponent?
        animation!!.setSprite(sprite)
        thing.team = Team.ENEMY
        if (flipHorizontal) {
            thing.facingDirection.x = -1.0f
        }

        // HACK.  These guys originally moved on their own, so let's keep them that way.
        thing.velocity.x = 75.0f * thing.facingDirection.x
        thing.targetVelocity.x = 75.0f * thing.facingDirection.x
        thing.add(render)
        thing.add(animation)
        thing.add(sprite)
        thing.add(patrol)
        thing.add(collision)
        thing.add(hitReact!!)
        thing.add(lifetime)
        addStaticData(GameObjectType.BAT, thing, sprite)
        sprite.playAnimation(0)
        return thing
    }

    fun spawnEnemySting(positionX: Float, positionY: Float, flipHorizontal: Boolean): GameObject? {
        val textureLibrary = sSystemRegistry.shortTermTextureLibrary
        val thing = gameObjectPool.allocate()
        thing!!.position[positionX] = positionY
        thing.activationRadius = normalActivationRadius
        thing.width = 64f
        thing.height = 64f
        var staticData = getStaticData(GameObjectType.STING)
        if (staticData == null) {
            val staticObjectCount = 2
            staticData = FixedSizeArray(staticObjectCount)
            val movement = allocateComponent(MovementComponent::class.java)
            val basicVulnerabilityVolume = FixedSizeArray<CollisionVolume>(1)
            basicVulnerabilityVolume.add(SphereCollisionVolume(16, 32, 16))
            basicVulnerabilityVolume[0]!!.hitType = HitType.HIT
            val basicAttackVolume = FixedSizeArray<CollisionVolume>(1)
            basicAttackVolume.add(SphereCollisionVolume(16, 32, 16, HitType.HIT))
            val idle = SpriteAnimation(0, 3)
            idle.addFrame(AnimationFrame(
                    textureLibrary!!.allocateTexture(R.drawable.enemy_sting01),
                    Utils.framesToTime(24, 1), basicAttackVolume, basicVulnerabilityVolume))
            idle.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_sting02),
                    Utils.framesToTime(24, 1), basicAttackVolume, basicVulnerabilityVolume))
            idle.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_sting03),
                    Utils.framesToTime(24, 1), basicAttackVolume, basicVulnerabilityVolume))
            idle.loop = true
            staticData.add(movement)
            staticData.add(idle)
            setStaticData(GameObjectType.STING, staticData)
        }
        val render = allocateComponent(RenderComponent::class.java) as RenderComponent?
        render!!.priority = SortConstants.GENERAL_ENEMY
        val sprite = allocateComponent(SpriteComponent::class.java) as SpriteComponent?
        sprite!!.setSize(thing.width.toInt(), thing.height.toInt())
        sprite.setRenderComponent(render)
        val patrol = allocateComponent(PatrolComponent::class.java) as PatrolComponent?
        patrol!!.setMovementSpeed(75.0f, 1000.0f)
        patrol.setTurnToFacePlayer(false)
        patrol.setFlying(true)
        val collision = allocateComponent(DynamicCollisionComponent::class.java) as DynamicCollisionComponent?
        sprite.setCollisionComponent(collision)
        val hitReact = allocateComponent(HitReactionComponent::class.java) as HitReactionComponent?
        collision!!.setHitReactionComponent(hitReact)
        val lifetime = allocateComponent(LifetimeComponent::class.java) as LifetimeComponent?
        lifetime!!.setObjectToSpawnOnDeath(GameObjectType.SMOKE_POOF)
        val sound = sSystemRegistry.soundSystem
        if (sound != null) {
            lifetime.setDeathSound(sound.load(R.raw.sound_stomp))
        }
        val animation = allocateComponent(EnemyAnimationComponent::class.java) as EnemyAnimationComponent?
        animation!!.setSprite(sprite)
        thing.team = Team.ENEMY
        if (flipHorizontal) {
            thing.facingDirection.x = -1.0f
        }

        // HACK.  These guys originally moved on their own, so let's keep them that way.
        thing.velocity.x = 25.0f * thing.facingDirection.x
        thing.targetVelocity.x = 25.0f * thing.facingDirection.x
        thing.add(render)
        thing.add(animation)
        thing.add(sprite)
        thing.add(patrol)
        thing.add(collision)
        thing.add(hitReact!!)
        thing.add(lifetime)
        addStaticData(GameObjectType.STING, thing, sprite)
        sprite.playAnimation(0)
        return thing
    }

    fun spawnEnemyOnion(positionX: Float, positionY: Float, flipHorizontal: Boolean): GameObject? {
        val textureLibrary = sSystemRegistry.shortTermTextureLibrary
        val thing = gameObjectPool.allocate()
        thing!!.position[positionX] = positionY
        thing.activationRadius = normalActivationRadius
        thing.width = 64f
        thing.height = 64f
        var staticData = getStaticData(GameObjectType.ONION)
        if (staticData == null) {
            val staticObjectCount = 5
            staticData = FixedSizeArray(staticObjectCount)
            val gravity = allocateComponent(GravityComponent::class.java)
            val movement = allocateComponent(MovementComponent::class.java)
            val physics = allocateComponent(SimplePhysicsComponent::class.java) as SimplePhysicsComponent?
            physics!!.setBounciness(0.2f)


            // Animations
            val basicVulnerabilityVolume = FixedSizeArray<CollisionVolume>(1)
            basicVulnerabilityVolume.add(SphereCollisionVolume(16, 32, 32))
            val basicAttackVolume = FixedSizeArray<CollisionVolume>(1)
            basicAttackVolume.add(SphereCollisionVolume(16, 32, 32, HitType.HIT))
            val idle = SpriteAnimation(EnemyAnimations.IDLE.ordinal, 1)
            idle.addFrame(AnimationFrame(
                    textureLibrary!!.allocateTexture(R.drawable.enemy_onion01),
                    Utils.framesToTime(24, 3), basicAttackVolume, basicVulnerabilityVolume))
            idle.loop = true
            val walk = SpriteAnimation(EnemyAnimations.MOVE.ordinal, 3)
            walk.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_onion01),
                    Utils.framesToTime(24, 1), basicAttackVolume, basicVulnerabilityVolume))
            walk.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_onion02),
                    Utils.framesToTime(24, 1), basicAttackVolume, basicVulnerabilityVolume))
            walk.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_onion03),
                    Utils.framesToTime(24, 1), basicAttackVolume, basicVulnerabilityVolume))
            walk.loop = true
            staticData.add(gravity)
            staticData.add(movement)
            staticData.add(physics)
            staticData.add(idle)
            staticData.add(walk)
            setStaticData(GameObjectType.ONION, staticData)
        }
        val render = allocateComponent(RenderComponent::class.java) as RenderComponent?
        render!!.priority = SortConstants.GENERAL_ENEMY
        val bgcollision = allocateComponent(BackgroundCollisionComponent::class.java) as BackgroundCollisionComponent?
        bgcollision!!.setSize(32, 48)
        bgcollision.setOffset(16, 5)
        val sprite = allocateComponent(SpriteComponent::class.java) as SpriteComponent?
        sprite!!.setSize(thing.width.toInt(), thing.height.toInt())
        sprite.setRenderComponent(render)
        val animation = allocateComponent(EnemyAnimationComponent::class.java) as EnemyAnimationComponent?
        animation!!.setSprite(sprite)
        val patrol = allocateComponent(PatrolComponent::class.java) as PatrolComponent?
        patrol!!.setMovementSpeed(50.0f, 1000.0f)
        val collision = allocateComponent(DynamicCollisionComponent::class.java) as DynamicCollisionComponent?
        sprite.setCollisionComponent(collision)
        val hitReact = allocateComponent(HitReactionComponent::class.java) as HitReactionComponent?
        collision!!.setHitReactionComponent(hitReact)
        val lifetime = allocateComponent(LifetimeComponent::class.java) as LifetimeComponent?
        lifetime!!.setVulnerableToDeathTiles(true)
        lifetime.setObjectToSpawnOnDeath(GameObjectType.SMOKE_POOF)
        val sound = sSystemRegistry.soundSystem
        if (sound != null) {
            lifetime.setDeathSound(sound.load(R.raw.sound_stomp))
        }
        thing.add(render)
        thing.add(sprite)
        thing.add(bgcollision)
        thing.add(animation)
        thing.add(patrol)
        thing.add(collision)
        thing.add(hitReact!!)
        thing.add(lifetime)
        thing.team = Team.ENEMY
        if (flipHorizontal) {
            thing.facingDirection.x = -1.0f
        }
        addStaticData(GameObjectType.ONION, thing, sprite)
        sprite.playAnimation(0)
        return thing
    }

    fun spawnEnemyWanda(positionX: Float, positionY: Float, flipHorizontal: Boolean): GameObject? {
        val textureLibrary = sSystemRegistry.shortTermTextureLibrary


        // Make sure related textures are loaded.
        textureLibrary!!.allocateTexture(R.drawable.energy_ball01)
        textureLibrary.allocateTexture(R.drawable.energy_ball02)
        textureLibrary.allocateTexture(R.drawable.energy_ball03)
        textureLibrary.allocateTexture(R.drawable.energy_ball04)
        val thing = gameObjectPool.allocate()
        thing!!.position[positionX] = positionY
        thing.activationRadius = alwaysActive
        thing.width = 64f
        thing.height = 128f
        var staticData = getStaticData(GameObjectType.WANDA)
        if (staticData == null) {
            val staticObjectCount = 9
            staticData = FixedSizeArray(staticObjectCount)
            val gravity = allocateComponent(GravityComponent::class.java)
            val movement = allocateComponent(MovementComponent::class.java)
            val physics = allocateComponent(SimplePhysicsComponent::class.java) as SimplePhysicsComponent?
            physics!!.setBounciness(0.0f)
            val basicVulnerabilityVolume = FixedSizeArray<CollisionVolume>(1)
            basicVulnerabilityVolume.add(AABoxCollisionVolume(20, 5, 26, 80, HitType.COLLECT))
            val idle = SpriteAnimation(NPCAnimationComponent.IDLE, 1)
            idle.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_wanda_stand),
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume))
            val walkFrame1 = AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_wanda_walk01),
                    Utils.framesToTime(24, 2), null, basicVulnerabilityVolume)
            val walkFrame2 = AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_wanda_walk02),
                    Utils.framesToTime(24, 2), null, basicVulnerabilityVolume)
            val walkFrame3 = AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_wanda_walk03),
                    Utils.framesToTime(24, 2), null, basicVulnerabilityVolume)
            val walkFrame4 = AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_wanda_walk04),
                    Utils.framesToTime(24, 2), null, basicVulnerabilityVolume)
            val walkFrame5 = AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_wanda_walk05),
                    Utils.framesToTime(24, 2), null, basicVulnerabilityVolume)
            val walk = SpriteAnimation(NPCAnimationComponent.WALK, 8)
            walk.addFrame(walkFrame1)
            walk.addFrame(walkFrame2)
            walk.addFrame(walkFrame3)
            walk.addFrame(walkFrame4)
            walk.addFrame(walkFrame5)
            walk.addFrame(walkFrame4)
            walk.addFrame(walkFrame3)
            walk.addFrame(walkFrame2)
            walk.loop = true
            val runFrame4 = AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_wanda_run04),
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume)
            val run = SpriteAnimation(NPCAnimationComponent.RUN, 9)
            run.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_wanda_run01),
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume))
            run.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_wanda_run02),
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume))
            run.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_wanda_run03),
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume))
            run.addFrame(runFrame4)
            run.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_wanda_run05),
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume))
            run.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_wanda_run06),
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume))
            run.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_wanda_run07),
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume))
            run.addFrame(runFrame4)
            run.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_wanda_run08),
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume))
            run.loop = true
            val jumpStart = SpriteAnimation(NPCAnimationComponent.JUMP_START, 4)
            val jump1 = AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_wanda_jump01),
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume)
            val jump2 = AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_wanda_jump01),
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume)
            jumpStart.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_wanda_run04),
                    Utils.framesToTime(24, 2), null, basicVulnerabilityVolume))
            jumpStart.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_wanda_crouch),
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume))
            jumpStart.addFrame(jump1)
            jumpStart.addFrame(jump2)
            val jumpAir = SpriteAnimation(NPCAnimationComponent.JUMP_AIR, 2)
            jumpAir.addFrame(jump1)
            jumpAir.addFrame(jump2)
            jumpAir.loop = true
            val attack = SpriteAnimation(NPCAnimationComponent.SHOOT, 11)
            attack.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_wanda_shoot01),
                    Utils.framesToTime(24, 2), null, basicVulnerabilityVolume))
            attack.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_wanda_shoot02),
                    Utils.framesToTime(24, 8), null, basicVulnerabilityVolume))
            attack.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_wanda_shoot03),
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume))
            attack.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_wanda_shoot04),
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume))
            attack.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_wanda_shoot05),
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume))
            attack.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_wanda_shoot06),
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume))
            attack.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_wanda_shoot07),
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume))
            attack.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_wanda_shoot08),
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume))
            attack.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_wanda_shoot09),
                    Utils.framesToTime(24, 2), null, basicVulnerabilityVolume))
            attack.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_wanda_shoot02),
                    Utils.framesToTime(24, 3), null, basicVulnerabilityVolume))
            attack.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_wanda_shoot01),
                    Utils.framesToTime(24, 3), null, basicVulnerabilityVolume))
            staticData.add(gravity)
            staticData.add(movement)
            staticData.add(physics)
            staticData.add(idle)
            staticData.add(walk)
            staticData.add(run)
            staticData.add(jumpStart)
            staticData.add(jumpAir)
            staticData.add(attack)
            setStaticData(GameObjectType.WANDA, staticData)
        }
        val render = allocateComponent(RenderComponent::class.java) as RenderComponent?
        render!!.priority = SortConstants.NPC
        val bgcollision = allocateComponent(BackgroundCollisionComponent::class.java) as BackgroundCollisionComponent?
        bgcollision!!.setSize(32, 82)
        bgcollision.setOffset(20, 5)
        val sprite = allocateComponent(SpriteComponent::class.java) as SpriteComponent?
        sprite!!.setSize(thing.width.toInt(), thing.height.toInt())
        sprite.setRenderComponent(render)
        val animation = allocateComponent(NPCAnimationComponent::class.java) as NPCAnimationComponent?
        animation!!.setSprite(sprite)
        val patrol = allocateComponent(NPCComponent::class.java) as NPCComponent?
        val collision = allocateComponent(DynamicCollisionComponent::class.java) as DynamicCollisionComponent?
        sprite.setCollisionComponent(collision)
        val hitReact = allocateComponent(HitReactionComponent::class.java) as HitReactionComponent?
        collision!!.setHitReactionComponent(hitReact)
        patrol!!.setHitReactionComponent(hitReact)
        val sound = sSystemRegistry.soundSystem
        val gun = allocateComponent(LaunchProjectileComponent::class.java) as LaunchProjectileComponent?
        gun!!.setShotsPerSet(1)
        gun.setSetsPerActivation(1)
        gun.setDelayBeforeFirstSet(Utils.framesToTime(24, 11))
        gun.setObjectTypeToSpawn(GameObjectType.WANDA_SHOT)
        gun.setOffsetX(45f)
        gun.setOffsetY(42f)
        gun.setRequiredAction(ActionType.ATTACK)
        gun.setVelocityX(300.0f)
        gun.setShootSound(sound!!.load(R.raw.sound_poing))
        thing.team = Team.ENEMY
        thing.life = 1
        if (flipHorizontal) {
            thing.facingDirection.x = -1.0f
        }
        thing.add(gun)
        thing.add(render)
        thing.add(sprite)
        thing.add(bgcollision)
        thing.add(animation)
        thing.add(patrol)
        thing.add(collision)
        thing.add(hitReact!!)
        addStaticData(GameObjectType.WANDA, thing, sprite)
        sprite.playAnimation(0)
        return thing
    }

    fun spawnEnemyKyle(positionX: Float, positionY: Float, flipHorizontal: Boolean): GameObject? {
        val textureLibrary = sSystemRegistry.shortTermTextureLibrary
        val thing = gameObjectPool.allocate()
        thing!!.position[positionX] = positionY
        thing.activationRadius = alwaysActive
        thing.width = 64f
        thing.height = 128f
        var staticData = getStaticData(GameObjectType.KYLE)
        if (staticData == null) {
            val staticObjectCount = 9
            staticData = FixedSizeArray(staticObjectCount)
            val gravity = allocateComponent(GravityComponent::class.java)
            val movement = allocateComponent(MovementComponent::class.java)
            val physics = allocateComponent(SimplePhysicsComponent::class.java) as SimplePhysicsComponent?
            physics!!.setBounciness(0.0f)
            val basicVulnerabilityVolume = FixedSizeArray<CollisionVolume>(1)
            basicVulnerabilityVolume.add(AABoxCollisionVolume(20, 5, 26, 80, HitType.COLLECT))
            val idle = SpriteAnimation(NPCAnimationComponent.IDLE, 1)
            idle.addFrame(AnimationFrame(
                    textureLibrary!!.allocateTexture(R.drawable.enemy_kyle_stand),
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume))
            val walkFrame1 = AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_kyle_walk01),
                    Utils.framesToTime(24, 2), null, basicVulnerabilityVolume)
            val walkFrame2 = AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_kyle_walk02),
                    Utils.framesToTime(24, 2), null, basicVulnerabilityVolume)
            val walkFrame3 = AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_kyle_walk03),
                    Utils.framesToTime(24, 2), null, basicVulnerabilityVolume)
            val walkFrame4 = AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_kyle_walk04),
                    Utils.framesToTime(24, 2), null, basicVulnerabilityVolume)
            val walkFrame5 = AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_kyle_walk05),
                    Utils.framesToTime(24, 2), null, basicVulnerabilityVolume)
            val walkFrame6 = AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_kyle_walk06),
                    Utils.framesToTime(24, 2), null, basicVulnerabilityVolume)
            val walkFrame7 = AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_kyle_walk07),
                    Utils.framesToTime(24, 2), null, basicVulnerabilityVolume)
            val walk = SpriteAnimation(NPCAnimationComponent.WALK, 12)
            walk.addFrame(walkFrame1)
            walk.addFrame(walkFrame2)
            walk.addFrame(walkFrame3)
            walk.addFrame(walkFrame4)
            walk.addFrame(walkFrame3)
            walk.addFrame(walkFrame2)
            walk.addFrame(walkFrame1)
            walk.addFrame(walkFrame5)
            walk.addFrame(walkFrame6)
            walk.addFrame(walkFrame7)
            walk.addFrame(walkFrame6)
            walk.addFrame(walkFrame5)
            walk.loop = true
            val crouch1 = AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_kyle_crouch01),
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume)
            val crouch2 = AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_kyle_crouch02),
                    Utils.framesToTime(24, 3), null, basicVulnerabilityVolume)
            val runStart = SpriteAnimation(NPCAnimationComponent.RUN_START, 2)
            runStart.addFrame(crouch1)
            runStart.addFrame(crouch2)
            val attackVolume = FixedSizeArray<CollisionVolume>(2)
            attackVolume.add(AABoxCollisionVolume(32, 32, 50, 32, HitType.HIT))
            attackVolume.add(AABoxCollisionVolume(32, 32, 50, 32, HitType.COLLECT))
            val run = SpriteAnimation(NPCAnimationComponent.RUN, 2)
            run.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_kyle_dash01),
                    Utils.framesToTime(24, 1), attackVolume, basicVulnerabilityVolume))
            run.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_kyle_dash02),
                    Utils.framesToTime(24, 1), attackVolume, basicVulnerabilityVolume))
            run.loop = true
            val jumpStart = SpriteAnimation(NPCAnimationComponent.JUMP_START, 2)
            jumpStart.addFrame(crouch1)
            jumpStart.addFrame(crouch2)
            val jumpAir = SpriteAnimation(NPCAnimationComponent.JUMP_AIR, 2)
            val jump1 = AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_kyle_jump01),
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume)
            val jump2 = AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_kyle_jump01),
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume)
            jumpAir.addFrame(jump1)
            jumpAir.addFrame(jump2)
            jumpAir.loop = true
            staticData.add(gravity)
            staticData.add(movement)
            staticData.add(physics)
            staticData.add(idle)
            staticData.add(walk)
            staticData.add(runStart)
            staticData.add(run)
            staticData.add(jumpStart)
            staticData.add(jumpAir)
            setStaticData(GameObjectType.KYLE, staticData)
        }
        val render = allocateComponent(RenderComponent::class.java) as RenderComponent?
        render!!.priority = SortConstants.NPC
        val bgcollision = allocateComponent(BackgroundCollisionComponent::class.java) as BackgroundCollisionComponent?
        bgcollision!!.setSize(32, 90)
        bgcollision.setOffset(20, 5)
        val sprite = allocateComponent(SpriteComponent::class.java) as SpriteComponent?
        sprite!!.setSize(thing.width.toInt(), thing.height.toInt())
        sprite.setRenderComponent(render)
        val animation = allocateComponent(NPCAnimationComponent::class.java) as NPCAnimationComponent?
        animation!!.setSprite(sprite)
        animation.setStopAtWalls(false) // Kyle can run through walls
        val patrol = allocateComponent(NPCComponent::class.java) as NPCComponent?
        patrol!!.setSpeeds(350.0f, 50.0f, 400.0f, -10.0f, 400.0f)
        patrol.setGameEvent(GameFlowEvent.EVENT_SHOW_ANIMATION, AnimationPlayerActivity.KYLE_DEATH, false)
        val collision = allocateComponent(DynamicCollisionComponent::class.java) as DynamicCollisionComponent?
        sprite.setCollisionComponent(collision)
        val hitReact = allocateComponent(HitReactionComponent::class.java) as HitReactionComponent?
        collision!!.setHitReactionComponent(hitReact)
        patrol.setHitReactionComponent(hitReact)
        val motionBlur = allocateComponent(MotionBlurComponent::class.java) as MotionBlurComponent?
        motionBlur!!.setTarget(render)
        val launcher = allocateComponent(LauncherComponent::class.java) as LauncherComponent?
        launcher!!.setup((Math.PI * 0.45f).toFloat(), 1000.0f, 0.0f, 0.0f, false)
        launcher.setLaunchEffect(GameObjectType.FLASH, 70.0f, 50.0f)
        hitReact!!.setLauncherComponent(launcher, HitType.HIT)
        thing.team = Team.NONE
        thing.life = 1
        if (flipHorizontal) {
            thing.facingDirection.x = -1.0f
        }
        thing.add(render)
        thing.add(sprite)
        thing.add(bgcollision)
        thing.add(animation)
        thing.add(patrol)
        thing.add(collision)
        thing.add(hitReact)
        thing.add(motionBlur)
        thing.add(launcher)
        addStaticData(GameObjectType.KYLE, thing, sprite)
        sprite.playAnimation(0)
        return thing
    }

    fun spawnEnemyKyleDead(positionX: Float, positionY: Float): GameObject? {
        val textureLibrary = sSystemRegistry.shortTermTextureLibrary
        val thing = gameObjectPool.allocate()
        thing!!.position[positionX] = positionY
        thing.activationRadius = tightActivationRadius
        thing.width = 128f
        thing.height = 32f
        var staticData = getStaticData(GameObjectType.KYLE_DEAD)
        if (staticData == null) {
            val staticObjectCount = 1
            staticData = FixedSizeArray(staticObjectCount)
            val basicVulnerabilityVolume = FixedSizeArray<CollisionVolume>(1)
            basicVulnerabilityVolume.add(AABoxCollisionVolume(32, 5, 64, 32, HitType.COLLECT))
            val idle = SpriteAnimation(0, 1)
            val frame1 = AnimationFrame(textureLibrary!!.allocateTexture(R.drawable.enemy_kyle_dead),
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume)
            idle.addFrame(frame1)
            idle.loop = true
            staticData.add(idle)
            setStaticData(GameObjectType.KYLE_DEAD, staticData)
        }
        val render = allocateComponent(RenderComponent::class.java) as RenderComponent?
        render!!.priority = SortConstants.GENERAL_OBJECT
        val sprite = allocateComponent(SpriteComponent::class.java) as SpriteComponent?
        sprite!!.setSize(thing.width.toInt(), thing.height.toInt())
        sprite.setRenderComponent(render)
        val dynamicCollision = allocateComponent(DynamicCollisionComponent::class.java) as DynamicCollisionComponent?
        sprite.setCollisionComponent(dynamicCollision)
        val hitReact = allocateComponent(HitReactionComponent::class.java) as HitReactionComponent?
        dynamicCollision!!.setHitReactionComponent(hitReact)
        hitReact!!.setSpawnGameEventOnHit(HitType.COLLECT, GameFlowEvent.EVENT_SHOW_DIALOG_CHARACTER2, 0)
        val dialogSelect = allocateComponent(SelectDialogComponent::class.java) as SelectDialogComponent?
        dialogSelect!!.setHitReact(hitReact)

        // Since this object doesn't have gravity or background collision, adjust down to simulate the position
        // at which a bounding volume would rest.
        thing.position.y -= 5.0f
        thing.add(dialogSelect)
        thing.add(render)
        thing.add(sprite)
        thing.add(dynamicCollision)
        thing.add(hitReact)
        addStaticData(GameObjectType.KYLE_DEAD, thing, sprite)
        sprite.playAnimation(0)
        return thing
    }

    fun spawnEnemyAndouDead(positionX: Float, positionY: Float): GameObject? {
        val textureLibrary = sSystemRegistry.shortTermTextureLibrary
        val thing = gameObjectPool.allocate()
        thing!!.position[positionX] = positionY
        thing.activationRadius = tightActivationRadius
        thing.width = 64f
        thing.height = 64f
        var staticData = getStaticData(GameObjectType.ANDOU_DEAD)
        if (staticData == null) {
            val staticObjectCount = 1
            staticData = FixedSizeArray(staticObjectCount)
            val idle = SpriteAnimation(0, 1)
            val frame1 = AnimationFrame(textureLibrary!!.allocateTexture(R.drawable.andou_explode12),
                    Utils.framesToTime(24, 1), null, null)
            idle.addFrame(frame1)
            idle.loop = true
            staticData.add(idle)
            setStaticData(GameObjectType.ANDOU_DEAD, staticData)
        }
        val render = allocateComponent(RenderComponent::class.java) as RenderComponent?
        render!!.priority = SortConstants.GENERAL_OBJECT
        val sprite = allocateComponent(SpriteComponent::class.java) as SpriteComponent?
        sprite!!.setSize(thing.width.toInt(), thing.height.toInt())
        sprite.setRenderComponent(render)
        val smokeGun = allocateComponent(LaunchProjectileComponent::class.java) as LaunchProjectileComponent?
        smokeGun!!.setDelayBetweenShots(0.25f)
        smokeGun.setObjectTypeToSpawn(GameObjectType.SMOKE_BIG)
        smokeGun.setOffsetX(32f)
        smokeGun.setOffsetY(15f)
        smokeGun.setVelocityX(-150.0f)
        smokeGun.setVelocityY(100.0f)
        smokeGun.setThetaError(0.1f)
        val smokeGun2 = allocateComponent(LaunchProjectileComponent::class.java) as LaunchProjectileComponent?
        smokeGun2!!.setDelayBetweenShots(0.35f)
        smokeGun2.setObjectTypeToSpawn(GameObjectType.SMOKE_SMALL)
        smokeGun2.setOffsetX(16f)
        smokeGun2.setOffsetY(15f)
        smokeGun2.setVelocityX(-150.0f)
        smokeGun2.setVelocityY(150.0f)
        smokeGun2.setThetaError(0.1f)
        thing.add(render)
        thing.add(sprite)
        thing.add(smokeGun)
        thing.add(smokeGun2)
        thing.facingDirection.x = -1.0f
        addStaticData(GameObjectType.ANDOU_DEAD, thing, sprite)
        sprite.playAnimation(0)
        return thing
    }

    fun spawnEnemyKabocha(positionX: Float, positionY: Float, flipHorizontal: Boolean): GameObject? {
        val textureLibrary = sSystemRegistry.shortTermTextureLibrary
        val thing = gameObjectPool.allocate()
        thing!!.position[positionX] = positionY
        thing.activationRadius = alwaysActive
        thing.width = 64f
        thing.height = 128f
        var staticData = getStaticData(GameObjectType.KABOCHA)
        if (staticData == null) {
            val staticObjectCount = 5
            staticData = FixedSizeArray(staticObjectCount)
            val gravity = allocateComponent(GravityComponent::class.java)
            val movement = allocateComponent(MovementComponent::class.java)
            val physics = allocateComponent(SimplePhysicsComponent::class.java) as SimplePhysicsComponent?
            physics!!.setBounciness(0.0f)
            val basicVulnerabilityVolume = FixedSizeArray<CollisionVolume>(1)
            basicVulnerabilityVolume.add(AABoxCollisionVolume(20, 5, 26, 80, HitType.COLLECT))
            val idle = SpriteAnimation(NPCAnimationComponent.IDLE, 1)
            idle.addFrame(AnimationFrame(
                    textureLibrary!!.allocateTexture(R.drawable.enemy_kabocha_stand),
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume))
            val walkFrame1 = AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_kabocha_walk01),
                    Utils.framesToTime(24, 3), null, basicVulnerabilityVolume)
            val walkFrame2 = AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_kabocha_walk02),
                    Utils.framesToTime(24, 3), null, basicVulnerabilityVolume)
            val walkFrame3 = AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_kabocha_walk03),
                    Utils.framesToTime(24, 3), null, basicVulnerabilityVolume)
            val walkFrame4 = AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_kabocha_walk04),
                    Utils.framesToTime(24, 3), null, basicVulnerabilityVolume)
            val walkFrame5 = AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_kabocha_walk05),
                    Utils.framesToTime(24, 3), null, basicVulnerabilityVolume)
            val walkFrame6 = AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_kabocha_walk06),
                    Utils.framesToTime(24, 3), null, basicVulnerabilityVolume)
            val walk = SpriteAnimation(NPCAnimationComponent.WALK, 6)
            walk.addFrame(walkFrame1)
            walk.addFrame(walkFrame2)
            walk.addFrame(walkFrame3)
            walk.addFrame(walkFrame4)
            walk.addFrame(walkFrame5)
            walk.addFrame(walkFrame6)
            walk.loop = true
            staticData.add(gravity)
            staticData.add(movement)
            staticData.add(physics)
            staticData.add(idle)
            staticData.add(walk)
            setStaticData(GameObjectType.KABOCHA, staticData)
        }
        val render = allocateComponent(RenderComponent::class.java) as RenderComponent?
        render!!.priority = SortConstants.NPC
        val bgcollision = allocateComponent(BackgroundCollisionComponent::class.java) as BackgroundCollisionComponent?
        bgcollision!!.setSize(38, 82)
        bgcollision.setOffset(16, 5)
        val sprite = allocateComponent(SpriteComponent::class.java) as SpriteComponent?
        sprite!!.setSize(thing.width.toInt(), thing.height.toInt())
        sprite.setRenderComponent(render)
        val animation = allocateComponent(NPCAnimationComponent::class.java) as NPCAnimationComponent?
        animation!!.setSprite(sprite)
        val patrol = allocateComponent(NPCComponent::class.java) as NPCComponent?
        val collision = allocateComponent(DynamicCollisionComponent::class.java) as DynamicCollisionComponent?
        sprite.setCollisionComponent(collision)
        val hitReact = allocateComponent(HitReactionComponent::class.java) as HitReactionComponent?
        collision!!.setHitReactionComponent(hitReact)
        patrol!!.setHitReactionComponent(hitReact)
        thing.team = Team.ENEMY
        thing.life = 1
        if (flipHorizontal) {
            thing.facingDirection.x = -1.0f
        }
        thing.add(render)
        thing.add(sprite)
        thing.add(bgcollision)
        thing.add(animation)
        thing.add(patrol)
        thing.add(collision)
        thing.add(hitReact!!)
        addStaticData(GameObjectType.KABOCHA, thing, sprite)
        sprite.playAnimation(0)
        return thing
    }

    fun spawnRokudouTerminal(positionX: Float, positionY: Float): GameObject? {
        val textureLibrary = sSystemRegistry.shortTermTextureLibrary
        val thing = gameObjectPool.allocate()
        thing!!.position[positionX] = positionY
        thing.activationRadius = tightActivationRadius
        thing.width = 64f
        thing.height = 64f
        var staticData = getStaticData(GameObjectType.ROKUDOU_TERMINAL)
        if (staticData == null) {
            val staticObjectCount = 1
            staticData = FixedSizeArray(staticObjectCount)
            val basicVulnerabilityVolume = FixedSizeArray<CollisionVolume>(1)
            basicVulnerabilityVolume.add(AABoxCollisionVolume(0, 0, 64, 64))
            basicVulnerabilityVolume[0]!!.hitType = HitType.COLLECT
            val frame1 = AnimationFrame(textureLibrary!!.allocateTexture(R.drawable.object_terminal01),
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume)
            val frame2 = AnimationFrame(textureLibrary.allocateTexture(R.drawable.object_terminal02),
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume)
            val frame3 = AnimationFrame(textureLibrary.allocateTexture(R.drawable.object_terminal03),
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume)
            val frame4 = AnimationFrame(textureLibrary.allocateTexture(R.drawable.object_terminal01),
                    Utils.framesToTime(24, 2), null, basicVulnerabilityVolume)
            val frame5 = AnimationFrame(textureLibrary.allocateTexture(R.drawable.object_terminal02),
                    Utils.framesToTime(24, 2), null, basicVulnerabilityVolume)
            val frame6 = AnimationFrame(textureLibrary.allocateTexture(R.drawable.object_terminal01),
                    1.0f, null, basicVulnerabilityVolume)
            val idle = SpriteAnimation(0, 12)
            idle.addFrame(frame1)
            idle.addFrame(frame5)
            idle.addFrame(frame4)
            idle.addFrame(frame3)
            idle.addFrame(frame2)
            idle.addFrame(frame6)
            idle.addFrame(frame6)
            idle.addFrame(frame3)
            idle.addFrame(frame2)
            idle.addFrame(frame1)
            idle.addFrame(frame2)
            idle.addFrame(frame6)
            idle.loop = true
            staticData.add(idle)
            setStaticData(GameObjectType.ROKUDOU_TERMINAL, staticData)
        }
        val render = allocateComponent(RenderComponent::class.java) as RenderComponent?
        render!!.priority = SortConstants.GENERAL_OBJECT
        val sprite = allocateComponent(SpriteComponent::class.java) as SpriteComponent?
        sprite!!.setSize(thing.width.toInt(), thing.height.toInt())
        sprite.setRenderComponent(render)
        val dynamicCollision = allocateComponent(DynamicCollisionComponent::class.java) as DynamicCollisionComponent?
        sprite.setCollisionComponent(dynamicCollision)
        val hitReact = allocateComponent(HitReactionComponent::class.java) as HitReactionComponent?
        hitReact!!.setSpawnGameEventOnHit(HitType.COLLECT, GameFlowEvent.EVENT_SHOW_DIALOG_CHARACTER2, 0)
        val dialogSelect = allocateComponent(SelectDialogComponent::class.java) as SelectDialogComponent?
        dialogSelect!!.setHitReact(hitReact)
        dynamicCollision!!.setHitReactionComponent(hitReact)
        thing.add(dialogSelect)
        thing.add(render)
        thing.add(sprite)
        thing.add(dynamicCollision)
        thing.add(hitReact)
        addStaticData(GameObjectType.ROKUDOU_TERMINAL, thing, sprite)
        sprite.playAnimation(0)
        return thing
    }

    fun spawnKabochaTerminal(positionX: Float, positionY: Float): GameObject? {
        val textureLibrary = sSystemRegistry.shortTermTextureLibrary
        val thing = gameObjectPool.allocate()
        thing!!.position[positionX] = positionY
        thing.activationRadius = tightActivationRadius
        thing.width = 64f
        thing.height = 64f
        var staticData = getStaticData(GameObjectType.KABOCHA_TERMINAL)
        if (staticData == null) {
            val staticObjectCount = 1
            staticData = FixedSizeArray(staticObjectCount)
            val basicVulnerabilityVolume = FixedSizeArray<CollisionVolume>(1)
            basicVulnerabilityVolume.add(AABoxCollisionVolume(0, 0, 64, 64))
            basicVulnerabilityVolume[0]!!.hitType = HitType.COLLECT
            val frame1 = AnimationFrame(textureLibrary!!.allocateTexture(R.drawable.object_terminal_kabocha01),
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume)
            val frame2 = AnimationFrame(textureLibrary.allocateTexture(R.drawable.object_terminal_kabocha02),
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume)
            val frame3 = AnimationFrame(textureLibrary.allocateTexture(R.drawable.object_terminal_kabocha03),
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume)
            val frame4 = AnimationFrame(textureLibrary.allocateTexture(R.drawable.object_terminal_kabocha01),
                    Utils.framesToTime(24, 2), null, basicVulnerabilityVolume)
            val frame5 = AnimationFrame(textureLibrary.allocateTexture(R.drawable.object_terminal_kabocha02),
                    Utils.framesToTime(24, 2), null, basicVulnerabilityVolume)
            val frame6 = AnimationFrame(textureLibrary.allocateTexture(R.drawable.object_terminal_kabocha01),
                    1.0f, null, basicVulnerabilityVolume)
            val idle = SpriteAnimation(0, 12)
            idle.addFrame(frame1)
            idle.addFrame(frame5)
            idle.addFrame(frame4)
            idle.addFrame(frame3)
            idle.addFrame(frame2)
            idle.addFrame(frame6)
            idle.addFrame(frame6)
            idle.addFrame(frame3)
            idle.addFrame(frame2)
            idle.addFrame(frame1)
            idle.addFrame(frame2)
            idle.addFrame(frame6)
            idle.loop = true
            staticData.add(idle)
            setStaticData(GameObjectType.KABOCHA_TERMINAL, staticData)
        }
        val render = allocateComponent(RenderComponent::class.java) as RenderComponent?
        render!!.priority = SortConstants.GENERAL_OBJECT
        val sprite = allocateComponent(SpriteComponent::class.java) as SpriteComponent?
        sprite!!.setSize(thing.width.toInt(), thing.height.toInt())
        sprite.setRenderComponent(render)
        val dynamicCollision = allocateComponent(DynamicCollisionComponent::class.java) as DynamicCollisionComponent?
        sprite.setCollisionComponent(dynamicCollision)
        val hitReact = allocateComponent(HitReactionComponent::class.java) as HitReactionComponent?
        hitReact!!.setSpawnGameEventOnHit(HitType.COLLECT, GameFlowEvent.EVENT_SHOW_DIALOG_CHARACTER2, 0)
        val dialogSelect = allocateComponent(SelectDialogComponent::class.java) as SelectDialogComponent?
        dialogSelect!!.setHitReact(hitReact)
        dynamicCollision!!.setHitReactionComponent(hitReact)
        thing.add(dialogSelect)
        thing.add(render)
        thing.add(sprite)
        thing.add(dynamicCollision)
        thing.add(hitReact)
        addStaticData(GameObjectType.KABOCHA_TERMINAL, thing, sprite)
        sprite.playAnimation(0)
        return thing
    }

    fun spawnEnemyEvilKabocha(positionX: Float, positionY: Float, flipHorizontal: Boolean): GameObject? {
        val textureLibrary = sSystemRegistry.shortTermTextureLibrary
        val thing = gameObjectPool.allocate()
        thing!!.position[positionX] = positionY
        thing.activationRadius = normalActivationRadius
        thing.width = 128f
        thing.height = 128f
        var staticData = getStaticData(GameObjectType.EVIL_KABOCHA)
        if (staticData == null) {
            val staticObjectCount = 8
            staticData = FixedSizeArray(staticObjectCount)
            val gravity = allocateComponent(GravityComponent::class.java)
            val movement = allocateComponent(MovementComponent::class.java)
            val physics = allocateComponent(SimplePhysicsComponent::class.java) as SimplePhysicsComponent?
            physics!!.setBounciness(0.0f)
            val basicVulnerabilityVolume = FixedSizeArray<CollisionVolume>(1)
            basicVulnerabilityVolume.add(AABoxCollisionVolume(52, 5, 26, 80, HitType.HIT))
            val idle = SpriteAnimation(NPCAnimationComponent.IDLE, 1)
            idle.addFrame(AnimationFrame(
                    textureLibrary!!.allocateTexture(R.drawable.enemy_kabocha_evil_stand),
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume))
            val walkFrame1 = AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_kabocha_evil_walk01),
                    Utils.framesToTime(24, 3), null, basicVulnerabilityVolume)
            val walkFrame2 = AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_kabocha_evil_walk02),
                    Utils.framesToTime(24, 3), null, basicVulnerabilityVolume)
            val walkFrame3 = AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_kabocha_evil_walk03),
                    Utils.framesToTime(24, 3), null, basicVulnerabilityVolume)
            val walkFrame4 = AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_kabocha_evil_walk04),
                    Utils.framesToTime(24, 3), null, basicVulnerabilityVolume)
            val walkFrame5 = AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_kabocha_evil_walk05),
                    Utils.framesToTime(24, 3), null, basicVulnerabilityVolume)
            val walkFrame6 = AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_kabocha_evil_walk06),
                    Utils.framesToTime(24, 3), null, basicVulnerabilityVolume)
            val walk = SpriteAnimation(NPCAnimationComponent.WALK, 6)
            walk.addFrame(walkFrame1)
            walk.addFrame(walkFrame2)
            walk.addFrame(walkFrame3)
            walk.addFrame(walkFrame4)
            walk.addFrame(walkFrame5)
            walk.addFrame(walkFrame6)
            walk.loop = true
            val surprised = SpriteAnimation(NPCAnimationComponent.SURPRISED, 1)
            surprised.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_kabocha_evil_surprised),
                    4.0f, null, null))
            val hit = SpriteAnimation(NPCAnimationComponent.TAKE_HIT, 2)
            hit.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_kabocha_evil_hit01),
                    Utils.framesToTime(24, 1), null, null))
            hit.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_kabocha_evil_hit02),
                    Utils.framesToTime(24, 10), null, null))
            val die = SpriteAnimation(NPCAnimationComponent.DEATH, 5)
            die.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_kabocha_evil_die01),
                    Utils.framesToTime(24, 6), null, null))
            die.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_kabocha_evil_stand),
                    Utils.framesToTime(24, 2), null, null))
            die.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_kabocha_evil_die02),
                    Utils.framesToTime(24, 2), null, null))
            die.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_kabocha_evil_die03),
                    Utils.framesToTime(24, 2), null, null))
            die.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_kabocha_evil_die04),
                    Utils.framesToTime(24, 6), null, null))
            staticData.add(gravity)
            staticData.add(movement)
            staticData.add(physics)
            staticData.add(idle)
            staticData.add(walk)
            staticData.add(surprised)
            staticData.add(hit)
            staticData.add(die)
            setStaticData(GameObjectType.EVIL_KABOCHA, staticData)
        }
        val render = allocateComponent(RenderComponent::class.java) as RenderComponent?
        render!!.priority = SortConstants.NPC
        val bgcollision = allocateComponent(BackgroundCollisionComponent::class.java) as BackgroundCollisionComponent?
        bgcollision!!.setSize(38, 82)
        bgcollision.setOffset(45, 5)
        val sprite = allocateComponent(SpriteComponent::class.java) as SpriteComponent?
        sprite!!.setSize(thing.width.toInt(), thing.height.toInt())
        sprite.setRenderComponent(render)
        val animation = allocateComponent(NPCAnimationComponent::class.java) as NPCAnimationComponent?
        animation!!.setSprite(sprite)
        var surpriseChannel: ChannelSystem.Channel? = null
        val channelSystem = sSystemRegistry.channelSystem
        surpriseChannel = channelSystem!!.registerChannel(sSurprisedNPCChannel)
        animation.setChannel(surpriseChannel)
        animation.setChannelTrigger(NPCAnimationComponent.SURPRISED)
        val patrol = allocateComponent(NPCComponent::class.java) as NPCComponent?
        patrol!!.setSpeeds(50.0f, 50.0f, 0.0f, -10.0f, 200.0f)
        patrol.setReactToHits(true)
        patrol.setGameEvent(GameFlowEvent.EVENT_SHOW_ANIMATION, AnimationPlayerActivity.ROKUDOU_ENDING, true)
        val collision = allocateComponent(DynamicCollisionComponent::class.java) as DynamicCollisionComponent?
        sprite.setCollisionComponent(collision)
        val hitReact = allocateComponent(HitReactionComponent::class.java) as HitReactionComponent?
        collision!!.setHitReactionComponent(hitReact)
        val sound = sSystemRegistry.soundSystem
        if (sound != null) {
            hitReact!!.setTakeHitSound(HitType.HIT, sound.load(R.raw.sound_kabocha_hit))
        }
        patrol.setHitReactionComponent(hitReact)
        thing.team = Team.ENEMY
        thing.life = 3
        if (flipHorizontal) {
            thing.facingDirection.x = -1.0f
        }
        thing.add(render)
        thing.add(sprite)
        thing.add(bgcollision)
        thing.add(animation)
        thing.add(patrol)
        thing.add(collision)
        thing.add(hitReact!!)
        addStaticData(GameObjectType.EVIL_KABOCHA, thing, sprite)
        sprite.playAnimation(0)
        return thing
    }

    fun spawnEnemyRokudou(positionX: Float, positionY: Float, flipHorizontal: Boolean): GameObject? {
        val textureLibrary = sSystemRegistry.shortTermTextureLibrary

        // Make sure related textures are loaded.
        textureLibrary!!.allocateTexture(R.drawable.energy_ball01)
        textureLibrary.allocateTexture(R.drawable.energy_ball02)
        textureLibrary.allocateTexture(R.drawable.energy_ball03)
        textureLibrary.allocateTexture(R.drawable.energy_ball04)
        textureLibrary.allocateTexture(R.drawable.effect_bullet01)
        textureLibrary.allocateTexture(R.drawable.effect_bullet02)
        val thing = gameObjectPool.allocate()
        thing!!.position[positionX] = positionY
        thing.activationRadius = normalActivationRadius
        thing.width = 128f
        thing.height = 128f
        var staticData = getStaticData(GameObjectType.ROKUDOU)
        if (staticData == null) {
            val staticObjectCount = 8
            staticData = FixedSizeArray(staticObjectCount)
            val movement = allocateComponent(MovementComponent::class.java)
            val physics = allocateComponent(SimplePhysicsComponent::class.java) as SimplePhysicsComponent?
            physics!!.setBounciness(0.0f)
            val basicVulnerabilityVolume = FixedSizeArray<CollisionVolume>(1)
            basicVulnerabilityVolume.add(AABoxCollisionVolume(45, 23, 42, 75, HitType.HIT))
            val idle = SpriteAnimation(NPCAnimationComponent.IDLE, 1)
            idle.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_rokudou_fight_stand),
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume))
            val fly = SpriteAnimation(NPCAnimationComponent.WALK, 2)
            fly.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_rokudou_fight_fly01),
                    1.0f, null, basicVulnerabilityVolume))
            fly.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_rokudou_fight_fly02),
                    1.0f, null, basicVulnerabilityVolume))
            fly.loop = true
            val shoot = SpriteAnimation(NPCAnimationComponent.SHOOT, 2)
            shoot.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_rokudou_fight_shoot01),
                    Utils.framesToTime(24, 2), null, basicVulnerabilityVolume))
            shoot.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_rokudou_fight_shoot02),
                    Utils.framesToTime(24, 2), null, basicVulnerabilityVolume))
            shoot.loop = true
            val surprised = SpriteAnimation(NPCAnimationComponent.SURPRISED, 1)
            surprised.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_rokudou_fight_surprise),
                    4.0f, null, null))
            val hit = SpriteAnimation(NPCAnimationComponent.TAKE_HIT, 7)
            val hitFrame1 = AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_rokudou_fight_hit01),
                    Utils.framesToTime(24, 2), null, null)
            val hitFrame2 = AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_rokudou_fight_hit02),
                    Utils.framesToTime(24, 1), null, null)
            val hitFrame3 = AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_rokudou_fight_hit03),
                    Utils.framesToTime(24, 1), null, null)
            hit.addFrame(hitFrame1)
            hit.addFrame(hitFrame2)
            hit.addFrame(hitFrame3)
            hit.addFrame(hitFrame2)
            hit.addFrame(hitFrame3)
            hit.addFrame(hitFrame2)
            hit.addFrame(hitFrame3)
            val die = SpriteAnimation(NPCAnimationComponent.DEATH, 5)
            die.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_rokudou_fight_stand),
                    Utils.framesToTime(24, 6), null, null))
            die.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_rokudou_fight_die01),
                    Utils.framesToTime(24, 2), null, null))
            die.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_rokudou_fight_die02),
                    Utils.framesToTime(24, 4), null, null))
            die.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_rokudou_fight_die03),
                    Utils.framesToTime(24, 6), null, null))
            die.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_rokudou_fight_die04),
                    Utils.framesToTime(24, 6), null, null))
            staticData.add(movement)
            staticData.add(physics)
            staticData.add(idle)
            staticData.add(fly)
            staticData.add(surprised)
            staticData.add(hit)
            staticData.add(die)
            staticData.add(shoot)
            setStaticData(GameObjectType.ROKUDOU, staticData)
        }
        val render = allocateComponent(RenderComponent::class.java) as RenderComponent?
        render!!.priority = SortConstants.NPC
        val bgcollision = allocateComponent(BackgroundCollisionComponent::class.java) as BackgroundCollisionComponent?
        bgcollision!!.setSize(45, 75)
        bgcollision.setOffset(45, 23)
        val sprite = allocateComponent(SpriteComponent::class.java) as SpriteComponent?
        sprite!!.setSize(thing.width.toInt(), thing.height.toInt())
        sprite.setRenderComponent(render)
        val animation = allocateComponent(NPCAnimationComponent::class.java) as NPCAnimationComponent?
        animation!!.setSprite(sprite)
        animation.setFlying(true)
        var surpriseChannel: ChannelSystem.Channel? = null
        val channelSystem = sSystemRegistry.channelSystem
        surpriseChannel = channelSystem!!.registerChannel(sSurprisedNPCChannel)
        animation.setChannel(surpriseChannel)
        animation.setChannelTrigger(NPCAnimationComponent.SURPRISED)
        val patrol = allocateComponent(NPCComponent::class.java) as NPCComponent?
        patrol!!.setSpeeds(500.0f, 100.0f, 100.0f, -100.0f, 400.0f)
        patrol.setFlying(true)
        patrol.setReactToHits(true)
        patrol.setGameEvent(GameFlowEvent.EVENT_SHOW_ANIMATION, AnimationPlayerActivity.KABOCHA_ENDING, true)
        patrol.setPauseOnAttack(false)
        val collision = allocateComponent(DynamicCollisionComponent::class.java) as DynamicCollisionComponent?
        sprite.setCollisionComponent(collision)
        val hitReact = allocateComponent(HitReactionComponent::class.java) as HitReactionComponent?
        collision!!.setHitReactionComponent(hitReact)
        val sound = sSystemRegistry.soundSystem
        if (sound != null) {
            hitReact!!.setTakeHitSound(HitType.HIT, sound.load(R.raw.sound_rokudou_hit))
        }
        patrol.setHitReactionComponent(hitReact)
        val deathSwap = allocateComponent(ChangeComponentsComponent::class.java) as ChangeComponentsComponent?
        deathSwap!!.addSwapInComponent(allocateComponent(GravityComponent::class.java))
        deathSwap.setSwapAction(ActionType.DEATH)
        val gun = allocateComponent(LaunchProjectileComponent::class.java) as LaunchProjectileComponent?
        gun!!.setShotsPerSet(1)
        gun.setSetsPerActivation(-1)
        gun.setDelayBetweenSets(1.5f)
        gun.setObjectTypeToSpawn(GameObjectType.ENERGY_BALL)
        gun.setOffsetX(75f)
        gun.setOffsetY(42f)
        gun.setRequiredAction(ActionType.ATTACK)
        gun.setVelocityX(300.0f)
        gun.setVelocityY(-300.0f)
        gun.setShootSound(sound!!.load(R.raw.sound_poing))
        val gun2 = allocateComponent(LaunchProjectileComponent::class.java) as LaunchProjectileComponent?
        gun2!!.setShotsPerSet(5)
        gun2.setDelayBetweenShots(0.1f)
        gun2.setSetsPerActivation(-1)
        gun2.setDelayBetweenSets(2.5f)
        gun2.setObjectTypeToSpawn(GameObjectType.TURRET_BULLET)
        gun2.setOffsetX(75f)
        gun2.setOffsetY(42f)
        gun2.setRequiredAction(ActionType.ATTACK)
        gun2.setVelocityX(300.0f)
        gun2.setVelocityY(-300.0f)
        gun.setShootSound(sound.load(R.raw.sound_gun))
        thing.team = Team.ENEMY
        thing.life = 3
        if (flipHorizontal) {
            thing.facingDirection.x = -1.0f
        }

        // HACK! Since there's no gravity and this is a big character, align him to the floor
        // manually.
        thing.position.y -= 23
        thing.add(render)
        thing.add(sprite)
        thing.add(bgcollision)
        thing.add(animation)
        thing.add(patrol)
        thing.add(collision)
        thing.add(hitReact!!)
        thing.add(deathSwap)
        thing.add(gun)
        thing.add(gun2)
        addStaticData(GameObjectType.ROKUDOU, thing, sprite)
        sprite.playAnimation(0)
        return thing
    }

    fun spawnPlayerGhost(positionX: Float, positionY: Float, player: GameObject?, lifeTime: Float): GameObject? {
        val textureLibrary = sSystemRegistry.longTermTextureLibrary
        val thing = gameObjectPool.allocate()
        thing!!.position[positionX] = positionY
        thing.activationRadius = alwaysActive
        thing.width = 64f
        thing.height = 64f
        var staticData = getStaticData(GameObjectType.GHOST)
        if (staticData == null) {
            val staticObjectCount = 4
            staticData = FixedSizeArray(staticObjectCount)

            //GravityComponent gravity = (GravityComponent)allocateComponent(GravityComponent.class);
            //gravity.setGravityMultiplier(0.1f);
            val movement = allocateComponent(MovementComponent::class.java)
            val physics = allocateComponent(SimplePhysicsComponent::class.java) as SimplePhysicsComponent?
            physics!!.setBounciness(0.6f)
            val ghost = allocateComponent(GhostComponent::class.java) as GhostComponent?
            ghost!!.setMovementSpeed(2000.0f)
            ghost.setAcceleration(700.0f) //300
            ghost.setUseOrientationSensor(true)
            ghost.setKillOnRelease(true)
            val sound = sSystemRegistry.soundSystem
            if (sound != null) {
                ghost.setAmbientSound(sound.load(R.raw.sound_possession))
            }
            val basicAttackVolume = FixedSizeArray<CollisionVolume>(1)
            basicAttackVolume.add(SphereCollisionVolume(32, 32, 32, HitType.POSSESS))
            val idle = SpriteAnimation(0, 4)
            idle.addFrame(AnimationFrame(
                    textureLibrary!!.getTextureByResource(R.drawable.effect_energyball01),
                    Utils.framesToTime(24, 1), basicAttackVolume, null))
            idle.addFrame(AnimationFrame(
                    textureLibrary.getTextureByResource(R.drawable.effect_energyball02),
                    Utils.framesToTime(24, 1), basicAttackVolume, null))
            idle.addFrame(AnimationFrame(
                    textureLibrary.getTextureByResource(R.drawable.effect_energyball03),
                    Utils.framesToTime(24, 1), basicAttackVolume, null))
            idle.addFrame(AnimationFrame(
                    textureLibrary.getTextureByResource(R.drawable.effect_energyball04),
                    Utils.framesToTime(24, 1), basicAttackVolume, null))
            idle.loop = true

            //staticData.add(gravity);
            staticData.add(movement)
            staticData.add(physics)
            staticData.add(ghost)
            staticData.add(idle)
            setStaticData(GameObjectType.GHOST, staticData)
        }
        val render = allocateComponent(RenderComponent::class.java) as RenderComponent?
        render!!.priority = SortConstants.PROJECTILE
        val bgcollision = allocateComponent(BackgroundCollisionComponent::class.java) as BackgroundCollisionComponent?
        bgcollision!!.setSize(64, 64)
        bgcollision.setOffset(0, 0)
        val sprite = allocateComponent(SpriteComponent::class.java) as SpriteComponent?
        sprite!!.setSize(thing.width.toInt(), thing.height.toInt())
        sprite.setRenderComponent(render)
        val dynamicCollision = allocateComponent(DynamicCollisionComponent::class.java) as DynamicCollisionComponent?
        sprite.setCollisionComponent(dynamicCollision)
        val hitReact = allocateComponent(HitReactionComponent::class.java) as HitReactionComponent?
        hitReact!!.setDieOnAttack(true)
        dynamicCollision!!.setHitReactionComponent(hitReact)
        val life = allocateComponent(LifetimeComponent::class.java) as LifetimeComponent?
        // when the ghost dies it either releases itself or passes control to another object, so we
        // don't want control to return to the player.
        life!!.setReleaseGhostOnDeath(false)
        thing.life = 1
        thing.add(bgcollision)
        thing.add(render)
        thing.add(sprite)
        thing.add(dynamicCollision)
        thing.add(hitReact)
        thing.add(life)
        addStaticData(GameObjectType.GHOST, thing, sprite)
        thing.commitUpdates()
        thing.findByClass(GhostComponent::class.java)?.setLifeTime(lifeTime)
        sprite.playAnimation(0)
        return thing
    }

    fun spawnEnergyBall(positionX: Float, positionY: Float, flipHorizontal: Boolean): GameObject? {
        val textureLibrary = sSystemRegistry.shortTermTextureLibrary
        val thing = gameObjectPool.allocate()
        thing!!.position[positionX] = positionY
        thing.activationRadius = tightActivationRadius
        thing.width = 32f
        thing.height = 32f
        var staticData = getStaticData(GameObjectType.ENERGY_BALL)
        if (staticData == null) {
            val staticObjectCount = 2
            staticData = FixedSizeArray(staticObjectCount)
            val movement = allocateComponent(MovementComponent::class.java)
            val basicAttackVolume = FixedSizeArray<CollisionVolume>(1)
            basicAttackVolume.add(SphereCollisionVolume(16, 16, 16, HitType.HIT))
            val idle = SpriteAnimation(0, 4)
            idle.addFrame(AnimationFrame(
                    textureLibrary!!.allocateTexture(R.drawable.energy_ball01),
                    Utils.framesToTime(24, 1), basicAttackVolume, null))
            idle.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.energy_ball02),
                    Utils.framesToTime(24, 1), basicAttackVolume, null))
            idle.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.energy_ball03),
                    Utils.framesToTime(24, 1), basicAttackVolume, null))
            idle.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.energy_ball04),
                    Utils.framesToTime(24, 1), basicAttackVolume, null))
            idle.loop = true
            staticData.add(movement)
            staticData.add(idle)
            setStaticData(GameObjectType.ENERGY_BALL, staticData)
        }
        val render = allocateComponent(RenderComponent::class.java) as RenderComponent?
        render!!.priority = SortConstants.PROJECTILE
        val lifetime = allocateComponent(LifetimeComponent::class.java) as LifetimeComponent?
        lifetime!!.setTimeUntilDeath(5.0f)
        val sprite = allocateComponent(SpriteComponent::class.java) as SpriteComponent?
        sprite!!.setSize(thing.width.toInt(), thing.height.toInt())
        sprite.setRenderComponent(render)
        val dynamicCollision = allocateComponent(DynamicCollisionComponent::class.java) as DynamicCollisionComponent?
        sprite.setCollisionComponent(dynamicCollision)
        val hitReact = allocateComponent(HitReactionComponent::class.java) as HitReactionComponent?
        hitReact!!.setDieOnAttack(true)
        dynamicCollision!!.setHitReactionComponent(hitReact)
        thing.life = 1
        thing.team = Team.ENEMY
        thing.destroyOnDeactivation = true
        if (flipHorizontal) {
            thing.facingDirection.x = -1.0f
        }
        thing.add(lifetime)
        thing.add(render)
        thing.add(sprite)
        thing.add(dynamicCollision)
        thing.add(hitReact)
        addStaticData(GameObjectType.ENERGY_BALL, thing, sprite)
        sprite.playAnimation(0)
        return thing
    }

    fun spawnWandaShot(positionX: Float, positionY: Float, flipHorizontal: Boolean): GameObject? {
        val textureLibrary = sSystemRegistry.shortTermTextureLibrary
        val thing = gameObjectPool.allocate()
        thing!!.position[positionX] = positionY
        thing.activationRadius = tightActivationRadius
        thing.width = 32f
        thing.height = 32f
        var staticData = getStaticData(GameObjectType.WANDA_SHOT)
        if (staticData == null) {
            val staticObjectCount = 2
            staticData = FixedSizeArray(staticObjectCount)
            val movement = allocateComponent(MovementComponent::class.java)
            val basicAttackVolume = FixedSizeArray<CollisionVolume>(1)
            basicAttackVolume.add(SphereCollisionVolume(16, 16, 16, HitType.HIT))
            val idle = SpriteAnimation(0, 4)
            idle.addFrame(AnimationFrame(
                    textureLibrary!!.allocateTexture(R.drawable.energy_ball01),
                    Utils.framesToTime(24, 1), basicAttackVolume, null))
            idle.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.energy_ball02),
                    Utils.framesToTime(24, 1), basicAttackVolume, null))
            idle.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.energy_ball03),
                    Utils.framesToTime(24, 1), basicAttackVolume, null))
            idle.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.energy_ball04),
                    Utils.framesToTime(24, 1), basicAttackVolume, null))
            idle.loop = true
            staticData.add(movement)
            staticData.add(idle)
            setStaticData(GameObjectType.WANDA_SHOT, staticData)
        }
        val render = allocateComponent(RenderComponent::class.java) as RenderComponent?
        render!!.priority = SortConstants.PROJECTILE
        val lifetime = allocateComponent(LifetimeComponent::class.java) as LifetimeComponent?
        lifetime!!.setTimeUntilDeath(5.0f)
        val sprite = allocateComponent(SpriteComponent::class.java) as SpriteComponent?
        sprite!!.setSize(thing.width.toInt(), thing.height.toInt())
        sprite.setRenderComponent(render)
        val dynamicCollision = allocateComponent(DynamicCollisionComponent::class.java) as DynamicCollisionComponent?
        sprite.setCollisionComponent(dynamicCollision)
        val hitReact = allocateComponent(HitReactionComponent::class.java) as HitReactionComponent?
        //hitReact.setDieOnAttack(true);
        dynamicCollision!!.setHitReactionComponent(hitReact)
        thing.life = 1
        thing.team = Team.NONE
        thing.destroyOnDeactivation = true
        if (flipHorizontal) {
            thing.facingDirection.x = -1.0f
        }
        thing.add(lifetime)
        thing.add(render)
        thing.add(sprite)
        thing.add(dynamicCollision)
        thing.add(hitReact!!)
        addStaticData(GameObjectType.WANDA_SHOT, thing, sprite)
        sprite.playAnimation(0)
        return thing
    }

    fun spawnCannonBall(positionX: Float, positionY: Float, flipHorizontal: Boolean): GameObject? {
        val textureLibrary = sSystemRegistry.shortTermTextureLibrary
        val thing = gameObjectPool.allocate()
        thing!!.position[positionX] = positionY
        thing.activationRadius = tightActivationRadius
        thing.width = 32f
        thing.height = 32f
        var staticData = getStaticData(GameObjectType.CANNON_BALL)
        if (staticData == null) {
            val staticObjectCount = 2
            staticData = FixedSizeArray(staticObjectCount)
            val movement = allocateComponent(MovementComponent::class.java)
            val basicAttackVolume = FixedSizeArray<CollisionVolume>(1)
            basicAttackVolume.add(SphereCollisionVolume(8, 16, 16, HitType.HIT))
            val idle = SpriteAnimation(0, 1)
            idle.addFrame(AnimationFrame(textureLibrary!!.allocateTexture(R.drawable.snail_bomb),
                    Utils.framesToTime(24, 1), basicAttackVolume, null))
            staticData.add(movement)
            staticData.add(idle)
            setStaticData(GameObjectType.CANNON_BALL, staticData)
        }
        val render = allocateComponent(RenderComponent::class.java) as RenderComponent?
        render!!.priority = SortConstants.PROJECTILE
        val lifetime = allocateComponent(LifetimeComponent::class.java) as LifetimeComponent?
        lifetime!!.setTimeUntilDeath(3.0f)
        lifetime.setDieOnHitBackground(true)
        val sprite = allocateComponent(SpriteComponent::class.java) as SpriteComponent?
        sprite!!.setSize(thing.width.toInt(), thing.height.toInt())
        sprite.setRenderComponent(render)
        val dynamicCollision = allocateComponent(DynamicCollisionComponent::class.java) as DynamicCollisionComponent?
        sprite.setCollisionComponent(dynamicCollision)
        val hitReact = allocateComponent(HitReactionComponent::class.java) as HitReactionComponent?
        hitReact!!.setDieOnAttack(true)
        dynamicCollision!!.setHitReactionComponent(hitReact)
        val collision = allocateComponent(SimpleCollisionComponent::class.java) as SimpleCollisionComponent?
        thing.life = 1
        thing.team = Team.ENEMY
        thing.destroyOnDeactivation = true
        if (flipHorizontal) {
            thing.facingDirection.x = -1.0f
        }
        thing.add(lifetime)
        thing.add(render)
        thing.add(sprite)
        thing.add(dynamicCollision)
        thing.add(hitReact)
        thing.add(collision!!)
        addStaticData(GameObjectType.CANNON_BALL, thing, sprite)
        sprite.playAnimation(0)
        return thing
    }

    fun spawnTurretBullet(positionX: Float, positionY: Float, flipHorizontal: Boolean): GameObject? {
        val textureLibrary = sSystemRegistry.shortTermTextureLibrary
        val thing = gameObjectPool.allocate()
        thing!!.position[positionX] = positionY
        thing.activationRadius = tightActivationRadius
        thing.width = 16f
        thing.height = 16f
        var staticData = getStaticData(GameObjectType.TURRET_BULLET)
        if (staticData == null) {
            val staticObjectCount = 2
            staticData = FixedSizeArray(staticObjectCount)
            val movement = allocateComponent(MovementComponent::class.java)
            val basicAttackVolume = FixedSizeArray<CollisionVolume>(1)
            basicAttackVolume.add(SphereCollisionVolume(8, 8, 8, HitType.HIT))
            val idle = SpriteAnimation(0, 2)
            idle.addFrame(AnimationFrame(textureLibrary!!.allocateTexture(R.drawable.effect_bullet01),
                    Utils.framesToTime(24, 1), basicAttackVolume, null))
            idle.addFrame(AnimationFrame(textureLibrary.allocateTexture(R.drawable.effect_bullet02),
                    Utils.framesToTime(24, 1), basicAttackVolume, null))
            idle.loop = true
            staticData.add(movement)
            staticData.add(idle)
            setStaticData(GameObjectType.TURRET_BULLET, staticData)
        }
        val render = allocateComponent(RenderComponent::class.java) as RenderComponent?
        render!!.priority = SortConstants.PROJECTILE
        val lifetime = allocateComponent(LifetimeComponent::class.java) as LifetimeComponent?
        lifetime!!.setTimeUntilDeath(3.0f)
        val sprite = allocateComponent(SpriteComponent::class.java) as SpriteComponent?
        sprite!!.setSize(thing.width.toInt(), thing.height.toInt())
        sprite.setRenderComponent(render)
        val dynamicCollision = allocateComponent(DynamicCollisionComponent::class.java) as DynamicCollisionComponent?
        sprite.setCollisionComponent(dynamicCollision)
        val hitReact = allocateComponent(HitReactionComponent::class.java) as HitReactionComponent?
        hitReact!!.setDieOnAttack(true)
        dynamicCollision!!.setHitReactionComponent(hitReact)
        thing.life = 1
        thing.team = Team.ENEMY
        thing.destroyOnDeactivation = true
        if (flipHorizontal) {
            thing.facingDirection.x = -1.0f
        }
        thing.add(lifetime)
        thing.add(render)
        thing.add(sprite)
        thing.add(dynamicCollision)
        thing.add(hitReact)
        addStaticData(GameObjectType.TURRET_BULLET, thing, sprite)
        sprite.playAnimation(0)
        return thing
    }

    fun spawnBrobotBullet(positionX: Float, positionY: Float, flipHorizontal: Boolean): GameObject? {
        val textureLibrary = sSystemRegistry.shortTermTextureLibrary
        val thing = gameObjectPool.allocate()
        thing!!.position[positionX] = positionY
        thing.activationRadius = tightActivationRadius
        thing.width = 64f
        thing.height = 64f
        var staticData = getStaticData(GameObjectType.BROBOT_BULLET)
        if (staticData == null) {
            val staticObjectCount = 2
            staticData = FixedSizeArray(staticObjectCount)
            val movement = allocateComponent(MovementComponent::class.java)
            val idle = SpriteAnimation(0, 3)
            idle.addFrame(AnimationFrame(
                    textureLibrary!!.allocateTexture(R.drawable.enemy_brobot_walk01),
                    Utils.framesToTime(24, 1), null, null))
            idle.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_brobot_walk02),
                    Utils.framesToTime(24, 1), null, null))
            idle.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_brobot_walk03),
                    Utils.framesToTime(24, 1), null, null))
            idle.loop = true
            staticData.add(movement)
            staticData.add(idle)
            setStaticData(GameObjectType.BROBOT_BULLET, staticData)
        }
        val render = allocateComponent(RenderComponent::class.java) as RenderComponent?
        render!!.priority = SortConstants.PROJECTILE
        val lifetime = allocateComponent(LifetimeComponent::class.java) as LifetimeComponent?
        lifetime!!.setTimeUntilDeath(3.0f)
        val sprite = allocateComponent(SpriteComponent::class.java) as SpriteComponent?
        sprite!!.setSize(thing.width.toInt(), thing.height.toInt())
        sprite.setRenderComponent(render)
        thing.life = 1
        thing.team = Team.ENEMY
        thing.destroyOnDeactivation = true
        if (flipHorizontal) {
            thing.facingDirection.x = -1.0f
        }
        thing.add(lifetime)
        thing.add(render)
        thing.add(sprite)
        addStaticData(GameObjectType.BROBOT_BULLET, thing, sprite)
        sprite.playAnimation(0)
        return thing
    }

    fun spawnCoin(positionX: Float, positionY: Float): GameObject? {
        val textureLibrary = sSystemRegistry.shortTermTextureLibrary
        val thing = gameObjectPool.allocate()
        thing!!.position[positionX] = positionY
        thing.activationRadius = tightActivationRadius
        thing.width = 16f
        thing.height = 16f
        var staticData = getStaticData(GameObjectType.COIN)
        if (staticData == null) {
            val staticObjectCount = 2
            staticData = FixedSizeArray(staticObjectCount)
            val basicVulnerabilityVolume: FixedSizeArray<CollisionVolume>? = null /*new FixedSizeArray<CollisionVolume>(1);
            basicVulnerabilityVolume.add(new SphereCollisionVolume(8, 8, 8));
            basicVulnerabilityVolume.get(0).setHitType(HitType.COLLECT);*/
            val idle = SpriteAnimation(0, 5)
            idle.addFrame(AnimationFrame(textureLibrary!!.allocateTexture(R.drawable.object_coin01),
                    Utils.framesToTime(24, 30), null, basicVulnerabilityVolume))
            idle.addFrame(AnimationFrame(textureLibrary.allocateTexture(R.drawable.object_coin02),
                    Utils.framesToTime(24, 2), null, basicVulnerabilityVolume))
            idle.addFrame(AnimationFrame(textureLibrary.allocateTexture(R.drawable.object_coin03),
                    Utils.framesToTime(24, 2), null, basicVulnerabilityVolume))
            idle.addFrame(AnimationFrame(textureLibrary.allocateTexture(R.drawable.object_coin04),
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume))
            idle.addFrame(AnimationFrame(textureLibrary.allocateTexture(R.drawable.object_coin05),
                    Utils.framesToTime(24, 2), null, basicVulnerabilityVolume))
            idle.loop = true
            val addCoin = UpdateRecord()
            addCoin.coinCount = 1
            staticData.add(addCoin)
            staticData.add(idle)
            setStaticData(GameObjectType.COIN, staticData)
        }
        val render = allocateComponent(RenderComponent::class.java) as RenderComponent?
        render!!.priority = SortConstants.GENERAL_OBJECT
        val sprite = allocateComponent(SpriteComponent::class.java) as SpriteComponent?
        sprite!!.setSize(thing.width.toInt(), thing.height.toInt())
        sprite.setRenderComponent(render)

        //DynamicCollisionComponent dynamicCollision = (DynamicCollisionComponent)allocateComponent(DynamicCollisionComponent.class);
        //sprite.setCollisionComponent(dynamicCollision);
        val hitReact = allocateComponent(HitReactionComponent::class.java) as HitReactionComponent?
        hitReact!!.setDieWhenCollected(true)
        hitReact.setInvincible(true)
        val hitPlayer = allocateComponent(HitPlayerComponent::class.java) as HitPlayerComponent?
        hitPlayer!!.setup(32f, hitReact, HitType.COLLECT, false)
        val sound = sSystemRegistry.soundSystem
        if (sound != null) {
            hitReact.setTakeHitSound(HitType.COLLECT, sound.load(R.raw.ding))
        }

        // TODO: this is pretty dumb.  The static data binding needs to be made generic.
        val staticDataSize = staticData.count
        for (x in 0 until staticDataSize) {
            val entry = staticData[x]
            if (entry is UpdateRecord) {
                hitReact.setInventoryUpdate(entry as UpdateRecord?)
                break
            }
        }

        //dynamicCollision.setHitReactionComponent(hitReact);
        val life = allocateComponent(LifetimeComponent::class.java) as LifetimeComponent?
        life!!.setIncrementEventCounter(EventRecorder.COUNTER_PEARLS_COLLECTED)
        thing.life = 1
        thing.add(render)
        thing.add(sprite)
        //object.add(dynamicCollision);
        thing.add(hitPlayer)
        thing.add(hitReact)
        thing.add(life)
        addStaticData(GameObjectType.COIN, thing, sprite)
        sprite.playAnimation(0)
        val recorder = sSystemRegistry.eventRecorder
        recorder!!.incrementEventCounter(EventRecorder.COUNTER_PEARLS_TOTAL)
        return thing
    }

    fun spawnRuby(positionX: Float, positionY: Float): GameObject? {
        val textureLibrary = sSystemRegistry.shortTermTextureLibrary
        val thing = gameObjectPool.allocate()
        thing!!.position[positionX] = positionY
        thing.activationRadius = tightActivationRadius
        thing.width = 32f
        thing.height = 32f
        var staticData = getStaticData(GameObjectType.RUBY)
        if (staticData == null) {
            val staticObjectCount = 2
            staticData = FixedSizeArray(staticObjectCount)
            val basicVulnerabilityVolume = FixedSizeArray<CollisionVolume>(1)
            basicVulnerabilityVolume.add(SphereCollisionVolume(16, 16, 16))
            basicVulnerabilityVolume[0]!!.hitType = HitType.COLLECT
            val idle = SpriteAnimation(0, 5)
            idle.addFrame(AnimationFrame(
                    textureLibrary!!.allocateTexture(R.drawable.object_ruby01),
                    2.0f, null, basicVulnerabilityVolume))
            idle.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.object_ruby02),
                    Utils.framesToTime(24, 2), null, basicVulnerabilityVolume))
            idle.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.object_ruby03),
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume))
            idle.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.object_ruby04),
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume))
            idle.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.object_ruby05),
                    Utils.framesToTime(24, 2), null, basicVulnerabilityVolume))
            idle.loop = true
            val addRuby = UpdateRecord()
            addRuby.rubyCount = 1
            staticData.add(addRuby)
            staticData.add(idle)
            setStaticData(GameObjectType.RUBY, staticData)
        }
        val render = allocateComponent(RenderComponent::class.java) as RenderComponent?
        render!!.priority = SortConstants.GENERAL_OBJECT
        val sprite = allocateComponent(SpriteComponent::class.java) as SpriteComponent?
        sprite!!.setSize(thing.width.toInt(), thing.height.toInt())
        sprite.setRenderComponent(render)
        val dynamicCollision = allocateComponent(DynamicCollisionComponent::class.java) as DynamicCollisionComponent?
        sprite.setCollisionComponent(dynamicCollision)
        val hitReact = allocateComponent(HitReactionComponent::class.java) as HitReactionComponent?
        hitReact!!.setDieWhenCollected(true)
        hitReact.setInvincible(true)
        // TODO: this is pretty dumb.  The static data binding needs to be made generic.
        val staticDataSize = staticData.count
        for (x in 0 until staticDataSize) {
            val entry = staticData[x]
            if (entry is UpdateRecord) {
                hitReact.setInventoryUpdate(entry as UpdateRecord?)
                break
            }
        }
        dynamicCollision!!.setHitReactionComponent(hitReact)
        val life = allocateComponent(LifetimeComponent::class.java) as LifetimeComponent?
        life!!.setObjectToSpawnOnDeath(GameObjectType.GEM_EFFECT_SPAWNER)
        thing.life = 1
        thing.add(render)
        thing.add(sprite)
        thing.add(dynamicCollision)
        thing.add(hitReact)
        thing.add(life)
        addStaticData(GameObjectType.RUBY, thing, sprite)
        sprite.playAnimation(0)
        return thing
    }

    fun spawnDiary(positionX: Float, positionY: Float): GameObject? {
        val textureLibrary = sSystemRegistry.shortTermTextureLibrary
        val level = sSystemRegistry.levelSystem
        if (level != null) {
            val currentLevel = level.currentLevel
            if (currentLevel != null && currentLevel.diaryCollected) {
                return null
            }
        }
        val thing = gameObjectPool.allocate()
        thing!!.position[positionX] = positionY
        thing.activationRadius = tightActivationRadius
        thing.width = 32f
        thing.height = 32f
        var staticData = getStaticData(GameObjectType.DIARY)
        if (staticData == null) {
            val staticObjectCount = 2
            staticData = FixedSizeArray(staticObjectCount)
            val basicVulnerabilityVolume = FixedSizeArray<CollisionVolume>(1)
            basicVulnerabilityVolume.add(SphereCollisionVolume(16, 16, 16))
            basicVulnerabilityVolume[0]!!.hitType = HitType.COLLECT
            val idle = SpriteAnimation(0, 8)
            val frame1 = AnimationFrame(textureLibrary!!.allocateTexture(R.drawable.object_diary01),
                    Utils.framesToTime(24, 2), null, basicVulnerabilityVolume)
            val frame2 = AnimationFrame(textureLibrary.allocateTexture(R.drawable.object_diary02),
                    Utils.framesToTime(24, 2), null, basicVulnerabilityVolume)
            idle.addFrame(AnimationFrame(textureLibrary.allocateTexture(R.drawable.object_diary01),
                    1.0f, null, basicVulnerabilityVolume))
            idle.addFrame(frame2)
            idle.addFrame(frame1)
            idle.addFrame(frame2)
            idle.addFrame(AnimationFrame(textureLibrary.allocateTexture(R.drawable.object_diary03),
                    Utils.framesToTime(24, 2), null, basicVulnerabilityVolume))
            idle.addFrame(AnimationFrame(textureLibrary.allocateTexture(R.drawable.object_diary04),
                    Utils.framesToTime(24, 2), null, basicVulnerabilityVolume))
            idle.addFrame(AnimationFrame(textureLibrary.allocateTexture(R.drawable.object_diary05),
                    Utils.framesToTime(24, 2), null, basicVulnerabilityVolume))
            idle.addFrame(AnimationFrame(textureLibrary.allocateTexture(R.drawable.object_diary06),
                    Utils.framesToTime(24, 2), null, basicVulnerabilityVolume))
            idle.loop = true
            val addDiary = UpdateRecord()
            addDiary.diaryCount = 1
            staticData.add(addDiary)
            staticData.add(idle)
            setStaticData(GameObjectType.DIARY, staticData)
        }
        val render = allocateComponent(RenderComponent::class.java) as RenderComponent?
        render!!.priority = SortConstants.GENERAL_OBJECT
        val sprite = allocateComponent(SpriteComponent::class.java) as SpriteComponent?
        sprite!!.setSize(thing.width.toInt(), thing.height.toInt())
        sprite.setRenderComponent(render)
        val dynamicCollision = allocateComponent(DynamicCollisionComponent::class.java) as DynamicCollisionComponent?
        sprite.setCollisionComponent(dynamicCollision)
        val hitReact = allocateComponent(HitReactionComponent::class.java) as HitReactionComponent?
        hitReact!!.setDieWhenCollected(true)
        hitReact.setInvincible(true)
        hitReact.setSpawnGameEventOnHit(HitType.COLLECT,
                GameFlowEvent.EVENT_SHOW_DIARY, 0)
        // TODO: this is pretty dumb.  The static data binding needs to be made generic.
        val staticDataSize = staticData.count
        for (x in 0 until staticDataSize) {
            val entry = staticData[x]
            if (entry is UpdateRecord) {
                hitReact.setInventoryUpdate(entry as UpdateRecord?)
                break
            }
        }
        dynamicCollision!!.setHitReactionComponent(hitReact)
        val life = allocateComponent(LifetimeComponent::class.java) as LifetimeComponent?
        thing.life = 1
        thing.add(render)
        thing.add(sprite)
        thing.add(dynamicCollision)
        thing.add(hitReact)
        thing.add(life!!)
        addStaticData(GameObjectType.DIARY, thing, sprite)
        sprite.playAnimation(0)
        return thing
    }

    fun spawnObjectDoor(positionX: Float, positionY: Float, type: GameObjectType, solid: Boolean): GameObject? {
        val textureLibrary = sSystemRegistry.shortTermTextureLibrary
        val thing = gameObjectPool.allocate()
        thing!!.position[positionX] = positionY
        thing.activationRadius = tightActivationRadius
        thing.width = 32f
        thing.height = 64f
        var staticData = getStaticData(type)
        if (staticData == null) {
            val staticObjectCount = 5
            staticData = FixedSizeArray(staticObjectCount)
            val red_frames = intArrayOf(
                    R.drawable.object_door_red01,
                    R.drawable.object_door_red02,
                    R.drawable.object_door_red03,
                    R.drawable.object_door_red04)
            val blue_frames = intArrayOf(
                    R.drawable.object_door_blue01,
                    R.drawable.object_door_blue02,
                    R.drawable.object_door_blue03,
                    R.drawable.object_door_blue04)
            val green_frames = intArrayOf(
                    R.drawable.object_door_green01,
                    R.drawable.object_door_green02,
                    R.drawable.object_door_green03,
                    R.drawable.object_door_green04)
            var frames = red_frames
            if (type == GameObjectType.DOOR_GREEN) {
                frames = green_frames
            } else if (type == GameObjectType.DOOR_BLUE) {
                frames = blue_frames
            }
            val vulnerabilityVolume: FixedSizeArray<CollisionVolume>? = null
            val frame1 = AnimationFrame(textureLibrary!!.allocateTexture(frames[0]),
                    Utils.framesToTime(24, 1), null, vulnerabilityVolume)
            val frame2 = AnimationFrame(textureLibrary.allocateTexture(frames[1]),
                    Utils.framesToTime(24, 2))
            val frame3 = AnimationFrame(textureLibrary.allocateTexture(frames[2]),
                    Utils.framesToTime(24, 2))
            val frame4 = AnimationFrame(textureLibrary.allocateTexture(frames[3]),
                    Utils.framesToTime(24, 1))

            // one frame of closing is deadly
            val attackVolume = FixedSizeArray<CollisionVolume>(1)
            attackVolume.add(AABoxCollisionVolume(12, 8, 8, 56))
            attackVolume[0]!!.hitType = HitType.DEATH
            val closeFrame2 = AnimationFrame(textureLibrary.allocateTexture(frames[1]),
                    Utils.framesToTime(24, 2), attackVolume, vulnerabilityVolume)
            val idle_closed = SpriteAnimation(DoorAnimationComponent.Animation.CLOSED, 1)
            idle_closed.addFrame(frame1)
            val idle_open = SpriteAnimation(DoorAnimationComponent.Animation.OPEN, 1)
            idle_open.addFrame(frame4)
            val open = SpriteAnimation(DoorAnimationComponent.Animation.OPENING, 2)
            open.addFrame(frame2)
            open.addFrame(frame3)
            val close = SpriteAnimation(DoorAnimationComponent.Animation.CLOSING, 2)
            close.addFrame(frame3)
            close.addFrame(closeFrame2)
            val solidSurface = allocateComponent(SolidSurfaceComponent::class.java) as SolidSurfaceComponent?
            solidSurface!!.inititalize(4)
            // box shape:
            // ___       ___1
            // | |      2| |3
            // ---       ---4
            val surface1Start = Vector2(0f, thing.height)
            val surface1End = Vector2(thing.width, thing.height)
            val surface1Normal = Vector2(0.0f, -1.0f)
            surface1Normal.normalize()
            val surface2Start = Vector2(0f, thing.height)
            val surface2End = Vector2(0, 0)
            val surface2Normal = Vector2(-1.0f, 0.0f)
            surface2Normal.normalize()
            val surface3Start = Vector2(thing.width, thing.height)
            val surface3End = Vector2(thing.width, 0f)
            val surface3Normal = Vector2(1.0f, 0f)
            val surface4Start = Vector2(0f, 0f)
            val surface4End = Vector2(thing.width, 0f)
            val surface4Normal = Vector2(0f, 1.0f)
            solidSurface.addSurface(surface1Start, surface1End, surface1Normal)
            solidSurface.addSurface(surface2Start, surface2End, surface2Normal)
            solidSurface.addSurface(surface3Start, surface3End, surface3Normal)
            solidSurface.addSurface(surface4Start, surface4End, surface4Normal)
            staticData.add(idle_open)
            staticData.add(idle_closed)
            staticData.add(open)
            staticData.add(close)
            staticData.add(solidSurface)
            setStaticData(type, staticData)
        }
        val render = allocateComponent(RenderComponent::class.java) as RenderComponent?
        render!!.priority = SortConstants.FOREGROUND_OBJECT
        val sprite = allocateComponent(SpriteComponent::class.java) as SpriteComponent?
        sprite!!.setSize(thing.width.toInt(), thing.height.toInt())
        sprite.setRenderComponent(render)
        val doorAnim = allocateComponent(DoorAnimationComponent::class.java) as DoorAnimationComponent?
        doorAnim!!.setSprite(sprite)
        val sound = sSystemRegistry.soundSystem
        if (sound != null) {
            doorAnim.setSounds(sound.load(R.raw.sound_open), sound.load(R.raw.sound_close))
        }
        var doorChannel: ChannelSystem.Channel? = null
        val channelSystem = sSystemRegistry.channelSystem
        when (type) {
            GameObjectType.DOOR_RED -> doorChannel = channelSystem!!.registerChannel(sRedButtonChannel)
            GameObjectType.DOOR_BLUE -> doorChannel = channelSystem!!.registerChannel(sBlueButtonChannel)
            GameObjectType.DOOR_GREEN -> doorChannel = channelSystem!!.registerChannel(sGreenButtonChannel)
            else -> {}
        }
        doorAnim.setChannel(doorChannel)
        val dynamicCollision = allocateComponent(DynamicCollisionComponent::class.java) as DynamicCollisionComponent?
        sprite.setCollisionComponent(dynamicCollision)
        val hitReact = allocateComponent(HitReactionComponent::class.java) as HitReactionComponent?
        dynamicCollision!!.setHitReactionComponent(hitReact)
        thing.add(render)
        thing.add(sprite)
        thing.add(doorAnim)
        thing.add(dynamicCollision)
        thing.add(hitReact!!)
        addStaticData(type, thing, sprite)
        thing.commitUpdates()
        val solidSurface = thing.findByClass(SolidSurfaceComponent::class.java)
        if (solid) {
            doorAnim.setSolidSurface(solidSurface)
        } else {
            thing.remove(solidSurface)
            thing.commitUpdates()
        }
        sprite.playAnimation(0)
        return thing
    }

    fun spawnObjectButton(positionX: Float, positionY: Float, type: GameObjectType): GameObject? {
        val textureLibrary = sSystemRegistry.shortTermTextureLibrary
        val thing = gameObjectPool.allocate()
        thing!!.position[positionX] = positionY
        thing.activationRadius = tightActivationRadius
        thing.width = 32f
        thing.height = 32f
        var staticData = getStaticData(type)
        if (staticData == null) {
            val staticObjectCount = 2
            staticData = FixedSizeArray(staticObjectCount)
            val red_frames = intArrayOf(
                    R.drawable.object_button_red,
                    R.drawable.object_button_pressed_red)
            val blue_frames = intArrayOf(
                    R.drawable.object_button_blue,
                    R.drawable.object_button_pressed_blue)
            val green_frames = intArrayOf(
                    R.drawable.object_button_green,
                    R.drawable.object_button_pressed_green)
            var frames = red_frames
            if (type == GameObjectType.BUTTON_GREEN) {
                frames = green_frames
            } else if (type == GameObjectType.BUTTON_BLUE) {
                frames = blue_frames
            }
            val vulnerabilityVolume = FixedSizeArray<CollisionVolume>(1)
            vulnerabilityVolume.add(AABoxCollisionVolume(0, 0, 32, 16))
            vulnerabilityVolume[0]!!.hitType = HitType.DEPRESS
            val frame1 = AnimationFrame(textureLibrary!!.allocateTexture(frames[0]),
                    Utils.framesToTime(24, 1), null, vulnerabilityVolume)
            val frame2 = AnimationFrame(textureLibrary.allocateTexture(frames[1]),
                    Utils.framesToTime(24, 1), null, vulnerabilityVolume)
            val idle = SpriteAnimation(ButtonAnimationComponent.Animation.UP, 1)
            idle.addFrame(frame1)
            val pressed = SpriteAnimation(ButtonAnimationComponent.Animation.DOWN, 1)
            pressed.addFrame(frame2)
            staticData.add(idle)
            staticData.add(pressed)
            setStaticData(type, staticData)
        }
        val render = allocateComponent(RenderComponent::class.java) as RenderComponent?
        render!!.priority = SortConstants.GENERAL_OBJECT
        val sprite = allocateComponent(SpriteComponent::class.java) as SpriteComponent?
        sprite!!.setSize(thing.width.toInt(), thing.height.toInt())
        sprite.setRenderComponent(render)
        val button = allocateComponent(ButtonAnimationComponent::class.java) as ButtonAnimationComponent?
        button!!.setSprite(sprite)
        val sound = sSystemRegistry.soundSystem
        if (sound != null) {
            button.setDepressSound(sound.load(R.raw.sound_button))
        }
        var buttonChannel: ChannelSystem.Channel? = null
        val channelSystem = sSystemRegistry.channelSystem
        when (type) {
            GameObjectType.BUTTON_RED -> buttonChannel = channelSystem!!.registerChannel(sRedButtonChannel)
            GameObjectType.BUTTON_BLUE -> buttonChannel = channelSystem!!.registerChannel(sBlueButtonChannel)
            GameObjectType.BUTTON_GREEN -> buttonChannel = channelSystem!!.registerChannel(sGreenButtonChannel)
            else -> {}
        }
        button.setChannel(buttonChannel)
        val dynamicCollision = allocateComponent(DynamicCollisionComponent::class.java) as DynamicCollisionComponent?
        sprite.setCollisionComponent(dynamicCollision)
        val hitReact = allocateComponent(HitReactionComponent::class.java) as HitReactionComponent?
        hitReact!!.setInvincible(false)
        dynamicCollision!!.setHitReactionComponent(hitReact)
        thing.team = Team.NONE
        thing.add(render)
        thing.add(sprite)
        thing.add(button)
        thing.add(dynamicCollision)
        thing.add(hitReact)
        addStaticData(type, thing, sprite)
        sprite.playAnimation(0)
        return thing
    }

    fun spawnObjectCannon(positionX: Float, positionY: Float): GameObject? {
        val textureLibrary = sSystemRegistry.shortTermTextureLibrary
        val thing = gameObjectPool.allocate()
        thing!!.position[positionX] = positionY
        thing.activationRadius = tightActivationRadius
        thing.width = 64f
        thing.height = 128f
        var staticData = getStaticData(GameObjectType.CANNON)
        if (staticData == null) {
            val staticObjectCount = 2
            staticData = FixedSizeArray(staticObjectCount)
            val attackVolume = FixedSizeArray<CollisionVolume>(1)
            attackVolume.add(AABoxCollisionVolume(16, 16, 32, 80))
            attackVolume[0]!!.hitType = HitType.LAUNCH
            val frame1 = AnimationFrame(
                    textureLibrary!!.allocateTexture(R.drawable.object_cannon),
                    1.0f, attackVolume, null)
            val idle = SpriteAnimation(GenericAnimationComponent.Animation.IDLE, 1)
            idle.addFrame(frame1)
            val frame1NoAttack = AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.object_cannon),
                    1.0f, null, null)
            val shoot = SpriteAnimation(GenericAnimationComponent.Animation.ATTACK, 1)
            shoot.addFrame(frame1NoAttack)
            staticData.add(idle)
            staticData.add(shoot)
            setStaticData(GameObjectType.CANNON, staticData)
        }
        val render = allocateComponent(RenderComponent::class.java) as RenderComponent?
        render!!.priority = SortConstants.FOREGROUND_OBJECT
        val sprite = allocateComponent(SpriteComponent::class.java) as SpriteComponent?
        sprite!!.setSize(thing.width.toInt(), thing.height.toInt())
        sprite.setRenderComponent(render)
        val launcher = allocateComponent(LauncherComponent::class.java) as LauncherComponent?
        launcher!!.setLaunchEffect(GameObjectType.SMOKE_POOF, 32.0f, 85.0f)
        val sound = sSystemRegistry.soundSystem
        if (sound != null) {
            launcher.setLaunchSound(sound.load(R.raw.sound_cannon))
        }
        val dynamicCollision = allocateComponent(DynamicCollisionComponent::class.java) as DynamicCollisionComponent?
        sprite.setCollisionComponent(dynamicCollision)
        val hitReact = allocateComponent(HitReactionComponent::class.java) as HitReactionComponent?
        hitReact!!.setInvincible(false)
        hitReact.setLauncherComponent(launcher, HitType.LAUNCH)
        dynamicCollision!!.setHitReactionComponent(hitReact)
        val anim = allocateComponent(GenericAnimationComponent::class.java) as GenericAnimationComponent?
        anim!!.setSprite(sprite)
        thing.team = Team.NONE
        thing.add(render)
        thing.add(sprite)
        thing.add(dynamicCollision)
        thing.add(hitReact)
        thing.add(launcher)
        thing.add(anim)
        addStaticData(GameObjectType.CANNON, thing, sprite)
        sprite.playAnimation(0)
        return thing
    }

    fun spawnObjectBrobotSpawner(positionX: Float, positionY: Float, flipHorizontal: Boolean): GameObject? {
        val textureLibrary = sSystemRegistry.shortTermTextureLibrary

        // This is pretty heavy-handed.
        // TODO: figure out a general solution for objects that depend on other objects.
        textureLibrary!!.allocateTexture(R.drawable.enemy_brobot_idle01)
        textureLibrary.allocateTexture(R.drawable.enemy_brobot_idle02)
        textureLibrary.allocateTexture(R.drawable.enemy_brobot_idle03)
        textureLibrary.allocateTexture(R.drawable.enemy_brobot_walk01)
        textureLibrary.allocateTexture(R.drawable.enemy_brobot_walk02)
        textureLibrary.allocateTexture(R.drawable.enemy_brobot_walk03)
        val thing = gameObjectPool.allocate()
        thing!!.position[positionX] = positionY
        thing.activationRadius = tightActivationRadius
        thing.width = 64f
        thing.height = 64f
        var staticData = getStaticData(GameObjectType.BROBOT_SPAWNER)
        if (staticData == null) {
            val staticObjectCount = 2
            staticData = FixedSizeArray(staticObjectCount)
            val basicVulnerabilityVolume = FixedSizeArray<CollisionVolume>(1)
            basicVulnerabilityVolume.add(SphereCollisionVolume(32, 32, 32))
            basicVulnerabilityVolume[0]!!.hitType = HitType.POSSESS
            val idle = SpriteAnimation(0, 1)
            idle.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.object_brobot_machine),
                    1.0f, null, basicVulnerabilityVolume))
            val solidSurface = allocateComponent(SolidSurfaceComponent::class.java) as SolidSurfaceComponent?
            solidSurface!!.inititalize(3)
            /*
                0:2,0:8,59:-0.99532399996093,0.09659262446878
                0:8,59:61,33:0.44551558813576,0.89527418187282
                0:61,33:61,-1:1,0

             */
            // trapezoid shape:
            // |\        |\2
            // | |      1| |3
            val surface1Start = Vector2(0, 0)
            val surface1End = Vector2(8.0f, 59.0f)
            val surface1Normal = Vector2(-0.9953f, 0.0965f)
            surface1Normal.normalize()
            val surface2Start = Vector2(8.0f, 59.0f)
            val surface2End = Vector2(61.0f, 33.0f)
            val surface2Normal = Vector2(0.445515f, 0.89527f)
            surface2Normal.normalize()
            val surface3Start = Vector2(61.0f, 33.0f)
            val surface3End = Vector2(61.0f, 0.0f)
            val surface3Normal = Vector2(1.0f, 0.0f)
            solidSurface.addSurface(surface1Start, surface1End, surface1Normal)
            solidSurface.addSurface(surface2Start, surface2End, surface2Normal)
            solidSurface.addSurface(surface3Start, surface3End, surface3Normal)
            staticData.add(solidSurface)
            staticData.add(idle)
            setStaticData(GameObjectType.BROBOT_SPAWNER, staticData)
        }
        val render = allocateComponent(RenderComponent::class.java) as RenderComponent?
        render!!.priority = SortConstants.GENERAL_OBJECT
        val sprite = allocateComponent(SpriteComponent::class.java) as SpriteComponent?
        sprite!!.setSize(thing.width.toInt(), thing.height.toInt())
        sprite.setRenderComponent(render)
        val collision = allocateComponent(DynamicCollisionComponent::class.java) as DynamicCollisionComponent?
        sprite.setCollisionComponent(collision)
        val hitReact = allocateComponent(HitReactionComponent::class.java) as HitReactionComponent?
        collision!!.setHitReactionComponent(hitReact)
        val gun = allocateComponent(LaunchProjectileComponent::class.java) as LaunchProjectileComponent?
        gun!!.setDelayBeforeFirstSet(3.0f)
        gun.setObjectTypeToSpawn(GameObjectType.BROBOT)
        gun.setOffsetX(36f)
        gun.setOffsetY(50f)
        gun.setVelocityX(100.0f)
        gun.setVelocityY(300.0f)
        gun.enableProjectileTracking(1)
        thing.team = Team.ENEMY
        if (flipHorizontal) {
            thing.facingDirection.x = -1.0f
        } else {
            thing.facingDirection.x = 1.0f
        }
        thing.add(render)
        thing.add(sprite)
        thing.add(gun)
        thing.add(collision)
        thing.add(hitReact!!)
        addStaticData(GameObjectType.BROBOT_SPAWNER, thing, sprite)
        thing.commitUpdates()
        sprite.playAnimation(0)
        return thing
    }

    fun spawnObjectInfiniteSpawner(positionX: Float, positionY: Float): GameObject? {
        val thing = spawnObjectBrobotSpawner(positionX, positionY, false)
        thing!!.facingDirection.y = (-1).toFloat() //vertical flip
        val gun = thing.findByClass(LaunchProjectileComponent::class.java)
        if (gun != null) {
            gun.disableProjectileTracking()
            gun.setDelayBetweenShots(0.15f)
            gun.setSetsPerActivation(1)
            gun.setShotsPerSet(60)
        }
        return thing
    }

    fun spawnObjectCrusherAndou(positionX: Float, positionY: Float): GameObject? {
        val textureLibrary = sSystemRegistry.shortTermTextureLibrary
        val thing = gameObjectPool.allocate()
        thing!!.position[positionX] = positionY
        thing.activationRadius = alwaysActive
        thing.width = 64f
        thing.height = 64f
        var staticData = getStaticData(GameObjectType.CRUSHER_ANDOU)
        if (staticData == null) {
            val staticObjectCount = 5
            staticData = FixedSizeArray(staticObjectCount)
            val gravity = allocateComponent(GravityComponent::class.java)
            val movement = allocateComponent(MovementComponent::class.java)
            val physics = allocateComponent(PhysicsComponent::class.java) as PhysicsComponent?
            physics!!.mass = 9.1f // ~90kg w/ earth gravity
            physics.dynamicFrictionCoeffecient = 0.2f
            physics.staticFrictionCoeffecient = 0.01f

            // Animation Data
            val basicVulnerabilityVolume = FixedSizeArray<CollisionVolume>(1)
            basicVulnerabilityVolume.add(SphereCollisionVolume(16, 32, 32))
            val idle = SpriteAnimation(GenericAnimationComponent.Animation.IDLE, 1)
            idle.addFrame(AnimationFrame(textureLibrary!!.allocateTexture(R.drawable.andou_stand),
                    1.0f, null, basicVulnerabilityVolume))
            val stompAttackVolume = FixedSizeArray<CollisionVolume>(1)
            stompAttackVolume.add(AABoxCollisionVolume(16f, -5.0f, 32f, 37f, HitType.HIT))
            val stomp = SpriteAnimation(GenericAnimationComponent.Animation.ATTACK, 4)
            stomp.addFrame(
                    AnimationFrame(textureLibrary.allocateTexture(R.drawable.andou_stomp01),
                            Utils.framesToTime(24, 1), stompAttackVolume, null))
            stomp.addFrame(
                    AnimationFrame(textureLibrary.allocateTexture(R.drawable.andou_stomp02),
                            Utils.framesToTime(24, 1), stompAttackVolume, null))
            stomp.addFrame(
                    AnimationFrame(textureLibrary.allocateTexture(R.drawable.andou_stomp03),
                            Utils.framesToTime(24, 1), stompAttackVolume, null))
            stomp.addFrame(
                    AnimationFrame(textureLibrary.allocateTexture(R.drawable.andou_stomp04),
                            Utils.framesToTime(24, 1), stompAttackVolume, null))


            // Save static data
            staticData.add(gravity)
            staticData.add(movement)
            staticData.add(physics)
            staticData.add(idle)
            staticData.add(stomp)
            setStaticData(GameObjectType.CRUSHER_ANDOU, staticData)
        }
        val render = allocateComponent(RenderComponent::class.java) as RenderComponent?
        render!!.priority = SortConstants.PLAYER
        val bgcollision = allocateComponent(BackgroundCollisionComponent::class.java) as BackgroundCollisionComponent?
        bgcollision!!.setSize(32, 48)
        bgcollision.setOffset(16, 0)
        val animation = allocateComponent(GenericAnimationComponent::class.java) as GenericAnimationComponent?
        val sprite = allocateComponent(SpriteComponent::class.java) as SpriteComponent?
        sprite!!.setSize(thing.width.toInt(), thing.height.toInt())
        sprite.setRenderComponent(render)
        animation!!.setSprite(sprite)
        val dynamicCollision = allocateComponent(DynamicCollisionComponent::class.java) as DynamicCollisionComponent?
        sprite.setCollisionComponent(dynamicCollision)
        val hitReact = allocateComponent(HitReactionComponent::class.java) as HitReactionComponent?
        hitReact!!.setBounceOnHit(true)
        hitReact.setPauseOnAttack(true)
        hitReact.setInvincibleTime(3.0f)
        hitReact.setSpawnOnDealHit(HitType.HIT, GameObjectType.CRUSH_FLASH, false, true)
        dynamicCollision!!.setHitReactionComponent(hitReact)
        thing.life = 1
        thing.team = Team.PLAYER
        thing.add(animation)
        thing.add(bgcollision)
        thing.add(render)
        thing.add(sprite)
        thing.add(dynamicCollision)
        thing.add(hitReact)
        addStaticData(GameObjectType.CRUSHER_ANDOU, thing, sprite)
        sprite.playAnimation(GenericAnimationComponent.Animation.IDLE)
        thing.commitUpdates()
        val swap = allocateComponent(ChangeComponentsComponent::class.java) as ChangeComponentsComponent?
        val count = thing.fetchCount()
        for (x in 0 until count) {
            swap!!.addSwapInComponent(thing.fetch(x) as GameComponent?)
        }
        thing.removeAll()
        val crusher = allocateComponent(CrusherAndouComponent::class.java) as CrusherAndouComponent?
        crusher!!.setSwap(swap)
        thing.add(swap!!)
        thing.add(crusher)
        thing.commitUpdates()
        return thing
    }

    fun spawnObjectBreakableBlock(positionX: Float, positionY: Float): GameObject? {
        val textureLibrary = sSystemRegistry.shortTermTextureLibrary

        // Preload block piece texture.
        textureLibrary!!.allocateTexture(R.drawable.object_debris_piece)
        val thing = gameObjectPool.allocate()
        thing!!.position[positionX] = positionY
        thing.activationRadius = tightActivationRadius
        thing.width = 32f
        thing.height = 32f
        var staticData = getStaticData(GameObjectType.BREAKABLE_BLOCK)
        if (staticData == null) {
            val staticObjectCount = 2
            staticData = FixedSizeArray(staticObjectCount)
            val basicVulnerabilityVolume = FixedSizeArray<CollisionVolume>(1)
            basicVulnerabilityVolume.add(AABoxCollisionVolume(7, 0, 32 - 7, 42, HitType.HIT))
            val idle = SpriteAnimation(0, 1)
            idle.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.object_debris_block),
                    1.0f, null, basicVulnerabilityVolume))
            val solidSurface = allocateComponent(SolidSurfaceComponent::class.java) as SolidSurfaceComponent?
            solidSurface!!.inititalize(4)

            // box shape:
            // ___       ___2
            // | |      1| |3
            // ---       ---4
            val surface1Start = Vector2(0.0f, 0.0f)
            val surface1End = Vector2(0.0f, 32.0f)
            val surface1Normal = Vector2(-1.0f, 0.0f)
            surface1Normal.normalize()
            val surface2Start = Vector2(0.0f, 32.0f)
            val surface2End = Vector2(32.0f, 32.0f)
            val surface2Normal = Vector2(0.0f, 1.0f)
            surface2Normal.normalize()
            val surface3Start = Vector2(32.0f, 32.0f)
            val surface3End = Vector2(32.0f, 0.0f)
            val surface3Normal = Vector2(1.0f, 0.0f)
            val surface4Start = Vector2(32.0f, 0.0f)
            val surface4End = Vector2(0.0f, 0.0f)
            val surface4Normal = Vector2(0.0f, -1.0f)
            solidSurface.addSurface(surface1Start, surface1End, surface1Normal)
            solidSurface.addSurface(surface2Start, surface2End, surface2Normal)
            solidSurface.addSurface(surface3Start, surface3End, surface3Normal)
            solidSurface.addSurface(surface4Start, surface4End, surface4Normal)
            staticData.add(solidSurface)
            staticData.add(idle)
            setStaticData(GameObjectType.BREAKABLE_BLOCK, staticData)
        }
        val render = allocateComponent(RenderComponent::class.java) as RenderComponent?
        render!!.priority = SortConstants.GENERAL_OBJECT
        val sprite = allocateComponent(SpriteComponent::class.java) as SpriteComponent?
        sprite!!.setSize(thing.width.toInt(), thing.height.toInt())
        sprite.setRenderComponent(render)
        val collision = allocateComponent(DynamicCollisionComponent::class.java) as DynamicCollisionComponent?
        sprite.setCollisionComponent(collision)
        val hitReact = allocateComponent(HitReactionComponent::class.java) as HitReactionComponent?
        collision!!.setHitReactionComponent(hitReact)
        val lifetime = allocateComponent(LifetimeComponent::class.java) as LifetimeComponent?
        lifetime!!.setObjectToSpawnOnDeath(GameObjectType.BREAKABLE_BLOCK_PIECE_SPAWNER)
        val sound = sSystemRegistry.soundSystem
        if (sound != null) {
            lifetime.setDeathSound(sound.load(R.raw.sound_break_block))
        }
        thing.life = 1
        thing.team = Team.ENEMY
        thing.add(render)
        thing.add(sprite)
        thing.add(collision)
        thing.add(hitReact!!)
        thing.add(lifetime)
        addStaticData(GameObjectType.BREAKABLE_BLOCK, thing, sprite)
        sprite.playAnimation(0)
        return thing
    }

    fun spawnObjectTheSource(positionX: Float, positionY: Float): GameObject? {
        val textureLibrary = sSystemRegistry.shortTermTextureLibrary
        val thing = gameObjectPool.allocate()
        thing!!.activationRadius = alwaysActive
        thing.width = 512f
        thing.height = 512f
        thing.position[positionX] = positionY
        val layer1Render = allocateComponent(RenderComponent::class.java) as RenderComponent?
        layer1Render!!.priority = SortConstants.THE_SOURCE_START
        val layer1Fade = allocateComponent(FadeDrawableComponent::class.java) as FadeDrawableComponent?
        layer1Fade!!.setRenderComponent(layer1Render)
        layer1Fade.setTexture(textureLibrary!!.allocateTexture(R.drawable.enemy_source_spikes))
        layer1Fade.setupFade(1.0f, 0.2f, 1.9f, FadeDrawableComponent.LOOP_TYPE_PING_PONG, FadeDrawableComponent.FADE_EASE, 0.0f)
        val layer2Render = allocateComponent(RenderComponent::class.java) as RenderComponent?
        layer2Render!!.priority = SortConstants.THE_SOURCE_START + 1
        val layer2Fade = allocateComponent(FadeDrawableComponent::class.java) as FadeDrawableComponent?
        layer2Fade!!.setRenderComponent(layer2Render)
        layer2Fade.setTexture(textureLibrary.allocateTexture(R.drawable.enemy_source_body))
        layer2Fade.setupFade(1.0f, 0.8f, 5.0f, FadeDrawableComponent.LOOP_TYPE_PING_PONG, FadeDrawableComponent.FADE_EASE, 0.0f)
        val layer3Render = allocateComponent(RenderComponent::class.java) as RenderComponent?
        layer3Render!!.priority = SortConstants.THE_SOURCE_START + 2
        val layer3Fade = allocateComponent(FadeDrawableComponent::class.java) as FadeDrawableComponent?
        layer3Fade!!.setRenderComponent(layer3Render)
        layer3Fade.setTexture(textureLibrary.allocateTexture(R.drawable.enemy_source_black))
        layer3Fade.setupFade(0.0f, 1.0f, 6.0f, FadeDrawableComponent.LOOP_TYPE_PING_PONG, FadeDrawableComponent.FADE_LINEAR, 0.0f)
        val layer4Render = allocateComponent(RenderComponent::class.java) as RenderComponent?
        layer4Render!!.priority = SortConstants.THE_SOURCE_START + 3
        val layer4Fade = allocateComponent(FadeDrawableComponent::class.java) as FadeDrawableComponent?
        layer4Fade!!.setRenderComponent(layer4Render)
        layer4Fade.setTexture(textureLibrary.allocateTexture(R.drawable.enemy_source_spots))
        layer4Fade.setupFade(0.0f, 1.0f, 2.3f, FadeDrawableComponent.LOOP_TYPE_PING_PONG, FadeDrawableComponent.FADE_EASE, 0.0f)
        val layer5Render = allocateComponent(RenderComponent::class.java) as RenderComponent?
        layer5Render!!.priority = SortConstants.THE_SOURCE_START + 4
        val layer5Fade = allocateComponent(FadeDrawableComponent::class.java) as FadeDrawableComponent?
        layer5Fade!!.setRenderComponent(layer5Render)
        layer5Fade.setTexture(textureLibrary.allocateTexture(R.drawable.enemy_source_core))
        layer5Fade.setupFade(0.2f, 1.0f, 1.2f, FadeDrawableComponent.LOOP_TYPE_PING_PONG, FadeDrawableComponent.FADE_EASE, 0.0f)
        val orbit = allocateComponent(OrbitalMagnetComponent::class.java) as OrbitalMagnetComponent?
        orbit!!.setup(320.0f, 220.0f)
        val collision = allocateComponent(DynamicCollisionComponent::class.java) as DynamicCollisionComponent?
        val vulnerabilityVolume = FixedSizeArray<CollisionVolume>(1)
        vulnerabilityVolume.add(SphereCollisionVolume(256, 256, 256, HitType.HIT))
        val attackVolume = FixedSizeArray<CollisionVolume>(1)
        attackVolume.add(SphereCollisionVolume(256, 256, 256, HitType.HIT))
        collision!!.setCollisionVolumes(attackVolume, vulnerabilityVolume)
        val hitReact = allocateComponent(HitReactionComponent::class.java) as HitReactionComponent?
        collision.setHitReactionComponent(hitReact)
        hitReact!!.setInvincibleTime(TheSourceComponent.SHAKE_TIME)
        val theSource = allocateComponent(TheSourceComponent::class.java) as TheSourceComponent?
        var surpriseChannel: ChannelSystem.Channel? = null
        val channelSystem = sSystemRegistry.channelSystem
        surpriseChannel = channelSystem!!.registerChannel(sSurprisedNPCChannel)
        theSource!!.setChannel(surpriseChannel)
        theSource.setGameEvent(GameFlowEvent.EVENT_SHOW_ANIMATION, AnimationPlayerActivity.WANDA_ENDING)
        thing.life = 3
        thing.team = Team.PLAYER
        thing.add(layer1Render)
        thing.add(layer2Render)
        thing.add(layer3Render)
        thing.add(layer4Render)
        thing.add(layer5Render)
        thing.add(layer1Fade)
        thing.add(layer2Fade)
        thing.add(layer3Fade)
        thing.add(layer4Fade)
        thing.add(layer5Fade)
        thing.add(orbit)
        thing.add(collision)
        thing.add(hitReact)
        thing.add(theSource)
        return thing
    }

    fun spawnObjectSign(positionX: Float, positionY: Float): GameObject? {
        val textureLibrary = sSystemRegistry.shortTermTextureLibrary
        val thing = gameObjectPool.allocate()
        thing!!.position[positionX] = positionY
        thing.activationRadius = tightActivationRadius
        thing.width = 32f
        thing.height = 32f
        var staticData = getStaticData(GameObjectType.HINT_SIGN)
        if (staticData == null) {
            val staticObjectCount = 1
            staticData = FixedSizeArray(staticObjectCount)
            val basicVulnerabilityVolume = FixedSizeArray<CollisionVolume>(1)
            basicVulnerabilityVolume.add(AABoxCollisionVolume(8, 0, 24, 32, HitType.COLLECT))
            val idle = SpriteAnimation(0, 1)
            val frame1 = AnimationFrame(textureLibrary!!.allocateTexture(R.drawable.object_sign),
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume)
            idle.addFrame(frame1)
            idle.loop = true
            staticData.add(idle)
            setStaticData(GameObjectType.HINT_SIGN, staticData)
        }
        val render = allocateComponent(RenderComponent::class.java) as RenderComponent?
        render!!.priority = SortConstants.GENERAL_OBJECT
        val sprite = allocateComponent(SpriteComponent::class.java) as SpriteComponent?
        sprite!!.setSize(thing.width.toInt(), thing.height.toInt())
        sprite.setRenderComponent(render)
        val dynamicCollision = allocateComponent(DynamicCollisionComponent::class.java) as DynamicCollisionComponent?
        sprite.setCollisionComponent(dynamicCollision)
        val hitReact = allocateComponent(HitReactionComponent::class.java) as HitReactionComponent?
        dynamicCollision!!.setHitReactionComponent(hitReact)
        hitReact!!.setSpawnGameEventOnHit(HitType.COLLECT, GameFlowEvent.EVENT_SHOW_DIALOG_CHARACTER2, 0)
        val dialogSelect = allocateComponent(SelectDialogComponent::class.java) as SelectDialogComponent?
        dialogSelect!!.setHitReact(hitReact)
        thing.add(dialogSelect)
        thing.add(render)
        thing.add(sprite)
        thing.add(dynamicCollision)
        thing.add(hitReact)
        addStaticData(GameObjectType.HINT_SIGN, thing, sprite)
        sprite.playAnimation(0)
        return thing
    }

    fun spawnObjectTurret(positionX: Float, positionY: Float, flipHorizontal: Boolean): GameObject? {
        val textureLibrary = sSystemRegistry.shortTermTextureLibrary

        // Make sure related textures are loaded.
        textureLibrary!!.allocateTexture(R.drawable.effect_bullet01)
        textureLibrary.allocateTexture(R.drawable.effect_bullet02)
        val thing = gameObjectPool.allocate()
        thing!!.position[positionX] = positionY
        thing.activationRadius = tightActivationRadius
        thing.width = 64f
        thing.height = 64f
        var staticData = getStaticData(GameObjectType.TURRET)
        if (staticData == null) {
            val staticObjectCount = 3
            staticData = FixedSizeArray(staticObjectCount)

            // Animations
            val basicVulnerabilityVolume = FixedSizeArray<CollisionVolume>(1)
            basicVulnerabilityVolume.add(SphereCollisionVolume(32, 32, 32))
            basicVulnerabilityVolume[0]!!.hitType = HitType.POSSESS
            val idle = SpriteAnimation(EnemyAnimations.IDLE.ordinal, 2)
            idle.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.object_gunturret01),
                    1.0f, null, basicVulnerabilityVolume))
            idle.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.object_gunturret_idle),
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume))
            idle.loop = true
            val attack = SpriteAnimation(EnemyAnimations.ATTACK.ordinal, 4)
            attack.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.object_gunturret02),
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume))
            attack.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.object_gunturret01),
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume))
            attack.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.object_gunturret03),
                    Utils.framesToTime(24, 2), null, basicVulnerabilityVolume))
            attack.addFrame(AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.object_gunturret01),
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume))
            attack.loop = true
            val ghost = allocateComponent(GhostComponent::class.java) as GhostComponent?
            ghost!!.setTargetAction(ActionType.IDLE)
            ghost.changeActionOnButton(ActionType.ATTACK)
            staticData.add(idle)
            staticData.add(attack)
            staticData.add(ghost)
            setStaticData(GameObjectType.TURRET, staticData)
        }
        val render = allocateComponent(RenderComponent::class.java) as RenderComponent?
        render!!.priority = SortConstants.GENERAL_OBJECT
        val sprite = allocateComponent(SpriteComponent::class.java) as SpriteComponent?
        sprite!!.setSize(thing.width.toInt(), thing.height.toInt())
        sprite.setRenderComponent(render)
        val animation = allocateComponent(GenericAnimationComponent::class.java) as GenericAnimationComponent?
        animation!!.setSprite(sprite)
        val attack = allocateComponent(AttackAtDistanceComponent::class.java) as AttackAtDistanceComponent?
        attack!!.setupAttack(300f, 0.0f, 1.0f, true)
        val collision = allocateComponent(DynamicCollisionComponent::class.java) as DynamicCollisionComponent?
        sprite.setCollisionComponent(collision)
        val hitReact = allocateComponent(HitReactionComponent::class.java) as HitReactionComponent?
        collision!!.setHitReactionComponent(hitReact)
        val lifetime = allocateComponent(LifetimeComponent::class.java) as LifetimeComponent?
        lifetime!!.setObjectToSpawnOnDeath(GameObjectType.EXPLOSION_LARGE)
        val sound = sSystemRegistry.soundSystem
        val gun = allocateComponent(LaunchProjectileComponent::class.java) as LaunchProjectileComponent?
        gun!!.setShotsPerSet(1)
        gun.setDelayBetweenShots(0.0f)
        gun.setDelayBetweenSets(0.3f)
        gun.setObjectTypeToSpawn(GameObjectType.TURRET_BULLET)
        gun.setOffsetX(54f)
        gun.setOffsetY(13f)
        gun.setRequiredAction(ActionType.ATTACK)
        gun.setVelocityX(300.0f)
        gun.setVelocityY(-300.0f)
        gun.setShootSound(sound!!.load(R.raw.sound_gun))

        // Components for possession
        val componentSwap = allocateComponent(ChangeComponentsComponent::class.java) as ChangeComponentsComponent?
        componentSwap!!.addSwapOutComponent(attack)
        componentSwap.setPingPongBehavior(true)
        hitReact!!.setPossessionComponent(componentSwap)
        thing.team = Team.ENEMY
        if (flipHorizontal) {
            thing.facingDirection.x = -1.0f
        } else {
            thing.facingDirection.x = 1.0f
        }
        thing.add(render)
        thing.add(sprite)
        thing.add(animation)
        thing.add(attack)
        thing.add(collision)
        thing.add(hitReact)
        thing.add(lifetime)
        thing.add(gun)
        thing.add(componentSwap)
        addStaticData(GameObjectType.TURRET, thing, sprite)
        thing.commitUpdates()
        val possessedGhost = thing.findByClass(GhostComponent::class.java)
        if (possessedGhost != null) {
            thing.remove(possessedGhost) // Not supposed to be added yet.
            componentSwap.addSwapInComponent(possessedGhost)
        }
        sprite.playAnimation(0)
        return thing
    }

    fun spawnDust(positionX: Float, positionY: Float, flipHorizontal: Boolean): GameObject? {
        val textureLibrary = sSystemRegistry.longTermTextureLibrary
        val thing = gameObjectPool.allocate()
        thing!!.position[positionX] = positionY
        thing.activationRadius = tightActivationRadius
        thing.width = 32f
        thing.height = 32f
        var staticData = getStaticData(GameObjectType.DUST)
        if (staticData == null) {
            val staticObjectCount = 1
            staticData = FixedSizeArray(staticObjectCount)
            val idle = SpriteAnimation(0, 5)
            idle.addFrame(AnimationFrame(textureLibrary!!.getTextureByResource(R.drawable.dust01),
                    Utils.framesToTime(24, 1)))
            idle.addFrame(AnimationFrame(textureLibrary.getTextureByResource(R.drawable.dust02),
                    Utils.framesToTime(24, 1)))
            idle.addFrame(AnimationFrame(textureLibrary.getTextureByResource(R.drawable.dust03),
                    Utils.framesToTime(24, 1)))
            idle.addFrame(AnimationFrame(textureLibrary.getTextureByResource(R.drawable.dust04),
                    Utils.framesToTime(24, 1)))
            idle.addFrame(AnimationFrame(textureLibrary.getTextureByResource(R.drawable.dust05),
                    Utils.framesToTime(24, 1)))
            staticData.add(idle)
            setStaticData(GameObjectType.DUST, staticData)
        }
        val render = allocateComponent(RenderComponent::class.java) as RenderComponent?
        render!!.priority = SortConstants.EFFECT
        val lifetime = allocateComponent(LifetimeComponent::class.java) as LifetimeComponent?
        lifetime!!.setTimeUntilDeath(0.30f)
        val sprite = allocateComponent(SpriteComponent::class.java) as SpriteComponent?
        sprite!!.setSize(thing.width.toInt(), thing.height.toInt())
        sprite.setRenderComponent(render)
        if (flipHorizontal) {
            thing.facingDirection.x = -1.0f
        }
        thing.destroyOnDeactivation = true
        thing.add(lifetime)
        thing.add(render)
        thing.add(sprite)
        addStaticData(GameObjectType.DUST, thing, sprite)
        sprite.playAnimation(0)
        return thing
    }

    fun spawnEffectExplosionSmall(positionX: Float, positionY: Float): GameObject? {
        val textureLibrary = sSystemRegistry.longTermTextureLibrary
        val thing = gameObjectPool.allocate()
        thing!!.position[positionX] = positionY
        thing.activationRadius = alwaysActive
        thing.width = 32f
        thing.height = 32f
        var staticData = getStaticData(GameObjectType.EXPLOSION_SMALL)
        if (staticData == null) {
            val staticObjectCount = 1
            staticData = FixedSizeArray(staticObjectCount)
            val basicAttackVolume = FixedSizeArray<CollisionVolume>(1)
            basicAttackVolume.add(SphereCollisionVolume(16, 16, 16, HitType.HIT))
            val idle = SpriteAnimation(0, 7)
            idle.addFrame(AnimationFrame(
                    textureLibrary!!.getTextureByResource(R.drawable.effect_explosion_small01),
                    Utils.framesToTime(24, 1), basicAttackVolume, null))
            idle.addFrame(AnimationFrame(
                    textureLibrary.getTextureByResource(R.drawable.effect_explosion_small02),
                    Utils.framesToTime(24, 1), basicAttackVolume, null))
            idle.addFrame(AnimationFrame(
                    textureLibrary.getTextureByResource(R.drawable.effect_explosion_small03),
                    Utils.framesToTime(24, 1), basicAttackVolume, null))
            idle.addFrame(AnimationFrame(
                    textureLibrary.getTextureByResource(R.drawable.effect_explosion_small04),
                    Utils.framesToTime(24, 1), basicAttackVolume, null))
            idle.addFrame(AnimationFrame(
                    textureLibrary.getTextureByResource(R.drawable.effect_explosion_small05),
                    Utils.framesToTime(24, 1), basicAttackVolume, null))
            idle.addFrame(AnimationFrame(
                    textureLibrary.getTextureByResource(R.drawable.effect_explosion_small06),
                    Utils.framesToTime(24, 1), basicAttackVolume, null))
            idle.addFrame(AnimationFrame(
                    textureLibrary.getTextureByResource(R.drawable.effect_explosion_small07),
                    Utils.framesToTime(24, 1), basicAttackVolume, null))
            staticData.add(idle)
            setStaticData(GameObjectType.EXPLOSION_SMALL, staticData)
        }
        val render = allocateComponent(RenderComponent::class.java) as RenderComponent?
        render!!.priority = SortConstants.EFFECT
        val sprite = allocateComponent(SpriteComponent::class.java) as SpriteComponent?
        sprite!!.setSize(thing.width.toInt(), thing.height.toInt())
        sprite.setRenderComponent(render)
        val lifetime = allocateComponent(LifetimeComponent::class.java) as LifetimeComponent?
        val dynamicCollision = allocateComponent(DynamicCollisionComponent::class.java) as DynamicCollisionComponent?
        sprite.setCollisionComponent(dynamicCollision)
        thing.add(dynamicCollision!!)
        thing.add(lifetime!!)
        thing.add(render)
        thing.add(sprite)
        addStaticData(GameObjectType.EXPLOSION_SMALL, thing, sprite)
        val idle = sprite.findAnimation(0)
        if (idle != null) {
            lifetime.setTimeUntilDeath(idle.length)
        }
        sprite.playAnimation(0)
        return thing
    }

    fun spawnEffectExplosionLarge(positionX: Float, positionY: Float): GameObject? {
        val textureLibrary = sSystemRegistry.longTermTextureLibrary
        val thing = gameObjectPool.allocate()
        thing!!.position[positionX] = positionY
        thing.activationRadius = alwaysActive
        thing.width = 64f
        thing.height = 64f
        var staticData = getStaticData(GameObjectType.EXPLOSION_LARGE)
        if (staticData == null) {
            val staticObjectCount = 1
            staticData = FixedSizeArray(staticObjectCount)
            val basicAttackVolume = FixedSizeArray<CollisionVolume>(1)
            basicAttackVolume.add(SphereCollisionVolume(32, 32, 32, HitType.HIT))
            val idle = SpriteAnimation(0, 9)
            idle.addFrame(AnimationFrame(
                    textureLibrary!!.getTextureByResource(R.drawable.effect_explosion_big01),
                    Utils.framesToTime(24, 1), basicAttackVolume, null))
            idle.addFrame(AnimationFrame(
                    textureLibrary.getTextureByResource(R.drawable.effect_explosion_big02),
                    Utils.framesToTime(24, 1), basicAttackVolume, null))
            idle.addFrame(AnimationFrame(
                    textureLibrary.getTextureByResource(R.drawable.effect_explosion_big03),
                    Utils.framesToTime(24, 1), basicAttackVolume, null))
            idle.addFrame(AnimationFrame(
                    textureLibrary.getTextureByResource(R.drawable.effect_explosion_big04),
                    Utils.framesToTime(24, 1), basicAttackVolume, null))
            idle.addFrame(AnimationFrame(
                    textureLibrary.getTextureByResource(R.drawable.effect_explosion_big05),
                    Utils.framesToTime(24, 1), basicAttackVolume, null))
            idle.addFrame(AnimationFrame(
                    textureLibrary.getTextureByResource(R.drawable.effect_explosion_big06),
                    Utils.framesToTime(24, 1), basicAttackVolume, null))
            idle.addFrame(AnimationFrame(
                    textureLibrary.getTextureByResource(R.drawable.effect_explosion_big07),
                    Utils.framesToTime(24, 1), basicAttackVolume, null))
            idle.addFrame(AnimationFrame(
                    textureLibrary.getTextureByResource(R.drawable.effect_explosion_big08),
                    Utils.framesToTime(24, 1), basicAttackVolume, null))
            idle.addFrame(AnimationFrame(
                    textureLibrary.getTextureByResource(R.drawable.effect_explosion_big09),
                    Utils.framesToTime(24, 1), basicAttackVolume, null))
            staticData.add(idle)
            setStaticData(GameObjectType.EXPLOSION_LARGE, staticData)
        }
        val render = allocateComponent(RenderComponent::class.java) as RenderComponent?
        render!!.priority = SortConstants.EFFECT
        val sprite = allocateComponent(SpriteComponent::class.java) as SpriteComponent?
        sprite!!.setSize(thing.width.toInt(), thing.height.toInt())
        sprite.setRenderComponent(render)
        val lifetime = allocateComponent(LifetimeComponent::class.java) as LifetimeComponent?
        val dynamicCollision = allocateComponent(DynamicCollisionComponent::class.java) as DynamicCollisionComponent?
        sprite.setCollisionComponent(dynamicCollision)
        val soundEffect = allocateComponent(PlaySingleSoundComponent::class.java) as PlaySingleSoundComponent?
        soundEffect!!.setSound(sSystemRegistry.soundSystem!!.load(R.raw.quick_explosion))
        thing.add(soundEffect)
        thing.add(dynamicCollision!!)
        thing.add(lifetime!!)
        thing.add(render)
        thing.add(sprite)
        addStaticData(GameObjectType.EXPLOSION_LARGE, thing, sprite)
        val idle = sprite.findAnimation(0)
        if (idle != null) {
            lifetime.setTimeUntilDeath(idle.length)
        }
        sprite.playAnimation(0)
        return thing
    }

    fun spawnEffectExplosionGiant(positionX: Float, positionY: Float): GameObject? {
        val textureLibrary = sSystemRegistry.longTermTextureLibrary
        val thing = gameObjectPool.allocate()
        thing!!.position[positionX] = positionY
        thing.activationRadius = alwaysActive
        thing.width = 64f
        thing.height = 64f
        var staticData = getStaticData(GameObjectType.EXPLOSION_GIANT)
        if (staticData == null) {
            val staticObjectCount = 4
            staticData = FixedSizeArray(staticObjectCount)
            val basicAttackVolume = FixedSizeArray<CollisionVolume>(1)
            basicAttackVolume.add(SphereCollisionVolume(64, 32, 32, HitType.HIT))
            val idle = SpriteAnimation(0, 9)
            idle.addFrame(AnimationFrame(
                    textureLibrary!!.getTextureByResource(R.drawable.effect_explosion_big01),
                    Utils.framesToTime(24, 1), basicAttackVolume, null))
            idle.addFrame(AnimationFrame(
                    textureLibrary.getTextureByResource(R.drawable.effect_explosion_big02),
                    Utils.framesToTime(24, 1), basicAttackVolume, null))
            idle.addFrame(AnimationFrame(
                    textureLibrary.getTextureByResource(R.drawable.effect_explosion_big03),
                    Utils.framesToTime(24, 1), basicAttackVolume, null))
            idle.addFrame(AnimationFrame(
                    textureLibrary.getTextureByResource(R.drawable.effect_explosion_big04),
                    Utils.framesToTime(24, 1), basicAttackVolume, null))
            idle.addFrame(AnimationFrame(
                    textureLibrary.getTextureByResource(R.drawable.effect_explosion_big05),
                    Utils.framesToTime(24, 1), basicAttackVolume, null))
            idle.addFrame(AnimationFrame(
                    textureLibrary.getTextureByResource(R.drawable.effect_explosion_big06),
                    Utils.framesToTime(24, 1), basicAttackVolume, null))
            idle.addFrame(AnimationFrame(
                    textureLibrary.getTextureByResource(R.drawable.effect_explosion_big07),
                    Utils.framesToTime(24, 1), basicAttackVolume, null))
            idle.addFrame(AnimationFrame(
                    textureLibrary.getTextureByResource(R.drawable.effect_explosion_big08),
                    Utils.framesToTime(24, 1), basicAttackVolume, null))
            idle.addFrame(AnimationFrame(
                    textureLibrary.getTextureByResource(R.drawable.effect_explosion_big09),
                    Utils.framesToTime(24, 1), basicAttackVolume, null))
            val smallFrame1 = AnimationFrame(
                    textureLibrary.getTextureByResource(R.drawable.effect_explosion_small01),
                    Utils.framesToTime(24, 1), null, null)
            val smallFrame2 = AnimationFrame(
                    textureLibrary.getTextureByResource(R.drawable.effect_explosion_small02),
                    Utils.framesToTime(24, 1), null, null)
            val smallFrame3 = AnimationFrame(
                    textureLibrary.getTextureByResource(R.drawable.effect_explosion_small03),
                    Utils.framesToTime(24, 1), null, null)
            val smallFrame4 = AnimationFrame(
                    textureLibrary.getTextureByResource(R.drawable.effect_explosion_small04),
                    Utils.framesToTime(24, 1), null, null)
            val smallFrame5 = AnimationFrame(
                    textureLibrary.getTextureByResource(R.drawable.effect_explosion_small05),
                    Utils.framesToTime(24, 1), null, null)
            val smallFrame6 = AnimationFrame(
                    textureLibrary.getTextureByResource(R.drawable.effect_explosion_small06),
                    Utils.framesToTime(24, 1), null, null)
            val smallFrame7 = AnimationFrame(textureLibrary.getTextureByResource(R.drawable.effect_explosion_small07),
                    Utils.framesToTime(24, 1), null, null)
            val smallBlast1 = SpriteAnimation(0, 7)
            smallBlast1.addFrame(smallFrame1)
            smallBlast1.addFrame(smallFrame2)
            smallBlast1.addFrame(smallFrame3)
            smallBlast1.addFrame(smallFrame4)
            smallBlast1.addFrame(smallFrame5)
            smallBlast1.addFrame(smallFrame6)
            smallBlast1.addFrame(smallFrame7)
            val smallBlast2 = SpriteAnimation(0, 8)
            smallBlast2.addFrame(AnimationFrame(null, Utils.framesToTime(24, 4), null, null))
            smallBlast2.addFrame(smallFrame1)
            smallBlast2.addFrame(smallFrame2)
            smallBlast2.addFrame(smallFrame3)
            smallBlast2.addFrame(smallFrame4)
            smallBlast2.addFrame(smallFrame5)
            smallBlast2.addFrame(smallFrame6)
            smallBlast2.addFrame(smallFrame7)
            val smallBlast3 = SpriteAnimation(0, 8)
            smallBlast3.addFrame(AnimationFrame(null, Utils.framesToTime(24, 8), null, null))
            smallBlast3.addFrame(smallFrame1)
            smallBlast3.addFrame(smallFrame2)
            smallBlast3.addFrame(smallFrame3)
            smallBlast3.addFrame(smallFrame4)
            smallBlast3.addFrame(smallFrame5)
            smallBlast3.addFrame(smallFrame6)
            smallBlast3.addFrame(smallFrame7)
            staticData.add(idle)
            staticData.add(smallBlast1)
            staticData.add(smallBlast2)
            staticData.add(smallBlast3)
            setStaticData(GameObjectType.EXPLOSION_GIANT, staticData)
        }
        val render = allocateComponent(RenderComponent::class.java) as RenderComponent?
        render!!.priority = SortConstants.EFFECT
        val sprite = allocateComponent(SpriteComponent::class.java) as SpriteComponent?
        sprite!!.setSize(thing.width.toInt(), thing.height.toInt())
        sprite.setRenderComponent(render)

        // Hack.  Use static data differently for this object so we can share three animations
        // amongst three separate sprites.
        val idle = staticData[0] as SpriteAnimation?
        val smallBlast1 = staticData[1] as SpriteAnimation?
        val smallBlast2 = staticData[2] as SpriteAnimation?
        val smallBlast3 = staticData[3] as SpriteAnimation?
        sprite.addAnimation(idle)
        sprite.playAnimation(0)
        val blast1Render = allocateComponent(RenderComponent::class.java) as RenderComponent?
        render.priority = SortConstants.EFFECT
        val blast1Sprite = allocateComponent(SpriteComponent::class.java) as SpriteComponent?
        blast1Sprite!!.setSize(32, 32)
        blast1Sprite.setRenderComponent(blast1Render)
        blast1Render!!.setDrawOffset(40f, 50f)
        blast1Sprite.addAnimation(smallBlast1)
        blast1Sprite.playAnimation(0)
        val blast2Render = allocateComponent(RenderComponent::class.java) as RenderComponent?
        render.priority = SortConstants.EFFECT
        val blast2Sprite = allocateComponent(SpriteComponent::class.java) as SpriteComponent?
        blast2Sprite!!.setSize(32, 32)
        blast2Sprite.setRenderComponent(blast2Render)
        blast2Render!!.setDrawOffset(-10f, 0f)
        blast2Sprite.addAnimation(smallBlast2)
        blast2Sprite.playAnimation(0)
        val blast3Render = allocateComponent(RenderComponent::class.java) as RenderComponent?
        render.priority = SortConstants.EFFECT
        val blast3Sprite = allocateComponent(SpriteComponent::class.java) as SpriteComponent?
        blast3Sprite!!.setSize(32, 32)
        blast3Sprite.setRenderComponent(blast3Render)
        blast3Render!!.setDrawOffset(0f, 32f)
        blast3Sprite.addAnimation(smallBlast3)
        blast3Sprite.playAnimation(0)
        val lifetime = allocateComponent(LifetimeComponent::class.java) as LifetimeComponent?
        lifetime!!.setTimeUntilDeath(max(
                max(
                        max(idle!!.length, smallBlast1!!.length),
                        smallBlast2!!.length),
                smallBlast3!!.length))
        val dynamicCollision = allocateComponent(DynamicCollisionComponent::class.java) as DynamicCollisionComponent?
        sprite.setCollisionComponent(dynamicCollision)
        val soundEffect = allocateComponent(PlaySingleSoundComponent::class.java) as PlaySingleSoundComponent?
        soundEffect!!.setSound(sSystemRegistry.soundSystem!!.load(R.raw.quick_explosion))
        thing.team = Team.PLAYER // Maybe this should be an argument to this function.
        thing.add(dynamicCollision!!)
        thing.add(lifetime)
        thing.add(render)
        thing.add(sprite)
        thing.add(soundEffect)
        thing.add(blast1Render)
        thing.add(blast1Sprite)
        thing.add(blast2Render)
        thing.add(blast2Sprite)
        thing.add(blast3Render)
        thing.add(blast3Sprite)
        return thing
    }

    fun spawnGhostNPC(positionX: Float, positionY: Float): GameObject? {
        val thing = gameObjectPool.allocate()
        thing!!.position[positionX] = positionY
        thing.activationRadius = alwaysActive
        thing.width = 32f
        thing.height = 32f
        var staticData = getStaticData(GameObjectType.GHOST_NPC)
        if (staticData == null) {
            val staticObjectCount = 2
            staticData = FixedSizeArray(staticObjectCount)
            val gravity = allocateComponent(GravityComponent::class.java)
            val movement = allocateComponent(MovementComponent::class.java)
            staticData.add(gravity)
            staticData.add(movement)
            setStaticData(GameObjectType.GHOST_NPC, staticData)
        }
        val patrol = allocateComponent(NPCComponent::class.java) as NPCComponent?
        val life = allocateComponent(LifetimeComponent::class.java) as LifetimeComponent?
        thing.team = Team.NONE
        thing.life = 1
        thing.add(patrol!!)
        thing.add(life!!)
        addStaticData(GameObjectType.GHOST_NPC, thing, null)
        return thing
    }

    private fun spawnCameraBias(positionX: Float, positionY: Float): GameObject? {
        val thing = gameObjectPool.allocate()
        thing!!.position[positionX] = positionY
        thing.activationRadius = tightActivationRadius
        thing.width = 32f
        thing.height = 32f
        var staticData = getStaticData(GameObjectType.CAMERA_BIAS)
        if (staticData == null) {
            val staticObjectCount = 1
            staticData = FixedSizeArray(staticObjectCount)
            val bias = allocateComponent(CameraBiasComponent::class.java)
            staticData.add(bias)
            setStaticData(GameObjectType.CAMERA_BIAS, staticData)
        }
        addStaticData(GameObjectType.CAMERA_BIAS, thing, null)
        return thing
    }

    fun spawnEffectSmokeBig(positionX: Float, positionY: Float): GameObject? {
        val textureLibrary = sSystemRegistry.longTermTextureLibrary
        var thing: GameObject? = null
        // This is just an effect, so we can live without it if our pools are exhausted.
        if (componentAvailable(RenderComponent::class.java, 1)) {
            thing = gameObjectPool.allocate()
            thing!!.position[positionX] = positionY
            thing.activationRadius = tightActivationRadius
            thing.width = 32f
            thing.height = 32f
            var staticData = getStaticData(GameObjectType.SMOKE_BIG)
            if (staticData == null) {
                val staticObjectCount = 6
                staticData = FixedSizeArray(staticObjectCount)
                val movement = allocateComponent(MovementComponent::class.java)
                val frame2 = AnimationFrame(
                        textureLibrary!!.getTextureByResource(R.drawable.effect_smoke_big02),
                        Utils.framesToTime(24, 1), null, null)
                val frame3 = AnimationFrame(
                        textureLibrary.getTextureByResource(R.drawable.effect_smoke_big03),
                        Utils.framesToTime(24, 1), null, null)
                val frame4 = AnimationFrame(
                        textureLibrary.getTextureByResource(R.drawable.effect_smoke_big04),
                        Utils.framesToTime(24, 1), null, null)
                val frame5 = AnimationFrame(
                        textureLibrary.getTextureByResource(R.drawable.effect_smoke_big05),
                        Utils.framesToTime(24, 1), null, null)
                val idle = SpriteAnimation(0, 5)
                idle.addFrame(AnimationFrame(
                        textureLibrary.getTextureByResource(R.drawable.effect_smoke_big01),
                        Utils.framesToTime(24, 10), null, null))
                idle.addFrame(frame2)
                idle.addFrame(frame3)
                idle.addFrame(frame4)
                idle.addFrame(frame5)
                val idle2 = SpriteAnimation(1, 5)
                idle2.addFrame(AnimationFrame(
                        textureLibrary.getTextureByResource(R.drawable.effect_smoke_big01),
                        Utils.framesToTime(24, 13), null, null))
                idle2.addFrame(frame2)
                idle2.addFrame(frame3)
                idle2.addFrame(frame4)
                idle2.addFrame(frame5)
                val idle3 = SpriteAnimation(2, 5)
                idle3.addFrame(AnimationFrame(
                        textureLibrary.getTextureByResource(R.drawable.effect_smoke_big01),
                        Utils.framesToTime(24, 8), null, null))
                idle3.addFrame(frame2)
                idle3.addFrame(frame3)
                idle3.addFrame(frame4)
                idle3.addFrame(frame5)
                val idle4 = SpriteAnimation(3, 5)
                idle4.addFrame(AnimationFrame(
                        textureLibrary.getTextureByResource(R.drawable.effect_smoke_big01),
                        Utils.framesToTime(24, 5), null, null))
                idle4.addFrame(frame2)
                idle4.addFrame(frame3)
                idle4.addFrame(frame4)
                idle4.addFrame(frame5)
                val idle5 = SpriteAnimation(4, 5)
                idle5.addFrame(AnimationFrame(
                        textureLibrary.getTextureByResource(R.drawable.effect_smoke_big01),
                        Utils.framesToTime(24, 15), null, null))
                idle5.addFrame(frame2)
                idle5.addFrame(frame3)
                idle5.addFrame(frame4)
                idle5.addFrame(frame5)
                staticData.add(idle)
                staticData.add(idle2)
                staticData.add(idle3)
                staticData.add(idle4)
                staticData.add(idle5)
                staticData.add(movement)
                setStaticData(GameObjectType.SMOKE_BIG, staticData)
            }
            val render = allocateComponent(RenderComponent::class.java) as RenderComponent?
            render!!.priority = SortConstants.EFFECT
            val sprite = allocateComponent(SpriteComponent::class.java) as SpriteComponent?
            sprite!!.setSize(thing.width.toInt(), thing.height.toInt())
            sprite.setRenderComponent(render)
            val lifetime = allocateComponent(LifetimeComponent::class.java) as LifetimeComponent?
            lifetime!!.setDieWhenInvisible(true)
            thing.destroyOnDeactivation = true
            thing.add(lifetime)
            thing.add(render)
            thing.add(sprite)
            addStaticData(GameObjectType.SMOKE_BIG, thing, sprite)
            val animIndex = (Math.random() * sprite.animationCount).toInt()
            val idle = sprite.findAnimation(animIndex)
            if (idle != null) {
                lifetime.setTimeUntilDeath(idle.length)
                sprite.playAnimation(animIndex)
            }
        }
        return thing
    }

    fun spawnEffectSmokeSmall(positionX: Float, positionY: Float): GameObject? {
        val textureLibrary = sSystemRegistry.longTermTextureLibrary
        var thing: GameObject? = null
        // This is just an effect, so we can live without it if our pools are exhausted.
        if (componentAvailable(RenderComponent::class.java, 1)) {
            thing = gameObjectPool.allocate()
            thing!!.position[positionX] = positionY
            thing.activationRadius = alwaysActive
            thing.width = 16f
            thing.height = 16f
            var staticData = getStaticData(GameObjectType.SMOKE_SMALL)
            if (staticData == null) {
                val staticObjectCount = 2
                staticData = FixedSizeArray(staticObjectCount)
                val movement = allocateComponent(MovementComponent::class.java)
                val idle = SpriteAnimation(0, 5)
                idle.addFrame(AnimationFrame(
                        textureLibrary!!.getTextureByResource(R.drawable.effect_smoke_small01),
                        Utils.framesToTime(24, 10), null, null))
                idle.addFrame(AnimationFrame(
                        textureLibrary.getTextureByResource(R.drawable.effect_smoke_small02),
                        Utils.framesToTime(24, 1), null, null))
                idle.addFrame(AnimationFrame(
                        textureLibrary.getTextureByResource(R.drawable.effect_smoke_small03),
                        Utils.framesToTime(24, 1), null, null))
                idle.addFrame(AnimationFrame(
                        textureLibrary.getTextureByResource(R.drawable.effect_smoke_small04),
                        Utils.framesToTime(24, 1), null, null))
                idle.addFrame(AnimationFrame(
                        textureLibrary.getTextureByResource(R.drawable.effect_smoke_small05),
                        Utils.framesToTime(24, 1), null, null))
                staticData.add(idle)
                staticData.add(movement)
                setStaticData(GameObjectType.SMOKE_SMALL, staticData)
            }
            val render = allocateComponent(RenderComponent::class.java) as RenderComponent?
            render!!.priority = SortConstants.EFFECT
            val sprite = allocateComponent(SpriteComponent::class.java) as SpriteComponent?
            sprite!!.setSize(thing.width.toInt(), thing.height.toInt())
            sprite.setRenderComponent(render)
            val lifetime = allocateComponent(LifetimeComponent::class.java) as LifetimeComponent?
            lifetime!!.setDieWhenInvisible(true)
            thing.destroyOnDeactivation = true
            thing.add(lifetime)
            thing.add(render)
            thing.add(sprite)
            addStaticData(GameObjectType.SMOKE_SMALL, thing, sprite)
            val idle = sprite.findAnimation(0)
            if (idle != null) {
                lifetime.setTimeUntilDeath(idle.length)
            }
            sprite.playAnimation(0)
        }
        return thing
    }

    fun spawnEffectCrushFlash(positionX: Float, positionY: Float): GameObject? {
        val textureLibrary = sSystemRegistry.longTermTextureLibrary
        var thing: GameObject? = null
        // This is just an effect, so we can live without it if our pools are exhausted.
        if (componentAvailable(RenderComponent::class.java, 1)) {
            thing = gameObjectPool.allocate()
            thing!!.position[positionX] = positionY
            thing.activationRadius = alwaysActive
            thing.width = 64f
            thing.height = 64f
            var staticData = getStaticData(GameObjectType.CRUSH_FLASH)
            if (staticData == null) {
                val staticObjectCount = 2
                staticData = FixedSizeArray(staticObjectCount)
                val back = SpriteAnimation(0, 3)
                back.addFrame(AnimationFrame(
                        textureLibrary!!.getTextureByResource(R.drawable.effect_crush_back01),
                        Utils.framesToTime(24, 1), null, null))
                back.addFrame(AnimationFrame(
                        textureLibrary.getTextureByResource(R.drawable.effect_crush_back02),
                        Utils.framesToTime(24, 1), null, null))
                back.addFrame(AnimationFrame(
                        textureLibrary.getTextureByResource(R.drawable.effect_crush_back03),
                        Utils.framesToTime(24, 1), null, null))
                val front = SpriteAnimation(1, 7)
                front.addFrame(AnimationFrame(
                        textureLibrary.getTextureByResource(R.drawable.effect_crush_front01),
                        Utils.framesToTime(24, 1), null, null))
                front.addFrame(AnimationFrame(
                        textureLibrary.getTextureByResource(R.drawable.effect_crush_front02),
                        Utils.framesToTime(24, 1), null, null))
                front.addFrame(AnimationFrame(
                        textureLibrary.getTextureByResource(R.drawable.effect_crush_front03),
                        Utils.framesToTime(24, 1), null, null))
                front.addFrame(AnimationFrame(
                        textureLibrary.getTextureByResource(R.drawable.effect_crush_front04),
                        Utils.framesToTime(24, 1), null, null))
                front.addFrame(AnimationFrame(
                        textureLibrary.getTextureByResource(R.drawable.effect_crush_front05),
                        Utils.framesToTime(24, 1), null, null))
                front.addFrame(AnimationFrame(
                        textureLibrary.getTextureByResource(R.drawable.effect_crush_front06),
                        Utils.framesToTime(24, 1), null, null))
                front.addFrame(AnimationFrame(
                        textureLibrary.getTextureByResource(R.drawable.effect_crush_front07),
                        Utils.framesToTime(24, 1), null, null))
                staticData.add(back)
                staticData.add(front)
                setStaticData(GameObjectType.CRUSH_FLASH, staticData)
            }
            val backRender = allocateComponent(RenderComponent::class.java) as RenderComponent?
            backRender!!.priority = SortConstants.EFFECT
            val backSprite = allocateComponent(SpriteComponent::class.java) as SpriteComponent?
            backSprite!!.setSize(thing.width.toInt(), thing.height.toInt())
            backSprite.setRenderComponent(backRender)
            val foreRender = allocateComponent(RenderComponent::class.java) as RenderComponent?
            foreRender!!.priority = SortConstants.FOREGROUND_EFFECT
            val foreSprite = allocateComponent(SpriteComponent::class.java) as SpriteComponent?
            foreSprite!!.setSize(thing.width.toInt(), thing.height.toInt())
            foreSprite.setRenderComponent(foreRender)
            val lifetime = allocateComponent(LifetimeComponent::class.java) as LifetimeComponent?
            thing.add(lifetime!!)
            thing.add(backRender)
            thing.add(foreRender)
            thing.add(foreSprite)
            thing.add(backSprite)
            addStaticData(GameObjectType.CRUSH_FLASH, thing, backSprite)
            addStaticData(GameObjectType.CRUSH_FLASH, null, foreSprite)
            val idle = foreSprite.findAnimation(1)
            if (idle != null) {
                lifetime.setTimeUntilDeath(idle.length)
            }
            backSprite.playAnimation(0)
            foreSprite.playAnimation(1)
        }
        return thing
    }

    fun spawnEffectFlash(positionX: Float, positionY: Float): GameObject? {
        val textureLibrary = sSystemRegistry.longTermTextureLibrary
        var thing: GameObject? = null
        // This is just an effect, so we can live without it if our pools are exhausted.
        if (componentAvailable(RenderComponent::class.java, 1)) {
            thing = gameObjectPool.allocate()
            thing!!.position[positionX] = positionY
            thing.activationRadius = alwaysActive
            thing.width = 64f
            thing.height = 64f
            var staticData = getStaticData(GameObjectType.FLASH)
            if (staticData == null) {
                val staticObjectCount = 1
                staticData = FixedSizeArray(staticObjectCount)
                val back = SpriteAnimation(0, 3)
                back.addFrame(AnimationFrame(
                        textureLibrary!!.getTextureByResource(R.drawable.effect_crush_back01),
                        Utils.framesToTime(24, 1), null, null))
                back.addFrame(AnimationFrame(
                        textureLibrary.getTextureByResource(R.drawable.effect_crush_back02),
                        Utils.framesToTime(24, 1), null, null))
                back.addFrame(AnimationFrame(
                        textureLibrary.getTextureByResource(R.drawable.effect_crush_back03),
                        Utils.framesToTime(24, 1), null, null))
                staticData.add(back)
                setStaticData(GameObjectType.FLASH, staticData)
            }
            val backRender = allocateComponent(RenderComponent::class.java) as RenderComponent?
            backRender!!.priority = SortConstants.EFFECT
            val backSprite = allocateComponent(SpriteComponent::class.java) as SpriteComponent?
            backSprite!!.setSize(thing.width.toInt(), thing.height.toInt())
            backSprite.setRenderComponent(backRender)
            val lifetime = allocateComponent(LifetimeComponent::class.java) as LifetimeComponent?
            thing.add(lifetime!!)
            thing.add(backRender)
            thing.add(backSprite)
            addStaticData(GameObjectType.FLASH, thing, backSprite)
            val idle = backSprite.findAnimation(0)
            if (idle != null) {
                lifetime.setTimeUntilDeath(idle.length)
            }
            backSprite.playAnimation(0)
        }
        return thing
    }

    fun spawnFrameRateWatcher(positionX: Float, positionY: Float): GameObject? {
        val textureLibrary = sSystemRegistry.shortTermTextureLibrary
        val params = sSystemRegistry.contextParameters
        val thing = gameObjectPool.allocate()
        thing!!.position[250f] = 0f // HACK!
        thing.activationRadius = alwaysActive
        thing.width = params!!.gameWidth.toFloat()
        thing.height = params.gameHeight.toFloat()
        val indicator = DrawableBitmap(
                textureLibrary!!.allocateTexture(R.drawable.framerate_warning),
                thing.width.toInt(),
                thing.height.toInt())
        indicator.setCrop(0, 8, 8, 8) // hack!  this shouldn't be hard-coded.
        val render = allocateComponent(RenderComponent::class.java) as RenderComponent?
        render!!.priority = SortConstants.OVERLAY
        render.setCameraRelative(false)
        val watcher = allocateComponent(FrameRateWatcherComponent::class.java) as FrameRateWatcherComponent?
        watcher!!.setup(render, indicator)
        thing.add(render)
        thing.add(watcher)
        return thing
    }

    fun spawnBreakableBlockPiece(positionX: Float, positionY: Float): GameObject? {
        val textureLibrary = sSystemRegistry.shortTermTextureLibrary
        val thing = gameObjectPool.allocate()
        thing!!.position[positionX] = positionY
        thing.activationRadius = tightActivationRadius
        thing.width = 16f
        thing.height = 16f
        var staticData = getStaticData(GameObjectType.BREAKABLE_BLOCK_PIECE)
        if (staticData == null) {
            val staticObjectCount = 4
            staticData = FixedSizeArray(staticObjectCount)
            val gravity = allocateComponent(GravityComponent::class.java)
            val movement = allocateComponent(MovementComponent::class.java)
            val physics = allocateComponent(SimplePhysicsComponent::class.java) as SimplePhysicsComponent?
            physics!!.setBounciness(0.3f)
            val piece = DrawableBitmap(
                    textureLibrary!!.getTextureByResource(R.drawable.object_debris_piece),
                    thing.width.toInt(),
                    thing.height.toInt())
            val render = allocateComponent(RenderComponent::class.java) as RenderComponent?
            render!!.priority = SortConstants.GENERAL_OBJECT
            render.drawable = piece
            staticData.add(render)
            staticData.add(movement)
            staticData.add(gravity)
            staticData.add(physics)
            setStaticData(GameObjectType.BREAKABLE_BLOCK_PIECE, staticData)
        }
        val lifetime = allocateComponent(LifetimeComponent::class.java) as LifetimeComponent?
        lifetime!!.setTimeUntilDeath(3.0f)
        val bgcollision = allocateComponent(BackgroundCollisionComponent::class.java) as BackgroundCollisionComponent?
        bgcollision!!.setSize(12, 12)
        bgcollision.setOffset(2, 2)
        thing.destroyOnDeactivation = true
        thing.add(lifetime)
        thing.add(bgcollision)
        addStaticData(GameObjectType.BREAKABLE_BLOCK_PIECE, thing, null)
        return thing
    }

    fun spawnBreakableBlockPieceSpawner(positionX: Float, positionY: Float): GameObject? {
        val thing = gameObjectPool.allocate()
        thing!!.position[positionX] = positionY
        thing.activationRadius = tightActivationRadius
        thing.width = 1f
        thing.height = 1f
        val lifetime = allocateComponent(LifetimeComponent::class.java) as LifetimeComponent?
        lifetime!!.setTimeUntilDeath(0.5f)
        val pieceSpawner = allocateComponent(LaunchProjectileComponent::class.java) as LaunchProjectileComponent?
        pieceSpawner!!.setObjectTypeToSpawn(GameObjectType.BREAKABLE_BLOCK_PIECE)
        pieceSpawner.setDelayBeforeFirstSet(0.0f)
        pieceSpawner.setSetsPerActivation(1)
        pieceSpawner.setShotsPerSet(3)
        pieceSpawner.setDelayBetweenShots(0.0f)
        pieceSpawner.setOffsetX(16f)
        pieceSpawner.setOffsetY(16f)
        pieceSpawner.setVelocityX(600.0f)
        pieceSpawner.setVelocityY(-1000.0f)
        pieceSpawner.setThetaError(1.0f)
        thing.life = 1
        thing.destroyOnDeactivation = true
        thing.add(lifetime)
        thing.add(pieceSpawner)
        return thing
    }

    fun spawnSmokePoof(positionX: Float, positionY: Float): GameObject? {
        var thing: GameObject? = null
        // This is just an effect, so we can live without it if our pools are exhausted.
        if (componentAvailable(LaunchProjectileComponent::class.java, 2)) {
            thing = gameObjectPool.allocate()
            thing!!.position[positionX] = positionY
            thing.activationRadius = tightActivationRadius
            thing.width = 1f
            thing.height = 1f
            val lifetime = allocateComponent(LifetimeComponent::class.java) as LifetimeComponent?
            lifetime!!.setTimeUntilDeath(0.5f)
            val smokeGun = allocateComponent(LaunchProjectileComponent::class.java) as LaunchProjectileComponent?
            smokeGun!!.setSetsPerActivation(1)
            smokeGun.setShotsPerSet(3)
            smokeGun.setDelayBetweenShots(0.0f)
            smokeGun.setObjectTypeToSpawn(GameObjectType.SMOKE_BIG)
            smokeGun.setVelocityX(200.0f)
            smokeGun.setVelocityY(200.0f)
            smokeGun.setOffsetX(16f)
            smokeGun.setOffsetY(16f)
            smokeGun.setThetaError(1.0f)
            val smokeGun2 = allocateComponent(LaunchProjectileComponent::class.java) as LaunchProjectileComponent?
            smokeGun2!!.setSetsPerActivation(1)
            smokeGun2.setShotsPerSet(3)
            smokeGun2.setDelayBetweenShots(0.0f)
            smokeGun2.setObjectTypeToSpawn(GameObjectType.SMOKE_SMALL)
            smokeGun2.setVelocityX(200.0f)
            smokeGun2.setVelocityY(200.0f)
            smokeGun2.setThetaError(1.0f)
            smokeGun2.setOffsetX(16f)
            smokeGun2.setOffsetY(16f)
            thing.life = 1
            thing.destroyOnDeactivation = true
            thing.add(lifetime)
            thing.add(smokeGun)
            thing.add(smokeGun2)
        }
        return thing
    }

    fun spawnGemEffect(positionX: Float, positionY: Float): GameObject? {
        val textureLibrary = sSystemRegistry.shortTermTextureLibrary
        val thing = gameObjectPool.allocate()
        thing!!.position[positionX] = positionY
        thing.activationRadius = tightActivationRadius
        thing.width = 32f
        thing.height = 32f
        var staticData = getStaticData(GameObjectType.GEM_EFFECT)
        if (staticData == null) {
            val staticObjectCount = 2
            staticData = FixedSizeArray(staticObjectCount)
            val movement = allocateComponent(MovementComponent::class.java)
            staticData.add(movement)
            setStaticData(GameObjectType.GEM_EFFECT, staticData)
        }
        val render = allocateComponent(RenderComponent::class.java) as RenderComponent?
        render!!.priority = SortConstants.EFFECT
        val lifetime = allocateComponent(LifetimeComponent::class.java) as LifetimeComponent?
        lifetime!!.setTimeUntilDeath(0.5f)
        val fadeOut = allocateComponent(FadeDrawableComponent::class.java) as FadeDrawableComponent?
        fadeOut!!.setupFade(1.0f, 0.0f, 0.5f, FadeDrawableComponent.LOOP_TYPE_NONE, FadeDrawableComponent.FADE_LINEAR, 0.0f)
        fadeOut.setTexture(textureLibrary!!.allocateTexture(R.drawable.object_ruby01))
        fadeOut.setRenderComponent(render)
        thing.destroyOnDeactivation = true
        thing.add(lifetime)
        thing.add(fadeOut)
        thing.add(render)
        addStaticData(GameObjectType.GEM_EFFECT, thing, null)
        return thing
    }

    fun spawnGemEffectSpawner(positionX: Float, positionY: Float): GameObject? {
        val thing = gameObjectPool.allocate()
        thing!!.position[positionX] = positionY
        thing.activationRadius = tightActivationRadius
        thing.width = 1f
        thing.height = 1f
        val lifetime = allocateComponent(LifetimeComponent::class.java) as LifetimeComponent?
        lifetime!!.setTimeUntilDeath(0.5f)
        val gems = 6
        val angleIncrement = (2.0f * Math.PI).toFloat() / gems
        for (x in 0 until gems) {
            val gemGun = allocateComponent(LaunchProjectileComponent::class.java) as LaunchProjectileComponent?
            gemGun!!.setSetsPerActivation(1)
            gemGun.setShotsPerSet(1)
            gemGun.setDelayBetweenShots(0.0f)
            gemGun.setObjectTypeToSpawn(GameObjectType.GEM_EFFECT)
            gemGun.setVelocityX(sin(angleIncrement * x.toDouble()).toFloat() * 150.0f)
            gemGun.setVelocityY(cos(angleIncrement * x.toDouble()).toFloat() * 150.0f)
            gemGun.setOffsetX(16f)
            gemGun.setOffsetY(16f)
            thing.add(gemGun)
        }
        thing.life = 1
        thing.destroyOnDeactivation = true
        thing.add(lifetime)
        return thing
    }

    /** Comparator for game objects objects.  */
    private class ComponentPoolComparator : Comparator<GameComponentPool?> {
        override fun compare(object1: GameComponentPool?, object2: GameComponentPool?): Int {
            var result = 0
            if (object1 == null && object2 != null) {
                result = 1
            } else if (object1 != null && object2 == null) {
                result = -1
            } else if (object1 != null && object2 != null) {
                result = object1.objectClass.hashCode() - object2.objectClass.hashCode()
            }
            return result
        }
    }

    class GameObjectPool : TObjectPool<GameObject?> {
        constructor() : super() {}
        constructor(size: Int) : super(size) {}

        override fun fill() {
            for (x in 0 until fetchSize()) {
                fetchAvailable()!!.add(GameObject())
            }
        }

        override fun release(entry: Any) {
            (entry as GameObject).reset()
            super.release(entry)
        }
    }

    companion object {
        private const val MAX_GAME_OBJECTS = 384
        private val sComponentPoolComparator = ComponentPoolComparator()
        private const val sRedButtonChannel = "RED BUTTON"
        private const val sBlueButtonChannel = "BLUE BUTTON"
        private const val sGreenButtonChannel = "GREEN BUTTON"
        private const val sSurprisedNPCChannel = "SURPRISED"
    }

    init {
        gameObjectPool = GameObjectPool(MAX_GAME_OBJECTS)
        val objectTypeCount = GameObjectType.OBJECT_COUNT.ordinal
        staticBaseObjectArray = FixedSizeArray(objectTypeCount)
        for (x in 0 until objectTypeCount) {
            staticBaseObjectArray.add(null)
        }
        val context = sSystemRegistry.contextParameters
        val halfHeight2 = context!!.gameHeight * 0.5f * (context.gameHeight * 0.5f)
        val halfWidth2 = context.gameWidth * 0.5f * (context.gameWidth * 0.5f)
        val screenSizeRadius = sqrt(halfHeight2 + halfWidth2.toDouble()).toFloat()
        tightActivationRadius = screenSizeRadius + 128.0f
        normalActivationRadius = screenSizeRadius * 1.25f
        wideActivationRadius = screenSizeRadius * 2.0f
        alwaysActive = -1.0f

        // TODO: I wish there was a way to do this automatically, but the ClassLoader doesn't seem
        // to provide access to the currently loaded class list.  There's some discussion of walking
        // the actual class file objects and using forName() to instantiate them, but that sounds
        // really heavy-weight.  For now I'll rely on (sucky) manual enumeration.

        data class ComponentClass (
            var typeThing: Class<*>? = null,
            var poolSize: Int = 0
//            fun ComponentClass(classType: Class<*>?, size: Int) {
//                type = classType
//                poolSize = size
//            }
        )

        val componentTypes = arrayOf(
                ComponentClass(AnimationComponent::class.java, 1),
                ComponentClass(AttackAtDistanceComponent::class.java, 16),
                ComponentClass(BackgroundCollisionComponent::class.java, 192),
                ComponentClass(ButtonAnimationComponent::class.java, 32),
                ComponentClass(CameraBiasComponent::class.java, 8),
                ComponentClass(ChangeComponentsComponent::class.java, 256),
                ComponentClass(CrusherAndouComponent::class.java, 1),
                ComponentClass(DoorAnimationComponent::class.java, 256),  //!
                ComponentClass(DynamicCollisionComponent::class.java, 256),
                ComponentClass(EnemyAnimationComponent::class.java, 256),
                ComponentClass(FadeDrawableComponent::class.java, 32),
                ComponentClass(FixedAnimationComponent::class.java, 8),
                ComponentClass(FrameRateWatcherComponent::class.java, 1),
                ComponentClass(GenericAnimationComponent::class.java, 32),
                ComponentClass(GhostComponent::class.java, 256),
                ComponentClass(GravityComponent::class.java, 128),
                ComponentClass(HitPlayerComponent::class.java, 256),
                ComponentClass(HitReactionComponent::class.java, 256),
                ComponentClass(InventoryComponent::class.java, 128),
                ComponentClass(LauncherComponent::class.java, 16),
                ComponentClass(LaunchProjectileComponent::class.java, 128),
                ComponentClass(LifetimeComponent::class.java, 384),
                ComponentClass(MotionBlurComponent::class.java, 1),
                ComponentClass(MovementComponent::class.java, 128),
                ComponentClass(NPCAnimationComponent::class.java, 8),
                ComponentClass(NPCComponent::class.java, 8),
                ComponentClass(OrbitalMagnetComponent::class.java, 1),
                ComponentClass(PatrolComponent::class.java, 256),
                ComponentClass(PhysicsComponent::class.java, 8),
                ComponentClass(PlayerComponent::class.java, 1),
                ComponentClass(PlaySingleSoundComponent::class.java, 128),
                ComponentClass(PopOutComponent::class.java, 32),
                ComponentClass(RenderComponent::class.java, 384),
                ComponentClass(ScrollerComponent::class.java, 8),
                ComponentClass(SelectDialogComponent::class.java, 8),
                ComponentClass(SimpleCollisionComponent::class.java, 32),
                ComponentClass(SimplePhysicsComponent::class.java, 256),
                ComponentClass(SleeperComponent::class.java, 32),
                ComponentClass(SolidSurfaceComponent::class.java, 16),
                ComponentClass(SpriteComponent::class.java, 384),
                ComponentClass(TheSourceComponent::class.java, 1))
        componentPools = FixedSizeArray(componentTypes.size, sComponentPoolComparator)
        for (x in componentTypes.indices) {
            val component = componentTypes[x]
            componentPools.add(GameComponentPool(component.typeThing, component.poolSize))
        }
        componentPools.sort(true)
        poolSearchDummy = GameComponentPool(Any::class.java, 1)
    }
}
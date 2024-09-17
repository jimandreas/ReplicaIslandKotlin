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
@file:Suppress("unused")
package com.replica.replicaisland

import kotlin.math.floor

/**
 * A system for testing positions against "hot spots" embedded in the level tile map data.
 * A level may contain a layer of "hot spots," tiles that provide a hint to the game objects about
 * how to act in that particular area of the game world.  Hot spots are commonly used to direct AI
 * characters, or to define areas where special collision rules apply (e.g. regions that cause
 * instant death when entered).
 */
class HotSpotSystem : BaseObject() {
    private var mWorld: TiledWorld? = null

    object HotSpotType {
        const val NONE = -1
        const val GO_RIGHT = 0
        const val GO_LEFT = 1
        const val GO_UP = 2
        const val GO_DOWN = 3
        const val WAIT_SHORT = 4
        const val WAIT_MEDIUM = 5
        const val WAIT_LONG = 6
        const val ATTACK = 7
        const val TALK = 8
        const val DIE = 9
        const val WALK_AND_TALK = 10
        const val TAKE_CAMERA_FOCUS = 11
        const val RELEASE_CAMERA_FOCUS = 12
        const val END_LEVEL = 13
        const val GAME_EVENT = 14
        const val NPC_RUN_QUEUED_COMMANDS = 15
        const val NPC_GO_RIGHT = 16
        const val NPC_GO_LEFT = 17
        const val NPC_GO_UP = 18
        const val NPC_GO_DOWN = 19
        const val NPC_GO_UP_RIGHT = 20
        const val NPC_GO_UP_LEFT = 21
        const val NPC_GO_DOWN_LEFT = 22
        const val NPC_GO_DOWN_RIGHT = 23
        const val NPC_GO_TOWARDS_PLAYER = 24
        const val NPC_GO_RANDOM = 25
        const val NPC_GO_UP_FROM_GROUND = 26
        const val NPC_GO_DOWN_FROM_CEILING = 27
        const val NPC_STOP = 28
        const val NPC_SLOW = 29
        const val NPC_SELECT_DIALOG_1_1 = 32
        const val NPC_SELECT_DIALOG_1_2 = 33
        const val NPC_SELECT_DIALOG_1_3 = 34
        const val NPC_SELECT_DIALOG_1_4 = 35
        const val NPC_SELECT_DIALOG_1_5 = 36
        const val NPC_SELECT_DIALOG_2_1 = 38
        const val NPC_SELECT_DIALOG_2_2 = 39
        const val NPC_SELECT_DIALOG_2_3 = 40
        const val NPC_SELECT_DIALOG_2_4 = 41
        const val NPC_SELECT_DIALOG_2_5 = 42
    }

    override fun reset() {
        mWorld = null
    }

    fun setWorld(world: TiledWorld?) {
        mWorld = world
    }

    fun getHotSpot(worldX: Float, worldY: Float): Int {
        //TOOD: take a region?  how do we deal with multiple hot spot intersections?
        var result = HotSpotType.NONE
        if (mWorld != null) {
            val xTile = getHitTileX(worldX)
            val yTile = getHitTileY(worldY)
            result = mWorld!!.getTile(xTile, yTile)
        }
        return result
    }

    fun getHotSpotByTile(tileX: Int, tileY: Int): Int {
        //TOOD: take a region?  how do we deal with multiple hot spot intersections?
        var result = HotSpotType.NONE
        if (mWorld != null) {
            result = mWorld!!.getTile(tileX, tileY)
        }
        return result
    }

    fun getHitTileX(worldX: Float): Int {
        var xTile = 0
        val level = sSystemRegistry.levelSystem
        if (mWorld != null && level != null) {
            val worldPixelWidth = level.levelWidth
            xTile = floor(worldX / worldPixelWidth * mWorld!!.fetchWidth().toDouble()).toInt()
        }
        return xTile
    }

    fun getHitTileY(worldY: Float): Int {
        var yTile = 0
        val level = sSystemRegistry.levelSystem
        if (mWorld != null && level != null) {
            val worldPixelHeight = level.levelHeight
            // TODO: it is stupid to keep doing this space conversion all over the code.  Fix this
            // in the TiledWorld code!
            val flippedY = worldPixelHeight - worldY
            yTile = floor(flippedY / worldPixelHeight * mWorld!!.fetchHeight().toDouble()).toInt()
        }
        return yTile
    }

    fun getTileCenterWorldPositionX(tileX: Int): Float {
        var worldX = 0.0f
        val level = sSystemRegistry.levelSystem
        if (mWorld != null && level != null) {
            val tileWidth = level.levelWidth / mWorld!!.fetchWidth()
            worldX = tileX * tileWidth + tileWidth / 2.0f
        }
        return worldX
    }
}
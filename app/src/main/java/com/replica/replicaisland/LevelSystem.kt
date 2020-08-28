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
@file:Suppress("unused")

package com.replica.replicaisland

import android.content.res.AssetManager.AssetInputStream
import java.io.IOException
import java.io.InputStream

/**
 * Manages information about the current level, including setup, deserialization, and tear-down.
 */
class LevelSystem : BaseObject() {
    private var mWidthInTiles = 0
    private var mHeightInTiles = 0
    private var mTileWidth = 0
    private var mTileHeight = 0
    private var mBackgroundObject: GameObject? = null
    private var mRoot: ObjectManager? = null
    private val mWorkspaceBytes: ByteArray = ByteArray(4)
    private var mSpawnLocations: TiledWorld? = null
    private val mGameFlowEvent: GameFlowEvent = GameFlowEvent()
    var attemptsCount = 0
        private set
    var currentLevel: LevelTree.Level? = null
        private set

    override fun reset() {
        if (mBackgroundObject != null && mRoot != null) {
            mBackgroundObject!!.removeAll()
            mBackgroundObject!!.commitUpdates()
            mRoot!!.remove(mBackgroundObject)
            mBackgroundObject = null
            mRoot = null
        }
        mSpawnLocations = null
        attemptsCount = 0
        currentLevel = null
    }

    val levelWidth: Float
        get() = (mWidthInTiles * mTileWidth).toFloat()
    val levelHeight: Float
        get() = (mHeightInTiles * mTileHeight).toFloat()

    fun sendRestartEvent() {
        mGameFlowEvent.post(GameFlowEvent.EVENT_RESTART_LEVEL, 0,
                sSystemRegistry.contextParameters!!.context)
    }

    fun sendNextLevelEvent() {
        mGameFlowEvent.post(GameFlowEvent.EVENT_GO_TO_NEXT_LEVEL, 0,
                sSystemRegistry.contextParameters!!.context)
    }

    fun sendGameEvent(type: Int, index: Int, immediate: Boolean) {
        if (immediate) {
            mGameFlowEvent.postImmediate(type, index,
                    sSystemRegistry.contextParameters!!.context)
        } else {
            mGameFlowEvent.post(type, index,
                    sSystemRegistry.contextParameters!!.context)
        }
    }

    /**
     * Loads a level from a binary file.  The file consists of several layers, including background
     * tile layers and at most one collision layer.  Each layer is used to bootstrap related systems
     * and provide them with layer data.
     * @param stream  The input stream for the level file resource.
     * @return
     */
    fun loadLevel(level: LevelTree.Level?, stream: InputStream, root: ObjectManager): Boolean {
        val success = false
        currentLevel = level
        val byteStream = stream as AssetInputStream
        val signature: Int
        try {
            signature = byteStream.read()
            if (signature == 96) {
                val layerCount: Int = byteStream.read()
                val backgroundIndex: Int = byteStream.read()
                mRoot = root
                mTileWidth = 32
                mTileHeight = 32
                val params = sSystemRegistry.contextParameters
                var currentPriority = SortConstants.BACKGROUND_START + 1
                for (x in 0 until layerCount) {
                    val type: Int = byteStream.read()
                    val tileIndex: Int = byteStream.read()
                    byteStream.read(mWorkspaceBytes, 0, 4)
                    val scrollSpeed = Utils.byteArrayToFloat(mWorkspaceBytes)

                    // TODO: use a pool here?  Seems pointless.
                    val world = TiledWorld(byteStream)
                    if (type == 0) { // it's a background layer
                        //TODO 2 - fix assert(mWidthInTiles != 0)
                        //TODO 2 - fix assert(mTileWidth != 0)

                        // We require a collision layer to set up the tile sizes before we load.
                        // TODO: this really sucks.  there's no reason each layer can't have its
                        // own tile widths and heights.  Refactor this crap.
                        if (mWidthInTiles > 0 && mTileWidth > 0) {
                            val builder = sSystemRegistry.levelBuilder
                            if (mBackgroundObject == null) {
                                mBackgroundObject = builder!!.buildBackground(
                                        backgroundIndex,
                                        mWidthInTiles * mTileWidth,
                                        mHeightInTiles * mTileHeight)
                                root.add(mBackgroundObject!!)
                            }
                            builder!!.addTileMapLayer(mBackgroundObject!!, currentPriority,
                                    scrollSpeed, params!!.gameWidth, params.gameHeight,
                                    mTileWidth, mTileHeight, world, tileIndex)
                            currentPriority++
                        }
                    } else if (type == 1) { // collision
                        // Collision always defines the world boundaries.
                        mWidthInTiles = world.fetchWidth()
                        mHeightInTiles = world.fetchHeight()
                        val collision = sSystemRegistry.collisionSystem
                        collision?.initialize(world, mTileWidth, mTileHeight)
                    } else if (type == 2) { // objects
                        mSpawnLocations = world
                        spawnObjects()
                    } else if (type == 3) { // hot spots
                        val hotSpots = sSystemRegistry.hotSpotSystem
                        hotSpots?.setWorld(world)
                    }
                }

                // hack!
                sSystemRegistry.levelBuilder!!.promoteForegroundLayer(mBackgroundObject!!)
            }
        } catch (e: IOException) {
            //TODO: figure out the best way to deal with this.  Assert?
        }
        return success
    }

    fun spawnObjects() {
        val factory = sSystemRegistry.gameObjectFactory
        if (factory != null && mSpawnLocations != null) {
            DebugLog.d("LevelSystem", "Spawning Objects!")
            factory.spawnFromWorld(mSpawnLocations!!, mTileWidth, mTileHeight)
        }
    }

    fun incrementAttemptsCount() {
        attemptsCount++
    }

    init {
        reset()
    }
}
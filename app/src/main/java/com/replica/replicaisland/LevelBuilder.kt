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
@file:Suppress("UnnecessaryVariable")

package com.replica.replicaisland

import kotlin.math.max
import kotlin.math.min

class LevelBuilder : BaseObject() {
    override fun reset() {}
    fun buildBackground(backgroundImage: Int, levelWidth: Int, levelHeight: Int): GameObject {
        // Generate the scrolling background.
        val textureLibrary = sSystemRegistry.shortTermTextureLibrary
        val background = GameObject()
        if (textureLibrary != null) {
            var backgroundResource = -1
            when (backgroundImage) {
                BACKGROUND_SUNSET -> backgroundResource = R.drawable.background_sunset
                BACKGROUND_ISLAND -> backgroundResource = R.drawable.background_island
                BACKGROUND_SEWER -> backgroundResource = R.drawable.background_sewage
                BACKGROUND_UNDERGROUND -> backgroundResource = R.drawable.background_underground
                BACKGROUND_FOREST -> backgroundResource = R.drawable.background_grass2
                BACKGROUND_ISLAND2 -> backgroundResource = R.drawable.background_island2
                BACKGROUND_LAB -> backgroundResource = R.drawable.background_lab01
                //TODO 2 - fix asserts else -> assert(false)
            }
            if (backgroundResource > -1) {

                // Background Layer //
                val backgroundRender = RenderComponent()
                backgroundRender.priority = SortConstants.BACKGROUND_START
                val params = sSystemRegistry.contextParameters
                // The background image is ideally 1.5 times the size of the largest screen axis
                // (normally the width, but just in case, let's calculate it).
                val idealSize = max(params!!.gameWidth * 1.5f, params.gameHeight * 1.5f).toInt()
                val scroller3 = ScrollerComponent(0.0f, 0.0f, idealSize, idealSize,
                        textureLibrary.allocateTexture(backgroundResource))
                scroller3.setRenderComponent(backgroundRender)

                // Scroll speeds such that the background will evenly match the beginning
                // and end of the level.  Don't allow speeds > 1.0, though; that would be faster than
                // the foreground, which is disorienting and looks like rotation.
                val scrollSpeedX = min((idealSize - params.gameWidth).toFloat() / (levelWidth - params.gameWidth), 1.0f)
                val scrollSpeedY = min((idealSize - params.gameHeight).toFloat() / (levelHeight - params.gameHeight), 1.0f)
                scroller3.setScrollSpeed(scrollSpeedX, scrollSpeedY)
                backgroundRender.setCameraRelative(false)
                background.add(scroller3)
                background.add(backgroundRender)
            }
        }
        return background
    }

    fun addTileMapLayer(background: GameObject, priorityIn: Int, scrollSpeed: Float,
                        width: Int, height: Int, tileWidth: Int, tileHeight: Int, world: TiledWorld,
                        theme: Int) {
        var priority = priorityIn
        var tileMapIndex = 0
        when (theme) {
            THEME_GRASS -> tileMapIndex = R.drawable.grass
            THEME_ISLAND -> tileMapIndex = R.drawable.island
            THEME_SEWER -> tileMapIndex = R.drawable.sewage
            THEME_UNDERGROUND -> tileMapIndex = R.drawable.cave
            THEME_LAB -> tileMapIndex = R.drawable.lab
            THEME_LIGHTING -> {
                tileMapIndex = R.drawable.titletileset
                priority = SortConstants.OVERLAY //hack!
            }
            THEME_TUTORIAL -> tileMapIndex = R.drawable.tutorial
            //TODO 2 - fix asserts else -> assert(false)
        }
        val backgroundRender = RenderComponent()
        backgroundRender.priority = priority

        //Vertex Buffer Code
        val textureLibrary = sSystemRegistry.shortTermTextureLibrary
        val bg = TiledVertexGrid(textureLibrary!!.allocateTexture(tileMapIndex),
                width, height, tileWidth, tileHeight)
        bg.setWorld(world)

        //TODO: The map format should really just output independent speeds for x and y,
        // but as a short term solution we can assume parallax layers lock in the smaller
        // direction of movement.
        var xScrollSpeed = 1.0f
        var yScrollSpeed = 1.0f
        if (world.fetchWidth() > world.fetchHeight()) {
            xScrollSpeed = scrollSpeed
        } else {
            yScrollSpeed = scrollSpeed
        }
        val scroller = ScrollerComponent(xScrollSpeed, yScrollSpeed,
                width, height, bg)
        scroller.setRenderComponent(backgroundRender)
        background.add(scroller)
        background.add(backgroundRender)
        backgroundRender.setCameraRelative(false)
    }

    // This method is a HACK to workaround the stupid map file format.
    // We want the foreground layer to be render priority FOREGROUND, but
    // we don't know which is the foreground layer until we've added them all.
    // So now that we've added them all, find foreground layer and make sure
    // its render priority is set.
    fun promoteForegroundLayer(backgroundObject: GameObject) {
        backgroundObject.commitUpdates() // Make sure layers are sorted.
        val componentCount = backgroundObject.fetchCount()
        for (x in componentCount - 1 downTo 0) {
            val component = backgroundObject.fetch(x) as GameComponent
            if (component is RenderComponent) {
                val render = component
                if (render.priority != SortConstants.OVERLAY) {
                    // found it.
                    render.priority = SortConstants.FOREGROUND
                    break
                }
            }
        }
    }

    companion object {
        private const val THEME_GRASS = 0
        private const val THEME_ISLAND = 1
        private const val THEME_SEWER = 2
        private const val THEME_UNDERGROUND = 3
        private const val THEME_LAB = 4
        private const val THEME_LIGHTING = 5
        private const val THEME_TUTORIAL = 6
        private const val BACKGROUND_SUNSET = 0
        private const val BACKGROUND_ISLAND = 1
        private const val BACKGROUND_SEWER = 2
        private const val BACKGROUND_UNDERGROUND = 3
        private const val BACKGROUND_FOREST = 4
        private const val BACKGROUND_ISLAND2 = 5
        private const val BACKGROUND_LAB = 6
    }
}
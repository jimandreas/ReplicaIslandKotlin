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
@file:Suppress("SENSELESS_COMPARISON")

package com.replica.replicaisland

/**
 * This class manages drawable objects that have short lifetimes (one or two frames).  It provides
 * type-specific allocator functions and a type-insensitive release function.  This class manages
 * pools of objects so no actual allocations occur after bootstrap.
 */
class DrawableFactory : BaseObject() {
    private val bitmapPool: DrawableBitmapPool
    private val scrollableBitmapPool: ScrollableBitmapPool
    private val tiledBackgroundVertexGridPool: TiledBackgroundVertexGridPool
    override fun reset() {}
    fun allocateDrawableBitmap(): DrawableBitmap {
        return bitmapPool.allocate()
    }

    fun allocateTiledBackgroundVertexGrid(): TiledBackgroundVertexGrid? {
        return tiledBackgroundVertexGridPool.allocate()
    }

    fun allocateScrollableBitmap(): ScrollableBitmap? {
        return scrollableBitmapPool.allocate()
    }

    fun release(`object`: DrawableObject) {
        val pool = `object`.parentPool
        pool?.release(`object`)
        // Objects with no pool weren't created by this factory.  Ignore them.
    }

    private inner class DrawableBitmapPool(size: Int) : TObjectPool<DrawableBitmap?>(size) {
        override fun reset() {}
        override fun fill() {
            val size = fetchSize()
            for (x in 0 until size) {
                val entry = DrawableBitmap(null, 0, 0)
                entry.parentPool = this
                fetchAvailable()!!.add(entry)
            }
        }

        override fun release(entry: Any) {
            (entry as DrawableBitmap).reset()
            super.release(entry)
        }

        override fun allocate(): DrawableBitmap {
            val result = super.allocate()!!
            val params = sSystemRegistry.contextParameters
            if (result != null && params != null) {
                result.setViewSize(params.gameWidth, params.gameHeight)
            }
            return result
        }
    }

    private inner class ScrollableBitmapPool : TObjectPool<ScrollableBitmap?>() {
        override fun reset() {}
        override fun fill() {
            val size = fetchSize()
            for (x in 0 until size) {
                val entry = ScrollableBitmap(null, 0, 0)
                entry.parentPool = this
                fetchAvailable()!!.add(entry)
            }
        }

        override fun release(entry: Any) {
            (entry as ScrollableBitmap).reset()
            super.release(entry)
        }
    }

    private inner class TiledBackgroundVertexGridPool : TObjectPool<TiledBackgroundVertexGrid?>() {
        override fun reset() {}
        override fun fill() {
            val size = fetchSize()
            for (x in 0 until size) {
                val entry = TiledBackgroundVertexGrid()
                entry.parentPool = this
                fetchAvailable()!!.add(entry)
            }
        }

        override fun release(entry: Any) {
            val bg = entry as TiledBackgroundVertexGrid
            bg.reset()
            super.release(entry)
        }
    }

    companion object {
        private const val BITMAP_POOL_SIZE = 768
    }

    // This class wraps several object pools and provides a type-sensitive release function.
    init {
        bitmapPool = DrawableBitmapPool(BITMAP_POOL_SIZE)
        tiledBackgroundVertexGridPool = TiledBackgroundVertexGridPool()
        scrollableBitmapPool = ScrollableBitmapPool()
    }
}
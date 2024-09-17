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
package com.replica.replicaisland

import javax.microedition.khronos.opengles.GL10

class BufferLibrary : BaseObject() {
    private val gridList: FixedSizeArray<Grid>
    override fun reset() {
        removeAll()
    }

    fun add(grid: Grid) {
        gridList.add(grid)
    }

    fun removeAll() {
        gridList.clear()
    }

    fun generateHardwareBuffers(gl: GL10?) {
        if (sSystemRegistry.contextParameters!!.supportsVBOs) {
            val count = gridList.count
            for (x in 0 until count) {
                val grid = gridList[x]
                grid!!.generateHardwareBuffers(gl)
            }
        }
    }

    fun releaseHardwareBuffers(gl: GL10?) {
        if (sSystemRegistry.contextParameters!!.supportsVBOs) {
            val count = gridList.count
            for (x in 0 until count) {
                val grid = gridList[x]
                grid!!.releaseHardwareBuffers(gl)
            }
        }
    }

    fun invalidateHardwareBuffers() {
        if (sSystemRegistry.contextParameters!!.supportsVBOs) {
            val count = gridList.count
            for (x in 0 until count) {
                val grid = gridList[x]
                grid!!.invalidateHardwareBuffers()
            }
        }
    }

    companion object {
        private const val GRID_LIST_SIZE = 256
    }

    init {
        gridList = FixedSizeArray(GRID_LIST_SIZE)
    }
}
package com.replica.replicaisland.utils

import com.replica.replicaisland.Grid
import com.replica.replicaisland.core.BaseObject
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
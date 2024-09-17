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
@file:Suppress("unused", "ConvertTwoComparisonsToRangeCheck")

package com.replica.replicaisland

import android.content.res.AssetManager.AssetInputStream
import com.replica.replicaisland.Utils.Companion.byteArrayToInt
import java.io.IOException
import java.io.InputStream

/**
 * TiledWorld manages a 2D map of tile indexes that define a "world" of tiles.  These may be
 * foreground or background layers in a scrolling game, or a layer of collision tiles, or some other
 * type of tile map entirely.  The TiledWorld maps xy positions to tile indices and also handles
 * deserialization of tilemap files.
 */
class TiledWorld : AllocationGuard {
    private lateinit var tilesArray: Array<IntArray>
    private var rowCount = 0
    private var colCount = 0
    private var workspaceBytes: ByteArray

    constructor(cols: Int, rows: Int) : super() {
        tilesArray = Array(cols) { IntArray(rows) }
        rowCount = rows
        colCount = cols
        for (x in 0 until cols) {
            for (y in 0 until rows) {
                tilesArray[x][y] = -1
            }
        }
        workspaceBytes = ByteArray(4)
        calculateSkips()
    }

    constructor(stream: InputStream) : super() {
        workspaceBytes = ByteArray(4)
        parseInput(stream)
        calculateSkips()
    }

    fun getTile(x: Int, y: Int): Int {
        var result = -1
        if (x >= 0 && x < colCount && y >= 0 && y < rowCount) {
            result = tilesArray[x][y]
        }
        return result
    }

    // Builds a tiled world from a simple map file input source.  The map file format is as follows:
    // First byte: signature.  Must always be decimal 42.
    // Second byte: width of the world in tiles.
    // Third byte: height of the world in tiles.
    // Subsequent bytes: actual tile data in column-major order.
    // TODO: add a checksum in here somewhere.
    private fun parseInput(stream: InputStream): Boolean {
        var success = false
        val byteStream = stream as AssetInputStream
        val signature: Int
        try {
            signature = byteStream.read()
            if (signature == 42) {
                byteStream.read(workspaceBytes, 0, 4)
                val width = byteArrayToInt(workspaceBytes)
                byteStream.read(workspaceBytes, 0, 4)
                val height = byteArrayToInt(workspaceBytes)
                val totalTiles = width * height
                val bytesRemaining = byteStream.available()
                //TODO 2 fix: assert(bytesRemaining >= totalTiles)
                if (bytesRemaining >= totalTiles) {
                    tilesArray = Array(width) { IntArray(height) }
                    rowCount = height
                    colCount = width
                    for (y in 0 until height) {
                        for (x in 0 until width) {
                            // there is probably a better way to do this byte-sign extension
                            var byteData = byteStream.read()
                            if (byteData > 127) {
                                byteData -= 256
                            }
                            tilesArray[x][y] = byteData
                        }
                    }
                    success = true
                }
            }
        } catch (e: IOException) {
            //TODO: figure out the best way to deal with this.  Assert?
        }
        return success
    }

    private fun calculateSkips() {
        var emptyTileCount = 0
        for (y in rowCount - 1 downTo 0) {
            for (x in colCount - 1 downTo 0) {
                if (tilesArray[x][y] < 0) {
                    emptyTileCount++
                    tilesArray[x][y] = -emptyTileCount
                } else {
                    emptyTileCount = 0
                }
            }
        }
    }

    fun fetchWidth(): Int {
        return colCount
    }

    fun fetchHeight(): Int {
        return rowCount
    }

    fun fetchTiles(): Array<IntArray> {
        return tilesArray
    }
}
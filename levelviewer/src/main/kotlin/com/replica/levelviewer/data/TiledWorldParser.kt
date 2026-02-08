package com.replica.levelviewer.data

import java.io.InputStream

object TiledWorldParser {
    private const val SIGNATURE = 42

    data class TiledWorldResult(
        val width: Int,
        val height: Int,
        val tiles: Array<IntArray>
    )

    fun parse(stream: InputStream): TiledWorldResult {
        val signature = stream.read()
        require(signature == SIGNATURE) { "Invalid TiledWorld signature: $signature (expected $SIGNATURE)" }

        val width = BinaryUtils.readLEInt(stream)
        val height = BinaryUtils.readLEInt(stream)
        val tiles = Array(width) { IntArray(height) }

        for (y in 0 until height) {
            for (x in 0 until width) {
                var byteData = stream.read()
                if (byteData > 127) {
                    byteData -= 256
                }
                tiles[x][y] = byteData
            }
        }

        return TiledWorldResult(width, height, tiles)
    }
}

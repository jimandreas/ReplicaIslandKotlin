package com.replica.levelviewer.data

import com.replica.levelviewer.model.LevelData
import com.replica.levelviewer.model.TileLayer
import java.io.InputStream

object LevelFileParser {
    private const val SIGNATURE = 96

    fun parse(stream: InputStream): LevelData {
        val signature = stream.read()
        require(signature == SIGNATURE) { "Invalid level signature: $signature (expected $SIGNATURE)" }

        val layerCount = stream.read()
        val backgroundIndex = stream.read()

        val layers = mutableListOf<TileLayer>()
        for (i in 0 until layerCount) {
            val type = stream.read()
            val themeIndex = stream.read()
            val scrollSpeed = BinaryUtils.readLEFloat(stream)
            val world = TiledWorldParser.parse(stream)

            layers.add(
                TileLayer(
                    type = type,
                    themeIndex = themeIndex,
                    scrollSpeed = scrollSpeed,
                    width = world.width,
                    height = world.height,
                    tiles = world.tiles
                )
            )
        }

        return LevelData(backgroundIndex, layers)
    }
}

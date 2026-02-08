package com.replica.levelviewer.data

import com.replica.levelviewer.model.CollisionData
import com.replica.levelviewer.model.LineSegment
import java.io.InputStream

object CollisionParser {
    private const val SIGNATURE = 52

    fun parse(stream: InputStream): CollisionData {
        val signature = stream.read()
        require(signature == SIGNATURE) { "Invalid collision signature: $signature (expected $SIGNATURE)" }

        val tileCount = stream.read()
        val tiles = mutableMapOf<Int, List<LineSegment>>()

        for (i in 0 until tileCount) {
            val tileIndex = stream.read()
            val segmentCount = stream.read()
            val segments = mutableListOf<LineSegment>()

            for (j in 0 until segmentCount) {
                val startX = BinaryUtils.readLEFloat(stream)
                val startY = BinaryUtils.readLEFloat(stream)
                val endX = BinaryUtils.readLEFloat(stream)
                val endY = BinaryUtils.readLEFloat(stream)
                val normalX = BinaryUtils.readLEFloat(stream)
                val normalY = BinaryUtils.readLEFloat(stream)
                segments.add(LineSegment(startX, startY, endX, endY, normalX, normalY))
            }

            if (segments.isNotEmpty()) {
                tiles[tileIndex] = segments
            }
        }

        return CollisionData(tiles)
    }
}

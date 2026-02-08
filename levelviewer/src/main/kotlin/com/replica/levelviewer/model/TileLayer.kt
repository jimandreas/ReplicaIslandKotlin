package com.replica.levelviewer.model

data class TileLayer(
    val type: Int,
    val themeIndex: Int,
    val scrollSpeed: Float,
    val width: Int,
    val height: Int,
    val tiles: Array<IntArray>
) {
    companion object {
        const val TYPE_BACKGROUND = 0
        const val TYPE_COLLISION = 1
        const val TYPE_OBJECTS = 2
        const val TYPE_HOTSPOTS = 3
    }
}

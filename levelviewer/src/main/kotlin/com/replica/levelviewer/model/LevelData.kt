package com.replica.levelviewer.model

data class LevelData(
    val backgroundIndex: Int,
    val layers: List<TileLayer>
) {
    val backgroundLayers: List<TileLayer> get() = layers.filter { it.type == TileLayer.TYPE_BACKGROUND }
    val collisionLayer: TileLayer? get() = layers.firstOrNull { it.type == TileLayer.TYPE_COLLISION }
    val objectLayer: TileLayer? get() = layers.firstOrNull { it.type == TileLayer.TYPE_OBJECTS }
    val hotspotLayer: TileLayer? get() = layers.firstOrNull { it.type == TileLayer.TYPE_HOTSPOTS }
}

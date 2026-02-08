package com.replica.levelviewer.model

data class LineSegment(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val normalX: Float,
    val normalY: Float
)

data class CollisionData(
    val tiles: Map<Int, List<LineSegment>>
)

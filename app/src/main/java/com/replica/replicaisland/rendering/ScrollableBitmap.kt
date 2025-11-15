package com.replica.replicaisland.rendering

/**
 * Implements a bitmap that can be scrolled in place, such as the background of a scrolling
 * world.
 */
open class ScrollableBitmap(texture: Texture?, width: Int, height: Int) : DrawableBitmap(texture, width, height) {
    var scrollOriginX = 0f
    var scrollOriginY = 0f
    fun setScrollOrigin(x: Float, y: Float) {
        scrollOriginX = x
        scrollOriginY = y
    }

    override fun draw(x: Float, y: Float, scaleX: Float, scaleY: Float) {
        super.draw(x - scrollOriginX, y - scrollOriginY, scaleX, scaleY)
    }
}
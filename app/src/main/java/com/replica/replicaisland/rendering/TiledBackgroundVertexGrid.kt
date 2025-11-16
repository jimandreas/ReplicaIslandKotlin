package com.replica.replicaisland.rendering

import com.replica.replicaisland.rendering.ScrollableBitmap

class TiledBackgroundVertexGrid : ScrollableBitmap(null, 0, 0) {
    private var mGrid: TiledVertexGrid? = null
    override fun reset() {
        super.reset()
        mGrid = null
    }

    fun setGrid(grid: TiledVertexGrid?) {
        mGrid = grid
    }

    override fun draw(x: Float, y: Float, scaleX: Float, scaleY: Float) {
        if (mGrid != null) {
            mGrid!!.draw(x, y, scrollOriginX, scrollOriginY)
        }
    }
}
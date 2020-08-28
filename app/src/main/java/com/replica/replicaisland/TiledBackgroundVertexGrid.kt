/*
 * Copyright (C) 2010 The Android Open Source Project
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
package com.replica.replicaisland

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
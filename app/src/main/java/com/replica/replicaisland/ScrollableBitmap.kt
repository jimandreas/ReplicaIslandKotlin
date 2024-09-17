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
package com.replica.replicaisland

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
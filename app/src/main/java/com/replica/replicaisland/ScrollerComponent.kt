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
@file:Suppress("unused")

package com.replica.replicaisland

/**
 * Adjusts the scroll position of a drawable object based on the camera's focus position.
 * May be used to scroll a ScrollableBitmap or TiledWorld to match the camera.  Uses DrawableFactory
 * to allocate fire-and-forget drawable objects every frame.
 */
class ScrollerComponent : GameComponent {
    private var mWidth = 0
    private var mHeight = 0
    private var halfWidth = 0f
    private var halfHeight = 0f
    private var renderComponent: RenderComponent? = null
    private var mSpeedX = 0f
    private var mSpeedY = 0f
    private var mTexture: Texture? = null
    private var vertGrid: TiledVertexGrid? = null

    constructor(speedX: Float, speedY: Float, width: Int, height: Int, texture: Texture?) : super() {
        reset()
        setup(speedX, speedY, width, height)
        setUseTexture(texture)
        setPhaseToThis(ComponentPhases.PRE_DRAW.ordinal)
    }

    constructor(speedX: Float, speedY: Float, width: Int, height: Int, grid: TiledVertexGrid?) : super() {
        reset()
        setup(speedX, speedY, width, height)
        vertGrid = grid
        setPhaseToThis(ComponentPhases.PRE_DRAW.ordinal)
    }

    constructor() : super() {
        reset()
        setPhaseToThis(ComponentPhases.PRE_DRAW.ordinal)
    }

    override fun reset() {
        mWidth = 0
        mHeight = 0
        halfWidth = 0.0f
        halfHeight = 0.0f
        renderComponent = null
        mSpeedX = 0.0f
        mSpeedY = 0.0f
        mTexture = null
        vertGrid = null
    }

    fun setScrollSpeed(speedX: Float, speedY: Float) {
        mSpeedX = speedX
        mSpeedY = speedY
    }

    fun setup(speedX: Float, speedY: Float, width: Int, height: Int) {
        mSpeedX = speedX
        mSpeedY = speedY
        mWidth = width
        mHeight = height
        halfWidth = sSystemRegistry.contextParameters!!.gameWidth / 2.0f //width / 2.0f;
        halfHeight = sSystemRegistry.contextParameters!!.gameHeight / 2.0f //height / 2.0f;
    }

    private fun setUseTexture(texture: Texture?) {
        mTexture = texture
    }

    override fun update(timeDelta: Float, parent: BaseObject?) {
        val drawableFactory = sSystemRegistry.drawableFactory
        if (renderComponent != null && drawableFactory != null) {
            val background: ScrollableBitmap?
            if (vertGrid != null) {
                val bg = drawableFactory.allocateTiledBackgroundVertexGrid()
                bg!!.setGrid(vertGrid)
                background = bg
            } else {
                background = drawableFactory.allocateScrollableBitmap()
                background!!.texture = mTexture
            }
            background.width = mWidth
            background.height = mHeight
            val camera = sSystemRegistry.cameraSystem
            var originX = camera!!.fetchFocusPositionX() - halfWidth
            var originY = camera.fetchFocusPositionY() - halfHeight
            originX *= mSpeedX
            originY *= mSpeedY
            background.setScrollOrigin(originX, originY)
            renderComponent!!.drawable = background
        }
    }

    fun setRenderComponent(render: RenderComponent?) {
        renderComponent = render
    }
}
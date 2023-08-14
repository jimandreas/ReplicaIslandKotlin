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
@file:Suppress("SENSELESS_COMPARISON")

package com.replica.replicaisland

/**
 * Provides an interface for controlling a sprite with animations.  Manages a list of animations
 * and provides a drawable surface with the correct animation frame to a render component each
 * frame.  Also manages horizontal and vertical flipping.
 */
class SpriteComponent : GameComponent {
    private var mAnimations: PhasedObjectManager
    var currentAnimationTime = 0f
    var currentAnimation = 0
        private set
    private var mWidth = 0
    private var mHeight = 0
    private var mOpacity = 0f
    private var renderComponent: RenderComponent? = null
    private var collisionComponent: DynamicCollisionComponent? = null
    var visible = false
    private var mCurrentAnimation: SpriteAnimation? = null
    private var animationsDirty = false

    constructor(width: Int, height: Int) : super() {
        mAnimations = PhasedObjectManager()
        reset()
        mWidth = width
        mHeight = height
        setPhaseToThis(ComponentPhases.PRE_DRAW.ordinal)
    }

    constructor() : super() {
        mAnimations = PhasedObjectManager()
        reset()
        setPhaseToThis(ComponentPhases.PRE_DRAW.ordinal)
    }

    override fun reset() {
        mWidth = 0
        mHeight = 0
        visible = true
        currentAnimation = -1
        mAnimations.removeAll()
        mAnimations.commitUpdates()
        currentAnimationTime = 0.0f
        renderComponent = null
        collisionComponent = null
        mCurrentAnimation = null
        mOpacity = 1.0f
        animationsDirty = false
    }

    override fun update(timeDelta: Float, parent: BaseObject?) {
        currentAnimationTime += timeDelta
        val animations = mAnimations
        val currentAnimIndex = currentAnimation
        if (animationsDirty) {
            animations.commitUpdates()
            animationsDirty = false
        }
        var validFrameAvailable = false
        if (animations.fetchCount() > 0 && currentAnimIndex != -1) {
            var currentAnimation = mCurrentAnimation
            if (currentAnimation == null && currentAnimIndex != -1) {
                currentAnimation = findAnimation(currentAnimIndex)
                if (currentAnimation == null) {
                    // We were asked to play an animation that doesn't exist.  Revert to our
                    // default animation.
                    // TODO: throw an assert here?
                    mCurrentAnimation = animations.fetch(0) as SpriteAnimation
                    currentAnimation = mCurrentAnimation
                } else {
                    mCurrentAnimation = currentAnimation
                }
            }
            val parentObject = parent as GameObject
            val currentFrame = currentAnimation!!.getFrame(currentAnimationTime)
            if (currentFrame != null) {
                validFrameAvailable = true
                val render = renderComponent
                if (render != null) {
                    val factory = sSystemRegistry.drawableFactory
                    if (visible && currentFrame.texture != null && factory != null) {
                        // Fire and forget.  Allocate a new bitmap for this animation frame, set it up, and
                        // pass it off to the render component for drawing.
                        val bitmap = factory.allocateDrawableBitmap()
                        bitmap.width = mWidth
                        bitmap.height = mHeight
                        bitmap.setOpacity(mOpacity)
                        updateFlip(bitmap, parentObject.facingDirection.x < 0.0f,
                                parentObject.facingDirection.y < 0.0f)
                        bitmap.texture = currentFrame.texture
                        render.drawable = bitmap
                    } else {
                        render.drawable = null
                    }
                }
                if (collisionComponent != null) {
                    collisionComponent!!.setCollisionVolumes(currentFrame.attackVolumes,
                            currentFrame.vulnerabilityVolumes)
                }
            }
        }
        if (!validFrameAvailable) {
            // No current frame = draw nothing!
            if (renderComponent != null) {
                renderComponent!!.drawable = null
            }
            if (collisionComponent != null) {
                collisionComponent!!.setCollisionVolumes(null, null)
            }
        }
    }

    fun playAnimation(index: Int) {
        if (currentAnimation != index) {
            currentAnimationTime = 0f
            currentAnimation = index
            mCurrentAnimation = null
        }
    }

    fun findAnimation(index: Int): SpriteAnimation? {
        if (mAnimations.find(index) == null) {
            return null
        }
        return mAnimations.find(index) as SpriteAnimation
    }

    fun addAnimation(anim: SpriteAnimation?) {
        mAnimations.add(anim as BaseObject)
        animationsDirty = true
    }

    fun animationFinished(): Boolean {
        var result = false
        if (mCurrentAnimation != null && !mCurrentAnimation!!.loop
                && currentAnimationTime > mCurrentAnimation!!.length) {
            result = true
        }
        return result
    }

    val width: Float
        get() = mWidth.toFloat()
    val height: Float
        get() = mHeight.toFloat()

    fun setSize(width: Int, height: Int) {
        mWidth = width
        mHeight = height
    }

    private fun updateFlip(bitmap: DrawableBitmap, horzFlip: Boolean, vertFlip: Boolean) {
        bitmap.setFlip(horzFlip, vertFlip)
    }

    fun setRenderComponent(component: RenderComponent?) {
        renderComponent = component
    }

    fun setCollisionComponent(component: DynamicCollisionComponent?) {
        collisionComponent = component
    }

    fun setOpacity(opacity: Float) {
        mOpacity = opacity
    }

    val animationCount: Int
        get() = mAnimations.fetchConcreteCount()
}
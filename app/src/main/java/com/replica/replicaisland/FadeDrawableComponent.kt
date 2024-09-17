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

import com.replica.replicaisland.Lerp.ease
import com.replica.replicaisland.Lerp.lerp

class FadeDrawableComponent : GameComponent() {
    private var mTexture: Texture? = null
    private var renderComponent: RenderComponent? = null
    private var initialOpacity = 0f
    private var targetOpacity = 0f
    private var startTime = 0f
    private var mDuration = 0f
    private var mLoopType = 0
    private var mFunction = 0
    private var mInitialDelay = 0f
    private var initialDelayTimer = 0f
    private var activateTime = 0f
    private var phaseDuration = 0f
    override fun reset() {
        mTexture = null
        renderComponent = null
        initialOpacity = 0.0f
        targetOpacity = 0.0f
        mDuration = 0.0f
        mLoopType = LOOP_TYPE_NONE
        mFunction = FADE_LINEAR
        startTime = 0.0f
        mInitialDelay = 0.0f
        activateTime = 0.0f
        phaseDuration = 0.0f
        initialDelayTimer = 0.0f
    }

    override fun update(timeDelta: Float, parent: BaseObject?) {
        if (renderComponent != null) {
            val time = sSystemRegistry.timeSystem
            val currentTime = time!!.gameTime

            // Support repeating "phases" on top of the looping fade itself.
            // Complexity++, but it lets this component handle several
            // different use cases.
            if (activateTime == 0.0f) {
                activateTime = currentTime
                initialDelayTimer = mInitialDelay
            } else if (phaseDuration > 0.0f && currentTime - activateTime > phaseDuration) {
                activateTime = currentTime
                initialDelayTimer = mInitialDelay
                startTime = 0.0f
            }
            if (initialDelayTimer > 0.0f) {
                initialDelayTimer -= timeDelta
            } else {
                if (startTime == 0f) {
                    startTime = currentTime
                }
                var elapsed = currentTime - startTime
                var opacity = initialOpacity
                if (mLoopType != LOOP_TYPE_NONE && elapsed > mDuration) {
                    val endTime = startTime + mDuration
                    elapsed = endTime - currentTime
                    startTime = endTime
                    if (mLoopType == LOOP_TYPE_PING_PONG) {
                        val temp = initialOpacity
                        initialOpacity = targetOpacity
                        targetOpacity = temp
                    }
                }
                if (elapsed > mDuration) {
                    opacity = targetOpacity
                } else if (elapsed != 0.0f) {
                    if (mFunction == FADE_LINEAR) {
                        opacity = lerp(initialOpacity, targetOpacity, mDuration, elapsed)
                    } else if (mFunction == FADE_EASE) {
                        opacity = ease(initialOpacity, targetOpacity, mDuration, elapsed)
                    }
                }
                if (mTexture != null) {
                    // If a texture is set then we supply a drawable to the render component.
                    // If not, we take whatever drawable the renderer already has.
                    val factory = sSystemRegistry.drawableFactory
                    if (factory != null) {
                        val parentObject = parent as GameObject
                        val bitmap = factory.allocateDrawableBitmap()
                        bitmap.resize(mTexture!!.width, mTexture!!.height)
                        //TODO: Super tricky scale.  fix this!
                        bitmap.width = parentObject.width.toInt()
                        bitmap.height = parentObject.height.toInt()
                        bitmap.setOpacity(opacity)
                        bitmap.texture = mTexture
                        renderComponent!!.drawable = bitmap
                    }
                } else {
                    val drawable = renderComponent!!.drawable
                    // TODO: ack, instanceof!  Fix this!
                    if (drawable != null && drawable is DrawableBitmap) {
                        drawable.setOpacity(opacity)
                    }
                }
            }
        }
    }

    fun setupFade(startOpacity: Float, endOpacity: Float, duration: Float, loopType: Int, function: Int, initialDelay: Float) {
        initialOpacity = startOpacity
        targetOpacity = endOpacity
        mDuration = duration
        mLoopType = loopType
        mFunction = function
        mInitialDelay = initialDelay
    }

    /** Enables phases; the initial delay will be re-started when the phase ends.  */
    fun setPhaseDuration(duration: Float) {
        phaseDuration = duration
    }

    /** If set to something non-null, this component will overwrite the drawable on the target render component.  */
    fun setTexture(texture: Texture?) {
        mTexture = texture
    }

    fun setRenderComponent(component: RenderComponent?) {
        renderComponent = component
    }

    fun resetPhase() {
        activateTime = 0.0f
    }

    companion object {
        const val LOOP_TYPE_NONE = 0
        const val LOOP_TYPE_LOOP = 1
        const val LOOP_TYPE_PING_PONG = 2
        const val FADE_LINEAR = 0
        const val FADE_EASE = 1
    }

    init {
        setPhaseToThis(ComponentPhases.PRE_DRAW.ordinal)
    }
}
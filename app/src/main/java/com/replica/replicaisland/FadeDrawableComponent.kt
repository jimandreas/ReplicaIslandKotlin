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
@file:Suppress("unused")

package com.replica.replicaisland

import com.replica.replicaisland.Lerp.ease
import com.replica.replicaisland.Lerp.lerp

class FadeDrawableComponent : GameComponent() {
    private var mTexture: Texture? = null
    private var mRenderComponent: RenderComponent? = null
    private var mInitialOpacity = 0f
    private var mTargetOpacity = 0f
    private var mStartTime = 0f
    private var mDuration = 0f
    private var mLoopType = 0
    private var mFunction = 0
    private var mInitialDelay = 0f
    private var mInitialDelayTimer = 0f
    private var mActivateTime = 0f
    private var mPhaseDuration = 0f
    override fun reset() {
        mTexture = null
        mRenderComponent = null
        mInitialOpacity = 0.0f
        mTargetOpacity = 0.0f
        mDuration = 0.0f
        mLoopType = LOOP_TYPE_NONE
        mFunction = FADE_LINEAR
        mStartTime = 0.0f
        mInitialDelay = 0.0f
        mActivateTime = 0.0f
        mPhaseDuration = 0.0f
        mInitialDelayTimer = 0.0f
    }

    override fun update(timeDelta: Float, parent: BaseObject?) {
        if (mRenderComponent != null) {
            val time = sSystemRegistry.timeSystem
            val currentTime = time!!.gameTime

            // Support repeating "phases" on top of the looping fade itself.
            // Complexity++, but it lets this component handle several
            // different use cases.
            if (mActivateTime == 0.0f) {
                mActivateTime = currentTime
                mInitialDelayTimer = mInitialDelay
            } else if (mPhaseDuration > 0.0f && currentTime - mActivateTime > mPhaseDuration) {
                mActivateTime = currentTime
                mInitialDelayTimer = mInitialDelay
                mStartTime = 0.0f
            }
            if (mInitialDelayTimer > 0.0f) {
                mInitialDelayTimer -= timeDelta
            } else {
                if (mStartTime == 0f) {
                    mStartTime = currentTime
                }
                var elapsed = currentTime - mStartTime
                var opacity = mInitialOpacity
                if (mLoopType != LOOP_TYPE_NONE && elapsed > mDuration) {
                    val endTime = mStartTime + mDuration
                    elapsed = endTime - currentTime
                    mStartTime = endTime
                    if (mLoopType == LOOP_TYPE_PING_PONG) {
                        val temp = mInitialOpacity
                        mInitialOpacity = mTargetOpacity
                        mTargetOpacity = temp
                    }
                }
                if (elapsed > mDuration) {
                    opacity = mTargetOpacity
                } else if (elapsed != 0.0f) {
                    if (mFunction == FADE_LINEAR) {
                        opacity = lerp(mInitialOpacity, mTargetOpacity, mDuration, elapsed)
                    } else if (mFunction == FADE_EASE) {
                        opacity = ease(mInitialOpacity, mTargetOpacity, mDuration, elapsed)
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
                        bitmap.width = parentObject!!.width.toInt()
                        bitmap.height = parentObject.height.toInt()
                        bitmap.setOpacity(opacity)
                        bitmap.texture = mTexture
                        mRenderComponent!!.drawable = bitmap
                    }
                } else {
                    val drawable = mRenderComponent!!.drawable
                    // TODO: ack, instanceof!  Fix this!
                    if (drawable != null && drawable is DrawableBitmap) {
                        drawable.setOpacity(opacity)
                    }
                }
            }
        }
    }

    fun setupFade(startOpacity: Float, endOpacity: Float, duration: Float, loopType: Int, function: Int, initialDelay: Float) {
        mInitialOpacity = startOpacity
        mTargetOpacity = endOpacity
        mDuration = duration
        mLoopType = loopType
        mFunction = function
        mInitialDelay = initialDelay
    }

    /** Enables phases; the initial delay will be re-started when the phase ends.  */
    fun setPhaseDuration(duration: Float) {
        mPhaseDuration = duration
    }

    /** If set to something non-null, this component will overwrite the drawable on the target render component.  */
    fun setTexture(texture: Texture?) {
        mTexture = texture
    }

    fun setRenderComponent(component: RenderComponent?) {
        mRenderComponent = component
    }

    fun resetPhase() {
        mActivateTime = 0.0f
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
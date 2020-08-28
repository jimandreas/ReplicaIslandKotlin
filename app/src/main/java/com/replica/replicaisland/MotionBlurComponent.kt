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

class MotionBlurComponent : GameComponent() {
    private val mHistory: Array<BlurRecord?>
    private var mBlurTarget: RenderComponent? = null
    private var mStepDelay = 0f
    private var mCurrentStep = 0
    private var mTimeSinceLastStep = 0f
    private var mTargetPriority = 0

    private class BlurRecord {
        var position = Vector2()
        var texture: Texture? = null
        var width = 0
        var height = 0
        var crop = IntArray(4)
    }

    override fun reset() {
        for (x in 0 until STEP_COUNT) {
            mHistory[x]!!.texture = null
            mHistory[x]!!.position.zero()
        }
        mStepDelay = STEP_DELAY
        mBlurTarget = null
        mCurrentStep = 0
        mTimeSinceLastStep = 0.0f
    }

    fun setTarget(target: RenderComponent?) {
        mBlurTarget = target
    }

    override fun update(timeDelta: Float, parent: BaseObject?) {
        if (mBlurTarget != null) {
            mTimeSinceLastStep += timeDelta
            if (mTimeSinceLastStep > mStepDelay) {
                val drawable = mBlurTarget!!.drawable as DrawableBitmap?
                if (drawable != null) {
                    val currentTexture = drawable.texture
                    mTargetPriority = mBlurTarget!!.priority
                    mHistory[mCurrentStep]!!.texture = currentTexture
                    mHistory[mCurrentStep]!!.position.set((parent as GameObject?)!!.position)
                    mHistory[mCurrentStep]!!.width = drawable.width
                    mHistory[mCurrentStep]!!.height = drawable.height
                    val drawableCrop = drawable.crop
                    mHistory[mCurrentStep]!!.crop[0] = drawableCrop[0]
                    mHistory[mCurrentStep]!!.crop[1] = drawableCrop[1]
                    mHistory[mCurrentStep]!!.crop[2] = drawableCrop[2]
                    mHistory[mCurrentStep]!!.crop[3] = drawableCrop[3]
                    mCurrentStep = (mCurrentStep + 1) % STEP_COUNT
                    mTimeSinceLastStep = 0.0f
                }
            }
            val renderer = sSystemRegistry.renderSystem
            val startStep = if (mCurrentStep > 0) mCurrentStep - 1 else STEP_COUNT - 1
            // draw each step
            for (x in 0 until STEP_COUNT) {
                val step = if (startStep - x < 0) STEP_COUNT + (startStep - x) else startStep - x
                val record = mHistory[step]
                if (record!!.texture != null) {
                    val stepImage = sSystemRegistry.drawableFactory!!.allocateDrawableBitmap()
                    stepImage.texture = record.texture
                    stepImage.width = record.width
                    stepImage.height = record.height
                    stepImage.setCrop(record.crop[0], record.crop[1], record.crop[2], -record.crop[3])
                    val opacity = (STEP_COUNT - x) * OPACITY_STEP
                    stepImage.setOpacity(opacity)
                    renderer!!.scheduleForDraw(stepImage, record.position, mTargetPriority - (x + 1), true)
                }
            }
        }
    }

    companion object {
        private const val STEP_COUNT = 4
        private const val STEP_DELAY = 0.1f
        private const val OPACITY_STEP = 1.0f / (STEP_COUNT + 1)
    }

    init {
        mHistory = arrayOfNulls(STEP_COUNT)
        for (x in 0 until STEP_COUNT) {
            mHistory[x] = BlurRecord()
        }
        reset()
        setPhaseToThis(ComponentPhases.PRE_DRAW.ordinal)
    }
}
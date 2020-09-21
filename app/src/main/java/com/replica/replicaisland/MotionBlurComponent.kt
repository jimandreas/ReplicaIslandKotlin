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
    private val history: Array<BlurRecord?>
    private var blurTarget: RenderComponent? = null
    private var stepDelay = 0f
    private var currentStep = 0
    private var timeSinceLastStep = 0f
    private var targetPriority = 0

    private class BlurRecord {
        var position = Vector2()
        var texture: Texture? = null
        var width = 0
        var height = 0
        var crop = IntArray(4)
    }

    override fun reset() {
        for (x in 0 until STEP_COUNT) {
            history[x]!!.texture = null
            history[x]!!.position.zero()
        }
        stepDelay = STEP_DELAY
        blurTarget = null
        currentStep = 0
        timeSinceLastStep = 0.0f
    }

    fun setTarget(target: RenderComponent?) {
        blurTarget = target
    }

    override fun update(timeDelta: Float, parent: BaseObject?) {
        if (blurTarget != null) {
            timeSinceLastStep += timeDelta
            if (timeSinceLastStep > stepDelay) {
                val drawable = blurTarget!!.drawable as DrawableBitmap?
                if (drawable != null) {
                    val currentTexture = drawable.texture
                    targetPriority = blurTarget!!.priority
                    history[currentStep]!!.texture = currentTexture
                    history[currentStep]!!.position.set((parent as GameObject?)!!.position)
                    history[currentStep]!!.width = drawable.width
                    history[currentStep]!!.height = drawable.height
                    val drawableCrop = drawable.crop
                    history[currentStep]!!.crop[0] = drawableCrop[0]
                    history[currentStep]!!.crop[1] = drawableCrop[1]
                    history[currentStep]!!.crop[2] = drawableCrop[2]
                    history[currentStep]!!.crop[3] = drawableCrop[3]
                    currentStep = (currentStep + 1) % STEP_COUNT
                    timeSinceLastStep = 0.0f
                }
            }
            val renderer = sSystemRegistry.renderSystem
            val startStep = if (currentStep > 0) currentStep - 1 else STEP_COUNT - 1
            // draw each step
            for (x in 0 until STEP_COUNT) {
                val step = if (startStep - x < 0) STEP_COUNT + (startStep - x) else startStep - x
                val record = history[step]
                if (record!!.texture != null) {
                    val stepImage = sSystemRegistry.drawableFactory!!.allocateDrawableBitmap()
                    stepImage.texture = record.texture
                    stepImage.width = record.width
                    stepImage.height = record.height
                    stepImage.setCrop(record.crop[0], record.crop[1], record.crop[2], -record.crop[3])
                    val opacity = (STEP_COUNT - x) * OPACITY_STEP
                    stepImage.setOpacity(opacity)
                    renderer!!.scheduleForDraw(stepImage, record.position, targetPriority - (x + 1), true)
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
        history = arrayOfNulls(STEP_COUNT)
        for (x in 0 until STEP_COUNT) {
            history[x] = BlurRecord()
        }
        reset()
        setPhaseToThis(ComponentPhases.PRE_DRAW.ordinal)
    }
}
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
@file:Suppress("unused", "CascadeIf", "NullChecksToSafeCall")

package com.replica.replicaisland

import com.replica.replicaisland.InventoryComponent.UpdateRecord

/**
 * A very simple manager for orthographic in-game UI elements.
 * TODO: This should probably manage a number of hud objects in keeping with the component-centric
 * architecture of this engine.  The current code is monolithic and should be refactored.
 */
class HudSystem : BaseObject() {
    private var mFuelDrawable: DrawableBitmap? = null
    private var mFuelBackgroundDrawable: DrawableBitmap? = null
    private var mFuelPercent = 0f
    private var mFuelTargetPercent = 0f
    private var mFadeTexture: Texture? = null
    private var mFadeStartTime = 0f
    private var mFadeDuration = 0f
    private var mFadeIn = false
    var isFading = false
        private set
    private var mFadePendingEventType = 0
    private var mFadePendingEventIndex = 0
    private var mFlyButtonEnabledDrawable: DrawableBitmap? = null
    private var mFlyButtonDisabledDrawable: DrawableBitmap? = null
    private var mFlyButtonDepressedDrawable: DrawableBitmap? = null
    private var mStompButtonEnabledDrawable: DrawableBitmap? = null
    private var mStompButtonDepressedDrawable: DrawableBitmap? = null
    private var mMovementSliderBaseDrawable: DrawableBitmap? = null
    private var mMovementSliderButtonDrawable: DrawableBitmap? = null
    private var mMovementSliderButtonDepressedDrawable: DrawableBitmap? = null
    private val mFlyButtonLocation: Vector2 = Vector2()
    private var mFlyButtonActive = false
    private var mFlyButtonPressed = false
    private val mStompButtonLocation: Vector2 = Vector2()
    private var mStompButtonPressed = false
    private val mMovementSliderBaseLocation: Vector2
    private val mMovementSliderButtonLocation: Vector2
    private var mMovementSliderMode = false
    private var mMovementSliderButtonPressed = false
    private var mRubyDrawable: DrawableBitmap? = null
    private var mCoinDrawable: DrawableBitmap? = null
    private var mCoinCount = 0
    private var mRubyCount = 0
    private val mCoinLocation: Vector2 = Vector2()
    private val mRubyLocation: Vector2 = Vector2()
    private val mCoinDigits: IntArray
    private val mRubyDigits: IntArray
    private var mCoinDigitsChanged = false
    private var mRubyDigitsChanged = false
    private var mFPS = 0
    private val mFPSLocation: Vector2 = Vector2()
    private val mFPSDigits: IntArray
    private var mFPSDigitsChanged = false
    private var mShowFPS = false
    private val mDigitDrawables: Array<DrawableBitmap?> = arrayOfNulls(10)
    private var mXDrawable: DrawableBitmap? = null
    override fun reset() {
        mFuelDrawable = null
        mFadeTexture = null
        mFuelPercent = 1.0f
        mFuelTargetPercent = 1.0f
        isFading = false
        mFlyButtonDisabledDrawable = null
        mFlyButtonEnabledDrawable = null
        mFlyButtonDepressedDrawable = null
        mFlyButtonLocation[FLY_BUTTON_X] = FLY_BUTTON_Y
        mFlyButtonActive = true
        mFlyButtonPressed = false
        mStompButtonEnabledDrawable = null
        mStompButtonDepressedDrawable = null
        mStompButtonLocation[STOMP_BUTTON_X] = STOMP_BUTTON_Y
        mStompButtonPressed = false
        mCoinCount = 0
        mRubyCount = 0
        mCoinDigits[0] = 0
        mCoinDigits[1] = -1
        mRubyDigits[0] = 0
        mRubyDigits[1] = -1
        mCoinDigitsChanged = true
        mRubyDigitsChanged = true
        mFPS = 0
        mFPSDigits[0] = 0
        mFPSDigits[1] = -1
        mFPSDigitsChanged = true
        mShowFPS = false
        for (x in mDigitDrawables.indices) {
            mDigitDrawables[x] = null
        }
        mXDrawable = null
        mFadePendingEventType = GameFlowEvent.EVENT_INVALID
        mFadePendingEventIndex = 0
        mMovementSliderBaseDrawable = null
        mMovementSliderButtonDrawable = null
        mMovementSliderButtonDepressedDrawable = null
        mMovementSliderBaseLocation[MOVEMENT_SLIDER_BASE_X] = MOVEMENT_SLIDER_BASE_Y
        mMovementSliderButtonLocation[MOVEMENT_SLIDER_BUTTON_X] = MOVEMENT_SLIDER_BUTTON_Y
        mMovementSliderMode = false
        mMovementSliderButtonPressed = false
    }

    fun setFuelPercent(percent: Float) {
        mFuelTargetPercent = percent
    }

    fun setFuelDrawable(fuel: DrawableBitmap?, background: DrawableBitmap?) {
        mFuelDrawable = fuel
        mFuelBackgroundDrawable = background
    }

    fun setFadeTexture(texture: Texture?) {
        mFadeTexture = texture
    }

    fun setButtonDrawables(disabled: DrawableBitmap?, enabled: DrawableBitmap?, depressed: DrawableBitmap?,
                           stompEnabled: DrawableBitmap?, stompDepressed: DrawableBitmap?,
                           sliderBase: DrawableBitmap?, sliderButton: DrawableBitmap?, sliderDepressed: DrawableBitmap?) {
        mFlyButtonDisabledDrawable = disabled
        mFlyButtonEnabledDrawable = enabled
        mFlyButtonDepressedDrawable = depressed
        mStompButtonEnabledDrawable = stompEnabled
        mStompButtonDepressedDrawable = stompDepressed
        mMovementSliderBaseDrawable = sliderBase
        mMovementSliderButtonDrawable = sliderButton
        mMovementSliderButtonDepressedDrawable = sliderDepressed
    }

    fun setDigitDrawables(digits: Array<DrawableBitmap>, xMark: DrawableBitmap?) {
        mXDrawable = xMark
        var x = 0
        while (x < mDigitDrawables.size && x < digits.size) {
            mDigitDrawables[x] = digits[x]
            x++
        }
    }

    fun setCollectableDrawables(coin: DrawableBitmap?, ruby: DrawableBitmap?) {
        mCoinDrawable = coin
        mRubyDrawable = ruby
    }

    fun setButtonState(pressed: Boolean, attackPressed: Boolean, sliderPressed: Boolean) {
        mFlyButtonPressed = pressed
        mStompButtonPressed = attackPressed
        mMovementSliderButtonPressed = sliderPressed
    }

    fun startFade(`in`: Boolean, duration: Float) {
        mFadeStartTime = sSystemRegistry.timeSystem!!.realTime
        mFadeDuration = duration
        mFadeIn = `in`
        isFading = true
    }

    fun clearFade() {
        isFading = false
    }

    fun updateInventory(newInventory: UpdateRecord) {
        mCoinDigitsChanged = mCoinCount != newInventory.coinCount
        mRubyDigitsChanged = mRubyCount != newInventory.rubyCount
        mCoinCount = newInventory.coinCount
        mRubyCount = newInventory.rubyCount
    }

    fun setFPS(fps: Int) {
        mFPSDigitsChanged = fps != mFPS
        mFPS = fps
    }

    fun setShowFPS(show: Boolean) {
        mShowFPS = show
    }

    fun setMovementSliderMode(sliderOn: Boolean) {
        mMovementSliderMode = sliderOn
        if (sliderOn) {
            val params = sSystemRegistry.contextParameters
            mFlyButtonLocation[params!!.gameWidth - FLY_BUTTON_WIDTH - FLY_BUTTON_X] = FLY_BUTTON_Y
            mStompButtonLocation[params.gameWidth - STOMP_BUTTON_WIDTH - STOMP_BUTTON_X] = STOMP_BUTTON_Y
        } else {
            mFlyButtonLocation[FLY_BUTTON_X] = FLY_BUTTON_Y
            mStompButtonLocation[STOMP_BUTTON_X] = STOMP_BUTTON_Y
        }
    }

    fun setMovementSliderOffset(offset: Float) {
        mMovementSliderButtonLocation[MOVEMENT_SLIDER_BUTTON_X + offset * (MOVEMENT_SLIDER_WIDTH / 2.0f)] = MOVEMENT_SLIDER_BUTTON_Y
    }

    override fun update(timeDelta: Float, parent: BaseObject?) {
        val render = sSystemRegistry.renderSystem
        val pool = sSystemRegistry.vectorPool
        val params = sSystemRegistry.contextParameters
        val factory = sSystemRegistry.drawableFactory
        val manager = sSystemRegistry.gameObjectManager
        if (manager != null && manager.player != null) {
            // Only draw player-specific HUD elements when there's a player.
            if (mFuelDrawable != null && mFuelBackgroundDrawable != null && render != null && pool != null && factory != null && params != null) {
                if (mFuelPercent < mFuelTargetPercent) {
                    mFuelPercent += FUEL_INCREASE_BAR_SPEED * timeDelta
                    if (mFuelPercent > mFuelTargetPercent) {
                        mFuelPercent = mFuelTargetPercent
                    }
                } else if (mFuelPercent > mFuelTargetPercent) {
                    mFuelPercent -= FUEL_DECREASE_BAR_SPEED * timeDelta
                    if (mFuelPercent < mFuelTargetPercent) {
                        mFuelPercent = mFuelTargetPercent
                    }
                }
                if (mFuelBackgroundDrawable!!.width == 0) {
                    // first time init
                    val tex = mFuelDrawable!!.texture
                    mFuelDrawable!!.resize(tex!!.width, tex.height)
                    val backgroundTex = mFuelBackgroundDrawable!!.texture
                    mFuelBackgroundDrawable!!.resize(backgroundTex!!.width, backgroundTex.height)
                }
                val height = mFuelDrawable!!.height
                val location = pool.allocate()
                location!![FUEL_BAR_EDGE_PADDING.toFloat()] = params.gameHeight - height - FUEL_BAR_EDGE_PADDING.toFloat()
                render.scheduleForDraw(mFuelBackgroundDrawable, location, SortConstants.HUD, false)
                location.x += 2
                location.y += 2
                val barWidth = ((100 - 4) * mFuelPercent).toInt()
                if (barWidth >= 1) {
                    val bitmap = factory.allocateDrawableBitmap()
                    bitmap.resize(barWidth, mFuelDrawable!!.height)
                    bitmap.texture = mFuelDrawable!!.texture
                    render.scheduleForDraw(bitmap, location, SortConstants.HUD + 1, false)
                }
                pool.release(location)
            }
            if (mFlyButtonDisabledDrawable != null && mFlyButtonEnabledDrawable != null && mFlyButtonDepressedDrawable != null) {
                var bitmap: DrawableBitmap = mFlyButtonEnabledDrawable!!
                if (mFlyButtonActive && mFlyButtonPressed) {
                    bitmap = mFlyButtonDepressedDrawable!!
                } else if (!mFlyButtonActive) {
                    bitmap = mFlyButtonDisabledDrawable!!
                }
                if (bitmap.width == 0) {
                    // first time init
                    val tex = bitmap.texture
                    bitmap.resize(tex!!.width, tex.height)
                }
                render!!.scheduleForDraw(bitmap, mFlyButtonLocation, SortConstants.HUD, false)
            }
            if (mStompButtonEnabledDrawable != null && mStompButtonDepressedDrawable != null) {
                var bitmap: DrawableBitmap = mStompButtonEnabledDrawable!!
                if (mStompButtonPressed) {
                    bitmap = mStompButtonDepressedDrawable!!
                }
                if (bitmap.width == 0) {
                    // first time init
                    val tex = bitmap.texture
                    bitmap.resize(tex!!.width, tex.height)
                    bitmap.width = (tex.width * STOMP_BUTTON_SCALE).toInt()
                    bitmap.height = (tex.height * STOMP_BUTTON_SCALE).toInt()
                }
                render!!.scheduleForDraw(bitmap, mStompButtonLocation, SortConstants.HUD, false)
            }
            if (mMovementSliderMode && mMovementSliderBaseDrawable != null && mMovementSliderButtonDrawable != null) {
                if (mMovementSliderBaseDrawable!!.width == 0) {
                    // first time init
                    val tex = mMovementSliderBaseDrawable!!.texture
                    mMovementSliderBaseDrawable!!.resize(tex!!.width, tex.height)
                }
                if (mMovementSliderButtonDrawable!!.width == 0) {
                    // first time init
                    val tex = mMovementSliderButtonDrawable!!.texture
                    mMovementSliderButtonDrawable!!.resize(tex!!.width, tex.height)
                }
                if (mMovementSliderButtonDepressedDrawable!!.width == 0) {
                    // first time init
                    val tex = mMovementSliderButtonDepressedDrawable!!.texture
                    mMovementSliderButtonDepressedDrawable!!.resize(tex!!.width, tex.height)
                }
                var bitmap = mMovementSliderButtonDrawable
                if (mMovementSliderButtonPressed) {
                    bitmap = mMovementSliderButtonDepressedDrawable
                }
                render!!.scheduleForDraw(mMovementSliderBaseDrawable, mMovementSliderBaseLocation, SortConstants.HUD, false)
                render.scheduleForDraw(bitmap, mMovementSliderButtonLocation, SortConstants.HUD + 1, false)
            }
            if (mCoinDrawable != null) {
                if (mCoinDrawable!!.width == 0) {
                    // first time init
                    val tex = mCoinDrawable!!.texture
                    mCoinDrawable!!.resize(tex!!.width, tex.height)
                    mCoinLocation.x = params!!.gameWidth / 2.0f - tex.width / 2.0f
                    mCoinLocation.y = (params.gameHeight - tex.height - COLLECTABLE_EDGE_PADDING).toFloat()
                }
                render!!.scheduleForDraw(mCoinDrawable, mCoinLocation, SortConstants.HUD, false)
                if (mCoinDigitsChanged) {
                    intToDigitArray(mCoinCount, mCoinDigits)
                    mCoinDigitsChanged = false
                }
                val offset = mCoinDrawable!!.width * 0.75f
                mCoinLocation.x += offset
                drawNumber(mCoinLocation, mCoinDigits, true)
                mCoinLocation.x -= offset
            }
            if (mRubyDrawable != null) {
                if (mRubyDrawable!!.width == 0) {
                    // first time init
                    val tex = mRubyDrawable!!.texture
                    mRubyDrawable!!.resize(tex!!.width, tex.height)
                    mRubyLocation.x = params!!.gameWidth / 2.0f + 100.0f
                    mRubyLocation.y = (params.gameHeight - tex.height - COLLECTABLE_EDGE_PADDING).toFloat()
                }
                render!!.scheduleForDraw(mRubyDrawable, mRubyLocation, SortConstants.HUD, false)
                if (mRubyDigitsChanged) {
                    intToDigitArray(mRubyCount, mRubyDigits)
                    mRubyDigitsChanged = false
                }
                val offset = mRubyDrawable!!.width * 0.75f
                mRubyLocation.x += offset
                drawNumber(mRubyLocation, mRubyDigits, true)
                mRubyLocation.x -= offset
            }
        }
        if (mShowFPS) {
            if (mFPSDigitsChanged) {
                val count = intToDigitArray(mFPS, mFPSDigits)
                mFPSDigitsChanged = false
                mFPSLocation[params!!.gameWidth - 10.0f - (count + 1) * (mDigitDrawables[0]!!.width / 2.0f)] = 10.0f
            }
            drawNumber(mFPSLocation, mFPSDigits, false)
        }
        if (isFading && factory != null) {
            val time = sSystemRegistry.timeSystem!!.realTime
            val fadeDelta = time - mFadeStartTime
            var percentComplete = 1.0f
            if (fadeDelta < mFadeDuration) {
                percentComplete = fadeDelta / mFadeDuration
            } else if (mFadeIn) {
                // We've faded in.  Turn fading off.
                isFading = false
            }
            if (percentComplete < 1.0f || !mFadeIn) {
                var opacityValue = percentComplete
                if (mFadeIn) {
                    opacityValue = 1.0f - percentComplete
                }
                val bitmap = factory.allocateDrawableBitmap()
                bitmap.width = params!!.gameWidth
                bitmap.height = params.gameHeight
                bitmap.texture = mFadeTexture
                bitmap.setCrop(0, mFadeTexture!!.height, mFadeTexture!!.width, mFadeTexture!!.height)
                bitmap.setOpacity(opacityValue)
                render!!.scheduleForDraw(bitmap, Vector2.ZERO, SortConstants.FADE, false)
            }
            if (percentComplete >= 1.0f && mFadePendingEventType != GameFlowEvent.EVENT_INVALID) {
                val level = sSystemRegistry.levelSystem
                if (level != null) {
                    level.sendGameEvent(mFadePendingEventType, mFadePendingEventIndex, false)
                    mFadePendingEventType = GameFlowEvent.EVENT_INVALID
                    mFadePendingEventIndex = 0
                }
            }
        }
    }

    private fun drawNumber(location: Vector2, digits: IntArray, drawX: Boolean) {
        val render = sSystemRegistry.renderSystem
        if (mDigitDrawables[0]!!.width == 0) {
            // first time init
            for (x in mDigitDrawables.indices) {
                val tex = mDigitDrawables[x]!!.texture
                mDigitDrawables[x]!!.resize(tex!!.width, tex.height)
            }
        }
        if (mXDrawable!!.width == 0) {
            // first time init
            val tex = mXDrawable!!.texture
            mXDrawable!!.resize(tex!!.width, tex.height)
        }
        val characterWidth = mDigitDrawables[0]!!.width / 2.0f
        var offset = 0.0f
        if (mXDrawable != null && drawX) {
            render!!.scheduleForDraw(mXDrawable, location, SortConstants.HUD, false)
            location.x += characterWidth
            offset += characterWidth
        }
        var x = 0
        while (x < digits.size && digits[x] != -1) {
            val index = digits[x]
            val digit = mDigitDrawables[index]
            if (digit != null) {
                render!!.scheduleForDraw(digit, location, SortConstants.HUD, false)
                location.x += characterWidth
                offset += characterWidth
            }
            x++
        }
        location.x -= offset
    }

    private fun intToDigitArray(value: Int, digits: IntArray): Int {
        var characterCount = 1
        if (value >= 1000) {
            characterCount = 4
        } else if (value >= 100) {
            characterCount = 3
        } else if (value >= 10) {
            characterCount = 2
        }
        var remainingValue = value
        var count = 0
        do {
            val index = if (remainingValue != 0) remainingValue % 10 else 0
            remainingValue /= 10
            digits[characterCount - 1 - count] = index
            count++
        } while (remainingValue > 0 && count < digits.size)
        if (count < digits.size) {
            digits[count] = -1
        }
        return characterCount
    }

    fun sendGameEventOnFadeComplete(eventType: Int, eventIndex: Int) {
        mFadePendingEventType = eventType
        mFadePendingEventIndex = eventIndex
    }

    companion object {
        private const val FUEL_BAR_EDGE_PADDING = 15
        private const val FUEL_DECREASE_BAR_SPEED = 0.75f
        private const val FUEL_INCREASE_BAR_SPEED = 2.0f
        private const val FLY_BUTTON_X = -12.0f
        private const val FLY_BUTTON_Y = -5.0f
        private const val STOMP_BUTTON_X = 85.0f
        private const val STOMP_BUTTON_Y = -10.0f
        private const val STOMP_BUTTON_SCALE = 0.65f
        private const val COLLECTABLE_EDGE_PADDING = 8
        private const val MAX_DIGITS = 4
        private const val MOVEMENT_SLIDER_BASE_X = 20.0f
        private const val MOVEMENT_SLIDER_BASE_Y = 32.0f
        private const val MOVEMENT_SLIDER_BUTTON_X = MOVEMENT_SLIDER_BASE_X + 32.0f
        private const val MOVEMENT_SLIDER_BUTTON_Y = MOVEMENT_SLIDER_BASE_Y - 16.0f
        private const val FLY_BUTTON_WIDTH = 128f
        private const val STOMP_BUTTON_WIDTH = FLY_BUTTON_WIDTH * STOMP_BUTTON_SCALE
        private const val MOVEMENT_SLIDER_WIDTH = 128f
    }

    init {
        mCoinDigits = IntArray(MAX_DIGITS)
        mRubyDigits = IntArray(MAX_DIGITS)
        mFPSDigits = IntArray(MAX_DIGITS)
        mMovementSliderBaseLocation = Vector2()
        mMovementSliderButtonLocation = Vector2()
        reset()
    }
}
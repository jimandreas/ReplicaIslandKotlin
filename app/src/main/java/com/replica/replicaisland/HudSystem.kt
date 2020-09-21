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
    private var fuelDrawable: DrawableBitmap? = null
    private var fuelBackgroundDrawable: DrawableBitmap? = null
    private var fuelPercent = 0f
    private var fuelTargetPercent = 0f
    private var fadeTexture: Texture? = null
    private var fadeStartTime = 0f
    private var fadeDuration = 0f
    private var fadeIn = false
    var isFading = false
        private set
    private var fadePendingEventType = 0
    private var fadePendingEventIndex = 0
    private var flyButtonEnabledDrawable: DrawableBitmap? = null
    private var flyButtonDisabledDrawable: DrawableBitmap? = null
    private var flyButtonDepressedDrawable: DrawableBitmap? = null
    private var stompButtonEnabledDrawable: DrawableBitmap? = null
    private var stompButtonDepressedDrawable: DrawableBitmap? = null
    private var movementSliderBaseDrawable: DrawableBitmap? = null
    private var movementSliderButtonDrawable: DrawableBitmap? = null
    private var movementSliderButtonDepressedDrawable: DrawableBitmap? = null
    private val flyButtonLocation: Vector2 = Vector2()
    private var flyButtonActive = false
    private var flyButtonPressed = false
    private val stompButtonLocation: Vector2 = Vector2()
    private var stompButtonPressed = false
    private val movementSliderBaseLocation: Vector2
    private val movementSliderButtonLocation: Vector2
    private var movementSliderMode = false
    private var movementSliderButtonPressed = false
    private var rubyDrawable: DrawableBitmap? = null
    private var coinDrawable: DrawableBitmap? = null
    private var mCoinCount = 0
    private var mRubyCount = 0
    private val coinLocation: Vector2 = Vector2()
    private val rubyLocation: Vector2 = Vector2()
    private val coinDigits: IntArray
    private val rubyDigits: IntArray
    private var coinDigitsChanged = false
    private var rubyDigitsChanged = false
    private var fPS = 0
    private val fPSLocation: Vector2 = Vector2()
    private val fPSDigits: IntArray
    private var fPSDigitsChanged = false
    private var showFPS = false
    private val digitDrawables: Array<DrawableBitmap?> = arrayOfNulls(10)
    private var xDrawable: DrawableBitmap? = null
    override fun reset() {
        fuelDrawable = null
        fadeTexture = null
        fuelPercent = 1.0f
        fuelTargetPercent = 1.0f
        isFading = false
        flyButtonDisabledDrawable = null
        flyButtonEnabledDrawable = null
        flyButtonDepressedDrawable = null
        flyButtonLocation[FLY_BUTTON_X] = FLY_BUTTON_Y
        flyButtonActive = true
        flyButtonPressed = false
        stompButtonEnabledDrawable = null
        stompButtonDepressedDrawable = null
        stompButtonLocation[STOMP_BUTTON_X] = STOMP_BUTTON_Y
        stompButtonPressed = false
        mCoinCount = 0
        mRubyCount = 0
        coinDigits[0] = 0
        coinDigits[1] = -1
        rubyDigits[0] = 0
        rubyDigits[1] = -1
        coinDigitsChanged = true
        rubyDigitsChanged = true
        fPS = 0
        fPSDigits[0] = 0
        fPSDigits[1] = -1
        fPSDigitsChanged = true
        showFPS = false
        for (x in digitDrawables.indices) {
            digitDrawables[x] = null
        }
        xDrawable = null
        fadePendingEventType = GameFlowEvent.EVENT_INVALID
        fadePendingEventIndex = 0
        movementSliderBaseDrawable = null
        movementSliderButtonDrawable = null
        movementSliderButtonDepressedDrawable = null
        movementSliderBaseLocation[MOVEMENT_SLIDER_BASE_X] = MOVEMENT_SLIDER_BASE_Y
        movementSliderButtonLocation[MOVEMENT_SLIDER_BUTTON_X] = MOVEMENT_SLIDER_BUTTON_Y
        movementSliderMode = false
        movementSliderButtonPressed = false
    }

    fun setFuelPercent(percent: Float) {
        fuelTargetPercent = percent
    }

    fun setFuelDrawable(fuel: DrawableBitmap?, background: DrawableBitmap?) {
        fuelDrawable = fuel
        fuelBackgroundDrawable = background
    }

    fun setFadeTexture(texture: Texture?) {
        fadeTexture = texture
    }

    fun setButtonDrawables(disabled: DrawableBitmap?, enabled: DrawableBitmap?, depressed: DrawableBitmap?,
                           stompEnabled: DrawableBitmap?, stompDepressed: DrawableBitmap?,
                           sliderBase: DrawableBitmap?, sliderButton: DrawableBitmap?, sliderDepressed: DrawableBitmap?) {
        flyButtonDisabledDrawable = disabled
        flyButtonEnabledDrawable = enabled
        flyButtonDepressedDrawable = depressed
        stompButtonEnabledDrawable = stompEnabled
        stompButtonDepressedDrawable = stompDepressed
        movementSliderBaseDrawable = sliderBase
        movementSliderButtonDrawable = sliderButton
        movementSliderButtonDepressedDrawable = sliderDepressed
    }

    fun setDigitDrawables(digits: Array<DrawableBitmap>, xMark: DrawableBitmap?) {
        xDrawable = xMark
        var x = 0
        while (x < digitDrawables.size && x < digits.size) {
            digitDrawables[x] = digits[x]
            x++
        }
    }

    fun setCollectableDrawables(coin: DrawableBitmap?, ruby: DrawableBitmap?) {
        coinDrawable = coin
        rubyDrawable = ruby
    }

    fun setButtonState(pressed: Boolean, attackPressed: Boolean, sliderPressed: Boolean) {
        flyButtonPressed = pressed
        stompButtonPressed = attackPressed
        movementSliderButtonPressed = sliderPressed
    }

    fun startFade(`in`: Boolean, duration: Float) {
        fadeStartTime = sSystemRegistry.timeSystem!!.realTime
        fadeDuration = duration
        fadeIn = `in`
        isFading = true
    }

    fun clearFade() {
        isFading = false
    }

    fun updateInventory(newInventory: UpdateRecord) {
        coinDigitsChanged = mCoinCount != newInventory.coinCount
        rubyDigitsChanged = mRubyCount != newInventory.rubyCount
        mCoinCount = newInventory.coinCount
        mRubyCount = newInventory.rubyCount
    }

    fun setFPS(fps: Int) {
        fPSDigitsChanged = fps != fPS
        fPS = fps
    }

    fun setShowFPS(show: Boolean) {
        showFPS = show
    }

    fun setMovementSliderMode(sliderOn: Boolean) {
        movementSliderMode = sliderOn
        if (sliderOn) {
            val params = sSystemRegistry.contextParameters
            flyButtonLocation[params!!.gameWidth - FLY_BUTTON_WIDTH - FLY_BUTTON_X] = FLY_BUTTON_Y
            stompButtonLocation[params.gameWidth - STOMP_BUTTON_WIDTH - STOMP_BUTTON_X] = STOMP_BUTTON_Y
        } else {
            flyButtonLocation[FLY_BUTTON_X] = FLY_BUTTON_Y
            stompButtonLocation[STOMP_BUTTON_X] = STOMP_BUTTON_Y
        }
    }

    fun setMovementSliderOffset(offset: Float) {
        movementSliderButtonLocation[MOVEMENT_SLIDER_BUTTON_X + offset * (MOVEMENT_SLIDER_WIDTH / 2.0f)] = MOVEMENT_SLIDER_BUTTON_Y
    }

    override fun update(timeDelta: Float, parent: BaseObject?) {
        val render = sSystemRegistry.renderSystem
        val pool = sSystemRegistry.vectorPool
        val params = sSystemRegistry.contextParameters
        val factory = sSystemRegistry.drawableFactory
        val manager = sSystemRegistry.gameObjectManager
        if (manager != null && manager.player != null) {
            // Only draw player-specific HUD elements when there's a player.
            if (fuelDrawable != null && fuelBackgroundDrawable != null && render != null && pool != null && factory != null && params != null) {
                if (fuelPercent < fuelTargetPercent) {
                    fuelPercent += FUEL_INCREASE_BAR_SPEED * timeDelta
                    if (fuelPercent > fuelTargetPercent) {
                        fuelPercent = fuelTargetPercent
                    }
                } else if (fuelPercent > fuelTargetPercent) {
                    fuelPercent -= FUEL_DECREASE_BAR_SPEED * timeDelta
                    if (fuelPercent < fuelTargetPercent) {
                        fuelPercent = fuelTargetPercent
                    }
                }
                if (fuelBackgroundDrawable!!.width == 0) {
                    // first time init
                    val tex = fuelDrawable!!.texture
                    fuelDrawable!!.resize(tex!!.width, tex.height)
                    val backgroundTex = fuelBackgroundDrawable!!.texture
                    fuelBackgroundDrawable!!.resize(backgroundTex!!.width, backgroundTex.height)
                }
                val height = fuelDrawable!!.height
                val location = pool.allocate()
                location!![FUEL_BAR_EDGE_PADDING.toFloat()] = params.gameHeight - height - FUEL_BAR_EDGE_PADDING.toFloat()
                render.scheduleForDraw(fuelBackgroundDrawable, location, SortConstants.HUD, false)
                location.x += 2
                location.y += 2
                val barWidth = ((100 - 4) * fuelPercent).toInt()
                if (barWidth >= 1) {
                    val bitmap = factory.allocateDrawableBitmap()
                    bitmap.resize(barWidth, fuelDrawable!!.height)
                    bitmap.texture = fuelDrawable!!.texture
                    render.scheduleForDraw(bitmap, location, SortConstants.HUD + 1, false)
                }
                pool.release(location)
            }
            if (flyButtonDisabledDrawable != null && flyButtonEnabledDrawable != null && flyButtonDepressedDrawable != null) {
                var bitmap: DrawableBitmap = flyButtonEnabledDrawable!!
                if (flyButtonActive && flyButtonPressed) {
                    bitmap = flyButtonDepressedDrawable!!
                } else if (!flyButtonActive) {
                    bitmap = flyButtonDisabledDrawable!!
                }
                if (bitmap.width == 0) {
                    // first time init
                    val tex = bitmap.texture
                    bitmap.resize(tex!!.width, tex.height)
                }
                render!!.scheduleForDraw(bitmap, flyButtonLocation, SortConstants.HUD, false)
            }
            if (stompButtonEnabledDrawable != null && stompButtonDepressedDrawable != null) {
                var bitmap: DrawableBitmap = stompButtonEnabledDrawable!!
                if (stompButtonPressed) {
                    bitmap = stompButtonDepressedDrawable!!
                }
                if (bitmap.width == 0) {
                    // first time init
                    val tex = bitmap.texture
                    bitmap.resize(tex!!.width, tex.height)
                    bitmap.width = (tex.width * STOMP_BUTTON_SCALE).toInt()
                    bitmap.height = (tex.height * STOMP_BUTTON_SCALE).toInt()
                }
                render!!.scheduleForDraw(bitmap, stompButtonLocation, SortConstants.HUD, false)
            }
            if (movementSliderMode && movementSliderBaseDrawable != null && movementSliderButtonDrawable != null) {
                if (movementSliderBaseDrawable!!.width == 0) {
                    // first time init
                    val tex = movementSliderBaseDrawable!!.texture
                    movementSliderBaseDrawable!!.resize(tex!!.width, tex.height)
                }
                if (movementSliderButtonDrawable!!.width == 0) {
                    // first time init
                    val tex = movementSliderButtonDrawable!!.texture
                    movementSliderButtonDrawable!!.resize(tex!!.width, tex.height)
                }
                if (movementSliderButtonDepressedDrawable!!.width == 0) {
                    // first time init
                    val tex = movementSliderButtonDepressedDrawable!!.texture
                    movementSliderButtonDepressedDrawable!!.resize(tex!!.width, tex.height)
                }
                var bitmap = movementSliderButtonDrawable
                if (movementSliderButtonPressed) {
                    bitmap = movementSliderButtonDepressedDrawable
                }
                render!!.scheduleForDraw(movementSliderBaseDrawable, movementSliderBaseLocation, SortConstants.HUD, false)
                render.scheduleForDraw(bitmap, movementSliderButtonLocation, SortConstants.HUD + 1, false)
            }
            if (coinDrawable != null) {
                if (coinDrawable!!.width == 0) {
                    // first time init
                    val tex = coinDrawable!!.texture
                    coinDrawable!!.resize(tex!!.width, tex.height)
                    coinLocation.x = params!!.gameWidth / 2.0f - tex.width / 2.0f
                    coinLocation.y = (params.gameHeight - tex.height - COLLECTABLE_EDGE_PADDING).toFloat()
                }
                render!!.scheduleForDraw(coinDrawable, coinLocation, SortConstants.HUD, false)
                if (coinDigitsChanged) {
                    intToDigitArray(mCoinCount, coinDigits)
                    coinDigitsChanged = false
                }
                val offset = coinDrawable!!.width * 0.75f
                coinLocation.x += offset
                drawNumber(coinLocation, coinDigits, true)
                coinLocation.x -= offset
            }
            if (rubyDrawable != null) {
                if (rubyDrawable!!.width == 0) {
                    // first time init
                    val tex = rubyDrawable!!.texture
                    rubyDrawable!!.resize(tex!!.width, tex.height)
                    rubyLocation.x = params!!.gameWidth / 2.0f + 100.0f
                    rubyLocation.y = (params.gameHeight - tex.height - COLLECTABLE_EDGE_PADDING).toFloat()
                }
                render!!.scheduleForDraw(rubyDrawable, rubyLocation, SortConstants.HUD, false)
                if (rubyDigitsChanged) {
                    intToDigitArray(mRubyCount, rubyDigits)
                    rubyDigitsChanged = false
                }
                val offset = rubyDrawable!!.width * 0.75f
                rubyLocation.x += offset
                drawNumber(rubyLocation, rubyDigits, true)
                rubyLocation.x -= offset
            }
        }
        if (showFPS) {
            if (fPSDigitsChanged) {
                val count = intToDigitArray(fPS, fPSDigits)
                fPSDigitsChanged = false
                fPSLocation[params!!.gameWidth - 10.0f - (count + 1) * (digitDrawables[0]!!.width / 2.0f)] = 10.0f
            }
            drawNumber(fPSLocation, fPSDigits, false)
        }
        if (isFading && factory != null) {
            val time = sSystemRegistry.timeSystem!!.realTime
            val fadeDelta = time - fadeStartTime
            var percentComplete = 1.0f
            if (fadeDelta < fadeDuration) {
                percentComplete = fadeDelta / fadeDuration
            } else if (fadeIn) {
                // We've faded in.  Turn fading off.
                isFading = false
            }
            if (percentComplete < 1.0f || !fadeIn) {
                var opacityValue = percentComplete
                if (fadeIn) {
                    opacityValue = 1.0f - percentComplete
                }
                val bitmap = factory.allocateDrawableBitmap()
                bitmap.width = params!!.gameWidth
                bitmap.height = params.gameHeight
                bitmap.texture = fadeTexture
                bitmap.setCrop(0, fadeTexture!!.height, fadeTexture!!.width, fadeTexture!!.height)
                bitmap.setOpacity(opacityValue)
                render!!.scheduleForDraw(bitmap, Vector2.ZERO, SortConstants.FADE, false)
            }
            if (percentComplete >= 1.0f && fadePendingEventType != GameFlowEvent.EVENT_INVALID) {
                val level = sSystemRegistry.levelSystem
                if (level != null) {
                    level.sendGameEvent(fadePendingEventType, fadePendingEventIndex, false)
                    fadePendingEventType = GameFlowEvent.EVENT_INVALID
                    fadePendingEventIndex = 0
                }
            }
        }
    }

    private fun drawNumber(location: Vector2, digits: IntArray, drawX: Boolean) {
        val render = sSystemRegistry.renderSystem
        if (digitDrawables[0]!!.width == 0) {
            // first time init
            for (x in digitDrawables.indices) {
                val tex = digitDrawables[x]!!.texture
                digitDrawables[x]!!.resize(tex!!.width, tex.height)
            }
        }
        if (xDrawable!!.width == 0) {
            // first time init
            val tex = xDrawable!!.texture
            xDrawable!!.resize(tex!!.width, tex.height)
        }
        val characterWidth = digitDrawables[0]!!.width / 2.0f
        var offset = 0.0f
        if (xDrawable != null && drawX) {
            render!!.scheduleForDraw(xDrawable, location, SortConstants.HUD, false)
            location.x += characterWidth
            offset += characterWidth
        }
        var x = 0
        while (x < digits.size && digits[x] != -1) {
            val index = digits[x]
            val digit = digitDrawables[index]
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
        fadePendingEventType = eventType
        fadePendingEventIndex = eventIndex
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
        coinDigits = IntArray(MAX_DIGITS)
        rubyDigits = IntArray(MAX_DIGITS)
        fPSDigits = IntArray(MAX_DIGITS)
        movementSliderBaseLocation = Vector2()
        movementSliderButtonLocation = Vector2()
        reset()
    }
}
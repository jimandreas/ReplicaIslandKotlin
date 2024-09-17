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
@file:Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER", "SameParameterValue")

package com.replica.replicaisland

import android.view.KeyEvent
import kotlin.math.abs
import kotlin.math.max

class InputGameInterface : BaseObject() {
    val jumpButton = InputButton()
    val attackButton = InputButton()
    val directionalPad = InputXY()
    val tilt = InputXY()
    private var leftKeyCode = KeyEvent.KEYCODE_DPAD_LEFT
    private var rightKeyCode = KeyEvent.KEYCODE_DPAD_RIGHT
    private var jumpKeyCode = KeyEvent.KEYCODE_SPACE
    private var attackKeyCode = KeyEvent.KEYCODE_SHIFT_LEFT
    private val orientationDeadZoneMin = ORIENTATION_DEAD_ZONE_MIN
    private val orientationDeadZoneMax = ORIENTATION_DEAD_ZONE_MAX
    private val orientationDeadZoneScale = ORIENTATION_DEAD_ZONE_SCALE
    private var orientationSensitivity = 1.0f
    private var orientationSensitivityFactor = 1.0f
    private var movementSensitivity = 1.0f
    private var useClickButtonForAttack = true
    private var useOrientationForMovement = false
    private var useOnScreenControls = false
    private var lastRollTime = 0f
    override fun reset() {
        jumpButton.release()
        attackButton.release()
        directionalPad.release()
        tilt.release()
    }

    override fun update(timeDelta: Float, parent: BaseObject?) {
        val input = sSystemRegistry.inputSystem
        val keys = input!!.fetchKeyboard().keys
        val orientation = input.fetchOrientationSensor()

        // tilt is easy
        tilt.clone(orientation)
        val touch = input.fetchTouchScreen()
        val gameTime = sSystemRegistry.timeSystem!!.gameTime
        var sliderOffset = 0f

        // update movement inputs
        if (useOnScreenControls) {
            val sliderTouch = touch.findPointerInRegion(
                    ButtonConstants.MOVEMENT_SLIDER_REGION_X.toFloat(),
                    ButtonConstants.MOVEMENT_SLIDER_REGION_Y.toFloat(),
                    ButtonConstants.MOVEMENT_SLIDER_REGION_WIDTH.toFloat(),
                    ButtonConstants.MOVEMENT_SLIDER_REGION_HEIGHT.toFloat())
            if (sliderTouch != null) {
                val halfWidth = ButtonConstants.MOVEMENT_SLIDER_BAR_WIDTH / 2.0f
                val center = ButtonConstants.MOVEMENT_SLIDER_X + halfWidth
                val offset = sliderTouch.retreiveXaxisMagnitude() - center
                val magnitudeRamp = if (abs(offset) > halfWidth) 1.0f else abs(offset) / halfWidth
                val magnitude = magnitudeRamp * Utils.sign(offset) * SLIDER_FILTER * movementSensitivity
                sliderOffset = magnitudeRamp * Utils.sign(offset)
                directionalPad.press(gameTime, magnitude, 0.0f)
            } else {
                directionalPad.release()
            }
        } else if (useOrientationForMovement) {
            directionalPad.clone(orientation)
            directionalPad.setMagnitude(
                    filterOrientationForMovement(orientation.retreiveXaxisMagnitude()),
                    filterOrientationForMovement(orientation.retreiveYaxisMagnitude()))
        } else {
            // keys or trackball
            val trackball = input.fetchTrackball()
            val left = keys[leftKeyCode]
            val right = keys[rightKeyCode]
            val leftPressedTime = left!!.lastPressedTime
            val rightPressedTime = right!!.lastPressedTime
            if (trackball.lastPressedTime > max(leftPressedTime, rightPressedTime)) {
                // The trackball never goes "up", so force it to turn off if it wasn't triggered in the last frame.
                // What follows is a bunch of code to filter trackball events into something like a dpad event.
                // The goals here are:
                // 	- For roll events that occur in quick succession to accumulate.
                //	- For roll events that occur with more time between them, lessen the impact of older events
                //	- In the absence of roll events, fade the roll out over time.
                if (gameTime - trackball.lastPressedTime < ROLL_TIMEOUT) {
                    val newX: Float
                    val newY: Float
                    val delay = max(ROLL_RESET_DELAY, timeDelta)
                    if (gameTime - lastRollTime <= delay) {
                        newX = directionalPad.retreiveXaxisMagnitude() + trackball.retreiveXaxisMagnitude() * ROLL_FILTER * movementSensitivity
                        newY = directionalPad.retreiveYaxisMagnitude() + trackball.retreiveYaxisMagnitude() * ROLL_FILTER * movementSensitivity
                    } else {
                        val oldX = if (directionalPad.retreiveXaxisMagnitude() != 0.0f) directionalPad.retreiveXaxisMagnitude() / 2.0f else 0.0f
                        val oldY = if (directionalPad.retreiveXaxisMagnitude() != 0.0f) directionalPad.retreiveXaxisMagnitude() / 2.0f else 0.0f
                        newX = oldX + trackball.retreiveXaxisMagnitude() * ROLL_FILTER * movementSensitivity
                        newY = oldY + trackball.retreiveXaxisMagnitude() * ROLL_FILTER * movementSensitivity
                    }
                    directionalPad.press(gameTime, newX, newY)
                    lastRollTime = gameTime
                    trackball.release()
                } else {
                    var x = directionalPad.retreiveXaxisMagnitude()
                    var y = directionalPad.retreiveYaxisMagnitude()
                    if (x != 0.0f) {
                        val sign = Utils.sign(x)
                        x -= sign * ROLL_DECAY * timeDelta
                        if (Utils.sign(x) != sign) {
                            x = 0.0f
                        }
                    }
                    if (y != 0.0f) {
                        val sign = Utils.sign(y)
                        y -= sign * ROLL_DECAY * timeDelta
                        if (Utils.sign(x) != sign) {
                            y = 0.0f
                        }
                    }
                    if (x == 0f && y == 0f) {
                        directionalPad.release()
                    } else {
                        directionalPad.setMagnitude(x, y)
                    }
                }
            } else {
                var xMagnitude = 0.0f
                val yMagnitude = 0.0f
                var pressTime = 0.0f
                // left and right are mutually exclusive
                if (leftPressedTime > rightPressedTime) {
                    xMagnitude = -left.magnitude * KEY_FILTER * movementSensitivity
                    pressTime = leftPressedTime
                } else {
                    xMagnitude = right.magnitude * KEY_FILTER * movementSensitivity
                    pressTime = rightPressedTime
                }
                if (xMagnitude != 0.0f) {
                    directionalPad.press(pressTime, xMagnitude, yMagnitude)
                } else {
                    directionalPad.release()
                }
            }
        }

        // update other buttons
        val jumpKey = keys[jumpKeyCode]

        // when on-screen movement controls are on, the fly and attack buttons are flipped.
        var flyButtonRegionX = ButtonConstants.FLY_BUTTON_REGION_X.toFloat()
        var stompButtonRegionX = ButtonConstants.STOMP_BUTTON_REGION_X.toFloat()
        if (useOnScreenControls) {
            val params = sSystemRegistry.contextParameters
            flyButtonRegionX = params!!.gameWidth - ButtonConstants.FLY_BUTTON_REGION_WIDTH - ButtonConstants.FLY_BUTTON_REGION_X.toFloat()
            stompButtonRegionX = params.gameWidth - ButtonConstants.STOMP_BUTTON_REGION_WIDTH - ButtonConstants.STOMP_BUTTON_REGION_X.toFloat()
        }
        val jumpTouch = touch.findPointerInRegion(
                flyButtonRegionX,
                ButtonConstants.FLY_BUTTON_REGION_Y.toFloat(),
                ButtonConstants.FLY_BUTTON_REGION_WIDTH.toFloat(),
                ButtonConstants.FLY_BUTTON_REGION_HEIGHT.toFloat())
        if (jumpKey!!.pressed) {
            jumpButton.press(jumpKey.lastPressedTime, jumpKey.magnitude)
        } else if (jumpTouch != null) {
            if (!jumpButton.pressed) {
                jumpButton.press(jumpTouch.lastPressedTime, 1.0f)
            }
        } else {
            jumpButton.release()
        }
        val attackKey = keys[attackKeyCode]
        val clickButton = keys[KeyEvent.KEYCODE_DPAD_CENTER] // special case
        val stompTouch = touch.findPointerInRegion(
                stompButtonRegionX,
                ButtonConstants.STOMP_BUTTON_REGION_Y.toFloat(),
                ButtonConstants.STOMP_BUTTON_REGION_WIDTH.toFloat(),
                ButtonConstants.STOMP_BUTTON_REGION_HEIGHT.toFloat())
        if (useClickButtonForAttack && clickButton!!.pressed) {
            attackButton.press(clickButton.lastPressedTime, clickButton.magnitude)
        } else if (attackKey!!.pressed) {
            attackButton.press(attackKey.lastPressedTime, attackKey.magnitude)
        } else if (stompTouch != null) {
            // Since touch events come in constantly, we only want to press the attack button
            // here if it's not already down.  That makes it act like the other buttons (down once then up).
            if (!attackButton.pressed) {
                attackButton.press(stompTouch.lastPressedTime, 1.0f)
            }
        } else {
            attackButton.release()
        }

        // This doesn't seem like exactly the right place to write to the HUD, but on the other hand,
        // putting this code elsewhere causes dependencies between exact HUD content and physics, which
        // we sometimes wish to avoid.
        val hud = sSystemRegistry.hudSystem
        if (hud != null) {
            hud.setButtonState(jumpButton.pressed, attackButton.pressed, directionalPad.pressed)
            hud.setMovementSliderOffset(sliderOffset)
        }
    }

    private fun filterOrientationForMovement(magnitude: Float): Float {
        val scaledMagnitude = magnitude * orientationSensitivityFactor
        return deadZoneFilter(scaledMagnitude, orientationDeadZoneMin, orientationDeadZoneMax, orientationDeadZoneScale)
    }

    private fun deadZoneFilter(magnitude: Float, minVal: Float, maxVal: Float, scale: Float): Float {
        var smoothedMagnatude = magnitude
        if (abs(magnitude) < minVal) {
            smoothedMagnatude = 0.0f // dead zone
        } else if (abs(magnitude) < maxVal) {
            smoothedMagnatude *= scale
        }
        return smoothedMagnatude
    }

    fun setKeys(left: Int, right: Int, jump: Int, attack: Int) {
        leftKeyCode = left
        rightKeyCode = right
        jumpKeyCode = jump
        attackKeyCode = attack
    }

    fun setUseClickForAttack(click: Boolean) {
        useClickButtonForAttack = click
    }

    fun setUseOrientationForMovement(orientation: Boolean) {
        useOrientationForMovement = orientation
    }

    fun setOrientationMovementSensitivity(sensitivity: Float) {
        orientationSensitivity = sensitivity
        orientationSensitivityFactor = 2.9f * sensitivity + 0.1f
    }

    fun setMovementSensitivity(sensitivity: Float) {
        movementSensitivity = sensitivity
    }

    fun setUseOnScreenControls(onscreen: Boolean) {
        useOnScreenControls = onscreen
    }

    companion object {
        private const val ORIENTATION_DEAD_ZONE_MIN = 0.03f
        private const val ORIENTATION_DEAD_ZONE_MAX = 0.1f
        private const val ORIENTATION_DEAD_ZONE_SCALE = 0.75f
        private const val ROLL_TIMEOUT = 0.1f
        private const val ROLL_RESET_DELAY = 0.075f

        // Raw trackball input is filtered by this value. Increasing it will
        // make the control more twitchy, while decreasing it will make the control more precise.
        private const val ROLL_FILTER = 0.4f
        private const val ROLL_DECAY = 8.0f
        private const val KEY_FILTER = 0.25f
        private const val SLIDER_FILTER = 0.25f
    }

    init {
        reset()
    }
}
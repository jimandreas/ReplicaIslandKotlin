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
@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package com.replica.replicaisland

import android.os.SystemClock

/**
 * The GameThread contains the main loop for the game engine logic.  It invokes the game graph,
 * manages synchronization of input events, and handles the draw queue swap with the rendering
 * thread.
 */
// TODO 2 : apply a kotlin solution to this issue
// https://stackoverflow.com/a/44589962/3853712

fun Any.wait() = (this as Object).wait()
fun Any.notify() = (this as Object).notify()
fun Any.notifyAll() = (this as Object).notifyAll()

class GameThread(renderer: GameRenderer) : Runnable {
    private var lastTime: Long
    private var mGameRoot: ObjectManager? = null
    private val mRenderer: GameRenderer
    private val pauseLock: Any
    private var finished: Boolean
    var paused = false
        private set
    private var profileFrames = 0
    private var profileTime: Long = 0
    override fun run() {
        lastTime = SystemClock.uptimeMillis()
        finished = false
        while (!finished) {
            if (mGameRoot != null) {
                mRenderer.waitDrawingComplete()
                val time = SystemClock.uptimeMillis()
                val timeDelta = time - lastTime
                var finalDelta = timeDelta
                if (timeDelta > 12) {
                    var secondsDelta = (time - lastTime) * 0.001f
                    if (secondsDelta > 0.1f) {
                        secondsDelta = 0.1f
                    }
                    lastTime = time
                    mGameRoot!!.update(secondsDelta, null)
                    val camera = BaseObject.sSystemRegistry.cameraSystem
                    var x = 0.0f
                    var y = 0.0f
                    if (camera != null) {
                        x = camera.fetchFocusPositionX()
                        y = camera.fetchFocusPositionY()
                    }
                    BaseObject.sSystemRegistry.renderSystem!!.swap(mRenderer, x, y)
                    val endTime = SystemClock.uptimeMillis()
                    finalDelta = endTime - time
                    profileTime += finalDelta
                    profileFrames++
                    if (profileTime > PROFILE_REPORT_DELAY * 1000) {
                        var averageFrameTime = profileTime / profileFrames
                        DebugLog.d("Game Profile", "Average: $averageFrameTime")
                        profileTime = 0
                        profileFrames = 0
                        if (averageFrameTime < 2) averageFrameTime = 1
                        BaseObject.sSystemRegistry.hudSystem!!.setFPS(1000 / averageFrameTime.toInt())
                    }
                }
                // If the game logic completed in less than 16ms, that means it's running
                // faster than 60fps, which is our target frame rate.  In that case we should
                // yield to the rendering thread, at least for the remaining frame.
                if (finalDelta < 16) {
                    try {
                        Thread.sleep(16 - finalDelta)
                    } catch (e: InterruptedException) {
                        // Interruptions here are no big deal.
                    }
                }
                synchronized(pauseLock) {
                    if (paused) {
                        val sound = BaseObject.sSystemRegistry.soundSystem
                        if (sound != null) {
                            sound.pauseAll()
                            BaseObject.sSystemRegistry.inputSystem!!.releaseAllKeys()
                        }
                        while (paused) {
                            try {
                                pauseLock.wait()
                            } catch (e: InterruptedException) {
                                // No big deal if this wait is interrupted.
                            }
                        }
                    }
                }
            }
        }
        // Make sure our dependence on the render system is cleaned up.
        BaseObject.sSystemRegistry.renderSystem!!.emptyQueues(mRenderer)
    }

    fun stopGame() {
        synchronized(pauseLock) {
            paused = false
            finished = true
            pauseLock.notifyAll()
        }
    }

    fun pauseGame() {
        synchronized(pauseLock) { paused = true }
    }

    fun resumeGame() {
        synchronized(pauseLock) {
            paused = false
            pauseLock.notifyAll()
        }
    }

    fun setGameRoot(gameRoot: ObjectManager?) {
        mGameRoot = gameRoot
    }

    companion object {
        private const val PROFILE_REPORT_DELAY = 3.0f
    }

    init {
        lastTime = SystemClock.uptimeMillis()
        mRenderer = renderer
        pauseLock = Any()
        finished = false
        paused = false
    }
}
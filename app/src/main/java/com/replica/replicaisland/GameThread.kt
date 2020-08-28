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
    private var mLastTime: Long
    private var mGameRoot: ObjectManager? = null
    private val mRenderer: GameRenderer
    private val mPauseLock: Any
    private var mFinished: Boolean
    var paused = false
        private set
    private var mProfileFrames = 0
    private var mProfileTime: Long = 0
    override fun run() {
        mLastTime = SystemClock.uptimeMillis()
        mFinished = false
        while (!mFinished) {
            if (mGameRoot != null) {
                mRenderer.waitDrawingComplete()
                val time = SystemClock.uptimeMillis()
                val timeDelta = time - mLastTime
                var finalDelta = timeDelta
                if (timeDelta > 12) {
                    var secondsDelta = (time - mLastTime) * 0.001f
                    if (secondsDelta > 0.1f) {
                        secondsDelta = 0.1f
                    }
                    mLastTime = time
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
                    mProfileTime += finalDelta
                    mProfileFrames++
                    if (mProfileTime > PROFILE_REPORT_DELAY * 1000) {
                        var averageFrameTime = mProfileTime / mProfileFrames
                        DebugLog.d("Game Profile", "Average: $averageFrameTime")
                        mProfileTime = 0
                        mProfileFrames = 0
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
                synchronized(mPauseLock) {
                    if (paused) {
                        val sound = BaseObject.sSystemRegistry.soundSystem
                        if (sound != null) {
                            sound.pauseAll()
                            BaseObject.sSystemRegistry.inputSystem!!.releaseAllKeys()
                        }
                        while (paused) {
                            try {
                                mPauseLock.wait()
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
        synchronized(mPauseLock) {
            paused = false
            mFinished = true
            mPauseLock.notifyAll()
        }
    }

    fun pauseGame() {
        synchronized(mPauseLock) { paused = true }
    }

    fun resumeGame() {
        synchronized(mPauseLock) {
            paused = false
            mPauseLock.notifyAll()
        }
    }

    fun setGameRoot(gameRoot: ObjectManager?) {
        mGameRoot = gameRoot
    }

    companion object {
        private const val PROFILE_REPORT_DELAY = 3.0f
    }

    init {
        mLastTime = SystemClock.uptimeMillis()
        mRenderer = renderer
        mPauseLock = Any()
        mFinished = false
        paused = false
    }
}
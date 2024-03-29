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

/**
 * Implements rendering of a drawable object for a game object.  If a drawable is set on this
 * component it will be passed to the renderer and drawn on the screen every frame.  Drawable
 * objects may be set to be "camera-relative" (meaning their screen position is relative to the
 * location of the camera focus in the scene) or not (meaning their screen position is relative to
 * the origin at the lower-left corner of the display).
 */
class RenderComponent : GameComponent() {
    var drawable: DrawableObject? = null
    var priority = 0
    private var cameraRelative = false
    private val positionWorkspace: Vector2
    private val screenLocation: Vector2
    private val drawOffset: Vector2
    override fun reset() {
        priority = 0
        cameraRelative = true
        drawable = null
        drawOffset.zero()
    }

    override fun update(timeDelta: Float, parent: BaseObject?) {
        if (drawable != null) {
            val system = sSystemRegistry.renderSystem
            if (system != null) {
                positionWorkspace.set((parent as GameObject).position)
                positionWorkspace.add(drawOffset)
                if (cameraRelative) {
                    val camera = sSystemRegistry.cameraSystem
                    val params = sSystemRegistry.contextParameters
                    screenLocation.x = (positionWorkspace.x - camera!!.fetchFocusPositionX()
                            + params!!.gameWidth / 2)
                    screenLocation.y = (positionWorkspace.y - camera.fetchFocusPositionY()
                            + params.gameHeight / 2)
                }
                // It might be better not to do culling here, as doing it in the render thread
                // would allow us to have multiple views into the same scene and things like that.
                // But at the moment significant CPU is being spent on sorting the list of objects
                // to draw per frame, so I'm going to go ahead and cull early.
                if (drawable!!.visibleAtPosition(screenLocation)) {
                    system.scheduleForDraw(drawable, positionWorkspace, priority, cameraRelative)
                } else if (drawable!!.parentPool != null) {
                    // Normally the render system releases drawable objects back to the factory
                    // pool, but in this case we're short-circuiting the render system, so we
                    // need to release the object manually.
                    sSystemRegistry.drawableFactory!!.release(drawable!!)
                    drawable = null
                }
            }
        }
    }

    fun setCameraRelative(relative: Boolean) {
        cameraRelative = relative
    }

    fun setDrawOffset(x: Float, y: Float) {
        drawOffset[x] = y
    }

    init {
        setPhaseToThis(ComponentPhases.DRAW.ordinal)
        positionWorkspace = Vector2()
        screenLocation = Vector2()
        drawOffset = Vector2()
        reset()
    }
}
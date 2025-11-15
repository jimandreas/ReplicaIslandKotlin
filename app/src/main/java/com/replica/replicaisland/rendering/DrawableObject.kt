package com.replica.replicaisland.rendering

import com.replica.replicaisland.AllocationGuard
import com.replica.replicaisland.ObjectPool
import com.replica.replicaisland.Vector2

/**
 * DrawableObject is the base object interface for objects that can be rendered to the screen.
 * Note that objects derived from DrawableObject are passed between threads, and that care must be
 * taken when modifying drawable parameters to avoid side-effects (for example, the DrawableFactory
 * class can be used to generate fire-and-forget drawables).
 */
abstract class DrawableObject : AllocationGuard() {
    var priority = 0f
    var parentPool: ObjectPool? = null
    abstract fun draw(x: Float, y: Float, scaleX: Float, scaleY: Float)

    // Override to allow drawables to be sorted by texture.
    open val texture: Texture?
        get() = null

    // Function to allow drawables to specify culling rules.
    open fun visibleAtPosition(position: Vector2?): Boolean {
        return true
    }
}
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
@file:Suppress("UNCHECKED_CAST")

package com.replica.replicaisland

/**
 * Manages a double-buffered queue of renderable objects.  The game thread submits drawable objects
 * to the the active render queue while the render thread consumes drawables from the alternate
 * queue.  When both threads complete a frame the queues are swapped.  Note that this class can
 * manage any number (>=2) of render queues, but increasing the number over two means that the game
 * logic will be running significantly ahead of the rendering thread, which may make the user feel
 * that the controls are "loose."
 */
class RenderSystem : BaseObject() {
    private val mElementPool: RenderElementPool
    private val renderQueues: Array<ObjectManager?>
    private var queueIndex: Int
    override fun reset() {}
    fun scheduleForDraw(`object`: DrawableObject?, position: Vector2, priority: Int, cameraRelative: Boolean) {
        val element = mElementPool.allocate()
        if (element != null) {
            element[`object`, position, priority] = cameraRelative
            renderQueues[queueIndex]!!.add(element)
        }
    }

    private fun clearQueue(objects: FixedSizeArray<BaseObject?>) {
        val count = objects.count
        val objectArray: Array<Any?> = objects.array as Array<Any?>
        val elementPool = mElementPool
        for (i in count - 1 downTo 0) {
            val element = objectArray[i] as RenderElement?
            elementPool.release(element!!)
            objects.removeLast()
        }
    }

    fun swap(renderer: GameRenderer, cameraX: Float, cameraY: Float) {
        renderQueues[queueIndex]!!.commitUpdates()

        // This code will block if the previous queue is still being executed.
        renderer.setDrawQueue(renderQueues[queueIndex], cameraX, cameraY)
        val lastQueue = if (queueIndex == 0) DRAW_QUEUE_COUNT - 1 else queueIndex - 1

        // Clear the old queue.
        val objects = renderQueues[lastQueue]!!.fetchObjects()
        clearQueue(objects)
        queueIndex = (queueIndex + 1) % DRAW_QUEUE_COUNT
    }

    /* Empties all draw queues and disconnects the game thread from the renderer. */
    fun emptyQueues(renderer: GameRenderer) {
        renderer.setDrawQueue(null, 0.0f, 0.0f)
        for (x in 0 until DRAW_QUEUE_COUNT) {
            renderQueues[x]!!.commitUpdates()
            val objects = renderQueues[x]!!.fetchObjects()
            clearQueue(objects)
        }
    }

    inner class RenderElement : PhasedObject() {
        operator fun set(drawable: DrawableObject?, position: Vector2, priority: Int, isCameraRelative: Boolean) {
            mDrawable = drawable
            x = position.x
            y = position.y
            cameraRelative = isCameraRelative
            val sortBucket = priority * TEXTURE_SORT_BUCKET_SIZE
            var sortOffset = 0
            if (drawable != null) {
                val tex = drawable.texture
                if (tex != null) {
                    sortOffset = tex.resource % TEXTURE_SORT_BUCKET_SIZE * Utils.sign(priority.toFloat())
                }
            }
            setPhaseToThis(sortBucket + sortOffset)
        }

        override fun reset() {
            mDrawable = null
            x = 0.0f
            y = 0.0f
            cameraRelative = false
        }

        @JvmField
        var mDrawable: DrawableObject? = null
        @JvmField
        var x = 0f
        @JvmField
        var y = 0f
        @JvmField
        var cameraRelative = false
    }

    private inner class RenderElementPool
        constructor(max: Int) : TObjectPool<RenderElement?>(max) {
        override fun release(entry: Any) {
            val renderable = entry as RenderElement
            // if this drawable came out of a pool, make sure it is returned to that pool.
            val pool = renderable.mDrawable!!.parentPool
            pool?.release(renderable.mDrawable!!)
            // reset on release
            renderable.reset()
            super.release(entry)
        }

        override fun fill() {
            for (x in 0 until fetchSize()) {
                fetchAvailable()!!.add(RenderElement())
            }
        }
    }

    companion object {
        private const val TEXTURE_SORT_BUCKET_SIZE = 1000
        private const val DRAW_QUEUE_COUNT = 2
        private const val MAX_RENDER_OBJECTS_PER_FRAME = 384
        private const val MAX_RENDER_OBJECTS = MAX_RENDER_OBJECTS_PER_FRAME * DRAW_QUEUE_COUNT
    }

    init {
        mElementPool = RenderElementPool(MAX_RENDER_OBJECTS)
        renderQueues = arrayOfNulls(DRAW_QUEUE_COUNT)
        for (x in 0 until DRAW_QUEUE_COUNT) {
            renderQueues[x] = PhasedObjectManager(MAX_RENDER_OBJECTS_PER_FRAME)
        }
        queueIndex = 0
    }
}
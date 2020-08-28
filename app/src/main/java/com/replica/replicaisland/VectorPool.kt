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
 * A pool of 2D vectors.
 */
class VectorPool : TObjectPool<Vector2?>() {
    override fun fill() {
        for (x in 0 until fetchSize()) {
            fetchAvailable()!!.add(Vector2())
        }
    }

    override fun release(entry: Any) {
        (entry as Vector2).zero()
        super.release(entry)
    }

    /** Allocates a vector and assigns the value of the passed source vector to it.  */
    fun allocate(source: Vector2?): Vector2 {
        val entry = super.allocate()!!
        entry.set(source!!)
        return entry
    }
}
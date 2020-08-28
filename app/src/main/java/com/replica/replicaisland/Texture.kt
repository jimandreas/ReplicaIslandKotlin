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
 * Simple container class for textures.  Serves as a mapping between Android resource ids and
 * OpenGL texture names, and also as a placeholder object for textures that may or may not have
 * been loaded into vram.  Objects can cache Texture objects but should *never* cache the texture
 * name itself, as it may change at any time.
 */
class Texture : AllocationGuard() {
    @JvmField
    var resource = 0
    @JvmField
    var name = 0
    @JvmField
    var width = 0
    @JvmField
    var height = 0
    @JvmField
    var loaded = false
    fun reset() {
        resource = -1
        name = -1
        width = 0
        height = 0
        loaded = false
    }

    init {
        reset()
    }
}
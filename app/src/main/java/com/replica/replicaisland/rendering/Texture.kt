package com.replica.replicaisland.rendering

import com.replica.replicaisland.utils.AllocationGuard

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
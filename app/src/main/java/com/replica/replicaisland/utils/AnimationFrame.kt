package com.replica.replicaisland.utils

import com.replica.replicaisland.mechanics.CollisionVolume
import com.replica.replicaisland.rendering.Texture

/**
 * A single animation frame.  Frames contain a texture, a hold time, and collision volumes to
 * use for "attacking" or "vulnerability."  This allows animated sprites to cheaply interact with
 * other objects in the game world by associating collision information with particular animation
 * frames.  Note that an animation frame may have a null texture and null collision volumes.  Null
 * collision volumes will exclude that frame from collision detection and a null texture will
 * prevent the sprite from drawing.
 */
class AnimationFrame : AllocationGuard {
    
    var texture: Texture? = null
    
    var holdTime: Float
    
    var attackVolumes: FixedSizeArray<CollisionVolume>? = null
    
    var vulnerabilityVolumes: FixedSizeArray<CollisionVolume>? = null

    constructor(textureObject: Texture?, animationHoldTime: Float) : super() {
        if (textureObject != null) {
            texture = textureObject
        }
        holdTime = animationHoldTime
    }

    constructor(textureObject: Texture?, animationHoldTime: Float,
                attackVolumeList: FixedSizeArray<CollisionVolume>?,
                vulnerabilityVolumeList: FixedSizeArray<CollisionVolume>?) : super() {
        if (textureObject != null) {
            texture = textureObject
        }
        holdTime = animationHoldTime
        attackVolumes = attackVolumeList
        vulnerabilityVolumes = vulnerabilityVolumeList
    }
}
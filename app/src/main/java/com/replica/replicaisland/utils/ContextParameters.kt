package com.replica.replicaisland.utils

import android.content.Context
import com.replica.replicaisland.core.BaseObject

/** Contains global (but typically constant) parameters about the current operating context  */
class ContextParameters : BaseObject() {
    
    var viewWidth = 0
    
    var viewHeight = 0
    
    var context: Context? = null
    
    var gameWidth = 0
    
    var gameHeight = 0
    
    var viewScaleX = 0f
    
    var viewScaleY = 0f
    
    var supportsDrawTexture = false
    
    var supportsVBOs = false
    
    var difficulty = 0
    override fun reset() {}
}
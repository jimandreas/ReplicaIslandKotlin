package com.replica.replicaisland.utils

import android.content.Context
import com.replica.replicaisland.core.BaseObject

/** Contains global (but typically constant) parameters about the current operating context  */
class ContextParameters : BaseObject() {
    @JvmField
    var viewWidth = 0
    @JvmField
    var viewHeight = 0
    @JvmField
    var context: Context? = null
    @JvmField
    var gameWidth = 0
    @JvmField
    var gameHeight = 0
    @JvmField
    var viewScaleX = 0f
    @JvmField
    var viewScaleY = 0f
    @JvmField
    var supportsDrawTexture = false
    @JvmField
    var supportsVBOs = false
    @JvmField
    var difficulty = 0
    override fun reset() {}
}
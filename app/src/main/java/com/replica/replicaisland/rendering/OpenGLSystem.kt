package com.replica.replicaisland.rendering

import com.replica.replicaisland.core.BaseObject
import javax.microedition.khronos.opengles.GL10
import javax.microedition.khronos.opengles.GL11
import javax.microedition.khronos.opengles.GL11Ext

/**
 * An object wrapper for a pointer to the OpenGL context.  Note that the context is only valid
 * in certain threads at certain times (namely, in the Rendering thread during draw time), and at
 * other times getGL() will return null.
 */
class OpenGLSystem : BaseObject {
    constructor() : super() {
        sGL = null
    }

    constructor(gl: GL10?) {
        sGL = gl
    }

    override fun reset() {}

    companion object {
        private var sGL: GL10? = null
        private var sLastBoundTexture = 0
        private var sLastSetCropSignature = 0
        @JvmStatic
        var gL: GL10?
            get() = sGL
            set(gl) {
                sGL = gl
                sLastBoundTexture = 0
                sLastSetCropSignature = 0
            }

        @JvmStatic
        fun bindTexture(target: Int, texture: Int) {
            if (sLastBoundTexture != texture) {
                sGL!!.glBindTexture(target, texture)
                sLastBoundTexture = texture
                sLastSetCropSignature = 0
            }
        }

        fun setTextureCrop(crop: IntArray) {
            var cropSignature = crop[0] + crop[1] shl 16
            cropSignature = cropSignature or crop[2] + crop[3]
            if (cropSignature != sLastSetCropSignature) {
                (sGL as GL11?)!!.glTexParameteriv(
                    GL10.GL_TEXTURE_2D, GL11Ext.GL_TEXTURE_CROP_RECT_OES,
                        crop, 0)
                sLastSetCropSignature = cropSignature
            }
        }
    }
}
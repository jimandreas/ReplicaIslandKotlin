/*
 * Copyright (C) 2010 The Android Open Source Project
 * Copyright (C) 2025 Jim Andreas kotlin conversion
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

@file:Suppress("unused")

package com.replica.replicaisland.rendering

import android.opengl.GLES20
import com.replica.replicaisland.core.BaseObject

/**
 * An object wrapper for OpenGL state management.
 * Provides texture binding with caching to avoid redundant GL calls.
 */
class OpenGLSystem : BaseObject() {
    override fun reset() {}

    companion object {
        var spriteShader: ShaderProgram? = null
        var spriteQuad: SpriteQuad? = null
        var matrixHelper: MatrixHelper? = null

        private var sLastBoundTexture = 0

        fun resetBindings() {
            sLastBoundTexture = 0
        }

        fun bindTexture(target: Int, texture: Int) {
            if (sLastBoundTexture != texture) {
                GLES20.glBindTexture(target, texture)
                sLastBoundTexture = texture
            }
        }
    }
}

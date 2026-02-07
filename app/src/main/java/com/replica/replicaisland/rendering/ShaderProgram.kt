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

package com.replica.replicaisland.rendering

import android.opengl.GLES20
import com.replica.replicaisland.ui.DebugLog

/**
 * Compiles, links, and manages an OpenGL ES 2.0 shader program.
 * Provides uniform/attribute locations for the shared vertex/fragment shaders
 * used by both sprite and tile rendering.
 */
class ShaderProgram {
    var programHandle: Int = 0
        private set

    // Attribute locations
    var aPositionLocation: Int = -1
        private set
    var aTexCoordLocation: Int = -1
        private set

    // Uniform locations
    var uMVPMatrixLocation: Int = -1
        private set
    var uColorLocation: Int = -1
        private set
    var uTextureLocation: Int = -1
        private set

    fun compile(): Boolean {
        val vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER_SOURCE)
        if (vertexShader == 0) return false

        val fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_SOURCE)
        if (fragmentShader == 0) return false

        programHandle = GLES20.glCreateProgram()
        if (programHandle == 0) {
            DebugLog.e(TAG, "Could not create program")
            return false
        }

        GLES20.glAttachShader(programHandle, vertexShader)
        GLES20.glAttachShader(programHandle, fragmentShader)
        GLES20.glLinkProgram(programHandle)

        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(programHandle, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            val log = GLES20.glGetProgramInfoLog(programHandle)
            DebugLog.e(TAG, "Could not link program: $log")
            GLES20.glDeleteProgram(programHandle)
            programHandle = 0
            return false
        }

        // Shaders are attached to program; we can delete the shader objects
        GLES20.glDeleteShader(vertexShader)
        GLES20.glDeleteShader(fragmentShader)

        // Cache locations
        aPositionLocation = GLES20.glGetAttribLocation(programHandle, "aPosition")
        aTexCoordLocation = GLES20.glGetAttribLocation(programHandle, "aTexCoord")
        uMVPMatrixLocation = GLES20.glGetUniformLocation(programHandle, "uMVPMatrix")
        uColorLocation = GLES20.glGetUniformLocation(programHandle, "uColor")
        uTextureLocation = GLES20.glGetUniformLocation(programHandle, "uTexture")

        return true
    }

    fun use() {
        GLES20.glUseProgram(programHandle)
    }

    fun delete() {
        if (programHandle != 0) {
            GLES20.glDeleteProgram(programHandle)
            programHandle = 0
        }
    }

    companion object {
        private const val TAG = "ShaderProgram"

        private const val VERTEX_SHADER_SOURCE = """
            uniform mat4 uMVPMatrix;
            attribute vec4 aPosition;
            attribute vec2 aTexCoord;
            varying vec2 vTexCoord;
            void main() {
                gl_Position = uMVPMatrix * aPosition;
                vTexCoord = aTexCoord;
            }
        """

        private const val FRAGMENT_SHADER_SOURCE = """
            precision mediump float;
            uniform sampler2D uTexture;
            uniform vec4 uColor;
            varying vec2 vTexCoord;
            void main() {
                gl_FragColor = texture2D(uTexture, vTexCoord) * uColor;
            }
        """

        private fun compileShader(type: Int, source: String): Int {
            val shader = GLES20.glCreateShader(type)
            if (shader == 0) {
                DebugLog.e(TAG, "Could not create shader type $type")
                return 0
            }
            GLES20.glShaderSource(shader, source)
            GLES20.glCompileShader(shader)
            val compiled = IntArray(1)
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
            if (compiled[0] == 0) {
                val log = GLES20.glGetShaderInfoLog(shader)
                DebugLog.e(TAG, "Could not compile shader type $type: $log")
                GLES20.glDeleteShader(shader)
                return 0
            }
            return shader
        }
    }
}

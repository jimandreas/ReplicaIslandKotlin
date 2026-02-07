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

import android.opengl.Matrix

/**
 * Wraps android.opengl.Matrix to replace the fixed-function matrix stack.
 * Provides projection, view, and model matrices, plus computeMVP() to
 * multiply them into a single MVP matrix for shader uniforms.
 */
class MatrixHelper {
    val projectionMatrix = FloatArray(16)
    val viewMatrix = FloatArray(16)
    val modelMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)
    val mvpMatrix = FloatArray(16)

    init {
        Matrix.setIdentityM(projectionMatrix, 0)
        Matrix.setIdentityM(viewMatrix, 0)
        Matrix.setIdentityM(modelMatrix, 0)
    }

    fun setOrthographicProjection(left: Float, right: Float, bottom: Float, top: Float, near: Float, far: Float) {
        Matrix.orthoM(projectionMatrix, 0, left, right, bottom, top, near, far)
    }

    fun setIdentityModel() {
        Matrix.setIdentityM(modelMatrix, 0)
    }

    fun translateModel(x: Float, y: Float, z: Float) {
        Matrix.translateM(modelMatrix, 0, x, y, z)
    }

    fun scaleModel(sx: Float, sy: Float, sz: Float) {
        Matrix.scaleM(modelMatrix, 0, sx, sy, sz)
    }

    fun computeMVP(): FloatArray {
        // MVP = Projection * View * Model
        Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
        return mvpMatrix
    }
}

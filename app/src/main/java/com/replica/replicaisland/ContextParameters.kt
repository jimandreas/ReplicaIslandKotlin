/*
 * Copyright (C) 2010 The Android Open Source Project
 * Copyright (C) 2024 Jim Andreas kotlin conversion and updating
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

import android.content.Context

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
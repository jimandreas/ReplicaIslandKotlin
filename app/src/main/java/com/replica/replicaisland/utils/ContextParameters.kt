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
    
    var difficulty = 0
    override fun reset() {}
}
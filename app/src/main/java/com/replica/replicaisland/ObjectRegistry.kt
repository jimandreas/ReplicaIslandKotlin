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
package com.replica.replicaisland

import com.replica.replicaisland.core.BaseObject
import com.replica.replicaisland.core.GameObjectFactory
import com.replica.replicaisland.core.GameObjectManager
import com.replica.replicaisland.entities.HitPointPool
import com.replica.replicaisland.input.InputGameInterface
import com.replica.replicaisland.input.InputSystem
import com.replica.replicaisland.levels.LevelBuilder
import com.replica.replicaisland.mechanics.ChannelSystem
import com.replica.replicaisland.mechanics.CollisionSystem
import com.replica.replicaisland.mechanics.EventRecorder
import com.replica.replicaisland.mechanics.HotSpotSystem
import com.replica.replicaisland.mechanics.TimeSystem
import com.replica.replicaisland.rendering.CameraSystem
import com.replica.replicaisland.rendering.DrawableFactory
import com.replica.replicaisland.rendering.OpenGLSystem
import com.replica.replicaisland.rendering.RenderSystem
import com.replica.replicaisland.rendering.TextureLibrary
import com.replica.replicaisland.sound.SoundSystem
import com.replica.replicaisland.ui.CustomToastSystem
import com.replica.replicaisland.ui.DebugSystem
import com.replica.replicaisland.ui.HudSystem
import com.replica.replicaisland.utils.BufferLibrary
import com.replica.replicaisland.utils.ContextParameters
import com.replica.replicaisland.utils.VectorPool
import java.util.*

/**
 * The object registry manages a collection of global singleton objects.  However, it differs from
 * the standard singleton pattern in a few important ways:
 * - The objects managed by the registry have an undefined lifetime.  They may become invalid at
 * any time, and they may not be valid at the beginning of the program.
 * - The only object that is always guaranteed to be valid is the ObjectRegistry itself.
 * - There may be more than one ObjectRegistry, and there may be more than one instance of any of
 * the systems managed by ObjectRegistry allocated at once.  For example, separate threads may
 * maintain their own separate ObjectRegistry instances.
 */
// TODO: fixup null references before converting
class ObjectRegistry : BaseObject() {
    
    var bufferLibrary: BufferLibrary? = null
    
    var cameraSystem: CameraSystem? = null
    
    var channelSystem: ChannelSystem? = null
    
    var collisionSystem: CollisionSystem? = null
    
    var contextParameters: ContextParameters? = null
    
    var customToastSystem: CustomToastSystem? = null
    
    var debugSystem: DebugSystem? = null
    
    var drawableFactory: DrawableFactory? = null
    
    var eventRecorder: EventRecorder? = null
    
    var gameObjectCollisionSystem: GameObjectCollisionSystem? = null
    
    var gameObjectFactory: GameObjectFactory? = null
    
    var gameObjectManager: GameObjectManager? = null
    
    var hitPointPool: HitPointPool? = null
    
    var hotSpotSystem: HotSpotSystem? = null
    
    var hudSystem: HudSystem? = null
    
    var inputGameInterface: InputGameInterface? = null
    
    var inputSystem: InputSystem? = null
    
    var levelBuilder: LevelBuilder? = null
    
    var levelSystem: LevelSystem? = null
    
    var openGLSystem: OpenGLSystem? = null
    
    var soundSystem: SoundSystem? = null
    
    var shortTermTextureLibrary: TextureLibrary? = null
    
    var longTermTextureLibrary: TextureLibrary? = null
    
    var timeSystem: TimeSystem? = null
    
    var renderSystem: RenderSystem? = null
    
    var vectorPool: VectorPool? = null
    
    var vibrationSystem: VibrationSystem? = null
    private val itemsNeedingReset = ArrayList<BaseObject>()
    fun registerForReset(`object`: BaseObject) {
        val contained = itemsNeedingReset.contains(`object`)
        // TODO: assert(!contained)
        if (!contained) {
            itemsNeedingReset.add(`object`)
        }
    }

    override fun reset() {
        val count = itemsNeedingReset.size
        for (x in 0 until count) {
            itemsNeedingReset[x].reset()
        }
    }
}
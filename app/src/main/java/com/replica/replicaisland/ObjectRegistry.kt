/*
 * Copyright (C) 2010 The Android Open Source Project
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
    @JvmField
    var bufferLibrary: BufferLibrary? = null
    @JvmField
    var cameraSystem: CameraSystem? = null
    @JvmField
    var channelSystem: ChannelSystem? = null
    @JvmField
    var collisionSystem: CollisionSystem? = null
    @JvmField
    var contextParameters: ContextParameters? = null
    @JvmField
    var customToastSystem: CustomToastSystem? = null
    @JvmField
    var debugSystem: DebugSystem? = null
    @JvmField
    var drawableFactory: DrawableFactory? = null
    @JvmField
    var eventRecorder: EventRecorder? = null
    @JvmField
    var gameObjectCollisionSystem: GameObjectCollisionSystem? = null
    @JvmField
    var gameObjectFactory: GameObjectFactory? = null
    @JvmField
    var gameObjectManager: GameObjectManager? = null
    @JvmField
    var hitPointPool: HitPointPool? = null
    @JvmField
    var hotSpotSystem: HotSpotSystem? = null
    @JvmField
    var hudSystem: HudSystem? = null
    @JvmField
    var inputGameInterface: InputGameInterface? = null
    @JvmField
    var inputSystem: InputSystem? = null
    @JvmField
    var levelBuilder: LevelBuilder? = null
    @JvmField
    var levelSystem: LevelSystem? = null
    @JvmField
    var openGLSystem: OpenGLSystem? = null
    @JvmField
    var soundSystem: SoundSystem? = null
    @JvmField
    var shortTermTextureLibrary: TextureLibrary? = null
    @JvmField
    var longTermTextureLibrary: TextureLibrary? = null
    @JvmField
    var timeSystem: TimeSystem? = null
    @JvmField
    var renderSystem: RenderSystem? = null
    @JvmField
    var vectorPool: VectorPool? = null
    @JvmField
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
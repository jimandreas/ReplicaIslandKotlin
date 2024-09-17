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

class InventoryComponent : GameComponent() {
    private val inventory: UpdateRecord
    private var inventoryChanged = false
    override fun reset() {
        inventoryChanged = true
        inventory.reset()
    }

    fun applyUpdate(record: UpdateRecord) {
        inventory.add(record)
        inventoryChanged = true
    }

    override fun update(timeDelta: Float, parent: BaseObject?) {
        if (inventoryChanged) {
            val hud = sSystemRegistry.hudSystem
            hud?.updateInventory(inventory)
            inventoryChanged = false
        }
    }

    fun fetchRecord(): UpdateRecord {
        return inventory
    }

    fun setChangedValue() {
        inventoryChanged = true
    }

    class UpdateRecord : BaseObject() {
        @JvmField
        var rubyCount = 0
        @JvmField
        var coinCount = 0
        @JvmField
        var diaryCount = 0
        override fun reset() {
            rubyCount = 0
            coinCount = 0
            diaryCount = 0
        }

        fun add(other: UpdateRecord) {
            rubyCount += other.rubyCount
            coinCount += other.coinCount
            diaryCount += other.diaryCount
        }
    }

    init {
        inventory = UpdateRecord()
        reset()
        setPhaseToThis(ComponentPhases.FRAME_END.ordinal)
    }
}
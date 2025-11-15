package com.replica.replicaisland.entities

import com.replica.replicaisland.GameComponent
import com.replica.replicaisland.core.BaseObject

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
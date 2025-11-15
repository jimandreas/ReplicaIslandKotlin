package com.replica.replicaisland.rendering

import com.replica.replicaisland.BaseObject
import com.replica.replicaisland.GameComponent
import com.replica.replicaisland.GameObject

class CameraBiasComponent : GameComponent() {
    override fun reset() {}
    override fun update(timeDelta: Float, parent: BaseObject?) {
        val parentObject = parent as GameObject
        val camera = sSystemRegistry.cameraSystem
        camera?.addCameraBias(parentObject.position)
    }

    init {
        setPhaseToThis(ComponentPhases.THINK.ordinal)
    }
}
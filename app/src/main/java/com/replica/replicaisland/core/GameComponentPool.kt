package com.replica.replicaisland.core

import com.replica.replicaisland.GameComponent
import com.replica.replicaisland.TObjectPool

class GameComponentPool : TObjectPool<GameComponent?> {
    @JvmField
    var objectClass: Class<*>?

    constructor(type: Class<*>?) : super() {
        objectClass = type
        fill()
    }

    constructor(type: Class<*>?, size: Int) : super(size) {
        objectClass = type
        fill()
    }

    override fun fill() {
        if (objectClass != null) {
            for (x in 0 until fetchSize()) {
                try {
                    fetchAvailable()!!.add(objectClass!!.newInstance())
                } catch (e: IllegalAccessException) {
                    // TODO Auto-generated catch block
                    e.printStackTrace()
                } catch (e: InstantiationException) {
                    // TODO Auto-generated catch block
                    e.printStackTrace()
                }
            }
        }
    }
}
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
@file:Suppress("unused")

package com.replica.replicaisland

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
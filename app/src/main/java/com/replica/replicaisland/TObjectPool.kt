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
@file:Suppress("UNCHECKED_CAST", "RemoveEmptySecondaryConstructorBody")

package com.replica.replicaisland

/**
 * TObjectPool is a generic version of ObjectPool that automatically casts to type T on
 * allocation.
 *
 * @param <T> The type of object managed by the pool.
</T> */
abstract class TObjectPool<T> : ObjectPool {
    constructor() : super() {}
    constructor(size: Int) : super(size) {}

    public override fun allocate(): T {
        return super.allocate() as T
    }
}
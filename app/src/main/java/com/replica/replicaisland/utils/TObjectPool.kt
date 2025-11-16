package com.replica.replicaisland.utils

/**
 * TObjectPool is a generic version of ObjectPool that automatically casts to type T on
 * allocation.
 *
 * @param <T> The type of object managed by the pool.
</T> */
abstract class TObjectPool<T> : ObjectPool {
    constructor() : super()
    constructor(size: Int) : super(size)

    public override fun allocate(): T {
        return super.allocate() as T
    }
}
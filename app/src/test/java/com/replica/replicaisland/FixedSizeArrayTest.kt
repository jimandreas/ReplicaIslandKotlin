package com.replica.replicaisland

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class FixedSizeArrayTest {

    class TestEntity {
        var entityValue = 0
    }

    lateinit var fsa : FixedSizeArray<TestEntity>

    @BeforeEach
    fun setUp() {
        fsa = FixedSizeArray(10)
    }

    @AfterEach
    fun tearDown() {
    }

    @Test
    fun getCount() {
        assertEquals(0,fsa.count)
    }

    @Test
    fun add() {
        val oneMoreEntity = TestEntity()
        fsa.add(oneMoreEntity)
        assertEquals(1, fsa.count)
    }

    @Test
    fun remove() {
        val oneMoreEntity = TestEntity()
        fsa.add(oneMoreEntity)
        assertEquals(1, fsa.count)
        fsa.remove(0)
        assertEquals(0, fsa.count)
    }

    @Test
    fun testRemove() {
    }

    @Test
    fun removeLast() {
        val oneMoreEntity = TestEntity()
        fsa.add(oneMoreEntity)
        val anotherEntity = TestEntity()
        anotherEntity.entityValue = 55
        fsa.add(anotherEntity)
        assertEquals(2, fsa.count)
        val removeOne = fsa.removeLast()
        assertEquals(55, removeOne!!.entityValue)
        val lastOne = fsa.removeLast()
        assertEquals(0, lastOne!!.entityValue)
    }

    @Test
    fun swapWithLast() {
    }

    @Test
    fun set() {
    }

    @Test
    fun clear() {
    }

    @Test
    fun get() {
    }

    @Test
    fun getArray() {
    }

    @Test
    fun getCapacity() {
    }
}
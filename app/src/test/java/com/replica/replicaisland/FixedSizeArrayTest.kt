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

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class FixedSizeArrayTest {

    class TestEntity {
        var entityValue = 0
    }

    private lateinit var fsa : FixedSizeArray<TestEntity>

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

    /*@Test
    fun failsIntentionally() {
        assertEquals(1, 0)
    }*/
}
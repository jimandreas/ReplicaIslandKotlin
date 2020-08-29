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
@file:Suppress("UNCHECKED_CAST", "ConvertTwoComparisonsToRangeCheck", "unused")

package com.replica.replicaisland

import java.util.*

/**
 * FixedSizeArray is an alternative to a standard Java collection like ArrayList.  It is designed
 * to provide a contiguous array of fixed length which can be accessed, sorted, and searched without
 * requiring any runtime allocation.  This implementation makes a distinction between the "capacity"
 * of an array (the maximum number of objects it can contain) and the "count" of an array
 * (the current number of objects inserted into the array).  Operations such as set() and remove()
 * can only operate on objects that have been explicitly add()-ed to the array; that is, indexes
 * larger than getCount() but smaller than getCapacity() can't be used on their own.
 * @param <T> The type of object that this array contains.
</T> */
class FixedSizeArray<T> : AllocationGuard {
    private val mContents: Array<T?>
    /** Returns the number of objects in the array.  */
    var count: Int
        private set
    private var mComparator: Comparator<T?>? = null
    private var mSorted: Boolean
    private var mSorter: Sorter<T?>

    constructor(size: Int) : super() {
        if (BuildConfig.DEBUG && size <= 0) {
            error("Assertion failed")
        }
        // Ugh!  No generic array construction in Java.
        mContents = arrayOfNulls<Any>(size) as Array<T?>
        count = 0
        mSorted = false
        mSorter = StandardSorter()
    }

    constructor(size: Int, comparator: Comparator<T?>?) : super() {
        //assert(size > 0)
        mContents = arrayOfNulls<Any>(size) as Array<T?>
        count = 0
        mComparator = comparator
        mSorted = false
        mSorter = StandardSorter()
    }

    /**
     * Inserts a new object into the array.  If the array is full, an assert is thrown and the
     * object is ignored.
     */
    fun add(`object`: T) {
        //assert(count < mContents.size) { "Array exhausted!" }
        if (count < mContents.size) {
            mContents[count] = `object`
            mSorted = false
            count++
        }
    }

    /**
     * Searches for an object and removes it from the array if it is found.  Other indexes in the
     * array are shifted up to fill the space left by the removed object.  Note that if
     * ignoreComparator is set to true, a linear search of object references will be performed.
     * Otherwise, the comparator set on this array (if any) will be used to find the object.
     */
    fun remove(`object`: T, ignoreComparator: Boolean) {
        val index = find(`object`, ignoreComparator)
        if (index != -1) {
            remove(index)
        }
    }

    /**
     * Removes the specified index from the array.  Subsequent entries in the array are shifted up
     * to fill the space.
     */
    fun remove(index: Int) {
        //assert(index < count)
        // ugh
        if (index < count) {
            for (x in index until count) {
                if (x + 1 < mContents.size && x + 1 < count) {
                    mContents[x] = mContents[x + 1]
                } else {
                    mContents[x] = null
                }
            }
            count--
        }
    }

    /**
     * Removes the last element in the array and returns it.  This method is faster than calling
     * remove(count -1);
     * @return The contents of the last element in the array.
     */
    fun removeLast(): T? {
        var `object`: T? = null
        if (count > 0) {
            `object` = mContents[count - 1]
            mContents[count - 1] = null
            count--
        }
        return `object`
    }

    /**
     * Swaps the element at the passed index with the element at the end of the array.  When
     * followed by removeLast(), this is useful for quickly removing array elements.
     */
    fun swapWithLast(index: Int) {
        if (count > 0 && index < count - 1) {
            val `object` = mContents[count - 1]
            mContents[count - 1] = mContents[index]
            mContents[index] = `object`
            mSorted = false
        }
    }

    /**
     * Sets the value of a specific index in the array.  An object must have already been added to
     * the array at that index for this command to complete.
     */
    operator fun set(index: Int, `object`: T) {
        //assert(index < count)
        if (index < count) {
            mContents[index] = `object`
        }
    }

    /**
     * Clears the contents of the array, releasing all references to objects it contains and
     * setting its count to zero.
     */
    fun clear() {
        for (x in 0 until count) {
            mContents[x] = null
        }
        count = 0
        mSorted = false
    }

    /**
     * Returns an entry from the array at the specified index.
     */
    operator fun get(index: Int): T? {
        //assert(index < count)
        var result: T? = null
        if (index < count && index >= 0) {
            result = mContents[index]
        }
        return result
    }

    /**
     * Returns the raw internal array.  Exposed here so that tight loops can cache this array
     * and walk it without the overhead of repeated function calls.  Beware that changing this array
     * can leave FixedSizeArray in an undefined state, so this function is potentially dangerous
     * and should be used in read-only cases.
     * @return The internal storage array.
     */
    val array: Array<T?>
        get() = mContents

    /**
     * Searches the array for the specified object.  If the array has been sorted with sort(),
     * and if no other order-changing events have occurred since the sort (e.g. add()), a
     * binary search will be performed.  If a comparator has been specified with setComparator(),
     * it will be used to perform the search.  If not, the default comparator for the object type
     * will be used.  If the array is unsorted, a linear search is performed.
     * Note that if ignoreComparator is set to true, a linear search of object references will be
     * performed. Otherwise, the comparator set on this array (if any) will be used to find the
     * object.
     * @param object  The object to search for.
     * @return  The index of the object in the array, or -1 if the object is not found.
     */
    fun find(`object`: T, ignoreComparator: Boolean): Int {
        var index = -1
        val count = count
        val sorted = mSorted
        val comparator: Comparator<T?>? = mComparator
        val contents = mContents
        if (sorted && !ignoreComparator && count > LINEAR_SEARCH_CUTOFF) {
            index = if (comparator != null) {
                Arrays.binarySearch(contents, `object`, comparator)
            } else {
                Arrays.binarySearch(contents, `object`)
            }
            // Arrays.binarySearch() returns a negative insertion index if the object isn't found,
            // but we just want a boolean.
            if (index < 0) {
                index = -1
            }
        } else {
            // unsorted, linear search
            if (comparator != null && !ignoreComparator) {
                for (x in 0 until count) {
                    val result = comparator.compare(contents[x], `object`)
                    if (result == 0) {
                        index = x
                        break
                    } else if (result > 0 && sorted) {
                        // we've passed the object, early out
                        break
                    }
                }
            } else {
                for (x in 0 until count) {
                    if (contents[x] === `object`) {
                        index = x
                        break
                    }
                }
            }
        }
        return index
    }

    /**
     * Sorts the array.  If the array is already sorted, no work will be performed unless
     * the forceResort parameter is set to true.  If a comparator has been specified with
     * setComparator(), it will be used for the sort; otherwise the object's natural ordering will
     * be used.
     * @param forceResort  If set to true, the array will be resorted even if the order of the
     * objects in the array has not changed since the last sort.
     */
    fun sort(forceResort: Boolean) {
        if (!mSorted || forceResort) {
            if (mComparator != null) {
                mSorter.sort(mContents, count, mComparator!!)
            } else {
                DebugLog.d("FixedSizeArray", "No comparator specified for this type, using Arrays.sort().")
                Arrays.sort(mContents, 0, count)
            }
            mSorted = true
        }
    }

    /** Returns the maximum number of objects that can be inserted inot this array.  */
    fun getCapacity(): Int {
        return mContents.size
    }

    /** Sets a comparator to use for sorting and searching.  */
    fun setComparator(comparator: Comparator<T?>?) {
        mComparator = comparator
        mSorted = false
    }

    fun setSorter(sorter: Sorter<T?>) {
        mSorter = sorter
    }

    companion object {
        private const val LINEAR_SEARCH_CUTOFF = 16
    }
}
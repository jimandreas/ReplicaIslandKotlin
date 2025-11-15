package com.replica.replicaisland.utils

import java.util.Arrays
import java.util.Comparator

class StandardSorter<T> : Sorter<T>() {

    override fun sort(array: Array<T>, count: Int, comparator: Comparator<T>) {
        Arrays.sort(array, 0, count, comparator)
    }

}
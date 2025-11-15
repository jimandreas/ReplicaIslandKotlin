package com.replica.replicaisland.utils

import java.util.Comparator

abstract class Sorter<T> {
    abstract fun sort(array: Array<T>, count: Int, comparator: Comparator<T>)
}
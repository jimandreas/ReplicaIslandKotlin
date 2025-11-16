package com.replica.replicaisland.utils

import com.replica.replicaisland.utils.AllocationGuard
import kotlin.math.sqrt

/**
 * Simple 2D vector class.  Handles basic vector math for 2D vectors.
 */
class Vector2 : AllocationGuard {
    
    var x = 0f
    
    var y = 0f

    constructor() : super() {}
    constructor(xValue: Float, yValue: Float) {
        set(xValue, yValue)
    }

    constructor(xValue: Int, yValue: Int) {
        set(xValue.toFloat(), yValue.toFloat())
    }

    constructor(other: Vector2) {
        set(other)
    }

    fun add(other: Vector2) {
        x += other.x
        y += other.y
    }

    fun add(otherX: Float, otherY: Float) {
        x += otherX
        y += otherY
    }

    fun subtract(other: Vector2) {
        x -= other.x
        y -= other.y
    }

    fun multiply(magnitude: Float) {
        x *= magnitude
        y *= magnitude
    }

    fun multiply(other: Vector2) {
        x *= other.x
        y *= other.y
    }

    fun divide(magnitude: Float) {
        if (magnitude != 0.0f) {
            x /= magnitude
            y /= magnitude
        }
    }

    fun set(other: Vector2) {
        x = other.x
        y = other.y
    }

    operator fun set(xValue: Float, yValue: Float) {
        x = xValue
        y = yValue
    }

    fun dot(other: Vector2): Float {
        return x * other.x + y * other.y
    }

    fun length(): Float {
        return sqrt(length2().toDouble()).toFloat()
    }

    fun length2(): Float {
        return x * x + y * y
    }

    fun distance2(other: Vector2): Float {
        val dx = x - other.x
        val dy = y - other.y
        return dx * dx + dy * dy
    }

    fun normalize(): Float {
        val magnitude = length()

        // TODO: I'm choosing safety over speed here.
        if (magnitude != 0.0f) {
            x /= magnitude
            y /= magnitude
        }
        return magnitude
    }

    fun zero() {
        set(0.0f, 0.0f)
    }

    fun flipHorizontal(aboutWidth: Float) {
        x = aboutWidth - x
    }

    fun flipVertical(aboutHeight: Float) {
        y = aboutHeight - y
    }

    companion object {
        
        val ZERO = Vector2(0f, 0f)
    }
}
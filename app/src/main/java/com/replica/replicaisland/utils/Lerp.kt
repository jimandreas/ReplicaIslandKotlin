package com.replica.replicaisland.utils

object Lerp {
    @JvmStatic
    fun lerp(start: Float, target: Float, duration: Float, timeSinceStart: Float): Float {
        var value = start
        if (timeSinceStart > 0.0f && timeSinceStart < duration) {
            val range = target - start
            val percent = timeSinceStart / duration
            value = start + range * percent
        } else if (timeSinceStart >= duration) {
            value = target
        }
        return value
    }

    @JvmStatic
    fun ease(start: Float, target: Float, duration: Float, timeSinceStart: Float): Float {
        var value = start
        if (timeSinceStart > 0.0f && timeSinceStart < duration) {
            val range = target - start
            val percent = timeSinceStart / (duration / 2.0f)
            value = if (percent < 1.0f) {
                start + range / 2.0f * percent * percent * percent
            } else {
                val shiftedPercent = percent - 2.0f
                start + range / 2.0f *
                        (shiftedPercent * shiftedPercent * shiftedPercent + 2.0f)
            }
        } else if (timeSinceStart >= duration) {
            value = target
        }
        return value
    }
}
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
@file:Suppress("UnnecessaryVariable")

package com.replica.replicaisland

import kotlin.math.abs


/** A collection of miscellaneous utility functions.  */
class Utils {
    companion object {
        private const val EPSILON = 0.0001f

        @JvmOverloads
        fun close(a: Float, b: Float, epsilon: Float = EPSILON): Boolean {
            return abs(a - b) < epsilon
        }

        fun sign(a: Float): Int {
            return if (a >= 0.0f) {
                1
            } else {
                -1
            }
        }

        fun clamp(value: Int, min: Int, max: Int): Int {
            var result = value
            if (min == max) {
                if (value != min) {
                    result = min
                }
            } else if (min < max) {
                if (value < min) {
                    result = min
                } else if (value > max) {
                    result = max
                }
            } else {
                result = clamp(value, max, min)
            }
            return result
        }

        @JvmStatic
        fun byteArrayToInt(bIn: ByteArray): Int {
            if (bIn.size != 4) {
                return 0
            }

            val b0 = bIn[0].toInt()
            val b1 = bIn[1].toInt()
            val b2 = bIn[2].toInt()
            val b3 = bIn[3].toInt()

            val i1 : Int = (b3 and 0xff) shl 24
            val i2 : Int = (b2 and 0xff) shl 16
            val i3 : Int = (b1 and 0xff) shl 8
            val i4 : Int = b0 and 0xff

            val i : Int = i1 or i2 or i3 or i4

            // Same as DataInputStream's 'readInt' method
            /*int i = (((b[0] & 0xff) << 24) | ((b[1] & 0xff) << 16) | ((b[2] & 0xff) << 8)
                | (b[3] & 0xff));*/

            // little endian
            return i
        }

        fun byteArrayToFloat(b: ByteArray): Float {

            // intBitsToFloat() converts bits as follows:
            /*
        int s = ((i >> 31) == 0) ? 1 : -1;
        int e = ((i >> 23) & 0xff);
        int m = (e == 0) ? (i & 0x7fffff) << 1 : (i & 0x7fffff) | 0x800000;
        */
            return java.lang.Float.intBitsToFloat(byteArrayToInt(b))
        }

        fun framesToTime(framesPerSecond: Int, frameCount: Int): Float {
            return 1.0f / framesPerSecond * frameCount
        }

    }
}
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
package com.replica.replicaisland

import android.util.Log

class DebugLog {


    companion object {

        private var loggingEnabled = true

        fun setDebugLogging(enabled: Boolean) {
            loggingEnabled = enabled
        }

        fun v(tag: String, msg: String): Int {
            var result = 0
            if (loggingEnabled) {
                result = Log.v(tag, msg)
            }
            return result
        }

        fun v(tag: String, msg: String, tr: Throwable?): Int {
            var result = 0
            if (loggingEnabled) {
                result = Log.v(tag, msg, tr)
            }
            return result
        }

        fun d(tag: String, msg: String): Int {
            var result = 0
            if (loggingEnabled) {
                result = Log.d(tag, msg)
            }
            return result
        }

        fun d(tag: String, msg: String, tr: Throwable?): Int {
            var result = 0
            if (loggingEnabled) {
                result = Log.d(tag, msg, tr)
            }
            return result
        }

        fun i(tag: String, msg: String): Int {
            var result = 0
            if (loggingEnabled) {
                result = Log.i(tag, msg)
            }
            return result
        }

        fun i(tag: String, msg: String, tr: Throwable?): Int {
            var result = 0
            if (loggingEnabled) {
                result = Log.i(tag, msg, tr)
            }
            return result
        }

        fun w(tag: String, msg: String): Int {
            var result = 0
            if (loggingEnabled) {
                result = Log.w(tag, msg)
            }
            return result
        }

        fun w(tag: String, msg: String, tr: Throwable?): Int {
            var result = 0
            if (loggingEnabled) {
                result = Log.w(tag, msg, tr)
            }
            return result
        }

        fun w(tag: String, tr: Throwable?): Int {
            var result = 0
            if (loggingEnabled) {
                result = Log.w(tag, tr)
            }
            return result
        }

        fun e(tag: String, msg: String): Int {
            var result = 0
            if (loggingEnabled) {
                result = Log.e(tag, msg)
            }
            return result
        }

        fun e(tag: String, msg: String, tr: Throwable?): Int {
            var result = 0
            if (loggingEnabled) {
                result = Log.e(tag, msg, tr)
            }
            return result
        }
    }
}
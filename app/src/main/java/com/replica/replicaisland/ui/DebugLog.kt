package com.replica.replicaisland.ui

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
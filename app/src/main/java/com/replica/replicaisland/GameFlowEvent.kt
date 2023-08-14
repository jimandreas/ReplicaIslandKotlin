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

import android.content.Context

class GameFlowEvent : Runnable {
    private var eventCode = 0
    private var dataIndex = 0
    private var mainActivity: AndouKun? = null
    fun post(event: Int, index: Int, context: Context?) {
        if (context is AndouKun) {
            DebugLog.d("GameFlowEvent", "Post Game Flow Event: $event, $index")
            eventCode = event
            dataIndex = index
            mainActivity = context
            mainActivity!!.runOnUiThread(this)
        }
    }

    fun postImmediate(event: Int, index: Int, context: Context?) {
        if (context is AndouKun) {
            DebugLog.d("GameFlowEvent", "Execute Immediate Game Flow Event: $event, $index")
            eventCode = event
            dataIndex = index
            mainActivity = context
            mainActivity!!.onGameFlowEvent(eventCode, dataIndex)
        }
    }

    override fun run() {
        if (mainActivity != null) {
            DebugLog.d("GameFlowEvent", "Execute Game Flow Event: $eventCode, $dataIndex")
            mainActivity!!.onGameFlowEvent(eventCode, dataIndex)
            mainActivity = null
        }
    }

    companion object {
        const val EVENT_INVALID = -1
        const val EVENT_RESTART_LEVEL = 0
        const val EVENT_END_GAME = 1
        const val EVENT_GO_TO_NEXT_LEVEL = 2
        const val EVENT_SHOW_DIARY = 3
        const val EVENT_SHOW_DIALOG_CHARACTER1 = 4
        const val EVENT_SHOW_DIALOG_CHARACTER2 = 5
        const val EVENT_SHOW_ANIMATION = 6
    }
}
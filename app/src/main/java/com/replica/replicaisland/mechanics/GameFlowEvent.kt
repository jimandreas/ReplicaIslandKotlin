package com.replica.replicaisland.mechanics

import android.content.Context
import com.replica.replicaisland.AndouKun
import com.replica.replicaisland.ui.DebugLog

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
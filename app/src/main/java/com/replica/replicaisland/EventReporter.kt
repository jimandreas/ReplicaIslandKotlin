@file:Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER")

package com.replica.replicaisland

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.*


// TODO 2 : apply a kotlin solution to this issue
//   presumably coroutines
// https://stackoverflow.com/a/44589962/3853712

// defined in GameThread.kt:
// fun Any.wait() = (this as Object).wait()
// fun Any.notify() = (this as Object).notify()
// fun Any.notifyAll() = (this as Object).notifyAll()

class EventReporter : Runnable {
    class Event {
        var eventType: String? = null
        var x = 0f
        var y = 0f
        var time = 0f
        var level: String? = null
        var version = 0
        var session: Long = 0
    }

    private val lock = Any()
    private val events = ArrayList<Event>()
    private val processedEvents = ArrayList<Event>()
    private var done = false
    override fun run() {
        while (!done) {
            synchronized(lock) {
                if (events.isEmpty()) {
                    while (events.isEmpty() && !done) {
                        try {
                            lock.wait()
                        } catch (e: InterruptedException) {
                        }
                    }
                }
                processedEvents.addAll(events)
                events.clear()
            }
            val count = processedEvents.size
            for (x in 0 until count) {
                recordEvent(processedEvents[x])
            }
            processedEvents.clear()
        }
    }

    fun addEvent(eventType: Int, x: Float, y: Float, time: Float, level: String?, version: Int, session: Long) {
        val event = Event()
        event.x = x
        event.y = y
        event.time = time
        event.level = level
        event.version = version
        event.session = session
        when (eventType) {
            EVENT_DEATH -> event.eventType = "death"
            EVENT_BEAT_LEVEL -> event.eventType = "beatLevel"
            EVENT_BEAT_GAME -> event.eventType = "beatGame"
        }
        synchronized(lock) {
            events.add(event)
            lock.notifyAll()
        }
    }

    private fun recordEvent(event: Event) {
        var serverAddress: URL? = null
        var connection: HttpURLConnection? = null
        if (REPORT_SERVER != null) {
            try {
                serverAddress = URL(REPORT_SERVER + "?"
                        + "event=" + event.eventType
                        + "&x=" + event.x
                        + "&y=" + event.y
                        + "&time=" + event.time
                        + "&level=" + URLEncoder.encode(event.level, "UTF-8")
                        + "&version=" + event.version
                        + "&session=" + event.session)

                //set up out communications stuff
                //Set up the initial connection
                connection = serverAddress.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.doOutput = true
                connection.readTimeout = 0
                connection.connect()
                val response = connection.responseCode
                DebugLog.d("Report Event", event.eventType + "  " + response + ":" + connection.url.toString())
            } catch (e: Exception) {
                // This code can silently fail.
                //e.printStackTrace();
            } finally {
                //close the connection
                connection!!.disconnect()
            }
        }
    }

    fun stop() {
        synchronized(lock) {
            done = true
            lock.notifyAll()
        }
    }

    companion object {
        const val EVENT_DEATH = 0
        const val EVENT_BEAT_LEVEL = 1
        const val EVENT_BEAT_GAME = 2
        val REPORT_SERVER: String? = null // insert your server here.
    }
}
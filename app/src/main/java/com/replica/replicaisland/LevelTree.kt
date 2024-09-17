/*
 * Copyright (C) 2010 The Android Open Source Project
 * Copyright (C) 2024 Jim Andreas kotlin conversion and updating
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
@file:Suppress("NullChecksToSafeCall", "SENSELESS_COMPARISON")

package com.replica.replicaisland

import android.content.Context
import com.replica.replicaisland.ConversationUtils.loadDialog
import org.xmlpull.v1.XmlPullParser
import java.util.*

object LevelTree {
    val levels = ArrayList<LevelGroup>()
    private var mLoaded = false
    private var loadedResource = 0
    fun fetch(row: Int, index: Int): Level {
        return levels[row].levels[index]
    }

    @JvmStatic
	fun isLoaded(resource: Int): Boolean {
        return mLoaded && loadedResource == resource
    }

    @JvmStatic
	fun loadLevelTree(resource: Int, context: Context) {
        if (levels.size > 0 && loadedResource == resource) {
            // already loaded
            return
        }
        val parser = context.resources.getXml(resource)
        levels.clear()
        var currentGroup: LevelGroup? = null
        var currentLevel: Level? = null
        var currentDialog: DialogEntry? = null
        try {
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    if (parser.name == "group") {
                        currentGroup = LevelGroup()
                        levels.add(currentGroup)
                        currentLevel = null
                        currentDialog = null
                    }
                    if (parser.name == "level" && currentGroup != null) {
                        var levelResource = 0
                        var titleString: String? = null
                        var timeStamp: String? = null
                        var inThePast = false
                        var restartable = true
                        var showWaitMessage = false
                        for (i in 0 until parser.attributeCount) {
                            if (parser.getAttributeName(i) == "past") {
                                if (parser.getAttributeValue(i) == "true") {
                                    inThePast = true
                                }
                            } else if (parser.getAttributeName(i) == "restartable") {
                                if (parser.getAttributeValue(i) == "false") {
                                    restartable = false
                                }
                            } else if (parser.getAttributeName(i) == "waitmessage") {
                                if (parser.getAttributeValue(i) == "true") {
                                    showWaitMessage = true
                                }
                            } else {
                                val value = parser.getAttributeResourceValue(i, -1)
                                if (value != -1) {
                                    if (parser.getAttributeName(i) == "resource") {
                                        levelResource = value
                                    }
                                    if (parser.getAttributeName(i) == "title") {
                                        titleString = context.getString(value)
                                    } else if (parser.getAttributeName(i) == "time") {
                                        timeStamp = context.getString(value)
                                    }
                                }
                            }
                        }
                        currentDialog = null
                        currentLevel = Level(levelResource, null, titleString, timeStamp, inThePast, restartable, showWaitMessage)
                        currentGroup.levels.add(currentLevel)
                    }
                    if (parser.name == "dialog" && currentLevel != null) {
                        currentDialog = DialogEntry()
                        currentLevel.dialogResources = currentDialog
                    }
                    if (parser.name == "diary" && currentDialog != null) {
                        for (i in 0 until parser.attributeCount) {
                            val value = parser.getAttributeResourceValue(i, -1)
                            if (value != -1) {
                                if (parser.getAttributeName(i) == "resource") {
                                    currentDialog.diaryEntry = value
                                }
                            }
                        }
                    }
                    if (parser.name == "character1" && currentDialog != null) {
                        for (i in 0 until parser.attributeCount) {
                            val value = parser.getAttributeResourceValue(i, -1)
                            if (value != -1) {
                                if (parser.getAttributeName(i) == "resource") {
                                    currentDialog.character1Entry = value
                                }
                            }
                        }
                    }
                    if (parser.name == "character2" && currentDialog != null) {
                        for (i in 0 until parser.attributeCount) {
                            val value = parser.getAttributeResourceValue(i, -1)
                            if (value != -1) {
                                if (parser.getAttributeName(i) == "resource") {
                                    currentDialog.character2Entry = value
                                }
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            DebugLog.e("LevelTree", e.stackTrace.toString())
        } finally {
            parser.close()
        }
        mLoaded = true
        loadedResource = resource
    }

    @JvmStatic
	fun loadAllDialog(context: Context?) {
        val levelGroupCount = levels.size
        for (x in 0 until levelGroupCount) {
            val row = levels[x].levels
            val levelCount = row.size
            for (y in 0 until levelCount) {
                val level = row[y]
                if (level != null && level.dialogResources != null) {
                    val dialog = level.dialogResources
                    if (dialog!!.character1Entry != 0) {
                        dialog.character1Conversations = loadDialog(dialog.character1Entry, context!!)
                    }
                    if (dialog.character2Entry != 0) {
                        dialog.character2Conversations = loadDialog(dialog.character2Entry, context!!)
                    }
                }
            }
        }
    }

    fun updateCompletedState(levelRow: Int, completedLevels: Int) {
        val rowCount = levels.size
        for (x in 0 until rowCount) {
            val group = levels[x]
            val levelCount = group.levels.size
            for (y in 0 until levelCount) {
                val level = group.levels[y]
                if (x < levelRow) {
                    level.completed = true
                } else if (x == levelRow) {
                    if (completedLevels and (1 shl y) != 0) {
                        level.completed = true
                    }
                } else {
                    level.completed = false
                }
            }
        }
    }

    fun packCompletedLevels(levelRow: Int): Int {
        var completed = 0
        val group = levels[levelRow]
        val levelCount = group.levels.size
        for (y in 0 until levelCount) {
            val level = group.levels[y]
            if (level.completed) {
                completed = completed or (1 shl y)
            }
        }
        return completed
    }

    fun levelIsValid(row: Int, index: Int): Boolean {
        var valid = false
        if (row >= 0 && row < levels.size) {
            val group = levels[row]
            if (index >= 0 && index < group.levels.size) {
                valid = true
            }
        }
        return valid
    }

    fun rowIsValid(row: Int): Boolean {
        var valid = false
        if (row >= 0 && row < levels.size) {
            valid = true
        }
        return valid
    }

    class LevelGroup {
        var levels = ArrayList<Level>()
    }

    class Level(var resource: Int, var dialogResources: DialogEntry?, var name: String?, var timeStamp: String?,
                var inThePast: Boolean, var restartable: Boolean, var showWaitMessage: Boolean) {
        var completed = false
        var diaryCollected = false
    }

    class DialogEntry {
        var diaryEntry = 0
        var character1Entry = 0
        var character2Entry = 0
        var character1Conversations: ArrayList<ConversationUtils.Conversation?>? = null
        var character2Conversations: ArrayList<ConversationUtils.Conversation?>? = null
    }
}
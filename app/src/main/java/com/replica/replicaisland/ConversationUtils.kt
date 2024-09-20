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
package com.replica.replicaisland

import android.content.Context
import org.xmlpull.v1.XmlPullParser

object ConversationUtils {
    //private const val MAX_CHARACTERS_PER_PAGE = 250
    fun loadDialog(resource: Int, context: Context): ArrayList<Conversation?>? {
        val parser = context.resources.getXml(resource)
        var dialogArray: ArrayList<Conversation?>? = null
        var currentConversation: Conversation? = null
        try {
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    if (parser.name == "conversation") {
                        if (dialogArray == null) {
                            dialogArray = ArrayList()
                        }
                        currentConversation = Conversation()
                        currentConversation.splittingComplete = false
                        dialogArray.add(currentConversation)
                    } else if (parser.name == "page") {
                        val page = ConversationPage()
                        for (i in 0 until parser.attributeCount) {
                            val value = parser.getAttributeResourceValue(i, -1)
                            if (value != -1) {
                                if (parser.getAttributeName(i) == "image") {
                                    page.imageResource = value
                                }
                                if (parser.getAttributeName(i) == "text") {
                                    page.text = context.getText(value)
                                }
                                if (parser.getAttributeName(i) == "title") {
                                    page.title = context.getString(value)
                                }
                            }
                        }
                        currentConversation!!.pages.add(page)
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            DebugLog.e("LoadDialog", e.stackTrace.toString())
        } finally {
            parser.close()
        }
        return dialogArray
    }

    class ConversationPage {
        var imageResource = 0
        var text: CharSequence? = null
        var title: String? = null
    }

    class Conversation {
        var pages = ArrayList<ConversationPage>()
        var splittingComplete = false
    }
}
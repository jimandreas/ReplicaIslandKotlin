package com.replica.replicaisland.ui

import android.content.Context
import com.replica.replicaisland.ui.DebugLog
import org.xmlpull.v1.XmlPullParser
import java.util.ArrayList

object ConversationUtils {
    //private const val MAX_CHARACTERS_PER_PAGE = 250
    @JvmStatic
    fun loadDialog(resource: Int, context: Context): ArrayList<Conversation?>? {
        val parser = context.resources.getXml(resource)
        var dialog: ArrayList<Conversation?>? = null
        var currentConversation: Conversation? = null
        try {
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    if (parser.name == "conversation") {
                        if (dialog == null) {
                            dialog = ArrayList()
                        }
                        currentConversation = Conversation()
                        currentConversation.splittingComplete = false
                        dialog.add(currentConversation)
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
            DebugLog.Companion.e("LoadDialog", e.stackTrace.toString())
        } finally {
            parser.close()
        }
        return dialog
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
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
@file:Suppress("RemoveEmptySecondaryConstructorBody")

package com.replica.replicaisland

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.os.SystemClock
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.replica.replicaisland.ConversationUtils.ConversationPage
import java.util.*

class ConversationDialogActivity : Activity() {
    private var mConversation: ConversationUtils.Conversation? = null
    private var mPages: ArrayList<ConversationPage>? = null
    private var mCurrentPage = 0
    private var okArrow: ImageView? = null
    private var okAnimation: AnimationDrawable? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            super.onCreate(savedInstanceState)
        } else {
            super.onCreate(null)
        }
        setContentView(R.layout.conversation_dialog)
        okArrow = findViewById<View>(R.id.ok) as ImageView
        okArrow!!.setBackgroundResource(R.drawable.ui_button)
        okAnimation = okArrow!!.background as AnimationDrawable
        okArrow!!.visibility = View.INVISIBLE
        val callingIntent = intent
        val levelRow = callingIntent.getIntExtra("levelRow", -1)
        val levelIndex = callingIntent.getIntExtra("levelIndex", -1)
        val index = callingIntent.getIntExtra("index", -1)
        val character = callingIntent.getIntExtra("character", 1)
        mPages = null

        // LevelTree.get(mLevelRow, mLevelIndex).dialogResources.character2Entry.get(index)
        if (levelRow != -1 && levelIndex != -1 && index != -1) {
            mConversation = if (character == 1) {
                LevelTree.fetch(levelRow, levelIndex).dialogResources!!.character1Conversations!![index]
            } else {
                LevelTree.fetch(levelRow, levelIndex).dialogResources!!.character2Conversations!![index]
            }
            val tv = findViewById<View>(R.id.typewritertext) as TypewriterTextView
            tv.setParentActivity(this)
        } else {
            // bail
            finish()
        }
    }

    private fun formatPages(conversation: ConversationUtils.Conversation?, textView: TextView) {
        val paint = Paint()
        val maxWidth = textView.width
        val maxHeight = textView.height
        paint.textSize = textView.textSize
        paint.typeface = textView.typeface
        for (page in conversation!!.pages.indices.reversed()) {
            val currentPage = conversation.pages[page]
            val text = currentPage.text
            // Iterate line by line through the text.  Add \n if it gets too wide,
            // and split into a new page if it gets too long.
            var currentOffset = 0
            val textLength = text!!.length
            val spannedText = SpannableStringBuilder(text)
            var lineCount = 0
            val fontHeight = -paint.ascent() + paint.descent()
            val maxLinesPerPage = (maxHeight / fontHeight).toInt()
            val newline: CharSequence = "\n"
            var addedPages = 0
            var lastPageStart = 0
            do {
                var fittingChars = paint.breakText(text, currentOffset, textLength, true, maxWidth.toFloat(), null)
                if (currentOffset + fittingChars < textLength) {
                    fittingChars -= 2
                    // Text doesn't fit on the line.  Insert a return after the last space.
                    var lastSpace = TextUtils.lastIndexOf(text, ' ', currentOffset + fittingChars - 1)
                    if (lastSpace == -1) {
                        // No spaces, just split at the last character.
                        lastSpace = currentOffset + fittingChars - 1
                    }
                    spannedText.replace(lastSpace, lastSpace + 1, newline, 0, 1)
                    lineCount++
                    currentOffset = lastSpace + 1
                } else {
                    lineCount++
                    currentOffset = textLength
                }
                if (lineCount >= maxLinesPerPage || currentOffset >= textLength) {
                    lineCount = 0
                    if (addedPages == 0) {
                        // overwrite the original page
                        currentPage.text = spannedText.subSequence(lastPageStart, currentOffset)
                    } else {
                        // split into a new page
                        val newPage = ConversationPage()
                        newPage.imageResource = currentPage.imageResource
                        newPage.text = spannedText.subSequence(lastPageStart, currentOffset)
                        newPage.title = currentPage.title
                        conversation.pages.add(page + addedPages, newPage)
                    }
                    lastPageStart = currentOffset
                    addedPages++
                }
            } while (currentOffset < textLength)
        }

        // Holy crap we did a lot of allocation there.
        Runtime.getRuntime().gc()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            val tv = findViewById<View>(R.id.typewritertext) as TypewriterTextView
            if (tv.remainingTime > 0) {
                tv.snapToEnd()
            } else {
                mCurrentPage++
                if (mCurrentPage < mPages!!.size) {
                    showPage(mPages!![mCurrentPage])
                } else {
                    finish()
                }
            }
        }
        // Sleep so that the main thread doesn't get flooded with UI events.
        try {
            Thread.sleep(32)
        } catch (e: InterruptedException) {
            // No big deal if this sleep is interrupted.
        }
        return true
    }

    private fun showPage(page: ConversationPage) {
        val tv = findViewById<View>(R.id.typewritertext) as TypewriterTextView
        tv.setTypewriterText(page.text)
        okArrow!!.visibility = View.INVISIBLE
        okAnimation!!.start()
        tv.setOkArrow(okArrow)
        val image = findViewById<View>(R.id.speaker) as ImageView
        if (page.imageResource != 0) {
            image.setImageResource(page.imageResource)
            image.visibility = View.VISIBLE
        } else {
            image.visibility = View.GONE
        }
        val title = findViewById<View>(R.id.speakername) as TextView
        if (page.title != null) {
            title.text = page.title
            title.visibility = View.VISIBLE
        } else {
            title.visibility = View.GONE
        }
    }

    fun processText() {
        if (!mConversation!!.splittingComplete) {
            val textView = findViewById<View>(R.id.typewritertext) as TextView
            formatPages(mConversation, textView)
            mConversation!!.splittingComplete = true
        }
        if (mPages == null) {
            mPages = mConversation!!.pages
            showPage(mPages!!.get(index = 0))
            mCurrentPage = 0
        }
    }

    class TypewriterTextView : TextView {
        private var currentCharacter = 0
        private var lastTime: Long = 0
        private var mText: CharSequence? = null
        private var okArrow: View? = null
        private var parentActivity // This really sucks.
                : ConversationDialogActivity? = null

        constructor(context: Context?) : super(context) {}
        constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {}
        constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {}

        fun setParentActivity(parent: ConversationDialogActivity?) {
            parentActivity = parent
        }

        fun setTypewriterText(text: CharSequence?) {
            mText = text
            currentCharacter = 0
            lastTime = 0
            postInvalidate()
        }

        val remainingTime: Long
            get() = ((mText!!.length - currentCharacter) * TEXT_CHARACTER_DELAY_MS).toLong()

        fun snapToEnd() {
            currentCharacter = mText!!.length - 1
        }

        fun setOkArrow(arrow: View?) {
            okArrow = arrow
        }

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            // We need to wait until layout has occurred before we can setup the
            // text page.  Ugh.  Bidirectional dependency!
            if (parentActivity != null) {
                parentActivity!!.processText()
            }
            super.onSizeChanged(w, h, oldw, oldh)
        }

        public override fun onDraw(canvas: Canvas) {
            val time = SystemClock.uptimeMillis()
            val delta = time - lastTime
            if (delta > TEXT_CHARACTER_DELAY_MS) {
                if (mText != null) {
                    if (currentCharacter <= mText!!.length) {
                        val subtext = mText!!.subSequence(0, currentCharacter)
                        setText(subtext, BufferType.SPANNABLE)
                        currentCharacter++
                        postInvalidateDelayed(TEXT_CHARACTER_DELAY_MS.toLong())
                    } else {
                        if (okArrow != null) {
                            okArrow!!.visibility = VISIBLE
                        }
                    }
                }
            }
            super.onDraw(canvas)
        }
    }

    companion object {
        private const val TEXT_CHARACTER_DELAY = 0.1f
        private const val TEXT_CHARACTER_DELAY_MS = (TEXT_CHARACTER_DELAY * 1000).toInt()
    }
}
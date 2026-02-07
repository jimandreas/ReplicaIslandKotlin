/*
 * Copyright (C) 2010 The Android Open Source Project
 * Copyright (C) 2025 Jim Andreas kotlin conversion
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

@file:Suppress("DEPRECATION")

package com.replica.replicaisland.ui

import android.app.Dialog
import android.content.pm.ActivityInfo
import android.graphics.Paint
import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.replica.replicaisland.R
import com.replica.replicaisland.levels.LevelTree

/**
 * DialogFragment that displays conversation dialogs with typewriter text effect.
 * Replaces ConversationDialogActivity with a modern fragment-based approach.
 */
class ConversationDialogFragment : DialogFragment(), TypewriterTextView.TypewriterCallback {

    companion object {
        private const val ARG_LEVEL_ROW = "levelRow"
        private const val ARG_LEVEL_INDEX = "levelIndex"
        private const val ARG_INDEX = "index"
        private const val ARG_CHARACTER = "character"
        const val TAG = "ConversationDialogFragment"

        /**
         * Factory method to create a new instance with conversation parameters.
         */
        fun newInstance(levelRow: Int, levelIndex: Int, index: Int, character: Int): ConversationDialogFragment {
            return ConversationDialogFragment().apply {
                arguments = bundleOf(
                    ARG_LEVEL_ROW to levelRow,
                    ARG_LEVEL_INDEX to levelIndex,
                    ARG_INDEX to index,
                    ARG_CHARACTER to character
                )
            }
        }
    }

    private var mConversation: ConversationUtils.Conversation? = null
    private var mPages: ArrayList<ConversationUtils.ConversationPage>? = null
    private var mCurrentPage = 0
    private var okArrow: ImageView? = null
    private var okAnimation: AnimationDrawable? = null
    private var typewriterTextView: TypewriterTextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.Theme_ConversationDialog)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.apply {
            setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_conversation_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Force landscape orientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        okArrow = view.findViewById(R.id.ok)
        okArrow!!.setBackgroundResource(R.drawable.ui_button)
        okAnimation = okArrow!!.background as AnimationDrawable
        okArrow!!.visibility = View.INVISIBLE

        val levelRow = arguments?.getInt(ARG_LEVEL_ROW, -1) ?: -1
        val levelIndex = arguments?.getInt(ARG_LEVEL_INDEX, -1) ?: -1
        val index = arguments?.getInt(ARG_INDEX, -1) ?: -1
        val character = arguments?.getInt(ARG_CHARACTER, 1) ?: 1

        mPages = null

        if (levelRow != -1 && levelIndex != -1 && index != -1) {
            mConversation = if (character == 1) {
                LevelTree.fetch(levelRow, levelIndex).dialogResources!!.character1Conversations!![index]
            } else {
                LevelTree.fetch(levelRow, levelIndex).dialogResources!!.character2Conversations!![index]
            }
            typewriterTextView = view.findViewById(R.id.typewritertext)
            typewriterTextView?.setTypewriterCallback(this)
        } else {
            // bail
            dismiss()
        }

        // Handle touch events on the dialog
        dialog?.window?.decorView?.setOnTouchListener { _, event ->
            handleTouchEvent(event)
            true
        }
    }

    private fun handleTouchEvent(event: MotionEvent) {
        if (event.action == MotionEvent.ACTION_UP) {
            val tv = typewriterTextView
            if (tv != null && tv.remainingTime > 0) {
                tv.snapToEnd()
            } else {
                mCurrentPage++
                if (mPages != null && mCurrentPage < mPages!!.size) {
                    showPage(mPages!![mCurrentPage])
                } else {
                    dismiss()
                }
            }
        }
        // Sleep so that the main thread doesn't get flooded with UI events.
        try {
            Thread.sleep(32)
        } catch (_: InterruptedException) {
            // No big deal if this sleep is interrupted.
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
            // Iterate line by line through the text. Add \n if it gets too wide,
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
                    // Text doesn't fit on the line. Insert a return after the last space.
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
                        val newPage = ConversationUtils.ConversationPage()
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

    private fun showPage(page: ConversationUtils.ConversationPage) {
        val tv = typewriterTextView ?: return
        tv.setTypewriterText(page.text)
        okArrow!!.visibility = View.INVISIBLE
        okAnimation!!.start()
        tv.setOkArrow(okArrow)

        val image = view?.findViewById<ImageView>(R.id.speaker)
        if (page.imageResource != 0) {
            image?.setImageResource(page.imageResource)
            image?.visibility = View.VISIBLE
        } else {
            image?.visibility = View.GONE
        }

        val title = view?.findViewById<TextView>(R.id.speakername)
        if (page.title != null) {
            title?.text = page.title
            title?.visibility = View.VISIBLE
        } else {
            title?.visibility = View.GONE
        }
    }

    override fun onTextLayoutComplete() {
        if (mConversation != null && !mConversation!!.splittingComplete) {
            typewriterTextView?.let { textView ->
                formatPages(mConversation, textView)
                mConversation!!.splittingComplete = true
            }
        }
        if (mPages == null && mConversation != null) {
            mPages = mConversation!!.pages
            if (mPages!!.isNotEmpty()) {
                showPage(mPages!![0])
            }
            mCurrentPage = 0
        }
    }
}

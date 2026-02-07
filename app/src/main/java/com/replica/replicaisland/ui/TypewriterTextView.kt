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

package com.replica.replicaisland.ui

import android.content.Context
import android.graphics.Canvas
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.widget.AppCompatTextView

/**
 * A TextView that displays text with a typewriter effect, revealing one character at a time.
 */
class TypewriterTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : AppCompatTextView(context, attrs, defStyle) {

    /**
     * Callback interface for typewriter text events.
     */
    interface TypewriterCallback {
        /**
         * Called when the view's size changes and text layout can begin.
         */
        fun onTextLayoutComplete()
    }

    private var currentCharacter = 0
    private var lastTime: Long = 0
    private var mText: CharSequence? = null
    private var okArrow: View? = null
    private var callback: TypewriterCallback? = null

    /**
     * Sets the callback for typewriter events.
     */
    fun setTypewriterCallback(callback: TypewriterCallback?) {
        this.callback = callback
    }

    /**
     * Sets the text to be displayed with typewriter effect.
     */
    fun setTypewriterText(text: CharSequence?) {
        mText = text
        currentCharacter = 0
        lastTime = 0
        postInvalidate()
    }

    /**
     * Returns the remaining time in milliseconds to display all characters.
     */
    val remainingTime: Long
        get() = ((mText?.length ?: 0) - currentCharacter) * TEXT_CHARACTER_DELAY_MS.toLong()

    /**
     * Immediately displays all remaining characters.
     */
    fun snapToEnd() {
        mText?.let {
            currentCharacter = it.length - 1
        }
    }

    /**
     * Sets the view to show when text is fully displayed.
     */
    fun setOkArrow(arrow: View?) {
        okArrow = arrow
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        // We need to wait until layout has occurred before we can setup the text page.
        callback?.onTextLayoutComplete()
        super.onSizeChanged(w, h, oldw, oldh)
    }

    public override fun onDraw(canvas: Canvas) {
        val time = SystemClock.uptimeMillis()
        val delta = time - lastTime
        if (delta > TEXT_CHARACTER_DELAY_MS) {
            mText?.let { text ->
                if (currentCharacter <= text.length) {
                    val subtext = text.subSequence(0, currentCharacter)
                    setText(subtext, BufferType.SPANNABLE)
                    currentCharacter++
                    postInvalidateDelayed(TEXT_CHARACTER_DELAY_MS.toLong())
                } else {
                    okArrow?.visibility = VISIBLE
                }
            }
        }
        super.onDraw(canvas)
    }

    companion object {
        private const val TEXT_CHARACTER_DELAY = 0.1f
        private const val TEXT_CHARACTER_DELAY_MS = (TEXT_CHARACTER_DELAY * 1000).toInt()
    }
}

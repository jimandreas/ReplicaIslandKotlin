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
@file:Suppress("DEPRECATION")

package com.replica.replicaisland

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.preference.DialogPreference
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.View
import android.widget.TextView

class KeyboardConfigDialogPreference @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null,
                                                               defStyle: Int = android.R.attr.dialogPreferenceStyle) : DialogPreference(context, attrs, defStyle), DialogInterface.OnKeyListener {
    private var mSharedPrefs: SharedPreferences? = null
    private var mContext: Context? = null
    private val mLeftPrefKey: String?
    private val mRightPrefKey: String?
    private val mJumpPrefKey: String?
    private val mAttackPrefKey: String?
    private var mKeyLabels: Array<String>? = null
    private var mListeningId = 0
    private var mLeftBorder: View? = null
    private var mRightBorder: View? = null
    private var mJumpBorder: View? = null
    private var mAttackBorder: View? = null
    private var mUnselectedBorder: Drawable? = null
    private var mSelectedBorder: Drawable? = null
    private var mLeftKeyCode = 0
    private var mRightKeyCode = 0
    private var mJumpKeyCode = 0
    private var mAttackKeyCode = 0
    private var mLeftText: TextView? = null
    private var mRightText: TextView? = null
    private var mJumpText: TextView? = null
    private var mAttackText: TextView? = null

    private inner class ConfigClickListener(private val mId: Int) : View.OnClickListener {
        override fun onClick(v: View) {
            selectId(mId)
        }
    }

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)
        if (mSharedPrefs != null) {
            mLeftKeyCode = mSharedPrefs!!.getInt(mLeftPrefKey, KeyEvent.KEYCODE_DPAD_LEFT)
            mRightKeyCode = mSharedPrefs!!.getInt(mRightPrefKey, KeyEvent.KEYCODE_DPAD_RIGHT)
            mJumpKeyCode = mSharedPrefs!!.getInt(mJumpPrefKey, KeyEvent.KEYCODE_SPACE)
            mAttackKeyCode = mSharedPrefs!!.getInt(mAttackPrefKey, KeyEvent.KEYCODE_SHIFT_LEFT)
            mLeftText = view.findViewById<View>(R.id.key_left) as TextView
            mLeftText!!.text = getKeyLabel(mLeftKeyCode)
            mRightText = view.findViewById<View>(R.id.key_right) as TextView
            mRightText!!.text = getKeyLabel(mRightKeyCode)
            mJumpText = view.findViewById<View>(R.id.key_jump) as TextView
            mJumpText!!.text = getKeyLabel(mJumpKeyCode)
            mAttackText = view.findViewById<View>(R.id.key_attack) as TextView
            mAttackText!!.text = getKeyLabel(mAttackKeyCode)
            mLeftBorder = view.findViewById(R.id.left_border)
            mRightBorder = view.findViewById(R.id.right_border)
            mJumpBorder = view.findViewById(R.id.jump_border)
            mAttackBorder = view.findViewById(R.id.attack_border)
            mLeftBorder!!.setOnClickListener(ConfigClickListener(R.id.key_left))
            mRightBorder!!.setOnClickListener(ConfigClickListener(R.id.key_right))
            mJumpBorder!!.setOnClickListener(ConfigClickListener(R.id.key_jump))
            mAttackBorder!!.setOnClickListener(ConfigClickListener(R.id.key_attack))
            mUnselectedBorder = mContext!!.resources.getDrawable(R.drawable.key_config_border)
            mSelectedBorder = mContext!!.resources.getDrawable(R.drawable.key_config_border_active)
        }
        mListeningId = 0
    }

    override fun showDialog(state: Bundle?) {
        if (state == null) {
            super.showDialog(null)
        } else {
            super.showDialog(state)
        }
        dialog.setOnKeyListener(this)
        dialog.takeKeyEvents(true)
    }

    private fun getKeyLabel(keycode: Int): String {
        var result = "Unknown Key"
        if (mKeyLabels == null) {
            mKeyLabels = mContext!!.resources.getStringArray(R.array.keycode_labels)
        }
        if (keycode > 0 && keycode < mKeyLabels!!.size) {
            result = mKeyLabels!![keycode - 1]
        }
        return result
    }

    fun selectId(id: Int) {
        if (mListeningId != 0) {
            // unselect the current box
            val border = getConfigViewById(mListeningId)
            border!!.setBackgroundDrawable(mUnselectedBorder)
        }
        mListeningId = if (id == mListeningId || id == 0) {
            0 // toggle off and end.
        } else {
            // select the new box
            val border = getConfigViewById(id)
            border!!.setBackgroundDrawable(mSelectedBorder)
            id
        }
    }

    private fun getConfigViewById(id: Int): View? {
        var config: View? = null
        when (id) {
            R.id.key_left -> config = mLeftBorder
            R.id.key_right -> config = mRightBorder
            R.id.key_jump -> config = mJumpBorder
            R.id.key_attack -> config = mAttackBorder
        }
        return config
    }

    @SuppressLint("ApplySharedPref")
    override fun onDialogClosed(positiveResult: Boolean) {
        super.onDialogClosed(positiveResult)
        if (positiveResult) {
            // save changes
            val editor = mSharedPrefs!!.edit()
            editor.putInt(mLeftPrefKey, mLeftKeyCode)
            editor.putInt(mRightPrefKey, mRightKeyCode)
            editor.putInt(mJumpPrefKey, mJumpKeyCode)
            editor.putInt(mAttackPrefKey, mAttackKeyCode)
            editor.commit()
        }
    }

    fun setPrefs(sharedPreferences: SharedPreferences?) {
        mSharedPrefs = sharedPreferences
    }

    fun setContext(context: Context?) {
        mContext = context
    }

    override fun onKey(dialog: DialogInterface, keyCode: Int, event: KeyEvent): Boolean {
        var eatKey = false
        if (mListeningId != 0) {
            eatKey = true
            when (mListeningId) {
                R.id.key_left -> {
                    mLeftText!!.text = getKeyLabel(keyCode)
                    mLeftKeyCode = keyCode
                }
                R.id.key_right -> {
                    mRightText!!.text = getKeyLabel(keyCode)
                    mRightKeyCode = keyCode
                }
                R.id.key_jump -> {
                    mJumpText!!.text = getKeyLabel(keyCode)
                    mJumpKeyCode = keyCode
                }
                R.id.key_attack -> {
                    mAttackText!!.text = getKeyLabel(keyCode)
                    mAttackKeyCode = keyCode
                }
            }
            selectId(0) // deselect the current config box;
        }
        return eatKey
    }

    init {
        val a = context.obtainStyledAttributes(attrs,
                R.styleable.KeyConfigPreference, defStyle, 0)
        mLeftPrefKey = a.getString(R.styleable.KeyConfigPreference_leftKey)
        mRightPrefKey = a.getString(R.styleable.KeyConfigPreference_rightKey)
        mJumpPrefKey = a.getString(R.styleable.KeyConfigPreference_jumpKey)
        mAttackPrefKey = a.getString(R.styleable.KeyConfigPreference_attackKey)
        a.recycle()
    }
}
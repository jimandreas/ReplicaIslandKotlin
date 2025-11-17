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

import android.R
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
                                                               defStyle: Int = R.attr.dialogPreferenceStyle) : DialogPreference(context, attrs, defStyle), DialogInterface.OnKeyListener {
    private var sharedPrefs: SharedPreferences? = null
    private var mContext: Context? = null
    private val leftPrefKey: String?
    private val rightPrefKey: String?
    private val jumpPrefKey: String?
    private val attackPrefKey: String?
    private var keyLabels: Array<String>? = null
    private var listeningId = 0
    private var leftBorder: View? = null
    private var rightBorder: View? = null
    private var jumpBorder: View? = null
    private var attackBorder: View? = null
    private var unselectedBorder: Drawable? = null
    private var mSelectedBorder: Drawable? = null
    private var leftKeyCode = 0
    private var rightKeyCode = 0
    private var jumpKeyCode = 0
    private var attackKeyCode = 0
    private var leftText: TextView? = null
    private var rightText: TextView? = null
    private var jumpText: TextView? = null
    private var attackText: TextView? = null

    private inner class ConfigClickListener(private val mId: Int) : View.OnClickListener {
        override fun onClick(v: View) {
            selectId(mId)
        }
    }

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)
        if (sharedPrefs != null) {
            leftKeyCode = sharedPrefs!!.getInt(leftPrefKey, KeyEvent.KEYCODE_DPAD_LEFT)
            rightKeyCode = sharedPrefs!!.getInt(rightPrefKey, KeyEvent.KEYCODE_DPAD_RIGHT)
            jumpKeyCode = sharedPrefs!!.getInt(jumpPrefKey, KeyEvent.KEYCODE_SPACE)
            attackKeyCode = sharedPrefs!!.getInt(attackPrefKey, KeyEvent.KEYCODE_SHIFT_LEFT)
            leftText = view.findViewById<View>(com.replica.replicaisland.R.id.key_left) as TextView
            leftText!!.text = getKeyLabel(leftKeyCode)
            rightText = view.findViewById<View>(com.replica.replicaisland.R.id.key_right) as TextView
            rightText!!.text = getKeyLabel(rightKeyCode)
            jumpText = view.findViewById<View>(com.replica.replicaisland.R.id.key_jump) as TextView
            jumpText!!.text = getKeyLabel(jumpKeyCode)
            attackText = view.findViewById<View>(com.replica.replicaisland.R.id.key_attack) as TextView
            attackText!!.text = getKeyLabel(attackKeyCode)
            leftBorder = view.findViewById(com.replica.replicaisland.R.id.left_border)
            rightBorder = view.findViewById(com.replica.replicaisland.R.id.right_border)
            jumpBorder = view.findViewById(com.replica.replicaisland.R.id.jump_border)
            attackBorder = view.findViewById(com.replica.replicaisland.R.id.attack_border)
            leftBorder!!.setOnClickListener(ConfigClickListener(com.replica.replicaisland.R.id.key_left))
            rightBorder!!.setOnClickListener(ConfigClickListener(com.replica.replicaisland.R.id.key_right))
            jumpBorder!!.setOnClickListener(ConfigClickListener(com.replica.replicaisland.R.id.key_jump))
            attackBorder!!.setOnClickListener(ConfigClickListener(com.replica.replicaisland.R.id.key_attack))
            unselectedBorder = mContext!!.resources.getDrawable(com.replica.replicaisland.R.drawable.key_config_border)
            mSelectedBorder = mContext!!.resources.getDrawable(com.replica.replicaisland.R.drawable.key_config_border_active)
        }
        listeningId = 0
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
        if (keyLabels == null) {
            keyLabels = mContext!!.resources.getStringArray(com.replica.replicaisland.R.array.keycode_labels)
        }
        if (keycode > 0 && keycode < keyLabels!!.size) {
            result = keyLabels!![keycode - 1]
        }
        return result
    }

    fun selectId(id: Int) {
        if (listeningId != 0) {
            // unselect the current box
            val border = getConfigViewById(listeningId)
            border!!.setBackgroundDrawable(unselectedBorder)
        }
        listeningId = if (id == listeningId || id == 0) {
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
            com.replica.replicaisland.R.id.key_left -> config = leftBorder
            com.replica.replicaisland.R.id.key_right -> config = rightBorder
            com.replica.replicaisland.R.id.key_jump -> config = jumpBorder
            com.replica.replicaisland.R.id.key_attack -> config = attackBorder
        }
        return config
    }

    @SuppressLint("ApplySharedPref")
    override fun onDialogClosed(positiveResult: Boolean) {
        super.onDialogClosed(positiveResult)
        if (positiveResult) {
            // save changes
            val editor = sharedPrefs!!.edit()
            editor.putInt(leftPrefKey, leftKeyCode)
            editor.putInt(rightPrefKey, rightKeyCode)
            editor.putInt(jumpPrefKey, jumpKeyCode)
            editor.putInt(attackPrefKey, attackKeyCode)
            editor.commit()
        }
    }

    fun setPrefs(sharedPreferences: SharedPreferences?) {
        sharedPrefs = sharedPreferences
    }

    fun setContext(context: Context?) {
        mContext = context
    }

    override fun onKey(dialog: DialogInterface, keyCode: Int, event: KeyEvent): Boolean {
        var eatKey = false
        if (listeningId != 0) {
            eatKey = true
            when (listeningId) {
                com.replica.replicaisland.R.id.key_left -> {
                    leftText!!.text = getKeyLabel(keyCode)
                    leftKeyCode = keyCode
                }
                com.replica.replicaisland.R.id.key_right -> {
                    rightText!!.text = getKeyLabel(keyCode)
                    rightKeyCode = keyCode
                }
                com.replica.replicaisland.R.id.key_jump -> {
                    jumpText!!.text = getKeyLabel(keyCode)
                    jumpKeyCode = keyCode
                }
                com.replica.replicaisland.R.id.key_attack -> {
                    attackText!!.text = getKeyLabel(keyCode)
                    attackKeyCode = keyCode
                }
            }
            selectId(0) // deselect the current config box;
        }
        return eatKey
    }

    init {
        val a = context.obtainStyledAttributes(attrs,
                com.replica.replicaisland.R.styleable.KeyConfigPreference, defStyle, 0)
        leftPrefKey = a.getString(com.replica.replicaisland.R.styleable.KeyConfigPreference_leftKey)
        rightPrefKey = a.getString(com.replica.replicaisland.R.styleable.KeyConfigPreference_rightKey)
        jumpPrefKey = a.getString(com.replica.replicaisland.R.styleable.KeyConfigPreference_jumpKey)
        attackPrefKey = a.getString(com.replica.replicaisland.R.styleable.KeyConfigPreference_attackKey)
        a.recycle()
    }
}
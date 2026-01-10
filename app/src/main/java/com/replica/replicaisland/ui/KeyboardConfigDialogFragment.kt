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

import android.content.DialogInterface
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceDialogFragmentCompat
import com.replica.replicaisland.R

class KeyboardConfigDialogFragment : PreferenceDialogFragmentCompat(), DialogInterface.OnKeyListener {

    private var keyLabels: Array<String>? = null
    private var listeningId = 0

    private var leftBorder: View? = null
    private var rightBorder: View? = null
    private var jumpBorder: View? = null
    private var attackBorder: View? = null

    private var unselectedBorder: Drawable? = null
    private var selectedBorder: Drawable? = null

    private var leftKeyCode = 0
    private var rightKeyCode = 0
    private var jumpKeyCode = 0
    private var attackKeyCode = 0

    private var leftText: TextView? = null
    private var rightText: TextView? = null
    private var jumpText: TextView? = null
    private var attackText: TextView? = null

    private inner class ConfigClickListener(private val id: Int) : View.OnClickListener {
        override fun onClick(v: View) {
            selectId(id)
        }
    }

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)

        val pref = preference as? KeyboardConfigDialogPreferenceCompat ?: return
        val prefs = pref.getPrefs() ?: return
        val context = requireContext()

        leftKeyCode = prefs.getInt(pref.leftPrefKey, KeyEvent.KEYCODE_DPAD_LEFT)
        rightKeyCode = prefs.getInt(pref.rightPrefKey, KeyEvent.KEYCODE_DPAD_RIGHT)
        jumpKeyCode = prefs.getInt(pref.jumpPrefKey, KeyEvent.KEYCODE_SPACE)
        attackKeyCode = prefs.getInt(pref.attackPrefKey, KeyEvent.KEYCODE_SHIFT_LEFT)

        leftText = view.findViewById(R.id.key_left)
        leftText?.text = getKeyLabel(leftKeyCode)

        rightText = view.findViewById(R.id.key_right)
        rightText?.text = getKeyLabel(rightKeyCode)

        jumpText = view.findViewById(R.id.key_jump)
        jumpText?.text = getKeyLabel(jumpKeyCode)

        attackText = view.findViewById(R.id.key_attack)
        attackText?.text = getKeyLabel(attackKeyCode)

        leftBorder = view.findViewById(R.id.left_border)
        rightBorder = view.findViewById(R.id.right_border)
        jumpBorder = view.findViewById(R.id.jump_border)
        attackBorder = view.findViewById(R.id.attack_border)

        leftBorder?.setOnClickListener(ConfigClickListener(R.id.key_left))
        rightBorder?.setOnClickListener(ConfigClickListener(R.id.key_right))
        jumpBorder?.setOnClickListener(ConfigClickListener(R.id.key_jump))
        attackBorder?.setOnClickListener(ConfigClickListener(R.id.key_attack))

        unselectedBorder = ContextCompat.getDrawable(context, R.drawable.key_config_border)
        selectedBorder = ContextCompat.getDrawable(context, R.drawable.key_config_border_active)

        listeningId = 0
    }

    override fun onStart() {
        super.onStart()
        dialog?.setOnKeyListener(this)
        dialog?.takeKeyEvents(true)
    }

    private fun getKeyLabel(keycode: Int): String {
        if (keyLabels == null) {
            keyLabels = resources.getStringArray(R.array.keycode_labels)
        }
        return if (keycode > 0 && keycode <= keyLabels!!.size) {
            keyLabels!![keycode - 1]
        } else {
            "Unknown Key"
        }
    }

    private fun selectId(id: Int) {
        if (listeningId != 0) {
            // unselect the current box
            val border = getConfigViewById(listeningId)
            border?.background = unselectedBorder
        }

        listeningId = if (id == listeningId || id == 0) {
            0 // toggle off and end
        } else {
            // select the new box
            val border = getConfigViewById(id)
            border?.background = selectedBorder
            id
        }
    }

    private fun getConfigViewById(id: Int): View? {
        return when (id) {
            R.id.key_left -> leftBorder
            R.id.key_right -> rightBorder
            R.id.key_jump -> jumpBorder
            R.id.key_attack -> attackBorder
            else -> null
        }
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            val pref = preference as? KeyboardConfigDialogPreferenceCompat ?: return
            val prefs = pref.getPrefs() ?: return

            prefs.edit().apply {
                putInt(pref.leftPrefKey, leftKeyCode)
                putInt(pref.rightPrefKey, rightKeyCode)
                putInt(pref.jumpPrefKey, jumpKeyCode)
                putInt(pref.attackPrefKey, attackKeyCode)
                apply()
            }
        }
    }

    override fun onKey(dialog: DialogInterface, keyCode: Int, event: KeyEvent): Boolean {
        if (listeningId == 0) return false

        when (listeningId) {
            R.id.key_left -> {
                leftText?.text = getKeyLabel(keyCode)
                leftKeyCode = keyCode
            }
            R.id.key_right -> {
                rightText?.text = getKeyLabel(keyCode)
                rightKeyCode = keyCode
            }
            R.id.key_jump -> {
                jumpText?.text = getKeyLabel(keyCode)
                jumpKeyCode = keyCode
            }
            R.id.key_attack -> {
                attackText?.text = getKeyLabel(keyCode)
                attackKeyCode = keyCode
            }
        }
        selectId(0) // deselect the current config box
        return true
    }

    companion object {
        fun newInstance(key: String): KeyboardConfigDialogFragment {
            return KeyboardConfigDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_KEY, key)
                }
            }
        }
    }
}

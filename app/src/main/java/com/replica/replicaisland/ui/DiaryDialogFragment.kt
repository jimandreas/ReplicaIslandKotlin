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
import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.replica.replicaisland.R
import com.replica.replicaisland.core.BaseObject

/**
 * DialogFragment that displays diary entries collected during gameplay.
 * Replaces DiaryActivity with a modern fragment-based approach.
 */
class DiaryDialogFragment : DialogFragment() {

    companion object {
        private const val ARG_TEXT_RESOURCE = "text"
        const val TAG = "DiaryDialogFragment"

        /**
         * Factory method to create a new instance with the specified text resource.
         */
        fun newInstance(textResource: Int): DiaryDialogFragment {
            return DiaryDialogFragment().apply {
                arguments = bundleOf(ARG_TEXT_RESOURCE to textResource)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.Theme_FullscreenDialogFragment)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.apply {
            setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_diary, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Force landscape orientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        val text = view.findViewById<TextView>(R.id.diarytext)
        val image = view.findViewById<ImageView>(R.id.diarybackground)

        // Start fade animation on background
        image.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.fade))

        // Set text from arguments
        val textResource = arguments?.getInt(ARG_TEXT_RESOURCE, -1) ?: -1
        if (textResource != -1) {
            text.setText(textResource)
        }

        // Setup OK button with animation
        val okArrow = view.findViewById<ImageView>(R.id.ok)
        okArrow.setOnClickListener {
            dismiss()
        }
        okArrow.setBackgroundResource(R.drawable.ui_button)
        val anim = okArrow.background as AnimationDrawable
        anim.start()

        // Show toast notification
        BaseObject.sSystemRegistry.customToastSystem?.toast(
            getString(R.string.diary_found),
            Toast.LENGTH_SHORT
        )
    }

    override fun onStart() {
        super.onStart()
        // Ensure dialog is fullscreen
        dialog?.window?.apply {
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
        }
    }
}

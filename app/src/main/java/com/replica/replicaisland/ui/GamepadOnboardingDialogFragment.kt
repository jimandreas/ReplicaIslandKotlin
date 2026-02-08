/*
 * Copyright (C) 2025 Jim Andreas
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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.replica.replicaisland.R

/**
 * Dialog that appears when a gamepad is detected while on-screen controls are active.
 * Shows a graphical gamepad diagram with control callouts and offers to switch input mode.
 */
class GamepadOnboardingDialogFragment : DialogFragment() {

    companion object {
        const val TAG = "GamepadOnboardingDialog"
        const val REQUEST_KEY = "gamepad_onboarding_request"
        const val RESULT_USE_GAMEPAD = "use_gamepad"

        fun newInstance(): GamepadOnboardingDialogFragment {
            return GamepadOnboardingDialogFragment()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, android.R.style.Theme_Translucent_NoTitleBar)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_gamepad_onboarding, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.gamepad_use_button).setOnClickListener {
            parentFragmentManager.setFragmentResult(
                REQUEST_KEY,
                bundleOf(RESULT_USE_GAMEPAD to true)
            )
            dismiss()
        }

        view.findViewById<Button>(R.id.gamepad_cancel_button).setOnClickListener {
            parentFragmentManager.setFragmentResult(
                REQUEST_KEY,
                bundleOf(RESULT_USE_GAMEPAD to false)
            )
            dismiss()
        }
    }
}

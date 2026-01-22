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

import android.app.Dialog
import android.os.Bundle
import android.text.Html
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.replica.replicaisland.R

/**
 * DialogFragment that displays the auto-selected control scheme to new users
 * and offers the option to change it. Uses Fragment Result API to communicate
 * the user's choice back to the activity.
 */
class ControlSetupDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val controlsString = requireArguments().getString(ARG_CONTROLS_STRING, "")
        val messageFormat = resources.getString(R.string.control_setup_dialog_message)
        val message = String.format(messageFormat, controlsString)
        val styledMessage = Html.fromHtml(message, Html.FROM_HTML_MODE_LEGACY)

        return AlertDialog.Builder(requireContext())
            .setTitle(R.string.control_setup_dialog_title)
            .setMessage(styledMessage)
            .setPositiveButton(R.string.control_setup_dialog_ok, null)
            .setNegativeButton(R.string.control_setup_dialog_change) { _, _ ->
                setFragmentResult(REQUEST_KEY, bundleOf(RESULT_OPEN_SETTINGS to true))
            }
            .create()
    }

    companion object {
        const val TAG = "ControlSetupDialogFragment"
        const val REQUEST_KEY = "control_setup_request"
        const val RESULT_OPEN_SETTINGS = "open_settings"
        private const val ARG_CONTROLS_STRING = "controls_string"

        fun newInstance(selectedControlsString: String) = ControlSetupDialogFragment().apply {
            arguments = bundleOf(ARG_CONTROLS_STRING to selectedControlsString)
        }
    }
}

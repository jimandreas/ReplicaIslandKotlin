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
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.replica.replicaisland.R

/**
 * DialogFragment that prompts users upgrading from tilt controls to switch to screen controls.
 * Uses Fragment Result API to communicate the user's choice back to the activity.
 */
class TiltControlsDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireContext())
            .setTitle(R.string.onscreen_tilt_dialog_title)
            .setMessage(R.string.onscreen_tilt_dialog_message)
            .setPositiveButton(R.string.onscreen_tilt_dialog_ok) { _, _ ->
                setFragmentResult(REQUEST_KEY, bundleOf(RESULT_ENABLE_SCREEN_CONTROLS to true))
            }
            .setNegativeButton(R.string.onscreen_tilt_dialog_cancel, null)
            .create()
    }

    companion object {
        const val TAG = "TiltControlsDialogFragment"
        const val REQUEST_KEY = "tilt_controls_request"
        const val RESULT_ENABLE_SCREEN_CONTROLS = "enable_screen_controls"

        fun newInstance() = TiltControlsDialogFragment()
    }
}

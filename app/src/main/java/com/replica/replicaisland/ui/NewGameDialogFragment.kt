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
 * DialogFragment that confirms starting a new game when there's an existing save.
 * Uses Fragment Result API to communicate the user's choice and game start type
 * back to the activity.
 */
class NewGameDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val gameStartType = requireArguments().getInt(ARG_GAME_START_TYPE)

        return AlertDialog.Builder(requireContext())
            .setTitle(R.string.new_game_dialog_title)
            .setMessage(R.string.new_game_dialog_message)
            .setPositiveButton(R.string.new_game_dialog_ok) { _, _ ->
                setFragmentResult(REQUEST_KEY, bundleOf(
                    RESULT_CONFIRMED to true,
                    RESULT_GAME_START_TYPE to gameStartType
                ))
            }
            .setNegativeButton(R.string.new_game_dialog_cancel, null)
            .create()
    }

    companion object {
        const val TAG = "NewGameDialogFragment"
        const val REQUEST_KEY = "new_game_request"
        const val RESULT_CONFIRMED = "confirmed"
        const val RESULT_GAME_START_TYPE = "game_start_type"
        private const val ARG_GAME_START_TYPE = "game_start_type"

        fun newInstance(pendingGameStart: Int) = NewGameDialogFragment().apply {
            arguments = bundleOf(ARG_GAME_START_TYPE to pendingGameStart)
        }
    }
}

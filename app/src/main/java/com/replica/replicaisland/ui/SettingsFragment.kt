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

import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.replica.replicaisland.R
import com.replica.replicaisland.data.DataStorePreferenceAdapter
import com.replica.replicaisland.data.PreferencesManager

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Use DataStore instead of SharedPreferences
        preferenceManager.preferenceDataStore = DataStorePreferenceAdapter.getInstance(requireContext())
        setPreferencesFromResource(R.xml.preferences, rootKey)

        // Setup YesNoDialogPreference listener
        findPreference<YesNoDialogPreferenceCompat>(KEY_ERASE_GAME)?.let { pref ->
            pref.setListener(activity as? YesNoDialogPreferenceCompat.YesNoDialogListener)
        }

        // Setup KeyboardConfigDialogPreference with PreferencesManager
        findPreference<KeyboardConfigDialogPreferenceCompat>(KEY_CONFIG)?.let { pref ->
            pref.setPreferencesManager(PreferencesManager.getInstance(requireContext()))
        }
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        val dialogFragment: DialogFragment? = when (preference) {
            is YesNoDialogPreferenceCompat -> {
                YesNoDialogFragment.newInstance(preference.key)
            }
            is KeyboardConfigDialogPreferenceCompat -> {
                KeyboardConfigDialogFragment.newInstance(preference.key)
            }
            else -> null
        }

        if (dialogFragment != null) {
            @Suppress("DEPRECATION")
            dialogFragment.setTargetFragment(this, 0)
            dialogFragment.show(parentFragmentManager, DIALOG_FRAGMENT_TAG)
        } else {
            super.onDisplayPreferenceDialog(preference)
        }
    }

    companion object {
        const val KEY_ERASE_GAME = "erasegame"
        const val KEY_CONFIG = "keyconfig"
        const val CONTROL_CONFIG_SCREEN_KEY = "controlConfigScreen"
        private const val DIALOG_FRAGMENT_TAG = "androidx.preference.PreferenceFragment.DIALOG"
    }
}

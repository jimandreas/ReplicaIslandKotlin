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
@file:Suppress("DEPRECATION", "SENSELESS_COMPARISON", "ApplySharedPref")

package com.replica.replicaisland

import android.os.Bundle
import android.preference.PreferenceActivity
import android.preference.PreferenceScreen
import android.widget.Toast
import com.replica.replicaisland.YesNoDialogPreference.YesNoDialogListener

class SetPreferencesActivity : PreferenceActivity(), YesNoDialogListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            super.onCreate(savedInstanceState)
        } else {
            super.onCreate(null)
        }
        preferenceManager.sharedPreferencesMode = MODE_PRIVATE
        preferenceManager.sharedPreferencesName = PreferenceConstants.PREFERENCE_NAME

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences)
        val eraseGameButton = preferenceManager.findPreference("erasegame")
        if (eraseGameButton != null) {
            val yesNo = eraseGameButton as YesNoDialogPreference
            yesNo.setListener(this)
        }
        val configureKeyboardPref = preferenceManager.findPreference("keyconfig")
        if (configureKeyboardPref != null) {
            val config = configureKeyboardPref as KeyboardConfigDialogPreference
            config.setPrefs(getSharedPreferences(PreferenceConstants.PREFERENCE_NAME, MODE_PRIVATE))
            config.context = this
        }
        if (intent.getBooleanExtra("controlConfig", false)) {
            val controlConfig = preferenceManager.findPreference("controlConfigScreen") as PreferenceScreen
            if (controlConfig != null) {
                preferenceScreen = controlConfig
            }
        }
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            val prefs = getSharedPreferences(PreferenceConstants.PREFERENCE_NAME, MODE_PRIVATE)
            val editor = prefs.edit()
            editor.remove(PreferenceConstants.PREFERENCE_LEVEL_ROW)
            editor.remove(PreferenceConstants.PREFERENCE_LEVEL_INDEX)
            editor.remove(PreferenceConstants.PREFERENCE_LEVEL_COMPLETED)
            editor.remove(PreferenceConstants.PREFERENCE_LINEAR_MODE)
            editor.remove(PreferenceConstants.PREFERENCE_TOTAL_GAME_TIME)
            editor.remove(PreferenceConstants.PREFERENCE_PEARLS_COLLECTED)
            editor.remove(PreferenceConstants.PREFERENCE_PEARLS_TOTAL)
            editor.remove(PreferenceConstants.PREFERENCE_ROBOTS_DESTROYED)
            editor.remove(PreferenceConstants.PREFERENCE_DIFFICULTY)
            editor.commit()
            Toast.makeText(this, R.string.saved_game_erased_notification,
                    Toast.LENGTH_SHORT).show()
        }
    }
}
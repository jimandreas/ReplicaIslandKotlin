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

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.preference.PreferenceFragmentCompat
import com.replica.replicaisland.R

class SetPreferencesActivity : AppCompatActivity(), YesNoDialogPreferenceCompat.YesNoDialogListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Landscape orientation and fullscreen setup
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())

        if (savedInstanceState == null) {
            val fragment = SettingsFragment()

            // Handle "controlConfig" intent extra to navigate directly to controls screen
            if (intent.getBooleanExtra("controlConfig", false)) {
                fragment.arguments = Bundle().apply {
                    putString(
                        PreferenceFragmentCompat.ARG_PREFERENCE_ROOT,
                        SettingsFragment.CONTROL_CONFIG_SCREEN_KEY
                    )
                }
            }

            supportFragmentManager.beginTransaction()
                .replace(R.id.settings_container, fragment)
                .commit()
        }
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            val prefs = getSharedPreferences(PreferenceConstants.PREFERENCE_NAME, MODE_PRIVATE)
            prefs.edit().apply {
                remove(PreferenceConstants.PREFERENCE_LEVEL_ROW)
                remove(PreferenceConstants.PREFERENCE_LEVEL_INDEX)
                remove(PreferenceConstants.PREFERENCE_LEVEL_COMPLETED)
                remove(PreferenceConstants.PREFERENCE_LINEAR_MODE)
                remove(PreferenceConstants.PREFERENCE_TOTAL_GAME_TIME)
                remove(PreferenceConstants.PREFERENCE_PEARLS_COLLECTED)
                remove(PreferenceConstants.PREFERENCE_PEARLS_TOTAL)
                remove(PreferenceConstants.PREFERENCE_ROBOTS_DESTROYED)
                remove(PreferenceConstants.PREFERENCE_DIFFICULTY)
                apply()
            }
            Toast.makeText(
                this,
                R.string.saved_game_erased_notification,
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}

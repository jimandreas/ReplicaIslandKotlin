package com.replica.replicaisland.ui

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.preference.PreferenceActivity
import android.preference.PreferenceScreen
import android.widget.Toast
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.replica.replicaisland.R

class SetPreferencesActivity : PreferenceActivity(), YesNoDialogPreference.YesNoDialogListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            super.onCreate(savedInstanceState)
        } else {
            super.onCreate(null)
        }

        // New method of landscape orientation.
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
        // end of new method of landscape orientation

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
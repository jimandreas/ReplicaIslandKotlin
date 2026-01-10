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

package com.replica.replicaisland.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.replica.replicaisland.ui.PreferenceConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * One-time migration helper from SharedPreferences to DataStore.
 * Runs on first app launch after update and copies all existing preferences.
 */
class PreferencesMigrationHelper(private val context: Context) {

    companion object {
        private const val TAG = "PrefsMigration"
        private const val MIGRATION_COMPLETE_KEY = "datastore_migration_complete"
    }

    /**
     * Migrates all preferences from SharedPreferences to DataStore if not already done.
     * This is a one-time operation that marks completion in SharedPreferences.
     */
    suspend fun migrateIfNeeded(repository: GamePreferencesRepository) {
        val sharedPrefs = context.getSharedPreferences(
            PreferenceConstants.PREFERENCE_NAME,
            Context.MODE_PRIVATE
        )

        if (sharedPrefs.getBoolean(MIGRATION_COMPLETE_KEY, false)) {
            Log.d(TAG, "Migration already complete, skipping")
            return
        }

        withContext(Dispatchers.IO) {
            Log.d(TAG, "Starting preferences migration to DataStore")

            // Migrate game progress
            migrateInt(sharedPrefs, PreferenceConstants.PREFERENCE_LEVEL_ROW) { row ->
                val index = sharedPrefs.getInt(PreferenceConstants.PREFERENCE_LEVEL_INDEX, 0)
                val completed = sharedPrefs.getInt(PreferenceConstants.PREFERENCE_LEVEL_COMPLETED, 0)
                repository.saveLevelProgress(row, index, completed)
            }
            migrateInt(sharedPrefs, PreferenceConstants.PREFERENCE_LINEAR_MODE) {
                repository.setLinearMode(it)
            }
            migrateInt(sharedPrefs, PreferenceConstants.PREFERENCE_DIFFICULTY) {
                repository.setDifficulty(it)
            }

            // Migrate game stats
            migrateFloat(sharedPrefs, PreferenceConstants.PREFERENCE_TOTAL_GAME_TIME) {
                repository.setTotalGameTime(it)
            }
            migrateInt(sharedPrefs, PreferenceConstants.PREFERENCE_PEARLS_COLLECTED) {
                repository.setPearlsCollected(it)
            }
            migrateInt(sharedPrefs, PreferenceConstants.PREFERENCE_PEARLS_TOTAL) {
                repository.setPearlsTotal(it)
            }
            migrateInt(sharedPrefs, PreferenceConstants.PREFERENCE_ROBOTS_DESTROYED) {
                repository.setRobotsDestroyed(it)
            }
            migrateInt(sharedPrefs, PreferenceConstants.PREFERENCE_LAST_ENDING) {
                repository.setLastEnding(it)
            }
            migrateBoolean(sharedPrefs, PreferenceConstants.PREFERENCE_EXTRAS_UNLOCKED) {
                repository.setExtrasUnlocked(it)
            }

            // Migrate settings
            migrateBoolean(sharedPrefs, PreferenceConstants.PREFERENCE_SOUND_ENABLED) {
                repository.setSoundEnabled(it)
            }
            migrateBoolean(sharedPrefs, PreferenceConstants.PREFERENCE_SAFE_MODE) {
                repository.setSafeMode(it)
            }
            migrateBoolean(sharedPrefs, PreferenceConstants.PREFERENCE_CLICK_ATTACK) {
                repository.setClickAttack(it)
            }
            migrateBoolean(sharedPrefs, PreferenceConstants.PREFERENCE_TILT_CONTROLS) {
                repository.setTiltControls(it)
            }
            migrateInt(sharedPrefs, PreferenceConstants.PREFERENCE_TILT_SENSITIVITY) {
                repository.setTiltSensitivity(it)
            }
            migrateInt(sharedPrefs, PreferenceConstants.PREFERENCE_MOVEMENT_SENSITIVITY) {
                repository.setMovementSensitivity(it)
            }
            migrateBoolean(sharedPrefs, PreferenceConstants.PREFERENCE_SCREEN_CONTROLS) {
                repository.setScreenControls(it)
            }
            migrateBoolean(sharedPrefs, PreferenceConstants.PREFERENCE_ENABLE_DEBUG) {
                repository.setDebugEnabled(it)
            }
            migrateBoolean(sharedPrefs, PreferenceConstants.PREFERENCE_STATS_ENABLED) {
                repository.setStatsEnabled(it)
            }

            // Migrate key config
            if (sharedPrefs.contains(PreferenceConstants.PREFERENCE_LEFT_KEY)) {
                val leftKey = sharedPrefs.getInt(PreferenceConstants.PREFERENCE_LEFT_KEY, 21)
                val rightKey = sharedPrefs.getInt(PreferenceConstants.PREFERENCE_RIGHT_KEY, 22)
                val jumpKey = sharedPrefs.getInt(PreferenceConstants.PREFERENCE_JUMP_KEY, 62)
                val attackKey = sharedPrefs.getInt(PreferenceConstants.PREFERENCE_ATTACK_KEY, 59)
                repository.saveKeyConfig(leftKey, rightKey, jumpKey, attackKey)
            }

            // Migrate session data
            migrateLong(sharedPrefs, PreferenceConstants.PREFERENCE_SESSION_ID) {
                repository.setSessionId(it)
            }
            migrateInt(sharedPrefs, PreferenceConstants.PREFERENCE_LAST_VERSION) {
                repository.setLastVersion(it)
            }

            // Mark migration complete
            sharedPrefs.edit().putBoolean(MIGRATION_COMPLETE_KEY, true).apply()
            Log.d(TAG, "Preferences migration completed successfully")
        }
    }

    private suspend fun migrateBoolean(
        prefs: SharedPreferences,
        key: String,
        setter: suspend (Boolean) -> Unit
    ) {
        if (prefs.contains(key)) {
            val value = prefs.getBoolean(key, false)
            setter(value)
            Log.d(TAG, "Migrated boolean $key = $value")
        }
    }

    private suspend fun migrateInt(
        prefs: SharedPreferences,
        key: String,
        setter: suspend (Int) -> Unit
    ) {
        if (prefs.contains(key)) {
            val value = prefs.getInt(key, 0)
            setter(value)
            Log.d(TAG, "Migrated int $key = $value")
        }
    }

    private suspend fun migrateFloat(
        prefs: SharedPreferences,
        key: String,
        setter: suspend (Float) -> Unit
    ) {
        if (prefs.contains(key)) {
            val value = prefs.getFloat(key, 0f)
            setter(value)
            Log.d(TAG, "Migrated float $key = $value")
        }
    }

    private suspend fun migrateLong(
        prefs: SharedPreferences,
        key: String,
        setter: suspend (Long) -> Unit
    ) {
        if (prefs.contains(key)) {
            val value = prefs.getLong(key, 0L)
            setter(value)
            Log.d(TAG, "Migrated long $key = $value")
        }
    }
}

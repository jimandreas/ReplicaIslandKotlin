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
import android.view.KeyEvent
import com.replica.replicaisland.ui.PreferenceConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import androidx.core.content.edit

/**
 * Facade for preference access during migration period.
 * Reads from SharedPreferences (for compatibility with PreferenceFragmentCompat).
 * Writes to BOTH SharedPreferences AND DataStore (dual-write strategy).
 *
 * This ensures data consistency during the transition period from SharedPreferences
 * to DataStore. Once Phase 4 is complete, this class will be updated to read
 * exclusively from DataStore.
 */
class PreferencesManager private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val sharedPrefs: SharedPreferences = appContext.getSharedPreferences(
        PreferenceConstants.PREFERENCE_NAME,
        Context.MODE_PRIVATE
    )
    private val repository = GamePreferencesRepository(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ============ Synchronous reads from SharedPreferences ============
    // (for game thread and UI compatibility)

    // Settings
    fun getSoundEnabled(): Boolean =
        sharedPrefs.getBoolean(PreferenceConstants.PREFERENCE_SOUND_ENABLED, true)

    fun getSafeMode(): Boolean =
        sharedPrefs.getBoolean(PreferenceConstants.PREFERENCE_SAFE_MODE, false)

    fun getClickAttack(): Boolean =
        sharedPrefs.getBoolean(PreferenceConstants.PREFERENCE_CLICK_ATTACK, true)

    fun getMovementSensitivity(): Int =
        sharedPrefs.getInt(PreferenceConstants.PREFERENCE_MOVEMENT_SENSITIVITY, 100)

    fun getScreenControls(): Boolean =
        sharedPrefs.getBoolean(PreferenceConstants.PREFERENCE_SCREEN_CONTROLS, false)

    fun getDebugEnabled(): Boolean =
        sharedPrefs.getBoolean(PreferenceConstants.PREFERENCE_ENABLE_DEBUG, false)

    fun getStatsEnabled(): Boolean =
        sharedPrefs.getBoolean(PreferenceConstants.PREFERENCE_STATS_ENABLED, true)

    // Game Progress
    fun getLevelRow(): Int =
        sharedPrefs.getInt(PreferenceConstants.PREFERENCE_LEVEL_ROW, 0)

    fun getLevelIndex(): Int =
        sharedPrefs.getInt(PreferenceConstants.PREFERENCE_LEVEL_INDEX, 0)

    fun getLevelCompleted(): Int =
        sharedPrefs.getInt(PreferenceConstants.PREFERENCE_LEVEL_COMPLETED, 0)

    fun getLinearMode(): Int =
        sharedPrefs.getInt(PreferenceConstants.PREFERENCE_LINEAR_MODE, 0)

    fun getDifficulty(): Int =
        sharedPrefs.getInt(PreferenceConstants.PREFERENCE_DIFFICULTY, 1)

    // Game Stats
    fun getTotalGameTime(): Float =
        sharedPrefs.getFloat(PreferenceConstants.PREFERENCE_TOTAL_GAME_TIME, 0f)

    fun getPearlsCollected(): Int =
        sharedPrefs.getInt(PreferenceConstants.PREFERENCE_PEARLS_COLLECTED, 0)

    fun getPearlsTotal(): Int =
        sharedPrefs.getInt(PreferenceConstants.PREFERENCE_PEARLS_TOTAL, 0)

    fun getRobotsDestroyed(): Int =
        sharedPrefs.getInt(PreferenceConstants.PREFERENCE_ROBOTS_DESTROYED, 0)

    fun getLastEnding(): Int =
        sharedPrefs.getInt(PreferenceConstants.PREFERENCE_LAST_ENDING, -1)

    fun getExtrasUnlocked(): Boolean =
        sharedPrefs.getBoolean(PreferenceConstants.PREFERENCE_EXTRAS_UNLOCKED, false)
        // hackjra - override extras unlocked
//        sharedPrefs.getBoolean(PreferenceConstants.PREFERENCE_EXTRAS_UNLOCKED, true)

    // Key Config
    fun getLeftKey(): Int =
        sharedPrefs.getInt(PreferenceConstants.PREFERENCE_LEFT_KEY, KeyEvent.KEYCODE_DPAD_LEFT)

    fun getRightKey(): Int =
        sharedPrefs.getInt(PreferenceConstants.PREFERENCE_RIGHT_KEY, KeyEvent.KEYCODE_DPAD_RIGHT)

    fun getJumpKey(): Int =
        sharedPrefs.getInt(PreferenceConstants.PREFERENCE_JUMP_KEY, KeyEvent.KEYCODE_SPACE)

    fun getAttackKey(): Int =
        sharedPrefs.getInt(PreferenceConstants.PREFERENCE_ATTACK_KEY, KeyEvent.KEYCODE_SHIFT_LEFT)

    // Session
    fun getSessionId(): Long =
        sharedPrefs.getLong(PreferenceConstants.PREFERENCE_SESSION_ID, 0L)

    fun getLastVersion(): Int =
        sharedPrefs.getInt(PreferenceConstants.PREFERENCE_LAST_VERSION, 0)

    // ============ Dual-write operations ============
    // Writes to both SharedPreferences AND DataStore

    fun setSoundEnabled(enabled: Boolean) {
        sharedPrefs.edit { putBoolean(PreferenceConstants.PREFERENCE_SOUND_ENABLED, enabled) }
        scope.launch { repository.setSoundEnabled(enabled) }
    }

    fun setSafeMode(enabled: Boolean) {
        sharedPrefs.edit { putBoolean(PreferenceConstants.PREFERENCE_SAFE_MODE, enabled)}
        scope.launch { repository.setSafeMode(enabled) }
    }

    fun setClickAttack(enabled: Boolean) {
        sharedPrefs.edit { putBoolean(PreferenceConstants.PREFERENCE_CLICK_ATTACK, enabled)}
        scope.launch { repository.setClickAttack(enabled) }
    }

    fun setMovementSensitivity(sensitivity: Int) {
        sharedPrefs.edit { putInt(PreferenceConstants.PREFERENCE_MOVEMENT_SENSITIVITY, sensitivity)}
        scope.launch { repository.setMovementSensitivity(sensitivity) }
    }

    fun setScreenControls(enabled: Boolean) {
        sharedPrefs.edit { putBoolean(PreferenceConstants.PREFERENCE_SCREEN_CONTROLS, enabled)}
        scope.launch { repository.setScreenControls(enabled) }
    }

    fun setDebugEnabled(enabled: Boolean) {
        sharedPrefs.edit { putBoolean(PreferenceConstants.PREFERENCE_ENABLE_DEBUG, enabled) }
        scope.launch { repository.setDebugEnabled(enabled) }
    }

    fun setStatsEnabled(enabled: Boolean) {
        sharedPrefs.edit { putBoolean(PreferenceConstants.PREFERENCE_STATS_ENABLED, enabled) }
        scope.launch { repository.setStatsEnabled(enabled) }
    }

    fun saveLevelProgress(row: Int, index: Int, completed: Int) {
        sharedPrefs.edit {
            putInt(PreferenceConstants.PREFERENCE_LEVEL_ROW, row)
                .putInt(PreferenceConstants.PREFERENCE_LEVEL_INDEX, index)
                .putInt(PreferenceConstants.PREFERENCE_LEVEL_COMPLETED, completed)
        }
        scope.launch { repository.saveLevelProgress(row, index, completed) }
    }

    fun setLinearMode(mode: Int) {
        sharedPrefs.edit {putInt(PreferenceConstants.PREFERENCE_LINEAR_MODE, mode) }
        scope.launch { repository.setLinearMode(mode) }
    }

    fun setDifficulty(difficulty: Int) {
        sharedPrefs.edit {putInt(PreferenceConstants.PREFERENCE_DIFFICULTY, difficulty) }
        scope.launch { repository.setDifficulty(difficulty) }
    }

    fun setTotalGameTime(time: Float) {
        sharedPrefs.edit {putFloat(PreferenceConstants.PREFERENCE_TOTAL_GAME_TIME, time) }
        scope.launch { repository.setTotalGameTime(time) }
    }

    fun setPearlsCollected(count: Int) {
        sharedPrefs.edit {putInt(PreferenceConstants.PREFERENCE_PEARLS_COLLECTED, count) }
        scope.launch { repository.setPearlsCollected(count) }
    }

    fun setPearlsTotal(count: Int) {
        sharedPrefs.edit {putInt(PreferenceConstants.PREFERENCE_PEARLS_TOTAL, count) }
        scope.launch { repository.setPearlsTotal(count) }
    }

    fun setRobotsDestroyed(count: Int) {
        sharedPrefs.edit {putInt(PreferenceConstants.PREFERENCE_ROBOTS_DESTROYED, count) }
        scope.launch { repository.setRobotsDestroyed(count) }
    }

    fun setLastEnding(ending: Int) {
        sharedPrefs.edit {putInt(PreferenceConstants.PREFERENCE_LAST_ENDING, ending) }
        scope.launch { repository.setLastEnding(ending) }
    }

    fun setExtrasUnlocked(unlocked: Boolean) {
        sharedPrefs.edit {putBoolean(PreferenceConstants.PREFERENCE_EXTRAS_UNLOCKED, unlocked) }
        scope.launch { repository.setExtrasUnlocked(unlocked) }
    }

    fun saveKeyConfig(leftKey: Int, rightKey: Int, jumpKey: Int, attackKey: Int) {
        sharedPrefs.edit {
                putInt(PreferenceConstants.PREFERENCE_LEFT_KEY, leftKey)
                    .putInt(PreferenceConstants.PREFERENCE_RIGHT_KEY, rightKey)
                    .putInt(PreferenceConstants.PREFERENCE_JUMP_KEY, jumpKey)
                    .putInt(PreferenceConstants.PREFERENCE_ATTACK_KEY, attackKey)
        }
        scope.launch { repository.saveKeyConfig(leftKey, rightKey, jumpKey, attackKey) }
    }

    fun setSessionId(sessionId: Long) {
        sharedPrefs.edit {putLong(PreferenceConstants.PREFERENCE_SESSION_ID, sessionId) }
        scope.launch { repository.setSessionId(sessionId) }
    }

    fun setLastVersion(version: Int) {
        sharedPrefs.edit {putInt(PreferenceConstants.PREFERENCE_LAST_VERSION, version) }
        scope.launch { repository.setLastVersion(version) }
    }

    fun saveGameStats(
        totalGameTime: Float,
        pearlsCollected: Int,
        pearlsTotal: Int,
        robotsDestroyed: Int
    ) {
        sharedPrefs.edit {
                putFloat(PreferenceConstants.PREFERENCE_TOTAL_GAME_TIME, totalGameTime)
                    .putInt(PreferenceConstants.PREFERENCE_PEARLS_COLLECTED, pearlsCollected)
                    .putInt(PreferenceConstants.PREFERENCE_PEARLS_TOTAL, pearlsTotal)
                    .putInt(PreferenceConstants.PREFERENCE_ROBOTS_DESTROYED, robotsDestroyed)
        }
        scope.launch {
            repository.saveGameStats(
                totalGameTime,
                pearlsCollected, pearlsTotal, robotsDestroyed) }
    }

    /**
     * Clears all game progress data (used when erasing save game).
     */
    fun clearGameProgress() {
        sharedPrefs.edit {
            remove(PreferenceConstants.PREFERENCE_LEVEL_ROW)
                .remove(PreferenceConstants.PREFERENCE_LEVEL_INDEX)
                .remove(PreferenceConstants.PREFERENCE_LEVEL_COMPLETED)
                .remove(PreferenceConstants.PREFERENCE_LINEAR_MODE)
                .remove(PreferenceConstants.PREFERENCE_TOTAL_GAME_TIME)
                .remove(PreferenceConstants.PREFERENCE_PEARLS_COLLECTED)
                .remove(PreferenceConstants.PREFERENCE_PEARLS_TOTAL)
                .remove(PreferenceConstants.PREFERENCE_ROBOTS_DESTROYED)
                .remove(PreferenceConstants.PREFERENCE_DIFFICULTY)
        }
        scope.launch { repository.clearGameProgress() }
    }

    /**
     * Access to repository for migration and advanced use cases.
     */
    fun getRepository(): GamePreferencesRepository = repository

    companion object {
        @Volatile
        private var instance: PreferencesManager? = null

        fun getInstance(context: Context): PreferencesManager {
            return instance ?: synchronized(this) {
                instance ?: PreferencesManager(context.applicationContext).also { instance = it }
            }
        }
    }
}

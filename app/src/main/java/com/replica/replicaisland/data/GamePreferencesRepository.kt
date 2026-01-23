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
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Repository for accessing game preferences via DataStore.
 * Provides both Flow-based reactive access and suspending functions for one-time reads.
 */
class GamePreferencesRepository(context: Context) {

    private val dataStore = context.replicaIslandDataStore

    // ============ Flow-based reads for reactive UI ============

    // Game Progress
    val levelRow: Flow<Int> = dataStore.data.map { it[GamePreferencesKeys.LEVEL_ROW] ?: 0 }
    val levelIndex: Flow<Int> = dataStore.data.map { it[GamePreferencesKeys.LEVEL_INDEX] ?: 0 }
    val levelCompleted: Flow<Int> = dataStore.data.map { it[GamePreferencesKeys.LEVEL_COMPLETED] ?: 0 }
    val linearMode: Flow<Int> = dataStore.data.map { it[GamePreferencesKeys.LINEAR_MODE] ?: 0 }
    val difficulty: Flow<Int> = dataStore.data.map { it[GamePreferencesKeys.DIFFICULTY] ?: 1 }

    // Game Stats
    val totalGameTime: Flow<Float> = dataStore.data.map { it[GamePreferencesKeys.TOTAL_GAME_TIME] ?: 0f }
    val pearlsCollected: Flow<Int> = dataStore.data.map { it[GamePreferencesKeys.PEARLS_COLLECTED] ?: 0 }
    val pearlsTotal: Flow<Int> = dataStore.data.map { it[GamePreferencesKeys.PEARLS_TOTAL] ?: 0 }
    val robotsDestroyed: Flow<Int> = dataStore.data.map { it[GamePreferencesKeys.ROBOTS_DESTROYED] ?: 0 }
    val lastEnding: Flow<Int> = dataStore.data.map { it[GamePreferencesKeys.LAST_ENDING] ?: -1 }
    val extrasUnlocked: Flow<Boolean> = dataStore.data.map { it[GamePreferencesKeys.EXTRAS_UNLOCKED] ?: false }

    // Settings
    val soundEnabled: Flow<Boolean> = dataStore.data.map { it[GamePreferencesKeys.SOUND_ENABLED] ?: true }
    val safeMode: Flow<Boolean> = dataStore.data.map { it[GamePreferencesKeys.SAFE_MODE] ?: false }
    val clickAttack: Flow<Boolean> = dataStore.data.map { it[GamePreferencesKeys.CLICK_ATTACK] ?: true }
    val tiltControls: Flow<Boolean> = dataStore.data.map { it[GamePreferencesKeys.TILT_CONTROLS] ?: false }
    val tiltSensitivity: Flow<Int> = dataStore.data.map { it[GamePreferencesKeys.TILT_SENSITIVITY] ?: 50 }
    val movementSensitivity: Flow<Int> = dataStore.data.map { it[GamePreferencesKeys.MOVEMENT_SENSITIVITY] ?: 100 }
    val screenControls: Flow<Boolean> = dataStore.data.map { it[GamePreferencesKeys.SCREEN_CONTROLS] ?: false }
    val debugEnabled: Flow<Boolean> = dataStore.data.map { it[GamePreferencesKeys.DEBUG_ENABLED] ?: false }
    val statsEnabled: Flow<Boolean> = dataStore.data.map { it[GamePreferencesKeys.STATS_ENABLED] ?: true }

    // Key Config
    val leftKey: Flow<Int> = dataStore.data.map { it[GamePreferencesKeys.LEFT_KEY] ?: 21 } // KEYCODE_DPAD_LEFT
    val rightKey: Flow<Int> = dataStore.data.map { it[GamePreferencesKeys.RIGHT_KEY] ?: 22 } // KEYCODE_DPAD_RIGHT
    val jumpKey: Flow<Int> = dataStore.data.map { it[GamePreferencesKeys.JUMP_KEY] ?: 62 } // KEYCODE_SPACE
    val attackKey: Flow<Int> = dataStore.data.map { it[GamePreferencesKeys.ATTACK_KEY] ?: 59 } // KEYCODE_SHIFT_LEFT

    // Session
    val sessionId: Flow<Long> = dataStore.data.map { it[GamePreferencesKeys.SESSION_ID] ?: 0L }
    val lastVersion: Flow<Int> = dataStore.data.map { it[GamePreferencesKeys.LAST_VERSION] ?: 0 }

    // ============ Suspending one-time reads ============

    suspend fun getSoundEnabled(): Boolean = dataStore.data.first()[GamePreferencesKeys.SOUND_ENABLED] ?: true
    suspend fun getSafeMode(): Boolean = dataStore.data.first()[GamePreferencesKeys.SAFE_MODE] ?: false
    suspend fun getClickAttack(): Boolean = dataStore.data.first()[GamePreferencesKeys.CLICK_ATTACK] ?: true
    suspend fun getTiltControls(): Boolean = dataStore.data.first()[GamePreferencesKeys.TILT_CONTROLS] ?: false
    suspend fun getTiltSensitivity(): Int = dataStore.data.first()[GamePreferencesKeys.TILT_SENSITIVITY] ?: 50
    suspend fun getMovementSensitivity(): Int = dataStore.data.first()[GamePreferencesKeys.MOVEMENT_SENSITIVITY] ?: 100
    suspend fun getScreenControls(): Boolean = dataStore.data.first()[GamePreferencesKeys.SCREEN_CONTROLS] ?: false
    suspend fun getDebugEnabled(): Boolean = dataStore.data.first()[GamePreferencesKeys.DEBUG_ENABLED] ?: false
    suspend fun getStatsEnabled(): Boolean = dataStore.data.first()[GamePreferencesKeys.STATS_ENABLED] ?: true

    suspend fun getLevelRow(): Int = dataStore.data.first()[GamePreferencesKeys.LEVEL_ROW] ?: 0
    suspend fun getLevelIndex(): Int = dataStore.data.first()[GamePreferencesKeys.LEVEL_INDEX] ?: 0
    suspend fun getLevelCompleted(): Int = dataStore.data.first()[GamePreferencesKeys.LEVEL_COMPLETED] ?: 0
    suspend fun getLinearMode(): Int = dataStore.data.first()[GamePreferencesKeys.LINEAR_MODE] ?: 0
    suspend fun getDifficulty(): Int = dataStore.data.first()[GamePreferencesKeys.DIFFICULTY] ?: 1

    suspend fun getTotalGameTime(): Float = dataStore.data.first()[GamePreferencesKeys.TOTAL_GAME_TIME] ?: 0f
    suspend fun getPearlsCollected(): Int = dataStore.data.first()[GamePreferencesKeys.PEARLS_COLLECTED] ?: 0
    suspend fun getPearlsTotal(): Int = dataStore.data.first()[GamePreferencesKeys.PEARLS_TOTAL] ?: 0
    suspend fun getRobotsDestroyed(): Int = dataStore.data.first()[GamePreferencesKeys.ROBOTS_DESTROYED] ?: 0
    suspend fun getLastEnding(): Int = dataStore.data.first()[GamePreferencesKeys.LAST_ENDING] ?: -1
    suspend fun getExtrasUnlocked(): Boolean = dataStore.data.first()[GamePreferencesKeys.EXTRAS_UNLOCKED] ?: false

    suspend fun getLeftKey(): Int = dataStore.data.first()[GamePreferencesKeys.LEFT_KEY] ?: 21
    suspend fun getRightKey(): Int = dataStore.data.first()[GamePreferencesKeys.RIGHT_KEY] ?: 22
    suspend fun getJumpKey(): Int = dataStore.data.first()[GamePreferencesKeys.JUMP_KEY] ?: 62
    suspend fun getAttackKey(): Int = dataStore.data.first()[GamePreferencesKeys.ATTACK_KEY] ?: 59

    suspend fun getSessionId(): Long = dataStore.data.first()[GamePreferencesKeys.SESSION_ID] ?: 0L
    suspend fun getLastVersion(): Int = dataStore.data.first()[GamePreferencesKeys.LAST_VERSION] ?: 0

    // ============ Write operations ============

    suspend fun setSoundEnabled(enabled: Boolean) {
        dataStore.edit { it[GamePreferencesKeys.SOUND_ENABLED] = enabled }
    }

    suspend fun setSafeMode(enabled: Boolean) {
        dataStore.edit { it[GamePreferencesKeys.SAFE_MODE] = enabled }
    }

    suspend fun setClickAttack(enabled: Boolean) {
        dataStore.edit { it[GamePreferencesKeys.CLICK_ATTACK] = enabled }
    }

    suspend fun setTiltControls(enabled: Boolean) {
        dataStore.edit { it[GamePreferencesKeys.TILT_CONTROLS] = enabled }
    }

    suspend fun setTiltSensitivity(sensitivity: Int) {
        dataStore.edit { it[GamePreferencesKeys.TILT_SENSITIVITY] = sensitivity }
    }

    suspend fun setMovementSensitivity(sensitivity: Int) {
        dataStore.edit { it[GamePreferencesKeys.MOVEMENT_SENSITIVITY] = sensitivity }
    }

    suspend fun setScreenControls(enabled: Boolean) {
        dataStore.edit { it[GamePreferencesKeys.SCREEN_CONTROLS] = enabled }
    }

    suspend fun setDebugEnabled(enabled: Boolean) {
        dataStore.edit { it[GamePreferencesKeys.DEBUG_ENABLED] = enabled }
    }

    suspend fun setStatsEnabled(enabled: Boolean) {
        dataStore.edit { it[GamePreferencesKeys.STATS_ENABLED] = enabled }
    }

    suspend fun saveLevelProgress(row: Int, index: Int, completed: Int) {
        dataStore.edit { prefs ->
            prefs[GamePreferencesKeys.LEVEL_ROW] = row
            prefs[GamePreferencesKeys.LEVEL_INDEX] = index
            prefs[GamePreferencesKeys.LEVEL_COMPLETED] = completed
        }
    }

    suspend fun setLinearMode(mode: Int) {
        dataStore.edit { it[GamePreferencesKeys.LINEAR_MODE] = mode }
    }

    suspend fun setDifficulty(difficulty: Int) {
        dataStore.edit { it[GamePreferencesKeys.DIFFICULTY] = difficulty }
    }

    suspend fun setTotalGameTime(time: Float) {
        dataStore.edit { it[GamePreferencesKeys.TOTAL_GAME_TIME] = time }
    }

    suspend fun setPearlsCollected(count: Int) {
        dataStore.edit { it[GamePreferencesKeys.PEARLS_COLLECTED] = count }
    }

    suspend fun setPearlsTotal(count: Int) {
        dataStore.edit { it[GamePreferencesKeys.PEARLS_TOTAL] = count }
    }

    suspend fun setRobotsDestroyed(count: Int) {
        dataStore.edit { it[GamePreferencesKeys.ROBOTS_DESTROYED] = count }
    }

    suspend fun setLastEnding(ending: Int) {
        dataStore.edit { it[GamePreferencesKeys.LAST_ENDING] = ending }
    }

    suspend fun setExtrasUnlocked(unlocked: Boolean) {
        dataStore.edit { it[GamePreferencesKeys.EXTRAS_UNLOCKED] = unlocked }
    }

    suspend fun saveKeyConfig(leftKey: Int, rightKey: Int, jumpKey: Int, attackKey: Int) {
        dataStore.edit { prefs ->
            prefs[GamePreferencesKeys.LEFT_KEY] = leftKey
            prefs[GamePreferencesKeys.RIGHT_KEY] = rightKey
            prefs[GamePreferencesKeys.JUMP_KEY] = jumpKey
            prefs[GamePreferencesKeys.ATTACK_KEY] = attackKey
        }
    }

    suspend fun setSessionId(sessionId: Long) {
        dataStore.edit { it[GamePreferencesKeys.SESSION_ID] = sessionId }
    }

    suspend fun setLastVersion(version: Int) {
        dataStore.edit { it[GamePreferencesKeys.LAST_VERSION] = version }
    }

    suspend fun saveGameStats(
        totalGameTime: Float,
        pearlsCollected: Int,
        pearlsTotal: Int,
        robotsDestroyed: Int
    ) {
        dataStore.edit { prefs ->
            prefs[GamePreferencesKeys.TOTAL_GAME_TIME] = totalGameTime
            prefs[GamePreferencesKeys.PEARLS_COLLECTED] = pearlsCollected
            prefs[GamePreferencesKeys.PEARLS_TOTAL] = pearlsTotal
            prefs[GamePreferencesKeys.ROBOTS_DESTROYED] = robotsDestroyed
        }
    }

    /**
     * Clears all game progress data (used when erasing save game).
     */
    suspend fun clearGameProgress() {
        dataStore.edit { prefs ->
            prefs.remove(GamePreferencesKeys.LEVEL_ROW)
            prefs.remove(GamePreferencesKeys.LEVEL_INDEX)
            prefs.remove(GamePreferencesKeys.LEVEL_COMPLETED)
            prefs.remove(GamePreferencesKeys.LINEAR_MODE)
            prefs.remove(GamePreferencesKeys.TOTAL_GAME_TIME)
            prefs.remove(GamePreferencesKeys.PEARLS_COLLECTED)
            prefs.remove(GamePreferencesKeys.PEARLS_TOTAL)
            prefs.remove(GamePreferencesKeys.ROBOTS_DESTROYED)
            prefs.remove(GamePreferencesKeys.DIFFICULTY)
        }
    }
}

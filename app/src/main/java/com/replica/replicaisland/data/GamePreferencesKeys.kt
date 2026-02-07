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

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey

/**
 * DataStore preference keys for game settings and progress.
 * These keys mirror the SharedPreferences keys in PreferenceConstants
 * to enable migration and dual-write strategy.
 */
object GamePreferencesKeys {
    // Game Progress
    val LEVEL_ROW = intPreferencesKey("levelRow")
    val LEVEL_INDEX = intPreferencesKey("levelIndex")
    val LEVEL_COMPLETED = intPreferencesKey("levelsCompleted")
    val LINEAR_MODE = intPreferencesKey("linearMode")
    val DIFFICULTY = intPreferencesKey("difficulty")

    // Game Stats
    val TOTAL_GAME_TIME = floatPreferencesKey("totalGameTime")
    val PEARLS_COLLECTED = intPreferencesKey("pearlsCollected")
    val PEARLS_TOTAL = intPreferencesKey("pearlsTotal")
    val ROBOTS_DESTROYED = intPreferencesKey("robotsDestroyed")
    val LAST_ENDING = intPreferencesKey("lastEnding")
    val EXTRAS_UNLOCKED = booleanPreferencesKey("extrasUnlocked")

    // Settings
    val SOUND_ENABLED = booleanPreferencesKey("enableSound")
    val SAFE_MODE = booleanPreferencesKey("safeMode")
    val CLICK_ATTACK = booleanPreferencesKey("enableClickAttack")
    val MOVEMENT_SENSITIVITY = intPreferencesKey("movementSensitivity")
    val SCREEN_CONTROLS = booleanPreferencesKey("enableScreenControls")
    val DEBUG_ENABLED = booleanPreferencesKey("enableDebug")
    val STATS_ENABLED = booleanPreferencesKey("enableStats")

    // Key Config
    val LEFT_KEY = intPreferencesKey("keyLeft")
    val RIGHT_KEY = intPreferencesKey("keyRight")
    val JUMP_KEY = intPreferencesKey("keyJump")
    val ATTACK_KEY = intPreferencesKey("keyAttack")

    // Session
    val SESSION_ID = longPreferencesKey("session")
    val LAST_VERSION = intPreferencesKey("lastVersion")
}

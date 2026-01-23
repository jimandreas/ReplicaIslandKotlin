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
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.preference.PreferenceDataStore
import com.replica.replicaisland.ui.PreferenceConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Adapter that connects PreferenceFragmentCompat to DataStore.
 * Implements PreferenceDataStore to intercept preference reads/writes
 * and route them to BOTH DataStore AND SharedPreferences (dual-write strategy).
 *
 * This ensures consistency with PreferencesManager, which reads from SharedPreferences
 * during the migration period.
 */
class DataStorePreferenceAdapter(context: Context) : PreferenceDataStore() {

    private val appContext = context.applicationContext
    private val dataStore = appContext.replicaIslandDataStore
    private val sharedPrefs: SharedPreferences = appContext.getSharedPreferences(
        PreferenceConstants.PREFERENCE_NAME,
        Context.MODE_PRIVATE
    )
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Boolean preferences - dual-write to both SharedPreferences and DataStore
    override fun putBoolean(key: String, value: Boolean) {
        // Write to SharedPreferences synchronously for immediate availability
        sharedPrefs.edit().putBoolean(key, value).apply()
        // Write to DataStore asynchronously
        scope.launch {
            dataStore.edit { prefs ->
                prefs[booleanPreferencesKey(key)] = value
            }
        }
    }

    override fun getBoolean(key: String, defValue: Boolean): Boolean {
        return runBlocking {
            dataStore.data.first()[booleanPreferencesKey(key)] ?: defValue
        }
    }

    // Integer preferences - dual-write to both SharedPreferences and DataStore
    override fun putInt(key: String, value: Int) {
        // Write to SharedPreferences synchronously for immediate availability
        sharedPrefs.edit().putInt(key, value).apply()
        // Write to DataStore asynchronously
        scope.launch {
            dataStore.edit { prefs ->
                prefs[intPreferencesKey(key)] = value
            }
        }
    }

    override fun getInt(key: String, defValue: Int): Int {
        return runBlocking {
            dataStore.data.first()[intPreferencesKey(key)] ?: defValue
        }
    }

    // String preferences - dual-write to both SharedPreferences and DataStore
    override fun putString(key: String, value: String?) {
        // Write to SharedPreferences synchronously for immediate availability
        sharedPrefs.edit().putString(key, value).apply()
        // Write to DataStore asynchronously
        scope.launch {
            dataStore.edit { prefs ->
                if (value != null) {
                    prefs[androidx.datastore.preferences.core.stringPreferencesKey(key)] = value
                } else {
                    prefs.remove(androidx.datastore.preferences.core.stringPreferencesKey(key))
                }
            }
        }
    }

    override fun getString(key: String, defValue: String?): String? {
        return runBlocking {
            dataStore.data.first()[androidx.datastore.preferences.core.stringPreferencesKey(key)] ?: defValue
        }
    }

    companion object {
        @Volatile
        private var instance: DataStorePreferenceAdapter? = null

        fun getInstance(context: Context): DataStorePreferenceAdapter {
            return instance ?: synchronized(this) {
                instance ?: DataStorePreferenceAdapter(context.applicationContext).also { instance = it }
            }
        }
    }
}

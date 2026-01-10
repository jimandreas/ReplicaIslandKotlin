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
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.preference.PreferenceDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

// Extension property for DataStore - single instance per app
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "replica_island_preferences"
)

/**
 * Adapter that connects PreferenceFragmentCompat to DataStore.
 * Implements PreferenceDataStore to intercept preference reads/writes
 * and route them to DataStore instead of SharedPreferences.
 */
class DataStorePreferenceAdapter(context: Context) : PreferenceDataStore() {

    private val dataStore = context.applicationContext.settingsDataStore
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Boolean preferences
    override fun putBoolean(key: String, value: Boolean) {
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

    // Integer preferences
    override fun putInt(key: String, value: Int) {
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

    // String preferences (for completeness, though not currently used)
    override fun putString(key: String, value: String?) {
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

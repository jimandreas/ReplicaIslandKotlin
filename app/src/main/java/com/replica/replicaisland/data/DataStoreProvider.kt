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
import androidx.datastore.preferences.preferencesDataStore

/**
 * Single source of truth for the app's DataStore instance.
 * This ensures only one DataStore is active for the preferences file.
 */
val Context.replicaIslandDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "replica_island_preferences"
)

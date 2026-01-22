/*
 * Copyright (C) 2025 Jim Andreas
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

/**
 * Constants for Fragment Result API communication between LevelSelectDialogFragment and its host.
 */
object LevelSelectResult {
    /** Request key for registering fragment result listener */
    const val REQUEST_KEY = "level_select_request"

    /** Bundle key for the level resource ID */
    const val RESULT_RESOURCE = "resource"

    /** Bundle key for the level row index */
    const val RESULT_ROW = "row"

    /** Bundle key for the level index within the row */
    const val RESULT_INDEX = "index"
}

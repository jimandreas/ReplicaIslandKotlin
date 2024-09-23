/*
 * Copyright (C) 2010 The Android Open Source Project
 * Copyright (C) 2024 Jim Andreas kotlin conversion and updating
 *
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

package com.replica.replicaisland

import android.util.Log
import androidx.fragment.app.testing.launchFragment
import org.junit.Assert.assertEquals


internal class BasicDialogTest01: Runnable {
    override fun run() {
        tryPostingDialog01()
    }

    private fun tryPostingDialog01() {
        with(launchFragment<MainDialogFragment>()) {
            onFragment { fragment ->
                assertEquals(1, 1)
                //fragment.dismiss()
                //fragment.parentFragmentManager.executePendingTransactions()

            }
        }
        Log.d("test01", "here now")
        pause(10000)
    }

}
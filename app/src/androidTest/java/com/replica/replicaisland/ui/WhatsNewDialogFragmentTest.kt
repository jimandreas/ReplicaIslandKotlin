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

import androidx.fragment.app.testing.launchFragment
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.replica.replicaisland.R
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for WhatsNewDialogFragment.
 */
@RunWith(AndroidJUnit4::class)
class WhatsNewDialogFragmentTest {

    @Test
    fun dialogDisplaysCorrectTitle() {
        launchFragment<WhatsNewDialogFragment>()

        onView(withText(R.string.whats_new_dialog_title))
            .check(matches(isDisplayed()))
    }

    @Test
    fun dialogDisplaysCorrectMessage() {
        launchFragment<WhatsNewDialogFragment>()

        onView(withText(R.string.whats_new_dialog_message))
            .check(matches(isDisplayed()))
    }

    @Test
    fun dialogDisplaysOkButton() {
        launchFragment<WhatsNewDialogFragment>()

        onView(withText(R.string.whats_new_dialog_ok))
            .check(matches(isDisplayed()))
    }
}

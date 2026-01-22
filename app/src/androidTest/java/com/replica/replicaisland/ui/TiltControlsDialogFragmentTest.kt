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
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.replica.replicaisland.R
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for TiltControlsDialogFragment.
 */
@RunWith(AndroidJUnit4::class)
class TiltControlsDialogFragmentTest {

    @Test
    fun dialogDisplaysCorrectTitle() {
        launchFragment<TiltControlsDialogFragment>(
            themeResId = R.style.Theme_FullscreenDialogFragment
        )

        onView(withText(R.string.onscreen_tilt_dialog_title))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
    }

    @Test
    fun dialogDisplaysCorrectMessage() {
        launchFragment<TiltControlsDialogFragment>(
            themeResId = R.style.Theme_FullscreenDialogFragment
        )

        onView(withText(R.string.onscreen_tilt_dialog_message))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
    }

    @Test
    fun dialogDisplaysOkAndCancelButtons() {
        launchFragment<TiltControlsDialogFragment>(
            themeResId = R.style.Theme_FullscreenDialogFragment
        )

        onView(withText(R.string.onscreen_tilt_dialog_ok))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        onView(withText(R.string.onscreen_tilt_dialog_cancel))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
    }

    @Test
    fun positiveButtonSendsFragmentResult() {
        var resultReceived = false
        val scenario = launchFragment<TiltControlsDialogFragment>(
            themeResId = R.style.Theme_FullscreenDialogFragment
        )

        scenario.onFragment { fragment ->
            fragment.parentFragmentManager.setFragmentResultListener(
                TiltControlsDialogFragment.REQUEST_KEY,
                fragment
            ) { _, bundle ->
                resultReceived = bundle.getBoolean(TiltControlsDialogFragment.RESULT_ENABLE_SCREEN_CONTROLS)
            }
        }

        onView(withText(R.string.onscreen_tilt_dialog_ok))
            .inRoot(isDialog())
            .perform(click())

        // Give time for result to be delivered
        Thread.sleep(100)
        assertTrue("Fragment result should be received with enable_screen_controls=true", resultReceived)
    }
}

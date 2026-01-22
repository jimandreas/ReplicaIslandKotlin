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
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.replica.replicaisland.R
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for ControlSetupDialogFragment.
 */
@RunWith(AndroidJUnit4::class)
class ControlSetupDialogFragmentTest {

    @Test
    fun dialogDisplaysCorrectTitle() {
        launchFragment<ControlSetupDialogFragment>(
            fragmentArgs = ControlSetupDialogFragment.newInstance("Test Controls").arguments
        )

        onView(withText(R.string.control_setup_dialog_title))
            .check(matches(isDisplayed()))
    }

    @Test
    fun dialogDisplaysOkAndChangeButtons() {
        launchFragment<ControlSetupDialogFragment>(
            fragmentArgs = ControlSetupDialogFragment.newInstance("Test Controls").arguments
        )

        onView(withText(R.string.control_setup_dialog_ok))
            .check(matches(isDisplayed()))
        onView(withText(R.string.control_setup_dialog_change))
            .check(matches(isDisplayed()))
    }

    @Test
    fun changeButtonSendsFragmentResult() {
        var resultReceived = false
        val scenario = launchFragment<ControlSetupDialogFragment>(
            fragmentArgs = ControlSetupDialogFragment.newInstance("Test Controls").arguments
        )

        scenario.onFragment { fragment ->
            fragment.parentFragmentManager.setFragmentResultListener(
                ControlSetupDialogFragment.REQUEST_KEY,
                fragment
            ) { _, bundle ->
                resultReceived = bundle.getBoolean(ControlSetupDialogFragment.RESULT_OPEN_SETTINGS)
            }
        }

        onView(withText(R.string.control_setup_dialog_change)).perform(click())

        // Give time for result to be delivered
        Thread.sleep(100)
        assertTrue("Fragment result should be received with open_settings=true", resultReceived)
    }
}

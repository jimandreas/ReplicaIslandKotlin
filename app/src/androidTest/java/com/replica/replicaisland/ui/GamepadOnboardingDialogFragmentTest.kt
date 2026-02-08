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

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.replica.replicaisland.R
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for GamepadOnboardingDialogFragment.
 */
@RunWith(AndroidJUnit4::class)
class GamepadOnboardingDialogFragmentTest {

    @Test
    fun dialogDisplaysTitle() {
        launchFragmentInContainer<GamepadOnboardingDialogFragment>(
            themeResId = R.style.Theme_FullscreenDialogFragment
        )

        onView(withId(R.id.gamepad_onboarding_title)).check(matches(isDisplayed()))
    }

    @Test
    fun dialogDisplaysMessage() {
        launchFragmentInContainer<GamepadOnboardingDialogFragment>(
            themeResId = R.style.Theme_FullscreenDialogFragment
        )

        onView(withId(R.id.gamepad_onboarding_message)).check(matches(isDisplayed()))
    }

    @Test
    fun dialogDisplaysUseGamepadButton() {
        launchFragmentInContainer<GamepadOnboardingDialogFragment>(
            themeResId = R.style.Theme_FullscreenDialogFragment
        )

        onView(withId(R.id.gamepad_use_button)).check(matches(isDisplayed()))
    }

    @Test
    fun dialogDisplaysCancelButton() {
        launchFragmentInContainer<GamepadOnboardingDialogFragment>(
            themeResId = R.style.Theme_FullscreenDialogFragment
        )

        onView(withId(R.id.gamepad_cancel_button)).check(matches(isDisplayed()))
    }

    @Test
    fun useGamepadButtonSendsCorrectResult() {
        var useGamepad = false
        val scenario = launchFragmentInContainer<GamepadOnboardingDialogFragment>(
            themeResId = R.style.Theme_FullscreenDialogFragment
        )

        scenario.onFragment { fragment ->
            fragment.parentFragmentManager.setFragmentResultListener(
                GamepadOnboardingDialogFragment.REQUEST_KEY,
                fragment
            ) { _, bundle ->
                useGamepad = bundle.getBoolean(GamepadOnboardingDialogFragment.RESULT_USE_GAMEPAD)
            }
        }

        onView(withId(R.id.gamepad_use_button)).perform(click())

        // Give time for result to be delivered
        Thread.sleep(100)
        assertTrue("Fragment result should be received with use_gamepad=true", useGamepad)
    }

    @Test
    fun cancelButtonSendsCorrectResult() {
        var resultReceived = false
        var useGamepad = true // default to true so we can verify it becomes false
        val scenario = launchFragmentInContainer<GamepadOnboardingDialogFragment>(
            themeResId = R.style.Theme_FullscreenDialogFragment
        )

        scenario.onFragment { fragment ->
            fragment.parentFragmentManager.setFragmentResultListener(
                GamepadOnboardingDialogFragment.REQUEST_KEY,
                fragment
            ) { _, bundle ->
                resultReceived = true
                useGamepad = bundle.getBoolean(GamepadOnboardingDialogFragment.RESULT_USE_GAMEPAD)
            }
        }

        onView(withId(R.id.gamepad_cancel_button)).perform(click())

        // Give time for result to be delivered
        Thread.sleep(100)
        assertTrue("Fragment result should be received", resultReceived)
        assertTrue("Fragment result should have use_gamepad=false", !useGamepad)
    }
}

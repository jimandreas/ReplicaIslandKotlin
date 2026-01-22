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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for NewGameDialogFragment.
 */
@RunWith(AndroidJUnit4::class)
class NewGameDialogFragmentTest {

    @Test
    fun dialogDisplaysCorrectTitle() {
        launchFragment<NewGameDialogFragment>(
            fragmentArgs = NewGameDialogFragment.newInstance(0).arguments
        )

        onView(withText(R.string.new_game_dialog_title))
            .check(matches(isDisplayed()))
    }

    @Test
    fun dialogDisplaysCorrectMessage() {
        launchFragment<NewGameDialogFragment>(
            fragmentArgs = NewGameDialogFragment.newInstance(0).arguments
        )

        onView(withText(R.string.new_game_dialog_message))
            .check(matches(isDisplayed()))
    }

    @Test
    fun dialogDisplaysOkAndCancelButtons() {
        launchFragment<NewGameDialogFragment>(
            fragmentArgs = NewGameDialogFragment.newInstance(0).arguments
        )

        onView(withText(R.string.new_game_dialog_ok))
            .check(matches(isDisplayed()))
        onView(withText(R.string.new_game_dialog_cancel))
            .check(matches(isDisplayed()))
    }

    @Test
    fun positiveButtonSendsFragmentResultWithGameType() {
        var confirmed = false
        var gameStartType = -1
        val expectedGameType = 1

        val scenario = launchFragment<NewGameDialogFragment>(
            fragmentArgs = NewGameDialogFragment.newInstance(expectedGameType).arguments
        )

        scenario.onFragment { fragment ->
            fragment.parentFragmentManager.setFragmentResultListener(
                NewGameDialogFragment.REQUEST_KEY,
                fragment
            ) { _, bundle ->
                confirmed = bundle.getBoolean(NewGameDialogFragment.RESULT_CONFIRMED)
                gameStartType = bundle.getInt(NewGameDialogFragment.RESULT_GAME_START_TYPE)
            }
        }

        onView(withText(R.string.new_game_dialog_ok)).perform(click())

        // Give time for result to be delivered
        Thread.sleep(100)
        assertTrue("Fragment result should have confirmed=true", confirmed)
        assertEquals("Game start type should match", expectedGameType, gameStartType)
    }
}

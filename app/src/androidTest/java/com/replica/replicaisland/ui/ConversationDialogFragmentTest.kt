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
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.replica.replicaisland.R
import com.replica.replicaisland.levels.LevelTree
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for ConversationDialogFragment.
 */
@RunWith(AndroidJUnit4::class)
class ConversationDialogFragmentTest {

    @Before
    fun setup() {
        // Load level tree if not already loaded
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        if (!LevelTree.isLoaded(R.xml.level_tree)) {
            LevelTree.loadLevelTree(R.xml.level_tree, context)
            LevelTree.loadAllDialog(context)
        }
    }

    @Test
    fun dialogDisplaysTypewriterTextView() {
        // Launch the fragment with valid level data (using first level with dialog)
        // Note: This test assumes level tree has been loaded with dialog resources
        launchFragmentInContainer<ConversationDialogFragment>(
            fragmentArgs = ConversationDialogFragment.newInstance(0, 0, 0, 1).arguments,
            themeResId = R.style.Theme_ConversationDialog
        )

        // Verify the typewriter text view is displayed
        onView(withId(R.id.typewritertext)).check(matches(isDisplayed()))
    }

    @Test
    fun speakerImageIsDisplayed() {
        launchFragmentInContainer<ConversationDialogFragment>(
            fragmentArgs = ConversationDialogFragment.newInstance(0, 0, 0, 1).arguments,
            themeResId = R.style.Theme_ConversationDialog
        )

        // Verify the speaker image is displayed
        onView(withId(R.id.speaker)).check(matches(isDisplayed()))
    }

    @Test
    fun speakerNameIsDisplayed() {
        launchFragmentInContainer<ConversationDialogFragment>(
            fragmentArgs = ConversationDialogFragment.newInstance(0, 0, 0, 1).arguments,
            themeResId = R.style.Theme_ConversationDialog
        )

        // Verify the speaker name is displayed
        onView(withId(R.id.speakername)).check(matches(isDisplayed()))
    }
}

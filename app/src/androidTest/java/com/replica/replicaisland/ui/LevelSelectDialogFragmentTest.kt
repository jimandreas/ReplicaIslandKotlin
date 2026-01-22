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
 * Instrumented tests for LevelSelectDialogFragment.
 */
@RunWith(AndroidJUnit4::class)
class LevelSelectDialogFragmentTest {

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
    fun dialogDisplaysLevelList() {
        // Launch the fragment
        launchFragmentInContainer<LevelSelectDialogFragment>(
            fragmentArgs = LevelSelectDialogFragment.newInstance(unlockAll = false).arguments,
            themeResId = R.style.Theme_FullscreenDialogFragment
        )

        // Verify the level list is displayed
        onView(withId(R.id.level_list)).check(matches(isDisplayed()))
    }

    @Test
    fun dialogWithUnlockAllShowsList() {
        // Launch the fragment with unlock all
        launchFragmentInContainer<LevelSelectDialogFragment>(
            fragmentArgs = LevelSelectDialogFragment.newInstance(unlockAll = true).arguments,
            themeResId = R.style.Theme_FullscreenDialogFragment
        )

        // Verify the level list is displayed
        onView(withId(R.id.level_list)).check(matches(isDisplayed()))
    }
}

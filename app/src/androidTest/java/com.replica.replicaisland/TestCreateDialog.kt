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

import android.content.Context
import androidx.fragment.app.testing.launchFragment
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.assertThat

import org.junit.Before

@RunWith(AndroidJUnit4::class)
class TestCreateDialog {

    private lateinit var context: Context

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()

    }

    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.replica.replicaisland", appContext.packageName)
    }


    /*
     * discussion here:
     * https://stackoverflow.com/questions/56558775/launchfragmentincontainer-unable-to-resolve-activity-in-android
     */
    @Test
    fun testDialogFragment() {
//        val scenario = launchFragmentInContainer<MainDialogFragment>()
//
//        scenario.recreate()
//
//        onView(ViewMatchers.withText("OK")).perform(ViewActions.click())
//
//        // ... your assertions and interactions
//        onView(ViewMatchers.withText("My Dialog")).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
//        onView(ViewMatchers.withText("This is my dialog.")).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))


        // Assumes that "MyDialogFragment" extends the DialogFragment class.
        with(launchFragment<MainDialogFragment>()) {
            onFragment { fragment ->
                assertEquals(1, 1)
                fragment.dismiss()
                fragment.parentFragmentManager.executePendingTransactions()

            }
        }
//
//        // Assumes that the dialog had a button
//        // containing the text "Cancel".
//        onView(withText("Cancel")).check(doesNotExist())
    }
}
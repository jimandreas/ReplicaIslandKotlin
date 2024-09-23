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
import androidx.test.espresso.IdlingPolicies
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.matcher.ViewMatchers.assertThat
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.filters.FlakyTest
import androidx.test.filters.LargeTest
import org.junit.After

import org.junit.Before
import java.util.concurrent.TimeUnit


/*

I am trying to debug an AndroidTest that posts a dialog using launchFragment().
If I set a breakpoint after the dialog fragment is posted, something times out
and the test fails with an exception. If I let it run the test passes.
What is timing out -is it the Android OS, or espresso, or the junit framework?
How to I fix this error so I can step through the test with the debugger?

Advice from Gemini about test timeouts:

You've stumbled upon a common challenge when debugging Android tests
involving UI elements like dialogs. The issue is likely related to
how Espresso synchronizes with the UI thread and handles idling resources.
Here's a breakdown of what might be happening and how to address it:
Espresso Idling Resources: Espresso has a mechanism called
"idling resources" to ensure that your test code doesn't execute
until the UI is idle and ready for interaction. When you set a
breakpoint and pause execution, Espresso might incorrectly assume
that the UI is still busy and eventually time out.
JUnit Timeouts: JUnit itself also has timeout mechanisms for tests.
If your test takes too long to complete due to the breakpoint,
it might exceed the configured timeout and fail. How to fix this:
Register Idling Resources (if applicable): If your DialogFragment
involves background tasks or asynchronous operations, you might need
to register an idling resource with Espresso. This tells Espresso to
wait until the background work is complete before proceeding with the test.

Disable Animations: Animations can sometimes interfere with Espresso's
synchronization. Consider disabling animations on the device or emulator while debugging.
By addressing these potential timeout issues, you should be able to
debug your Android tests involving dialogs more effectively.
Remember to remove or adjust any temporary workarounds like Thread.sleep()
once you're done debugging. If you can share the relevant parts of your
test code and DialogFragment, I might be able to provide more specific guidance
 */

@RunWith(AndroidJUnit4::class)
@LargeTest
@FlakyTest(detail = "does this affect timout?")
class TestCreateDialog {

    private lateinit var context: Context
    private val idlingResource = DialogIdlingResource()

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
        IdlingPolicies.setIdlingResourceTimeout(1, TimeUnit.HOURS)
        IdlingPolicies.setMasterPolicyTimeout(1, TimeUnit.HOURS)

        IdlingRegistry.getInstance().register(idlingResource)
        idlingResource.setIdleState(false)

    }

    @After
    fun tearDown() {
        IdlingRegistry.getInstance().unregister(idlingResource)
    }

    @Test(timeout = 100000) // Set a 100-second timeout
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.replica.replicaisland", appContext.packageName)
    }

    // https://developer.android.com/guide/fragments/test

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
        // Assumes that the dialog had a button
        // containing the text "Cancel".
        onView(withText("OK")).check(doesNotExist())
        assertEquals(1,1)

        idlingResource.setIdleState(true)
//
//        // Assumes that the dialog had a button
//        // containing the text "Cancel".
//        onView(withText("Cancel")).check(doesNotExist())
    }
    // ?? Got obituary of 10891:com.replica.replicaislands
}

class DialogIdlingResource : IdlingResource {
    private var isIdle = false
    private var resourceCallback: IdlingResource.ResourceCallback? = null

    override fun getName(): String {
        return "DialogIdlingResource"
    }

    override fun isIdleNow(): Boolean {
        return isIdle
    }

    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback) {
        this.resourceCallback = callback
    }

    fun setIdleState(idle: Boolean) {
        isIdle = idle
        if (isIdle && resourceCallback != null) {
            resourceCallback!!.onTransitionToIdle()
        }
    }
}
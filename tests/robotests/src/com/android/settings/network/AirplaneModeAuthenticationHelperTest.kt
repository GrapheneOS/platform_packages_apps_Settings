/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.settings.network

import android.app.Application
import android.app.KeyguardManager
import android.hardware.biometrics.BiometricPrompt
import android.os.CancellationSignal
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executor
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class AirplaneModeAuthenticationHelperTest {
    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val keyguardManager = mock<KeyguardManager>()
    private val executor = Executor { it.run() }
    private lateinit var helper: AirplaneModeAuthenticationHelper
    private var callback: BiometricPrompt.AuthenticationCallback? = null
    private var promptCount = 0

    @Before
    fun setUp() {
        helper = AirplaneModeAuthenticationHelper(context, keyguardManager, executor)
        helper.showPrompt =
            { _: CancellationSignal,
              _: Executor,
              authenticationCallback: BiometricPrompt.AuthenticationCallback ->
                promptCount++
                callback = authenticationCallback
            }
    }

    @Test
    fun runAfterAuthentication_noSecureLock_runsImmediately() {
        whenever(keyguardManager.isDeviceSecure).thenReturn(false)
        var actionRan = false

        helper.runAfterAuthentication { actionRan = true }

        assertThat(actionRan).isTrue()
        assertThat(promptCount).isEqualTo(0)
    }

    @Test
    fun runAfterAuthentication_secureLock_waitsForSuccessfulSystemAuthentication() {
        whenever(keyguardManager.isDeviceSecure).thenReturn(true)
        var actionRan = false

        helper.runAfterAuthentication { actionRan = true }

        assertThat(actionRan).isFalse()
        assertThat(promptCount).isEqualTo(1)

        callback!!.onAuthenticationSucceeded(mock())

        assertThat(actionRan).isTrue()
    }

    @Test
    fun runAfterAuthentication_errorDoesNotRunAction() {
        whenever(keyguardManager.isDeviceSecure).thenReturn(true)
        var actionRan = false

        helper.runAfterAuthentication { actionRan = true }
        callback!!.onAuthenticationError(1, "cancelled")

        assertThat(actionRan).isFalse()
    }

    @Test
    fun runAfterAuthentication_promptAlreadyShowing_ignoresSecondRequest() {
        whenever(keyguardManager.isDeviceSecure).thenReturn(true)
        var firstActionRan = false
        var secondActionRan = false

        helper.runAfterAuthentication { firstActionRan = true }
        helper.runAfterAuthentication { secondActionRan = true }
        callback!!.onAuthenticationSucceeded(mock())

        assertThat(promptCount).isEqualTo(1)
        assertThat(firstActionRan).isTrue()
        assertThat(secondActionRan).isFalse()
    }

    @Test
    fun cancel_cancelsActiveSystemPrompt() {
        whenever(keyguardManager.isDeviceSecure).thenReturn(true)
        val cancellationSignal = mock<CancellationSignal>()
        helper.cancellationSignalFactory = { cancellationSignal }

        helper.runAfterAuthentication {}
        helper.cancel()

        verify(cancellationSignal).cancel()
    }

    @Test
    fun cancel_withoutActivePrompt_doesNothing() {
        whenever(keyguardManager.isDeviceSecure).thenReturn(true)
        val cancellationSignal = mock<CancellationSignal>()
        helper.cancellationSignalFactory = { cancellationSignal }

        helper.cancel()

        verify(cancellationSignal, never()).cancel()
    }
}

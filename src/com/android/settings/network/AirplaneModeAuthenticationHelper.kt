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

import android.app.KeyguardManager
import android.content.Context
import android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_STRONG
import android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import android.hardware.biometrics.BiometricPrompt
import android.os.CancellationSignal
import androidx.annotation.VisibleForTesting
import com.android.settings.R
import java.util.concurrent.Executor

/** Runs an action only after a fresh system biometric or device-credential authentication. */
open class AirplaneModeAuthenticationHelper
@VisibleForTesting
internal constructor(
    private val context: Context,
    private val keyguardManager: KeyguardManager,
    private val executor: Executor,
) {
    constructor(context: Context) :
        this(
            context,
            context.getSystemService(KeyguardManager::class.java),
            context.mainExecutor,
        )

    private val lock = Any()
    private var activeCancellationSignal: CancellationSignal? = null

    @VisibleForTesting
    internal var cancellationSignalFactory: () -> CancellationSignal = { CancellationSignal() }

    @VisibleForTesting
    internal var showPrompt:
        (CancellationSignal, Executor, BiometricPrompt.AuthenticationCallback) -> Unit =
        { cancellationSignal, callbackExecutor, callback ->
            BiometricPrompt.Builder(context)
                .setTitle(context.getString(R.string.airplane_mode_authentication_title))
                .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
                .setConfirmationRequired(true)
                .build()
                .authenticate(cancellationSignal, callbackExecutor, callback)
        }

    open fun runAfterAuthentication(action: Runnable) {
        if (!keyguardManager.isDeviceSecure) {
            action.run()
            return
        }

        val cancellationSignal =
            synchronized(lock) {
                if (activeCancellationSignal != null) return
                cancellationSignalFactory().also { activeCancellationSignal = it }
            }
        val callback =
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    finish(cancellationSignal, action)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    finish(cancellationSignal, null)
                }
            }

        try {
            showPrompt(cancellationSignal, executor, callback)
        } catch (_: RuntimeException) {
            finish(cancellationSignal, null)
        }
    }

    open fun cancel() {
        val cancellationSignal = synchronized(lock) { activeCancellationSignal }
        cancellationSignal?.cancel()
        finish(cancellationSignal, null)
    }

    private fun finish(cancellationSignal: CancellationSignal?, action: Runnable?) {
        val shouldRun =
            synchronized(lock) {
                if (activeCancellationSignal !== cancellationSignal) return
                activeCancellationSignal = null
                action != null
            }
        if (shouldRun) action!!.run()
    }
}

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
import android.ext.settings.ExtSettings
import androidx.preference.Preference
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.ReadWritePermit
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class AirplaneModeAuthenticationPreferenceTest {
    private val context = ApplicationProvider.getApplicationContext<Application>()
    private lateinit var preference: AirplaneModeAuthenticationPreference

    @Before
    fun setUp() {
        ExtSettings.REQUIRE_AUTHENTICATION_TO_DISABLE_AIRPLANE_MODE.put(context, false)
        preference = AirplaneModeAuthenticationPreference()
    }

    @After
    fun tearDown() {
        ExtSettings.REQUIRE_AUTHENTICATION_TO_DISABLE_AIRPLANE_MODE.put(context, false)
    }

    @Test
    fun isAvailable_secureLockConfigured_returnsTrue() {
        preference.isDeviceSecure = { true }

        assertThat(preference.isAvailable(context)).isTrue()
    }

    @Test
    fun isAvailable_noSecureLock_returnsFalse() {
        preference.isDeviceSecure = { false }

        assertThat(preference.isAvailable(context)).isFalse()
    }

    @Test
    fun storage_writesProtectedGlobalSetting() {
        preference.storage(context).setBoolean(AirplaneModeAuthenticationPreference.KEY, true)

        assertThat(ExtSettings.REQUIRE_AUTHENTICATION_TO_DISABLE_AIRPLANE_MODE.get(context)).isTrue()
    }

    @Test
    fun metadata_hasExpectedSummaryAndReadPermit() {
        assertThat(preference.getSummary(context))
            .isEqualTo(context.getText(R.string.airplane_mode_authentication_summary))
        assertThat(preference.getReadPermit(context, 0, 0)).isEqualTo(ReadWritePermit.ALLOW)
    }

    @Test
    fun getWritePermit_requirementEnabled_blocksExternalDisable() {
        preference.isAuthenticationRequired = { true }

        val permit = preference.getWritePermit(context, false, 0, 0)

        assertThat(permit).isEqualTo(ReadWritePermit.DISALLOW)
    }

    @Test
    fun getWritePermit_requirementEnabled_blocksUnknownValue() {
        preference.isAuthenticationRequired = { true }

        val permit = preference.getWritePermit(context, null, 0, 0)

        assertThat(permit).isEqualTo(ReadWritePermit.DISALLOW)
    }

    @Test
    fun getWritePermit_requirementEnabled_allowsExternalEnable() {
        preference.isAuthenticationRequired = { true }

        val permit = preference.getWritePermit(context, true, 0, 0)

        assertThat(permit).isEqualTo(ReadWritePermit.ALLOW)
    }

    @Test
    fun onCreate_disabling_waitsForAuthentication() {
        val widget = mock<Preference>()
        val lifecycleContext =
            mock<PreferenceLifecycleContext> {
                on { requirePreference<Preference>(AirplaneModeAuthenticationPreference.KEY) }
                    .thenReturn(widget)
            }
        val authenticationHelper = mock<AirplaneModeAuthenticationHelper>()
        preference.authenticationHelper = authenticationHelper
        preference.isAuthenticationRequired = { true }

        preference.onCreate(lifecycleContext)
        val listener = argumentCaptor<Preference.OnPreferenceChangeListener>()
        verify(widget).onPreferenceChangeListener = listener.capture()
        val accepted = listener.lastValue.onPreferenceChange(widget, false)

        assertThat(accepted).isFalse()
        verify(authenticationHelper).runAfterAuthentication(any())
    }

    @Test
    fun onCreate_disablingAfterAuthentication_rechecksAndWritesSetting() {
        ExtSettings.REQUIRE_AUTHENTICATION_TO_DISABLE_AIRPLANE_MODE.put(context, true)
        val widget = mock<Preference>()
        val lifecycleContext =
            mock<PreferenceLifecycleContext> {
                on { requirePreference<Preference>(AirplaneModeAuthenticationPreference.KEY) }
                    .thenReturn(widget)
                on { applicationContext }.thenReturn(context)
                on { contentResolver }.thenReturn(context.contentResolver)
            }
        val authenticationHelper = mock<AirplaneModeAuthenticationHelper>()
        doAnswer { invocation ->
                invocation.getArgument<Runnable>(0).run()
                null
            }
            .`when`(authenticationHelper)
            .runAfterAuthentication(any())
        preference.authenticationHelper = authenticationHelper

        preference.onCreate(lifecycleContext)
        val listener = argumentCaptor<Preference.OnPreferenceChangeListener>()
        verify(widget).onPreferenceChangeListener = listener.capture()
        val accepted = listener.lastValue.onPreferenceChange(widget, false)

        assertThat(accepted).isFalse()
        assertThat(ExtSettings.REQUIRE_AUTHENTICATION_TO_DISABLE_AIRPLANE_MODE.get(context)).isFalse()
    }

    @Test
    fun onDestroy_cancelsAuthentication() {
        val authenticationHelper = mock<AirplaneModeAuthenticationHelper>()
        preference.authenticationHelper = authenticationHelper

        preference.onDestroy(mock())

        verify(authenticationHelper).cancel()
        assertThat(preference.authenticationHelper).isNull()
    }
}

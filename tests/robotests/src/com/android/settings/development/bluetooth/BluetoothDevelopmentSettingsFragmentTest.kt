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

package com.android.settings.development.bluetooth

import android.app.Application
import android.os.SystemProperties
import androidx.fragment.app.testing.FragmentScenario
import androidx.preference.Preference
import androidx.test.core.app.ApplicationProvider
import com.android.settings.development.BluetoothA2dpHwOffloadPreferenceController
import com.android.settingslib.development.DevelopmentSettingsEnabler
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BluetoothDevelopmentSettingsFragmentTest {
    private val context = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun searchIndexProvider_isPageSearchEnabled_returnsTrueWhenDevelopmentSettingsEnabled() {
        DevelopmentSettingsEnabler.setDevelopmentSettingsEnabled(context, true)

        val nonIndexableKeys =
            BluetoothDevelopmentSettingsFragment.SEARCH_INDEX_DATA_PROVIDER.getNonIndexableKeys(
                context
            )

        // The entire screen shouldn't be hidden by default just based on the master toggle here
        assertThat(nonIndexableKeys).doesNotContain("bluetooth_development_settings_screen")
    }

    @Test
    fun searchIndexProvider_isPageSearchEnabled_returnsFalseWhenDevelopmentSettingsDisabled() {
        DevelopmentSettingsEnabler.setDevelopmentSettingsEnabled(context, false)

        val nonIndexableKeys =
            BluetoothDevelopmentSettingsFragment.SEARCH_INDEX_DATA_PROVIDER.getNonIndexableKeys(
                context
            )

        // The entire screen should be hidden
        assertThat(nonIndexableKeys).contains("bluetooth_development_settings_screen")
    }

    @Test
    fun searchIndexProvider_createPreferenceControllers_returnsNonEmptyList() {
        val controllers =
            BluetoothDevelopmentSettingsFragment.SEARCH_INDEX_DATA_PROVIDER
                .createPreferenceControllers(context)
        assertThat(controllers).isNotEmpty()
    }

    @Test
    fun onRebootDialogConfirmed_updatesA2dpOffloadProperty() {
        val property = "persist.bluetooth.a2dp_offload.disabled"
        val leAudioProperty = "persist.bluetooth.leaudio_offload.disabled"
        SystemProperties.set(property, "false")
        SystemProperties.set(leAudioProperty, "false")
        try {
            FragmentScenario.launch(BluetoothDevelopmentSettingsFragment::class.java).onFragment {
                attachedFragment ->
                val controller =
                    attachedFragment.use(BluetoothA2dpHwOffloadPreferenceController::class.java)
                controller?.onPreferenceChange(mock(Preference::class.java), true)
                attachedFragment.onRebootDialogConfirmed()

                assertThat(SystemProperties.getBoolean(property, false)).isTrue()
            }
        } finally {
            SystemProperties.set(property, "false")
            SystemProperties.set(leAudioProperty, "false")
        }
    }
}

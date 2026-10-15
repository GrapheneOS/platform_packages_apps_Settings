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

package com.android.settings.development;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.provider.Settings;

import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class ForceScreenshotSecureWindowsPreferenceControllerTest {

    @Mock
    private SwitchPreferenceCompat mPreference;
    @Mock
    private PreferenceScreen mScreen;

    private Context mContext;
    private ForceScreenshotSecureWindowsPreferenceController mController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = new ForceScreenshotSecureWindowsPreferenceController(mContext);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        mController.displayPreference(mScreen);
    }

    @Test
    public void onPreferenceChange_settingEnabled_shouldSetSettingToOne() {
        mController.onPreferenceChange(mPreference, true);

        assertThat(Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.FORCE_SCREENSHOT_SECURE_WINDOWS, 0)).isEqualTo(1);
    }

    @Test
    public void onPreferenceChange_settingDisabled_shouldSetSettingToZero() {
        mController.onPreferenceChange(mPreference, false);

        assertThat(Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.FORCE_SCREENSHOT_SECURE_WINDOWS, 0)).isEqualTo(0);
    }

    @Test
    public void updateState_settingEnabled_shouldCheckPreference() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.FORCE_SCREENSHOT_SECURE_WINDOWS, 1);

        mController.updateState(mPreference);

        verify(mPreference).setChecked(true);
    }

    @Test
    public void updateState_settingDisabled_shouldUncheckPreference() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.FORCE_SCREENSHOT_SECURE_WINDOWS, 0);

        mController.updateState(mPreference);

        verify(mPreference).setChecked(false);
    }

    @Test
    public void onDeveloperOptionsSwitchDisabled_shouldResetSettingAndUncheck() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.FORCE_SCREENSHOT_SECURE_WINDOWS, 1);

        mController.onDeveloperOptionsSwitchDisabled();

        assertThat(Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.FORCE_SCREENSHOT_SECURE_WINDOWS, 1)).isEqualTo(0);
        verify(mPreference).setChecked(false);
    }
}

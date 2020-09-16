/*
 * Copyright (C) 2020 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.security;

import android.content.Context;

import android.os.UserHandle;
import android.os.UserManager;
import android.os.SystemProperties;

import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;
import androidx.preference.SwitchPreference;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.events.OnResume;

public class KeyguardCameraPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, OnResume, Preference.OnPreferenceChangeListener {

    private static final String SYS_KEY_KEYGUARD_CAMERA = "persist.keyguard.camera";
    private static final String PREF_KEY_KEYGUARD_CAMERA = "keyguard_camera";
    private static final String PREF_KEY_SECURITY_CATEGORY = "security_category";

    private PreferenceCategory mSecurityCategory;
    private SwitchPreference mKeyguardCamera;

    public KeyguardCameraPreferenceController(Context context) {
        super(context);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mSecurityCategory = screen.findPreference(PREF_KEY_SECURITY_CATEGORY);
        updatePreferenceState();
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return PREF_KEY_KEYGUARD_CAMERA;
    }

    // TODO: should we use onCreatePreferences() instead?
    private void updatePreferenceState() {
        if (mSecurityCategory == null) {
            return;
        }
        mKeyguardCamera = (SwitchPreference) mSecurityCategory.findPreference(PREF_KEY_KEYGUARD_CAMERA);
        mKeyguardCamera.setChecked(SystemProperties.getBoolean(SYS_KEY_KEYGUARD_CAMERA, true));
    }

    @Override
    public void onResume() {
        updatePreferenceState();
        if (mKeyguardCamera != null) {
            boolean mode = mKeyguardCamera.isChecked();
            SystemProperties.set(SYS_KEY_KEYGUARD_CAMERA, Boolean.toString(mode));
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        final String key = preference.getKey();
        if (PREF_KEY_KEYGUARD_CAMERA.equals(key)) {
            final boolean mode = !mKeyguardCamera.isChecked();
            SystemProperties.set(SYS_KEY_KEYGUARD_CAMERA, Boolean.toString(mode));
        }
        return true;
    }
}

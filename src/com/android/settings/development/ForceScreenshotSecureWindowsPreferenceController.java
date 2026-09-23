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

import android.content.Context;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class ForceScreenshotSecureWindowsPreferenceController
        extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    private static final String KEY = "force_screenshot_secure_windows";

    public ForceScreenshotSecureWindowsPreferenceController(Context context) {
        super(context);
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.FORCE_SCREENSHOT_SECURE_WINDOWS,
                (Boolean) newValue ? 1 : 0);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        final int value = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.FORCE_SCREENSHOT_SECURE_WINDOWS, 0);
        ((TwoStatePreference) preference).setChecked(value != 0);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.FORCE_SCREENSHOT_SECURE_WINDOWS, 0);
        ((TwoStatePreference) mPreference).setChecked(false);
    }
}

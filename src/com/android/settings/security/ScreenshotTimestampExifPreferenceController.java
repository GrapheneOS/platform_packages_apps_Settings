/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.security;

import android.content.Context;

import android.os.UserHandle;

import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;
import androidx.preference.SwitchPreference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.events.OnResume;

public class ScreenshotTimestampExifPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, OnResume, Preference.OnPreferenceChangeListener {

    private static final String PREF_KEY_SCREENSHOT_TIMESTAMP_EXIF = "screenshot_timestamp_exif";
    private static final String PREF_KEY_SECURITY_CATEGORY = "security_category";

    private PreferenceCategory mSecurityCategory;
    private SwitchPreference mDisableScreenshotTimestampExif;

    public ScreenshotTimestampExifPreferenceController(Context context) {
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
        return PREF_KEY_SCREENSHOT_TIMESTAMP_EXIF;
    }

    private void updatePreferenceState() {
        if (mSecurityCategory == null) {
            return;
        }
        mDisableScreenshotTimestampExif = (SwitchPreference) mSecurityCategory.findPreference(PREF_KEY_SCREENSHOT_TIMESTAMP_EXIF);
        mDisableScreenshotTimestampExif.setChecked(Settings.Secure.getInt(mContext.getContentResolver(), Settings.Secure.SCREENSHOT_TIMESTAMP_EXIF, 1) != 0);
    }

    @Override
    public void onResume() {
        updatePreferenceState();
        if (mDisableScreenshotTimestampExif != null) {
            boolean mode = mDisableScreenshotTimestampExif.isChecked();
            Settings.Secure.putInt(mContext.getContentResolver(), Settings.Secure.SCREENSHOT_TIMESTAMP_EXIF, (mode) ? 0 : 1);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        final String key = preference.getKey();
        if (PREF_KEY_SCREENSHOT_TIMESTAMP_EXIF.equals(key)) {
            final boolean mode = !mDisableScreenshotTimestampExif.isChecked();
            Settings.Secure.putInt(mContext.getContentResolver(), Settings.Secure.SCREENSHOT_TIMESTAMP_EXIF, (mode) ? 1 : 0);
        }
        return true;
    }
}
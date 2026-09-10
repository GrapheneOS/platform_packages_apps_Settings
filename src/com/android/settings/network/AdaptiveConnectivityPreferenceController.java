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
 * limitations under the License.
 */

package com.android.settings.network;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;

import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.BasePreferenceController;

/**
 * {@link BasePreferenceController} that shows Adaptive connectivity on/off state.
 */
public class AdaptiveConnectivityPreferenceController extends BasePreferenceController {

    public AdaptiveConnectivityPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
    }

    @Override
    public int getAvailabilityStatus() {
        if (!Utils.isMobileDataCapable(mContext)) {
            return UNSUPPORTED_ON_DEVICE;
        }
        return AVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        final ContentResolver resolver = mContext.getContentResolver();
        boolean isEnabled = Settings.Secure.getInt(resolver,
            Settings.Secure.ADAPTIVE_CONNECTIVITY_ENABLED, 1) == 1;

        final int wifiSetting = Settings.Secure.getInt(resolver,
            Settings.Secure.ADAPTIVE_CONNECTIVITY_WIFI_ENABLED, -1);
        final int mobileSetting = Settings.Secure.getInt(resolver,
            Settings.Secure.ADAPTIVE_CONNECTIVITY_MOBILE_NETWORK_ENABLED, -1);

        if (wifiSetting != -1 || mobileSetting != -1) {
            isEnabled = (wifiSetting == 1) || (mobileSetting == 1);
        }

        return mContext.getString(isEnabled
            ? R.string.adaptive_connectivity_switch_on
            : R.string.adaptive_connectivity_switch_off);
    }
}

/*
 * Copyright (C) 2017 The Android Open Source Project
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

import static android.provider.Settings.Global.CAPTIVE_PORTAL_HTTP_URL;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.net.LinkProperties;
import android.net.Network;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.ListPreference;

import com.android.internal.util.ArrayUtils;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.RestrictedLockUtilsInternal;

import com.android.settingslib.core.lifecycle.events.OnResume;

public class ConnectivityCheckPreferenceController extends BasePreferenceController
        implements PreferenceControllerMixin, Preference.OnPreferenceChangeListener, OnResume {

    private static final String GRAPHENEOS_CAPTIVE_PORTAL_HTTP_URL =
            "http://connectivitycheck.grapheneos.org/generate_204";
    private static final String STANDARD_CAPTIVE_PORTAL_HTTP_URL = 
            "http://connectivitycheck.gstatic.com/generate_204";

    private static final int GRAPHENEOS_CAPTIVE_PORTAL_HTTP_URL_INTVAL = 0;
    private static final int STANDARD_CAPTIVE_PORTAL_HTTP_URL_INTVAL = 1;

    private static final String KEY_CONNECTIVITY_CHECK_SETTINGS = "connectivity_check_settings";

    private ListPreference mConnectivityPreference;

    public ConnectivityCheckPreferenceController(Context context) {
        super(context, KEY_CONNECTIVITY_CHECK_SETTINGS);
    }

    @Override
    public int getAvailabilityStatus() {
        return BasePreferenceController.AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mConnectivityPreference = screen.findPreference(KEY_CONNECTIVITY_CHECK_SETTINGS);
        updatePreferenceState();
    }

    @Override
    public String getPreferenceKey() {
        return KEY_CONNECTIVITY_CHECK_SETTINGS;
    }

    private void updatePreferenceState() {
        mConnectivityPreference.setValueIndex(connectivityCheckURLStringToValueIndex(
                Settings.Global.getString(mContext.getContentResolver(),
                Settings.Global.CAPTIVE_PORTAL_HTTP_URL)));
    }

    @Override
    public void onResume() {
        updatePreferenceState();
        if (mConnectivityPreference != null) {
            int mode = Integer.parseInt(mConnectivityPreference.getValue());
            Settings.Global.putString(mContext.getContentResolver(), Settings.Global.CAPTIVE_PORTAL_HTTP_URL,
                    connectivityCheckOptionValToURLString(mode));
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        final String key = preference.getKey();
        if (KEY_CONNECTIVITY_CHECK_SETTINGS.equals(key)) {
            int mode = Integer.parseInt((String) value);
            Settings.Global.putString(mContext.getContentResolver(), Settings.Global.CAPTIVE_PORTAL_HTTP_URL,
                    connectivityCheckOptionValToURLString(mode));
            return true;
        } else {
            return false;
        }
    }

    private String connectivityCheckOptionValToURLString(int option) {
        switch(option) {
            case GRAPHENEOS_CAPTIVE_PORTAL_HTTP_URL_INTVAL:
                return GRAPHENEOS_CAPTIVE_PORTAL_HTTP_URL;
            case STANDARD_CAPTIVE_PORTAL_HTTP_URL_INTVAL:
                return STANDARD_CAPTIVE_PORTAL_HTTP_URL;
            default:
                return STANDARD_CAPTIVE_PORTAL_HTTP_URL;
        }
    }

    private int connectivityCheckURLStringToValueIndex(String option) {
        switch(option) {
            case GRAPHENEOS_CAPTIVE_PORTAL_HTTP_URL:
                return GRAPHENEOS_CAPTIVE_PORTAL_HTTP_URL_INTVAL;
            case STANDARD_CAPTIVE_PORTAL_HTTP_URL:
                return STANDARD_CAPTIVE_PORTAL_HTTP_URL_INTVAL;
            default:
                return STANDARD_CAPTIVE_PORTAL_HTTP_URL_INTVAL;
        }
    }
}

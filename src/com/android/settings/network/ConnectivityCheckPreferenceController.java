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
import android.content.res.Resources;
import android.database.ContentObserver;
import android.net.LinkProperties;
import android.net.Network;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import com.android.internal.util.ArrayUtils;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.core.lifecycle.events.OnResume;

public class ConnectivityCheckPreferenceController
        extends BasePreferenceController
        implements PreferenceControllerMixin, Preference.OnPreferenceChangeListener,
        OnResume {

    private static final String GRAPHENEOS_CAPTIVE_PORTAL_HTTPS_URL =
            "https://connectivitycheck.grapheneos.network/generate_204";
    private static final String GRAPHENEOS_CAPTIVE_PORTAL_HTTP_URL =
            "http://connectivitycheck.grapheneos.network/generate_204";
    private static final String GRAPHENEOS_CAPTIVE_PORTAL_FALLBACK_URL =
            "http://grapheneos.online/gen_204";
    private static final String GRAPHENEOS_CAPTIVE_PORTAL_OTHER_FALLBACK_URL =
            "http://grapheneos.online/generate_204";

    // imported defaults from AOSP NetworkStack
    private static final String STANDARD_HTTPS_URL =
            "https://www.google.com/generate_204";
    private static final String STANDARD_HTTP_URL =
            "http://connectivitycheck.gstatic.com/generate_204";
    private static final String STANDARD_FALLBACK_URL =
            "http://www.google.com/gen_204";
    private static final String STANDARD_OTHER_FALLBACK_URLS =
            "http://play.googleapis.com/generate_204";

    private static final int GRAPHENEOS_CAPTIVE_PORTAL_HTTP_URL_INTVAL = 0;
    private static final int STANDARD_CAPTIVE_PORTAL_HTTP_URL_INTVAL = 1;
    private static final int DISABLED_CAPTIVE_PORTAL_INTVAL = 2;

    private static final String KEY_CONNECTIVITY_CHECK_SETTINGS =
            "connectivity_check_settings";

    private ListPreference mConnectivityPreference;

    public ConnectivityCheckPreferenceController(Context context) {
        super(context, KEY_CONNECTIVITY_CHECK_SETTINGS);
    }

    @Override
    public int getAvailabilityStatus() {
        if (isDisabledByAdmin()) {
            return BasePreferenceController.DISABLED_FOR_USER;
        }
        return BasePreferenceController.AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        ListPreference captiveList = new ListPreference(screen.getContext());
        captiveList.setKey(KEY_CONNECTIVITY_CHECK_SETTINGS);
        captiveList.setOrder(30);
        captiveList.setIcon(R.drawable.ic_settings_language);
        captiveList.setTitle(R.string.connectivity_check_title);
        captiveList.setSummary(R.string.connectivity_check_summary);
        captiveList.setEntries(R.array.connectivity_check_entries);
        captiveList.setEntryValues(R.array.connectivity_check_values);

        if (mConnectivityPreference == null) {
            screen.addPreference(captiveList);
            mConnectivityPreference = captiveList;
        }
        super.displayPreference(screen);
        updatePreferenceState();
    }

    @Override
    public String getPreferenceKey() {
        return KEY_CONNECTIVITY_CHECK_SETTINGS;
    }

    private void updatePreferenceState() {
        if (Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.CAPTIVE_PORTAL_MODE, Settings.Global.CAPTIVE_PORTAL_MODE_PROMPT)
                == Settings.Global.CAPTIVE_PORTAL_MODE_IGNORE) {
            mConnectivityPreference.setValueIndex(DISABLED_CAPTIVE_PORTAL_INTVAL);
            return;
        }

        String pref = Settings.Global.getString(
                mContext.getContentResolver(), Settings.Global.CAPTIVE_PORTAL_HTTP_URL);
        if (STANDARD_HTTP_URL.equals(pref)) {
            mConnectivityPreference.setValueIndex(
                    STANDARD_CAPTIVE_PORTAL_HTTP_URL_INTVAL);
        } else {
            mConnectivityPreference.setValueIndex(
                    GRAPHENEOS_CAPTIVE_PORTAL_HTTP_URL_INTVAL);
        }
    }

    @Override
    public void onResume() {
        updatePreferenceState();
        if (mConnectivityPreference != null) {
            setCaptivePortalURLs(
                    mContext.getContentResolver(),
                    Integer.parseInt(mConnectivityPreference.getValue()));
        }
    }

    private void setCaptivePortalURLs(ContentResolver cr, int mode) {
        switch (mode) {
            case STANDARD_CAPTIVE_PORTAL_HTTP_URL_INTVAL:
                Settings.Global.putString(cr, Settings.Global.CAPTIVE_PORTAL_HTTP_URL,
                        STANDARD_HTTP_URL);
                Settings.Global.putString(cr, Settings.Global.CAPTIVE_PORTAL_HTTPS_URL,
                        STANDARD_HTTPS_URL);
                Settings.Global.putString(cr, Settings.Global.CAPTIVE_PORTAL_FALLBACK_URL,
                        STANDARD_FALLBACK_URL);
                Settings.Global.putString(
                        cr, Settings.Global.CAPTIVE_PORTAL_OTHER_FALLBACK_URLS,
                        STANDARD_OTHER_FALLBACK_URLS);
                Settings.Global.putInt(cr, Settings.Global.CAPTIVE_PORTAL_MODE,
                        Settings.Global.CAPTIVE_PORTAL_MODE_PROMPT);
                break;
            case GRAPHENEOS_CAPTIVE_PORTAL_HTTP_URL_INTVAL:
                Settings.Global.putString(cr, Settings.Global.CAPTIVE_PORTAL_HTTP_URL,
                        GRAPHENEOS_CAPTIVE_PORTAL_HTTP_URL);
                Settings.Global.putString(cr, Settings.Global.CAPTIVE_PORTAL_HTTPS_URL,
                        GRAPHENEOS_CAPTIVE_PORTAL_HTTPS_URL);
                Settings.Global.putString(cr, Settings.Global.CAPTIVE_PORTAL_FALLBACK_URL,
                        GRAPHENEOS_CAPTIVE_PORTAL_FALLBACK_URL);
                Settings.Global.putString(
                        cr, Settings.Global.CAPTIVE_PORTAL_OTHER_FALLBACK_URLS,
                        GRAPHENEOS_CAPTIVE_PORTAL_OTHER_FALLBACK_URL);
                Settings.Global.putInt(cr, Settings.Global.CAPTIVE_PORTAL_MODE,
                        Settings.Global.CAPTIVE_PORTAL_MODE_PROMPT);
                break;
            default:
                // GrapheneOS URLs as placeholder
                Settings.Global.putString(cr, Settings.Global.CAPTIVE_PORTAL_HTTP_URL,
                        GRAPHENEOS_CAPTIVE_PORTAL_HTTP_URL);
                Settings.Global.putString(cr, Settings.Global.CAPTIVE_PORTAL_HTTPS_URL,
                        GRAPHENEOS_CAPTIVE_PORTAL_HTTPS_URL);
                Settings.Global.putString(cr, Settings.Global.CAPTIVE_PORTAL_FALLBACK_URL,
                        GRAPHENEOS_CAPTIVE_PORTAL_FALLBACK_URL);
                Settings.Global.putString(
                        cr, Settings.Global.CAPTIVE_PORTAL_OTHER_FALLBACK_URLS,
                        GRAPHENEOS_CAPTIVE_PORTAL_OTHER_FALLBACK_URL);
                Settings.Global.putInt(cr, Settings.Global.CAPTIVE_PORTAL_MODE,
                        Settings.Global.CAPTIVE_PORTAL_MODE_IGNORE);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        final String key = preference.getKey();
        if (KEY_CONNECTIVITY_CHECK_SETTINGS.equals(key)) {
            setCaptivePortalURLs(mContext.getContentResolver(),
                    Integer.parseInt((String)value));
            return true;
        } else {
            return false;
        }
    }

    private EnforcedAdmin getEnforcedAdmin() {
        return RestrictedLockUtilsInternal.checkIfRestrictionEnforced(
                mContext, UserManager.DISALLOW_CONFIG_PRIVATE_DNS,
                UserHandle.myUserId());
    }

    private boolean isDisabledByAdmin() { return getEnforcedAdmin() != null; }
}

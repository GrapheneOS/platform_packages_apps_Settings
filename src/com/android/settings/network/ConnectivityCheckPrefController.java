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
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import android.ext.settings.ExtSettings;
import android.ext.settings.ConnectivityCheckConstants;

import com.android.settings.R;
import com.android.settings.ext.IntSettingPrefController;
import com.android.settingslib.RestrictedLockUtilsInternal;

import static android.ext.settings.ConnectivityCheckConstants.GRAPHENEOS_CAPTIVE_PORTAL_HTTP_URL_INTVAL;
import static android.ext.settings.ConnectivityCheckConstants.STANDARD_CAPTIVE_PORTAL_HTTP_URL_INTVAL;
import static android.ext.settings.ConnectivityCheckConstants.DISABLED_CAPTIVE_PORTAL_INTVAL;

import static android.ext.settings.ConnectivityCheckConstants.GRAPHENEOS_CAPTIVE_PORTAL_HTTPS_URL;
import static android.ext.settings.ConnectivityCheckConstants.GRAPHENEOS_CAPTIVE_PORTAL_HTTP_URL;
import static android.ext.settings.ConnectivityCheckConstants.GRAPHENEOS_CAPTIVE_PORTAL_FALLBACK_URL;
import static android.ext.settings.ConnectivityCheckConstants.GRAPHENEOS_CAPTIVE_PORTAL_OTHER_FALLBACK_URL;

import static android.ext.settings.ConnectivityCheckConstants.STANDARD_HTTPS_URL;
import static android.ext.settings.ConnectivityCheckConstants.STANDARD_HTTP_URL;
import static android.ext.settings.ConnectivityCheckConstants.STANDARD_FALLBACK_URL;
import static android.ext.settings.ConnectivityCheckConstants.STANDARD_OTHER_FALLBACK_URLS;

public class ConnectivityCheckPrefController extends IntSettingPrefController {

    public ConnectivityCheckPrefController(Context ctx, String key) {
        super(ctx, key, ExtSettings.CONNECTIVITY_CHECK_SERVER);
    }

    @Override
    protected void getEntries(Entries entries) {
        entries.add(R.string.connectivity_check_grapheneos_server, GRAPHENEOS_CAPTIVE_PORTAL_HTTP_URL_INTVAL);
        entries.add(R.string.connectivity_check_standard_server, STANDARD_CAPTIVE_PORTAL_HTTP_URL_INTVAL);
        entries.add(R.string.connectivity_check_disabled, DISABLED_CAPTIVE_PORTAL_INTVAL);
    }

    @Override
    public int getAvailabilityStatus() {
        return isDisabledByAdmin() ? DISABLED_FOR_USER : AVAILABLE;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        if (super.onPreferenceChange(preference, value)) {
            setCaptivePortalUrls(Integer.parseInt((String) value));
            return true;
        }

        return false;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        updatePreferenceState();
    }

    private void updatePreferenceState() {
        final boolean disabled = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.CAPTIVE_PORTAL_MODE, Settings.Global.CAPTIVE_PORTAL_MODE_PROMPT) == Settings.Global.CAPTIVE_PORTAL_MODE_IGNORE;

        if (disabled) {
            ExtSettings.CONNECTIVITY_CHECK_SERVER.put(mContext, DISABLED_CAPTIVE_PORTAL_INTVAL);
            return;
        }

        final String pref = Settings.Global.getString(mContext.getContentResolver(), Settings.Global.CAPTIVE_PORTAL_HTTP_URL);

        if (STANDARD_HTTP_URL.equals(pref)) {
            ExtSettings.CONNECTIVITY_CHECK_SERVER.put(mContext, STANDARD_CAPTIVE_PORTAL_HTTP_URL_INTVAL);
        } else {
            ExtSettings.CONNECTIVITY_CHECK_SERVER.put(mContext, GRAPHENEOS_CAPTIVE_PORTAL_HTTP_URL_INTVAL);
        }
    }

    private boolean isDisabledByAdmin() {
        return RestrictedLockUtilsInternal.checkIfRestrictionEnforced(
                mContext, UserManager.DISALLOW_CONFIG_PRIVATE_DNS,
                UserHandle.myUserId()) != null;
    }

    private void setCaptivePortalUrls(int mode) {
        final ContentResolver cr = mContext.getContentResolver();

        switch (mode) {
            case GRAPHENEOS_CAPTIVE_PORTAL_HTTP_URL_INTVAL:
                Settings.Global.putString(cr, Settings.Global.CAPTIVE_PORTAL_HTTP_URL,
                        GRAPHENEOS_CAPTIVE_PORTAL_HTTP_URL);
                Settings.Global.putString(cr, Settings.Global.CAPTIVE_PORTAL_HTTPS_URL,
                        GRAPHENEOS_CAPTIVE_PORTAL_HTTPS_URL);
                Settings.Global.putString(cr, Settings.Global.CAPTIVE_PORTAL_FALLBACK_URL,
                        GRAPHENEOS_CAPTIVE_PORTAL_FALLBACK_URL);
                Settings.Global.putString(cr, Settings.Global.CAPTIVE_PORTAL_OTHER_FALLBACK_URLS,
                        GRAPHENEOS_CAPTIVE_PORTAL_OTHER_FALLBACK_URL);
                Settings.Global.putInt(cr, Settings.Global.CAPTIVE_PORTAL_MODE,
                        Settings.Global.CAPTIVE_PORTAL_MODE_PROMPT);
                break;
            case STANDARD_CAPTIVE_PORTAL_HTTP_URL_INTVAL:
                Settings.Global.putString(cr, Settings.Global.CAPTIVE_PORTAL_HTTP_URL,
                        STANDARD_HTTP_URL);
                Settings.Global.putString(cr, Settings.Global.CAPTIVE_PORTAL_HTTPS_URL,
                        STANDARD_HTTPS_URL);
                Settings.Global.putString(cr, Settings.Global.CAPTIVE_PORTAL_FALLBACK_URL,
                        STANDARD_FALLBACK_URL);
                Settings.Global.putString(cr, Settings.Global.CAPTIVE_PORTAL_OTHER_FALLBACK_URLS,
                        STANDARD_OTHER_FALLBACK_URLS);
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
                Settings.Global.putString(cr, Settings.Global.CAPTIVE_PORTAL_OTHER_FALLBACK_URLS,
                        GRAPHENEOS_CAPTIVE_PORTAL_OTHER_FALLBACK_URL);
                Settings.Global.putInt(cr, Settings.Global.CAPTIVE_PORTAL_MODE,
                        Settings.Global.CAPTIVE_PORTAL_MODE_IGNORE);
        }
    }
}
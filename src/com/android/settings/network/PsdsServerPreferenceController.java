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

package com.android.settings.network;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.net.LinkProperties;
import android.net.Network;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.Process;
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

public class PsdsServerPreferenceController
        extends BasePreferenceController
        implements PreferenceControllerMixin, Preference.OnPreferenceChangeListener,
        OnResume {

    private static final int GRAPHENEOS_PSDS_SERVER_INTVAL = 0;
    private static final int STANDARD_PSDS_SERVER_INTVAL = 1;

    private static final String KEY_PSDS_SERVER_SETTINGS =
            "psds_server_settings";

    private ListPreference mServerPreference;

    public PsdsServerPreferenceController(Context context) {
        super(context, KEY_PSDS_SERVER_SETTINGS);
    }

    @Override
    public int getAvailabilityStatus() {
        if (!Process.myUserHandle().isSystem()) {
            return BasePreferenceController.DISABLED_FOR_USER;
        }
        String device = android.os.Build.HARDWARE;
        if (!device.equals("oriole") && !device.equals("raven") && !device.equals("bluejay") && !device.equals("panther") && !device.equals("cheetah")) {
            return BasePreferenceController.DISABLED_FOR_USER;
        }
        return BasePreferenceController.AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        ListPreference psdsServerList = new ListPreference(screen.getContext());
        psdsServerList.setKey(KEY_PSDS_SERVER_SETTINGS);
        psdsServerList.setOrder(40);
        psdsServerList.setIcon(R.drawable.ic_satellite_alt);
        psdsServerList.setTitle(R.string.psds_server_title);
        psdsServerList.setSummary(R.string.psds_server_summary);
        psdsServerList.setEntries(R.array.psds_server_entries);
        psdsServerList.setEntryValues(R.array.psds_server_values);

        if (mServerPreference == null) {
            screen.addPreference(psdsServerList);
            mServerPreference = psdsServerList;
        }
        super.displayPreference(screen);
        updatePreferenceState();
    }

    @Override
    public String getPreferenceKey() {
        return KEY_PSDS_SERVER_SETTINGS;
    }

    private void updatePreferenceState() {
        int pref = Settings.Global.getInt(mContext.getContentResolver(), Settings.Global.PSDS_SERVER, GRAPHENEOS_PSDS_SERVER_INTVAL);
        if (GRAPHENEOS_PSDS_SERVER_INTVAL == pref) {
            mServerPreference.setValueIndex(GRAPHENEOS_PSDS_SERVER_INTVAL);
        } else {
            mServerPreference.setValueIndex(STANDARD_PSDS_SERVER_INTVAL);
        }
    }

    @Override
    public void onResume() {
        updatePreferenceState();
        if (mServerPreference != null) {
            setPsdsUrl(mContext.getContentResolver(), Integer.parseInt(mServerPreference.getValue()));
        }
    }

    private void setPsdsUrl(ContentResolver cr, int mode) {
        switch (mode) {
            case STANDARD_PSDS_SERVER_INTVAL:
                Settings.Global.putInt(cr, Settings.Global.PSDS_SERVER, STANDARD_PSDS_SERVER_INTVAL);
                break;
            case GRAPHENEOS_PSDS_SERVER_INTVAL:
                Settings.Global.putInt(cr, Settings.Global.PSDS_SERVER, GRAPHENEOS_PSDS_SERVER_INTVAL);
                break;
            default:
                // GrapheneOS URL as placeholder
                Settings.Global.putInt(cr, Settings.Global.PSDS_SERVER, GRAPHENEOS_PSDS_SERVER_INTVAL);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        final String key = preference.getKey();
        if (KEY_PSDS_SERVER_SETTINGS.equals(key)) {
                setPsdsUrl(mContext.getContentResolver(), Integer.parseInt((String)value));
            return true;
        } else {
            return false;
        }
    }

}

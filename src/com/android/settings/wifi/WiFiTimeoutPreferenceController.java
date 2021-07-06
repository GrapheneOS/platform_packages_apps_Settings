package com.android.settings.wifi;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import com.android.settings.R;

public class WiFiTimeoutPreferenceController {
    private static final String TAG = "WiFiTimeoutPrefCtrl";

    public static final int FALLBACK_WIFI_TIMEOUT_VALUE = 0;
    private final Context mContext;

    public WiFiTimeoutPreferenceController(Context context) {
        mContext = context;
    }

    public void updateState(Preference preference) {
        final ListPreference timeoutListPreference = (ListPreference) preference;
        final long currentTimeout = Settings.Global.getLong(mContext.getContentResolver(),
                Settings.Global.WIFI_OFF_TIMEOUT, FALLBACK_WIFI_TIMEOUT_VALUE);
        timeoutListPreference.setValue(String.valueOf(currentTimeout));
        updateTimeoutPreferenceDescription(timeoutListPreference,
                Long.parseLong(timeoutListPreference.getValue()));
    }

    public boolean preferenceChange(Preference preference, Object newValue) {
        try {
            long value = Long.parseLong((String) newValue);
            Settings.Global.putLong(mContext.getContentResolver(), Settings.Global.WIFI_OFF_TIMEOUT, value);
            updateState(preference);
        } catch (NumberFormatException e) {
            Log.e(TAG, "could not persist bluetooth timeout setting", e);
        }
        return true;
    }

    public static CharSequence getTimeoutDescription(
            long currentTimeout, CharSequence[] entries, CharSequence[] values) {
        if (currentTimeout < 0 || entries == null || values == null
                || values.length != entries.length) {
            return null;
        }

        for (int i = 0; i < values.length; i++) {
            long timeout = Long.parseLong(values[i].toString());
            if (currentTimeout == timeout) {
                return entries[i];
            }
        }
        return null;
    }

    private void updateTimeoutPreferenceDescription(ListPreference preference,
                                                    long currentTimeout) {
        final CharSequence[] entries = preference.getEntries();
        final CharSequence[] values = preference.getEntryValues();
        final CharSequence timeoutDescription = getTimeoutDescription(
                currentTimeout, entries, values);
        String summary = "";
        if (timeoutDescription != null) {
            if (currentTimeout != 0) {
                summary = mContext.getString(R.string.wifi_auto_timeout_summary, timeoutDescription);
            } else {
                summary = mContext.getString(R.string.wifi_auto_no_timeout_summary);
            }
        }
        preference.setSummary(summary);
        Log.d(TAG, "updateTimeoutPreferenceDescription  Summry " + summary +  "   summry :"  + preference.getSummary());
    }
}

package com.android.settings.connecteddevice;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

public class NfcTimeoutPreferenceController extends BasePreferenceController {

    private final Context mContext;
    private final CharSequence[] mInitialEntries;
    private final CharSequence[] mInitialValues;

    public NfcTimeoutPreferenceController(Context context, String key) {
        super(context, key);
        mContext = context;
        mInitialEntries = context.getResources().getStringArray(R.array.nfc_timeout_entries);
        mInitialValues = context.getResources().getStringArray(R.array.nfc_timeout_values);
    }

    public static CharSequence getTimeoutDescription(long currentTimeout, CharSequence[] entries,
            CharSequence[] values) {
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

    public static long getNfcTimeoutDuration(Context context) {
        return Settings.Global.getLong(context.getContentResolver(),
                Settings.Global.NFC_OFF_TIMEOUT, 0);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    protected void refreshSummary(Preference preference) {
        super.refreshSummary(preference);
        updateState(preference);
    }

    @Override
    public void updateState(Preference preference) {
        long currentTimeout = getNfcTimeoutDuration(mContext);
        CharSequence duration = getTimeoutDescription(currentTimeout, mInitialEntries,
                mInitialValues);

        String summary = (currentTimeout != 0) ? mContext.getString(R.string.nfc_timeout_summary,
                duration) : mContext.getString(R.string.nfc_timeout_summary_off);

        preference.setSummary(summary);
    }
}

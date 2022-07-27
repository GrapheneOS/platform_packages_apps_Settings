package com.android.settings.wifi;

import android.content.Context;
import android.provider.Settings;
import android.text.format.DateUtils;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

public class WiFiTimeoutPreferenceController extends BasePreferenceController {

    public WiFiTimeoutPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        long currentTimeout = Settings.Global.getLong(mContext.getContentResolver(),
                Settings.Global.WIFI_OFF_TIMEOUT, 0);

        return (currentTimeout != 0) ? mContext.getString(R.string.wifi_timeout_summary,
                DateUtils.formatDuration(currentTimeout).toString()
        ) : mContext.getString(R.string.wifi_timeout_simple_summary);
    }
}
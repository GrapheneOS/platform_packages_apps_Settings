package com.android.settings.bluetooth;

import android.content.Context;
import android.provider.Settings;
import android.text.format.DateUtils;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

public class BtTimeoutPreferenceController extends BasePreferenceController {


    public BtTimeoutPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        long currentTimeout = Settings.Global.getLong(
                mContext.getContentResolver(),
                BtTimeoutSettings.TIMEOUT_KEY,
                0);

        return (currentTimeout != 0) ? mContext.getString(R.string.bt_timeout_summary,
                DateUtils.formatDuration(currentTimeout).toString()
        ) : mContext.getString(R.string.bt_timeout_simple_summary);
    }
}

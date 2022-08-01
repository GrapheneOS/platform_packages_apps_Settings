package com.android.settings.security;

import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.format.DateUtils;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

public class AutoRebootPreferenceController extends BasePreferenceController {

    public AutoRebootPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        if (mContext.getUserId() == UserHandle.SYSTEM.getIdentifier()) {
            return AVAILABLE;
        } else {
            return DISABLED_FOR_USER;
        }
    }

    @Override
    public CharSequence getSummary() {
        long timeout = Settings.Global.getLong(mContext.getContentResolver(),
                Settings.Global.SETTINGS_REBOOT_AFTER_TIMEOUT, 0);

        return (timeout != 0) ? mContext.getString(R.string.auto_reboot_summary,
                DateUtils.formatDuration(timeout).toString()) : mContext.getString(
                R.string.auto_reboot_simple_summary);
    }

}

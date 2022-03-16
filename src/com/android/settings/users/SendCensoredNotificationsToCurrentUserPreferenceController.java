package com.android.settings.users;

import android.content.Context;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

public class SendCensoredNotificationsToCurrentUserPreferenceController
        extends TogglePreferenceController {
    private final UserCapabilities mUserCaps;

    public SendCensoredNotificationsToCurrentUserPreferenceController(Context context, String key) {
        super(context, key);
        mUserCaps = UserCapabilities.create(context);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        mUserCaps.updateAddUserCapabilities(mContext);
        if (!isAvailable()) {
            preference.setVisible(false);
        } else {
            preference.setVisible(mUserCaps.mUserSwitcherEnabled);
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return mUserCaps.mUserSwitcherEnabled ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public boolean isChecked() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.SEND_CENSORED_NOTIFICATIONS_TO_CURRENT_USER, 0) != 0;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.SEND_CENSORED_NOTIFICATIONS_TO_CURRENT_USER, isChecked ? 1 : 0);
    }

    @Override
    public CharSequence getSummary() {
        return mContext.getString(
                R.string.user_settings_send_censored_notifications_to_current_summary);
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_system;
    }
}

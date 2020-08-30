package com.android.settings.users;

import android.content.Context;
import android.provider.Settings;
import android.widget.Toast;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

public class LimitNumberOfRunningUsersPreferenceController extends TogglePreferenceController {
    private final UserCapabilities mUserCaps;

    public LimitNumberOfRunningUsersPreferenceController(Context context, String key) {
        super(context, key);
        mUserCaps = UserCapabilities.create(context);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        mUserCaps.updateAddUserCapabilities(mContext);

        preference.setSummary(getSummary());
        if (!isAvailable()) {
            preference.setVisible(false);
        } else {
            preference.setVisible(mUserCaps.mUserSwitcherEnabled);
        }
    }

    @Override
    public int getAvailabilityStatus() {
        if (!mUserCaps.isAdmin()) {
            return DISABLED_FOR_USER;
        } else {
            return mUserCaps.mUserSwitcherEnabled ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
        }
    }

    @Override
    public boolean isChecked() {
        return Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.RUNNING_USERS_LIMIT_ENABLED, 1) != 0;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        if (isChecked) {
            // Tell the user that enabling the setting won't enforce the limit right away.
            Toast.makeText(mContext, R.string.user_settings_limit_number_active_users_toast,
                    Toast.LENGTH_SHORT).show();
        }

        return Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.RUNNING_USERS_LIMIT_ENABLED, isChecked ? 1 : 0);
    }

    @Override
    public CharSequence getSummary() {
        final int maxRunningUsers = mContext.getResources().getInteger(
                com.android.internal.R.integer.config_multiuserMaxRunningUsers);
        return mContext.getString(R.string.user_settings_limit_number_active_users_summary,
                maxRunningUsers);
    }
}


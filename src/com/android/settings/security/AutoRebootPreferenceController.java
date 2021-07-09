package com.android.settings.security;

import android.content.Context;
import android.os.UserManager;
import android.provider.Settings;
import android.util.Log;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.events.OnResume;

public class AutoRebootPreferenceController extends AbstractPreferenceController
    implements PreferenceControllerMixin, OnResume,
           Preference.OnPreferenceChangeListener {

    private static final String KEY_AUTO_REBOOT = "auto_reboot";
    private static final String PREF_KEY_SECURITY_CATEGORY = "security_category";


    private PreferenceCategory mSecurityCategory;
    private boolean mIsAdmin;
    private final UserManager mUm;

    public AutoRebootPreferenceController(Context context) {
        super(context);
        mUm = UserManager.get(context);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mSecurityCategory = screen.findPreference(PREF_KEY_SECURITY_CATEGORY);
        updatePreferenceState();
    }

    @Override
    public boolean isAvailable() {
        mIsAdmin = mUm.isAdminUser();
        return mIsAdmin;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_AUTO_REBOOT;
    }

    // TODO: should we use onCreatePreferences() instead?
    private void updatePreferenceState() {
        if (mSecurityCategory == null) {
            return;
        }

        if (mIsAdmin) {
            ListPreference autoReboot =
                    (ListPreference) mSecurityCategory.findPreference(KEY_AUTO_REBOOT);
            autoReboot.setValue(Integer.toString(Settings.Global.getInt(
                    mContext.getContentResolver(), Settings.Global.SETTINGS_REBOOT_AFTER_TIMEOUT, 0)));
        } else {
            mSecurityCategory.removePreference(
                    mSecurityCategory.findPreference(KEY_AUTO_REBOOT));
        }
    }

    @Override
    public void onResume() {
        updatePreferenceState();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        final String key = preference.getKey();
        if (KEY_AUTO_REBOOT.equals(key) && value instanceof String && mIsAdmin) {
            int timeout = Integer.parseInt((String) value);
            Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.SETTINGS_REBOOT_AFTER_TIMEOUT, timeout);
        }
        return true;
    }
}

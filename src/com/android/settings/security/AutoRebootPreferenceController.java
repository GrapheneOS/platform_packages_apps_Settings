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
        implements PreferenceControllerMixin, OnResume, Preference.OnPreferenceChangeListener {

    private static final String KEY_AUTO_REBOOT = "auto_reboot";
    private static final String PREF_KEY_SECURITY_CATEGORY = "security_category";
    private static final String VALUE_STORE_KEY = "settings_reboot_after_timeout";
    private static final boolean DEBUG = false;

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
            log("updatePreferenceState called with null mSecurityCategory");
            return;
        }

        if (mIsAdmin) {
            ListPreference autoRebootPsfs = (ListPreference) mSecurityCategory.findPreference(KEY_AUTO_REBOOT);
            log("updatePreferenceState called from admin/owner user update psfs value with currentValue :  " + currentValueInHours());
            if (autoRebootPsfs != null) {
                autoRebootPsfs.setValue(currentValueInHours());
            }
        } else {
            log("updatePreferenceState isn't called from admin/owner user removing ");
            mSecurityCategory.removePreference(mSecurityCategory.findPreference(KEY_AUTO_REBOOT));
        }
    }

    @Override
    public void onResume() {
        updatePreferenceState();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        final String key = preference.getKey();
        log("onPreferenceChange key : " + key + " value : " + value + " isString? : " + (value instanceof  String) + " isAdminUser? : " + mUm.isAdminUser());
        if (KEY_AUTO_REBOOT.equals(key) && value instanceof String && mIsAdmin) {
            int timeout = Integer.parseInt((String) value);
            Settings.Global.putLong(mContext.getContentResolver(), VALUE_STORE_KEY, hoursToMilli(timeout));
        }
        return true;
    }

    private String currentValueInHours() {
        int value = Settings.Global.getLong(mContext.getContentResolver(), VALUE_STORE_KEY, 0);
        return String.valueOf(milliToHours(value));
    }

    private long milliToHours(long milli){
        return TimeUnit.MILLISECONDS.toHours(milli);
    }

    private long hoursToMilli(long hour){
        return TimeUnit.HOURS.toMillis(hour);
    }

    private void log(String log) {
        if (DEBUG && log != null) {
            Log.d("AutoReboot", log);
        }
    }

}

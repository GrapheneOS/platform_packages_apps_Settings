package com.android.settings.security;

import android.content.Context;

import android.os.UserHandle;
import android.os.UserManager;
import android.os.SystemProperties;

import android.provider.Settings;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.events.OnResume;

public class PinScramblePreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, OnResume, Preference.OnPreferenceChangeListener {

    private static final String KEY_SCRAMBLE_PIN_LAYOUT = "scramble_pin_layout";
    private static final String PREF_KEY_SECURITY_CATEGORY = "security_category";

    private PreferenceCategory mSecurityCategory;
    private ListPreference mScramblePin;

    public PinScramblePreferenceController(Context context) {
        super(context);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mSecurityCategory = screen.findPreference(PREF_KEY_SECURITY_CATEGORY);
        updatePreferenceState();
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_SCRAMBLE_PIN_LAYOUT;
    }

    // TODO: should we use onCreatePreferences() instead?
    private void updatePreferenceState() {
        if (mSecurityCategory == null) {
            return;
        }
        mScramblePin = (ListPreference) mSecurityCategory.findPreference(KEY_SCRAMBLE_PIN_LAYOUT);
        mScramblePin.setValue(Boolean.toString(Settings.System.getInt(mContext.getContentResolver(), Settings.System.SCRAMBLE_PIN_LAYOUT, 0) != 0));
    }

    @Override
    public void onResume() {
        updatePreferenceState();
        if (mScramblePin != null) {
            boolean mode = Boolean.parseBoolean(mScramblePin.getValue());
            Settings.System.putInt(mContext.getContentResolver(), Settings.System.SCRAMBLE_PIN_LAYOUT, (mode) ? 0 : 1);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        final String key = preference.getKey();
        if (KEY_SCRAMBLE_PIN_LAYOUT.equals(key)) {
            boolean mode = Boolean.parseBoolean((String) value);
            Settings.System.putInt(mContext.getContentResolver(), Settings.System.SCRAMBLE_PIN_LAYOUT, (mode) ? 1 : 0);
        }
        return true;
    }
}

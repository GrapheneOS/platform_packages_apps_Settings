package com.android.settings.wifi;

import android.content.Context;
import android.os.SystemProperties;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

public class RandomizeMacPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, Preference.OnPreferenceChangeListener {

    private static final String KEY_RANDOMIZE_MAC = "randomize_mac";

    private SwitchPreference mRandomizeMacPreference;

    public RandomizeMacPreferenceController(Context context) {
        super(context);
    }

    @Override
    public void updateState(Preference preference) {
        boolean value = SystemProperties.getBoolean("persist.privacy.randomize_mac", true);
        ((SwitchPreference) preference).setChecked(value);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        SystemProperties.set("persist.privacy.randomize_mac", (Boolean) newValue ? "1" : "0");
        return true;
    }

    @Override
    public boolean isAvailable() {
        return SystemProperties.getBoolean("ro.randomize_mac", false);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_RANDOMIZE_MAC;
    }
}

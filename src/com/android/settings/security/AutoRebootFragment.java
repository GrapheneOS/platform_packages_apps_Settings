package com.android.settings.security;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.provider.Settings;
import android.text.format.DateUtils;

import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.widget.RadioButtonPickerFragment;
import com.android.settings.widget.TimeoutPickerFragment;
import com.android.settingslib.widget.CandidateInfo;
import com.android.settingslib.widget.RadioButtonPreference;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class AutoRebootFragment extends TimeoutPickerFragment {

    static final long[] CANDIDATES = {
            0,
            TimeUnit.MINUTES.toMillis(10),
            TimeUnit.MINUTES.toMillis(30),
            TimeUnit.HOURS.toMillis(1),
            TimeUnit.HOURS.toMillis(2),
            TimeUnit.HOURS.toMillis(4),
            TimeUnit.HOURS.toMillis(8),
            TimeUnit.HOURS.toMillis(12),
            TimeUnit.HOURS.toMillis(24),
            TimeUnit.HOURS.toMillis(36),
            TimeUnit.HOURS.toMillis(48),
            TimeUnit.HOURS.toMillis(72),
    };

    @Override
    protected long[] getSelectableTimeouts() {
        return CANDIDATES;
    }

    @Override
    protected String getSettingsKey() {
        return Settings.Global.SETTINGS_REBOOT_AFTER_TIMEOUT;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.auto_reboot_settings;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SECURITY;
    }

}

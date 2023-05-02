package com.android.settings.security;

import android.content.Context;
import android.ext.settings.ExtSettings;

import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.ext.IntSettingPrefController;
import com.android.settings.ext.RadioButtonPickerFragment2;

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MINUTES;

public class AutoRebootPrefController extends IntSettingPrefController {

    public AutoRebootPrefController(Context ctx, String key) {
        super(ctx, key, ExtSettings.AUTO_REBOOT_TIMEOUT);
    }

    @Override
    public void addPrefsBeforeList(RadioButtonPickerFragment2 fragment, PreferenceScreen screen) {
        addFooterPreference(screen, R.string.auto_reboot_footer,
                "https://grapheneos.org/features#auto-reboot");
    }

    @Override
    protected void getEntries(Entries entries) {
        entries.add(3, DAYS); // default
        entries.add(R.string.switch_off_text, 0);
        entries.add(10, MINUTES);
        entries.add(30, MINUTES);
        entries.add(1, HOURS);
        entries.add(2, HOURS);
        entries.add(4, HOURS);
        entries.add(8, HOURS);
        entries.add(12, HOURS);
        entries.add(1, DAYS);
        entries.add(36, HOURS);
        entries.add(2, DAYS);
    }
}

package com.android.settings.security;

import android.content.Context;
import android.ext.settings.LockAction;

import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.ext.IntSettingPrefController;
import com.android.settings.ext.RadioButtonPickerFragment2;

public class LockActionTypePrefController extends IntSettingPrefController {

    public LockActionTypePrefController(Context ctx, String key) {
        super(ctx, key, LockAction.ACTION_SETTING);
    }

    @Override
    public void addPrefsBeforeList(RadioButtonPickerFragment2 fragment, PreferenceScreen screen) {
        addFooterPreference(screen, R.string.lock_action_footer);
    }

    @Override
    protected void getEntries(Entries entries) {
        entries.add(R.string.lock_action_reboot_title, R.string.lock_action_reboot_summary,
                LockAction.ACTION_REBOOT);
        entries.add(R.string.lock_action_shutdown_title, R.string.lock_action_shutdown_summary,
                LockAction.ACTION_SHUTDOWN);
    }
}
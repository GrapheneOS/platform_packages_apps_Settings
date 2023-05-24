package com.android.settings.network;

import android.content.Context;
import android.ext.settings.ConnChecksSetting;
import android.ext.settings.ExtSettings;

import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.ext.IntSettingPrefController;
import com.android.settings.ext.RadioButtonPickerFragment2;

public class ConnectivityChecksPrefController extends IntSettingPrefController {

    public ConnectivityChecksPrefController(Context ctx, String key) {
        super(ctx, key, ExtSettings.CONNECTIVITY_CHECKS);
    }

    @Override
    protected void getEntries(Entries entries) {
        entries.add(R.string.conn_checks_grapheneos_server, ConnChecksSetting.VAL_GRAPHENEOS);
        entries.add(R.string.conn_checks_google_server, ConnChecksSetting.VAL_STANDARD);
        entries.add(R.string.conn_checks_disabled, ConnChecksSetting.VAL_DISABLED);
    }

    @Override
    public void addPrefsAfterList(RadioButtonPickerFragment2 fragment, PreferenceScreen screen) {
        addFooterPreference(screen, R.string.conn_checks_footer);
    }
}

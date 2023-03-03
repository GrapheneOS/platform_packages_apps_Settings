package com.android.settings.location;

import android.content.Context;
import android.ext.settings.ExtSettings;
import android.ext.settings.GnssConstants;

import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.ext.IntSettingPrefController;
import com.android.settings.ext.RadioButtonPickerFragment2;

public class GnssSuplPrefController extends IntSettingPrefController {

    public GnssSuplPrefController(Context ctx, String key) {
        super(ctx, key, ExtSettings.GNSS_SUPL);
    }

    @Override
    public void addPrefsAfterList(RadioButtonPickerFragment2 fragment, PreferenceScreen screen) {
        addFooterPreference(screen, R.string.pref_gnss_supl_footer);
    }

    @Override
    protected void getEntries(Entries entries) {
        entries.add(R.string.supl_enabled_grapheneos_proxy, GnssConstants.SUPL_SERVER_GRAPHENEOS_PROXY);
        entries.add(R.string.supl_enabled_standard_server, GnssConstants.SUPL_SERVER_STANDARD);
        entries.add(R.string.supl_disabled, R.string.supl_disabled_summary, GnssConstants.SUPL_DISABLED);
    }
}

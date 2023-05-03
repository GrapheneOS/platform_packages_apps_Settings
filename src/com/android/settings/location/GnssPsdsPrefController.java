package com.android.settings.location;

import android.content.Context;
import android.ext.settings.ExtSettings;
import android.ext.settings.GnssConstants;

import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.ext.IntSettingPrefController;
import com.android.settings.ext.RadioButtonPickerFragment2;

public class GnssPsdsPrefController extends IntSettingPrefController {
    private final String psdsType;

    public GnssPsdsPrefController(Context ctx, String key) {
        super(ctx, key, ExtSettings.getGnssPsdsSetting(ctx));
        psdsType = ctx.getString(com.android.internal.R.string.config_gnssPsdsType);
    }

    @Override
    public int getAvailabilityStatus() {
        int result = super.getAvailabilityStatus();
        if (result == AVAILABLE) {
            if (psdsType.isEmpty()) {
                result = UNSUPPORTED_ON_DEVICE;
            }
        }
        return result;
    }

    @Override
    public void addPrefsAfterList(RadioButtonPickerFragment2 fragment, PreferenceScreen screen) {
        addFooterPreference(screen, R.string.gnss_psds_footer);
    }

    @Override
    protected void getEntries(Entries entries) {
        entries.add(R.string.psds_enabled_grapheneos_server, GnssConstants.PSDS_SERVER_GRAPHENEOS);
        int standardServerString;
        switch (psdsType) {
            case GnssConstants.PSDS_TYPE_QUALCOMM_XTRA:
                standardServerString = R.string.psds_enabled_qualcomm_server;
                break;
            default:
                standardServerString = R.string.psds_enabled_standard_server;
                break;
        }
        entries.add(standardServerString, GnssConstants.PSDS_SERVER_STANDARD);
        entries.add(R.string.psds_disabled, R.string.psds_disabled_summary, GnssConstants.PSDS_DISABLED);
    }
}

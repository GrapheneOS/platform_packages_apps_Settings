package com.android.settings.nfc;

import android.content.Context;
import android.content.pm.PackageManager;
import android.ext.settings.ExtSettings;

import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.ext.AutoOffSetting;
import com.android.settings.ext.IntSettingPrefController;
import com.android.settings.ext.RadioButtonPickerFragment2;

public class NfcAutoOffPrefController extends IntSettingPrefController {

    public NfcAutoOffPrefController(Context ctx, String key) {
        super(ctx, key, ExtSettings.NFC_AUTO_OFF);
    }

    @Override
    public int getAvailabilityStatus() {
        if (!mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_NFC_ANY)) {
            return UNSUPPORTED_ON_DEVICE;
        }
        return super.getAvailabilityStatus();
    }

    @Override
    public void addPrefsBeforeList(RadioButtonPickerFragment2 fragment, PreferenceScreen screen) {
        addFooterPreference(screen, R.string.nfc_auto_off_footer);
    }

    @Override
    protected void getEntries(Entries entries) {
        AutoOffSetting.getEntries(entries);
    }
}

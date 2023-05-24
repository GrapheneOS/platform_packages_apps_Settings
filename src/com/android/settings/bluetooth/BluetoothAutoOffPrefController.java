package com.android.settings.bluetooth;

import android.content.Context;
import android.content.pm.PackageManager;
import android.ext.settings.ExtSettings;

import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.ext.AutoOffSetting;
import com.android.settings.ext.IntSettingPrefController;
import com.android.settings.ext.RadioButtonPickerFragment2;

public class BluetoothAutoOffPrefController extends IntSettingPrefController {

    public BluetoothAutoOffPrefController(Context ctx, String key) {
        super(ctx, key, ExtSettings.BLUETOOTH_AUTO_OFF);
    }

    @Override
    public int getAvailabilityStatus() {
        int r = super.getAvailabilityStatus();
        if (r == AVAILABLE) {
            return mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH) ?
                    AVAILABLE : UNSUPPORTED_ON_DEVICE;
        }
        return r;
    }

    @Override
    public void addPrefsBeforeList(RadioButtonPickerFragment2 fragment, PreferenceScreen screen) {
        addFooterPreference(screen, R.string.bluetooth_auto_off_footer);
    }

    @Override
    protected void getEntries(Entries entries) {
        AutoOffSetting.getEntries(entries);
    }
}

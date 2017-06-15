package com.android.settings.deviceinfo;

import android.content.Context;
import android.os.SystemProperties;
import android.support.v7.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

public class BootloaderVersionPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin {

    private static final String BOOTLOADER_PROPERTY = "ro.bootloader";
    private static final String KEY_BOOTLOADER_VERSION = "bootloader_version";

    public BootloaderVersionPreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_BOOTLOADER_VERSION;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        preference.setSummary(SystemProperties.get(BOOTLOADER_PROPERTY,
                mContext.getString(R.string.device_info_default)));
    }
}

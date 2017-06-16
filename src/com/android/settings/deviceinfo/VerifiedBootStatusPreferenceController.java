package com.android.settings.deviceinfo;

import android.content.Context;
import android.os.SystemProperties;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

public class VerifiedBootStatusPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin {

    private static final String PROPERTY_VERIFIED_BOOT_STATE = "ro.boot.verifiedbootstate";
    private static final String PROPERTY_VERITY_MODE = "ro.boot.veritymode";
    private static final String PROPERTY_PARTITION_SYSTEM_VERIFIED = "partition.system.verified";
    private static final String PROPERTY_PARTITION_VENDOR_VERIFIED = "partition.vendor.verified";
    private static final String KEY_VERIFIED_BOOT_STATUS = "verified_boot_status";

    public VerifiedBootStatusPreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_VERIFIED_BOOT_STATUS;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        final Preference pref = screen.findPreference(KEY_VERIFIED_BOOT_STATUS);
        if (pref == null) {
            return;
        }
        final String verifiedBootState = SystemProperties.get(PROPERTY_VERIFIED_BOOT_STATE);
        final String verityMode = SystemProperties.get(PROPERTY_VERITY_MODE);
        final int partitionSystemVerified = SystemProperties.getInt(PROPERTY_PARTITION_SYSTEM_VERIFIED, 0);
        final int partitionVendorVerified = SystemProperties.getInt(PROPERTY_PARTITION_VENDOR_VERIFIED, 0);
        if (("green".equals(verifiedBootState) || "yellow".equals(verifiedBootState)) &&
                "enforcing".equals(verityMode) && partitionSystemVerified == 2 && partitionVendorVerified == 2) {
            pref.setSummary(mContext.getString(R.string.verified_boot_status_enforcing));
        }
    }
}

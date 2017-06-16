package com.android.settings.deviceinfo;

import android.content.Context;
import android.os.SystemProperties;
import android.service.persistentdata.PersistentDataBlockManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

public class AntiTheftProtectionStatusPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin {

    private static final String KEY_ANTI_THEFT_PROTECTION_STATUS = "anti_theft_protection_status";
    private static final String PROPERTY_CRYPTO_TYPE = "ro.crypto.type";

    public AntiTheftProtectionStatusPreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_ANTI_THEFT_PROTECTION_STATUS;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        final Preference pref = screen.findPreference(KEY_ANTI_THEFT_PROTECTION_STATUS);
        if (pref == null) {
            return;
        }
        final PersistentDataBlockManager manager = (PersistentDataBlockManager)
                mContext.getSystemService(Context.PERSISTENT_DATA_BLOCK_SERVICE);
        if (manager == null || !"file".equals(SystemProperties.get(PROPERTY_CRYPTO_TYPE))) {
            pref.setSummary(mContext.getString(R.string.anti_theft_protection_status_not_available));
        } else if (manager.getOemUnlockEnabled() || manager.getFlashLockState() != manager.FLASH_LOCK_LOCKED) {
            pref.setSummary(mContext.getString(R.string.anti_theft_protection_status_disabled));
        }
    }
}

package com.android.settings.privacy;

import android.content.Context;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

public class AutoGrantOtherSensorsPermissionPrefController extends TogglePreferenceController {

    public AutoGrantOtherSensorsPermissionPrefController(Context ctx, String key) {
        super(ctx, key);
    }

    @Override
    public boolean isChecked() {
        var cr = mContext.getContentResolver();
        var key = Settings.Secure.AUTO_GRANT_OTHER_SENSORS_PERMISSION;
        int def = Settings.Secure.AUTO_GRANT_OTHER_SENSORS_PERMISSION_DEFAULT;

        return Settings.Secure.getInt(cr, key, def) != 0;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        var cr = mContext.getContentResolver();
        var key = Settings.Secure.AUTO_GRANT_OTHER_SENSORS_PERMISSION;

        return Settings.Secure.putInt(cr, key, isChecked ? 1 : 0);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_privacy;
    }
}

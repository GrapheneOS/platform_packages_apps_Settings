package com.android.settings.location;

import android.content.Context;
import android.os.Process;
import android.provider.Settings;

import com.android.settings.core.TogglePreferenceController;

public class ForceDisableSuplPrefController extends TogglePreferenceController {

    public ForceDisableSuplPrefController(Context ctx, String key) {
        super(ctx, key);
    }

    @Override
    public int getAvailabilityStatus() {
        if (!Process.myUserHandle().isSystem()) {
            return DISABLED_FOR_USER;
        }

        return AVAILABLE;
    }

    @Override
    public boolean isChecked() {
        var cr = mContext.getContentResolver();
        String key = Settings.Global.FORCE_DISABLE_SUPL;
        int def = Settings.Global.FORCE_DISABLE_SUPL_DEFAULT;

        return Settings.Global.getInt(cr, key, def) == 1;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        var cr = mContext.getContentResolver();
        String key = Settings.Global.FORCE_DISABLE_SUPL;

        return Settings.Global.putInt(cr, key, isChecked ? 1 : 0);
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return NO_RES;
    }
}

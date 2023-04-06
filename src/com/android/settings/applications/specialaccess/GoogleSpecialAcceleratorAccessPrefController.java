package com.android.settings.applications.specialaccess;

import android.content.Context;
import android.ext.settings.ExtSettings;

import com.android.settings.R;
import com.android.settings.ext.BoolSettingFragmentPrefController;

public class GoogleSpecialAcceleratorAccessPrefController extends BoolSettingFragmentPrefController {

    public GoogleSpecialAcceleratorAccessPrefController(Context ctx, String key) {
        super(ctx, key, ExtSettings.ALLOW_GOOGLE_APPS_SPECIAL_ACCESS_TO_ACCELERATORS);
    }

    @Override
    public int getAvailabilityStatus() {
        if (!mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_Google_apps_can_have_special_access_to_accelerators)) {
            return UNSUPPORTED_ON_DEVICE;
        }

        return super.getAvailabilityStatus();
    }

    @Override
    protected CharSequence getSummaryOn() {
        return resText(R.string.Google_apps_special_accelerator_access_summary_granted);
    }

    @Override
    protected CharSequence getSummaryOff() {
        return resText(R.string.Google_apps_special_accelerator_access_summary_not_granted);
    }
}

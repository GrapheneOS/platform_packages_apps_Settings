package com.android.settings.applications.specialaccess;

import android.app.ActivityThread;
import android.ext.settings.BoolSetting;
import android.ext.settings.ExtSettings;
import android.os.RemoteException;

import com.android.internal.util.GoogleCameraUtils;
import com.android.settings.R;
import com.android.settings.ext.BoolSettingFragment;
import com.android.settingslib.widget.FooterPreference;

public class GoogleSpecialAcceleratorAccessFragment extends BoolSettingFragment {
    private static final String TAG = GoogleSpecialAcceleratorAccessFragment.class.getSimpleName();

    @Override
    protected BoolSetting getSetting() {
        return ExtSettings.ALLOW_GOOGLE_APPS_SPECIAL_ACCESS_TO_ACCELERATORS;
    }

    @Override
    protected CharSequence getTitle() {
        return resText(R.string.Google_apps_special_accelerator_access_title);
    }

    @Override
    protected CharSequence getMainSwitchTitle() {
        return resText(R.string.Google_apps_special_accelerator_access_main_switch);
    }

    @Override
    protected void onMainSwitchChanged(boolean state) {
        if (GoogleCameraUtils.isCustomSeInfoNeededForAccessToAccelerators(requireContext())) {
            try {
                ActivityThread.getPackageManager().updateSeInfo(GoogleCameraUtils.PACKAGE_NAME);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    @Override
    protected FooterPreference makeFooterPref(FooterPreference.Builder builder) {
        return builder.setTitle(R.string.Google_apps_special_accelerator_access_footer).build();
    }
}

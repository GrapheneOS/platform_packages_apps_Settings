package com.android.settings.security;

import android.ext.settings.BoolSetting;
import android.ext.settings.ExtSettings;

import com.android.settings.R;
import com.android.settings.ext.AppPrefUtils;
import com.android.settings.ext.DefaultOfPerAppSettingFragment;
import com.android.settingslib.widget.FooterPreference;

public class NativeDebuggingFragment extends DefaultOfPerAppSettingFragment {

    @Override
    protected BoolSetting getSetting() {
        invertSetting = true;
        return ExtSettings.ALLOW_NATIVE_DEBUG_BY_DEFAULT;
    }

    @Override
    protected CharSequence getTitle() {
        return getText(R.string.native_debugging_title);
    }

    @Override
    protected CharSequence getMainSwitchTitle() {
        return getText(R.string.native_debugging_main_switch);
    }

    @Override
    protected FooterPreference makeFooterPref(FooterPreference.Builder builder) {
        String text = AppPrefUtils.getFooterForDefaultHardeningSetting(requireContext(), R.string.native_debugging_footer);
        return builder.setTitle(text).build();
    }
}

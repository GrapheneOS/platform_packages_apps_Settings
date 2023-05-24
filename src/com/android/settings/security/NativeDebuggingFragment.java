package com.android.settings.security;

import android.ext.settings.BoolSetting;
import android.ext.settings.ExtSettings;
import android.net.Uri;

import com.android.settings.R;
import com.android.settings.ext.BoolSettingFragment;
import com.android.settingslib.widget.FooterPreference;

public class NativeDebuggingFragment extends BoolSettingFragment {

    @Override
    protected BoolSetting getSetting() {
        return ExtSettings.NATIVE_DEBUGGING;
    }

    @Override
    protected CharSequence getTitle() {
        return getText(R.string.native_debugging_title);
    }

    @Override
    protected CharSequence getMainSwitchTitle() {
        return getText(R.string.native_debugging_title_inner);
    }

    @Override
    protected FooterPreference makeFooterPref(FooterPreference.Builder builder) {
        return builder.setTitle(R.string.native_debugging_footer).build();
    }
}

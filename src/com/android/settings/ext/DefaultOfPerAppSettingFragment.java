package com.android.settings.ext;

import android.content.res.Resources;

import androidx.annotation.StringRes;

import com.android.settings.R;

public abstract class DefaultOfPerAppSettingFragment extends BoolSettingFragment {

    protected CharSequence getMainSwitchSummary() {
        return getText(R.string.summary_of_default_of_per_app_setting);
    }
}

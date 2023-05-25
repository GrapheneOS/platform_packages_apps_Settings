package com.android.settings.security;

import android.ext.settings.BoolSetting;
import android.ext.settings.ExtSettings;
import android.net.Uri;

import com.android.settings.R;
import com.android.settings.ext.BoolSettingFragment;
import com.android.settingslib.widget.FooterPreference;

public class ExecSpawningFragment extends BoolSettingFragment {

    @Override
    protected BoolSetting getSetting() {
        return ExtSettings.EXEC_SPAWNING;
    }

    @Override
    protected CharSequence getTitle() {
        return getText(R.string.exec_spawning_title);
    }

    @Override
    protected CharSequence getMainSwitchTitle() {
        return getText(R.string.exec_spawning_title_inner);
    }

    @Override
    protected FooterPreference makeFooterPref(FooterPreference.Builder builder) {
        FooterPreference p = builder.setTitle(R.string.exec_spawning_footer).build();
        setFooterPrefLearnMoreUri(p, Uri.parse("https://grapheneos.org/usage#exec-spawning"));
        return p;
    }
}

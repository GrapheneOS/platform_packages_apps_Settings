package com.android.settings.ext;

import android.content.Context;
import android.ext.settings.BoolSetting;

import com.android.settings.R;

public abstract class BoolSettingFragmentPrefController extends ExtSettingFragmentPrefController<BoolSetting> {

    protected BoolSettingFragmentPrefController(Context ctx, String key, BoolSetting setting) {
        super(ctx, key, setting);
    }

    @Override
    public CharSequence getSummary() {
        return setting.get(mContext) ? getSummaryOn() : getSummaryOff();
    }

    protected CharSequence getSummaryOn() {
        return resText(R.string.bool_setting_enabled);
    }

    protected CharSequence getSummaryOff() {
        return resText(R.string.bool_setting_disabled);
    }
}

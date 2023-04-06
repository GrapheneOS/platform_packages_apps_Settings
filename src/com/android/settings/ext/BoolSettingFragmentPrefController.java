package com.android.settings.ext;

import android.content.Context;
import android.ext.settings.BoolSetting;

public abstract class BoolSettingFragmentPrefController extends ExtSettingFragmentPrefController<BoolSetting> {

    protected BoolSettingFragmentPrefController(Context ctx, String key, BoolSetting setting) {
        super(ctx, key, setting);
    }

    @Override
    public CharSequence getSummary() {
        return setting.get(mContext) ? getSummaryOn() : getSummaryOff();
    }

    protected abstract CharSequence getSummaryOn();
    protected abstract CharSequence getSummaryOff();
}

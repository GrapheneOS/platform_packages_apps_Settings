/*
 * Copyright (C) 2022 GrapheneOS
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.ext;

import android.content.Context;
import android.ext.settings.IntSetting;

public abstract class IntSettingPrefController extends AbstractListPreferenceController
        implements ExtSettingPrefController<IntSetting>
{
    private final IntSetting setting;

    private final ExtSettingControllerHelper<IntSetting> helper;

    protected IntSettingPrefController(Context ctx, String key, IntSetting setting) {
        super(ctx, key);
        this.setting = setting;
        helper = new ExtSettingControllerHelper(ctx, setting);
    }

    @Override
    public int getAvailabilityStatus() {
        return helper.getAvailabilityStatus();
    }

    @Override
    public final ExtSettingControllerHelper<IntSetting> getHelper() {
        return helper;
    }

    @Override
    protected final int getCurrentValue() {
        return setting.get(mContext);
    }

    @Override
    protected final boolean setValue(int val) {
        return setting.put(mContext, val);
    }

    // called by the setting observer
    @Override
    public void accept(IntSetting intSetting) {
        updatePreference();
    }
}

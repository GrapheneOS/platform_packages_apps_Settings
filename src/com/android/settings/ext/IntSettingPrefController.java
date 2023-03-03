/*
 * Copyright (C) 2022 GrapheneOS
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.ext;

import android.content.Context;
import android.ext.settings.IntSetting;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;

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
    protected final int getCurrentValue() {
        return setting.get(mContext);
    }

    @Override
    protected final boolean setValue(int val) {
        return setting.put(mContext, val);
    }

    @Override
    public void onResume(@NonNull LifecycleOwner owner) {
        helper.onResume(this);
    }

    @Override
    public void onPause(@NonNull LifecycleOwner owner) {
        helper.onPause(this);
    }

    // called by the setting observer
    @Override
    public void accept(IntSetting intSetting) {
        updatePreference();
    }
}

/*
 * Copyright (C) 2022 GrapheneOS
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.ext;

import android.content.Context;
import android.ext.settings.IntSetting;
import android.ext.settings.Setting;
import android.os.Process;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import java.util.function.Consumer;

public abstract class IntSettingPrefController extends AbstractListPreferenceController
        implements DefaultLifecycleObserver, Consumer<IntSetting>
{
    private final IntSetting setting;

    protected IntSettingPrefController(Context ctx, String key, IntSetting setting) {
        super(ctx, key);
        this.setting = setting;
    }

    @Override
    public int getAvailabilityStatus() {
        if (setting.getScope() == Setting.Scope.GLOBAL) {
            if (!Process.myUserHandle().isSystem()) {
                return DISABLED_FOR_USER;
            }
        }
        return AVAILABLE;
    }

    @Override
    protected final int getCurrentValue() {
        return setting.get(mContext);
    }

    @Override
    protected final boolean setValue(int val) {
        return setting.put(mContext, val);
    }

    private Object observer;

    @Override
    public void onResume(@NonNull LifecycleOwner owner) {
        if (setting.canObserveState()) {
            observer = setting.registerObserver(mContext, this, mContext.getMainThreadHandler());
        }
    }

    // called by the setting observer
    @Override
    public void accept(IntSetting intSetting) {
        updatePreference();
    }

    @Override
    public void onPause(@NonNull LifecycleOwner owner) {
        if (setting.canObserveState()) {
            setting.unregisterObserver(mContext, observer);
        }
    }
}

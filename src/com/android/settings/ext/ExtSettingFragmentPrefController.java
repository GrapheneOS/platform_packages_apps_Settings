package com.android.settings.ext;

import android.content.Context;
import android.ext.settings.Setting;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;

public abstract class ExtSettingFragmentPrefController<T extends Setting> extends FragmentPrefController
        implements ExtSettingPrefController<T> {
    protected final T setting;
    protected final ExtSettingControllerHelper<T> helper;

    protected ExtSettingFragmentPrefController(Context ctx, String key, T setting) {
        super(ctx, key);
        this.setting = setting;
        helper = new ExtSettingControllerHelper<T>(ctx, setting);
    }

    @Override
    public int getAvailabilityStatus() {
        return helper.getAvailabilityStatus();
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
    public void accept(T setting) {
        if (preference != null) {
            updateState(preference);
        }
    }
}

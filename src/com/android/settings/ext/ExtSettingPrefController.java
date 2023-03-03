package com.android.settings.ext;

import android.ext.settings.Setting;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import java.util.function.Consumer;

interface ExtSettingPrefController<T extends Setting> extends DefaultLifecycleObserver, Consumer<T> {

    ExtSettingControllerHelper<T> getHelper();

    @Override
    default void onResume(@NonNull LifecycleOwner owner) {
        getHelper().registerObserver(this);
    }

    @Override
    default void onPause(@NonNull LifecycleOwner owner) {
        getHelper().unregisterObserver();
    }
}

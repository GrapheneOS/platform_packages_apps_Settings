package com.android.settings.ext;

import android.ext.settings.Setting;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import java.util.function.Consumer;

interface ExtSettingPrefController<T extends Setting> extends DefaultLifecycleObserver, Consumer<T> {
}

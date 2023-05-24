package com.android.settings.security;

import android.content.Context;
import android.ext.settings.ExtSettings;

import com.android.settings.ext.BoolSettingFragmentPrefController;

public class NativeDebuggingPrefController extends BoolSettingFragmentPrefController {

    public NativeDebuggingPrefController(Context ctx, String key) {
        super(ctx, key, ExtSettings.NATIVE_DEBUGGING);
    }
}

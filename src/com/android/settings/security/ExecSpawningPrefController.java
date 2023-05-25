package com.android.settings.security;

import android.content.Context;
import android.ext.settings.ExtSettings;

import com.android.settings.ext.BoolSettingFragmentPrefController;

public class ExecSpawningPrefController extends BoolSettingFragmentPrefController {

    public ExecSpawningPrefController(Context ctx, String key) {
        super(ctx, key, ExtSettings.EXEC_SPAWNING);
    }
}

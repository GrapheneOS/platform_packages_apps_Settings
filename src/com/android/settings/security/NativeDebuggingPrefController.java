package com.android.settings.security;

import android.content.Context;
import android.ext.settings.ExtSettings;

import com.android.settings.R;
import com.android.settings.ext.BoolSettingFragmentPrefController;

public class NativeDebuggingPrefController extends BoolSettingFragmentPrefController {

    public NativeDebuggingPrefController(Context ctx, String key) {
        super(ctx, key, ExtSettings.ALLOW_NATIVE_DEBUG_BY_DEFAULT);
    }

    public CharSequence getSummary() {
        return mContext.getText(setting.get(mContext) ?
            R.string.native_debugging_summary_default_3p_allowed :
            R.string.native_debugging_summary_default_blocked);
    }
}

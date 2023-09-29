package com.android.settings.ext;

import android.content.Context;

import com.android.settings.R;

import androidx.annotation.StringRes;

public class AppPrefUtils {

    public static String getFooterForDefaultHardeningSetting(Context ctx, @StringRes int baseText) {
        return ctx.getString(R.string.app_exploit_protection_default_value_warning) + "\n\n" + ctx.getString(baseText);
    }
}

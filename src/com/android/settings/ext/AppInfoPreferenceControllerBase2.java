package com.android.settings.ext;

import android.content.Context;
import android.content.pm.GosPackageState;
import android.text.TextUtils;

import androidx.preference.Preference;

import com.android.settings.applications.appinfo.AppInfoPreferenceControllerBase;

public abstract class AppInfoPreferenceControllerBase2 extends AppInfoPreferenceControllerBase {

    protected AppInfoPreferenceControllerBase2(Context ctx, String key) {
        super(ctx, key);
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return false;
        }

        onPreferenceClick(getPackageName());
        return true;
    }

    public abstract void onPreferenceClick(String packageName);

    protected boolean hasGosPackageStateFlags(int flags) {
        var ps = GosPackageState.get(getPackageName());
        return ps != null && ps.hasFlags(flags);
    }

    protected String getPackageName() {
        return mParent.getPackageInfo().packageName;
    }
}

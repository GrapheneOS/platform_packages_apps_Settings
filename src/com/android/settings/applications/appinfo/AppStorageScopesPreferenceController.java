package com.android.settings.applications.appinfo;

import android.app.StorageScope;
import android.content.Context;
import android.content.pm.GosPackageState;
import android.text.TextUtils;

import androidx.preference.Preference;

public class AppStorageScopesPreferenceController extends AppInfoPreferenceControllerBase {
    public AppStorageScopesPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        GosPackageState ps = GosPackageState.get(getPackageName());
        if (ps != null && ps.hasFlag(GosPackageState.FLAG_STORAGE_SCOPES_ENABLED)) {
            return AVAILABLE;
        }

        return CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return false;
        }

        mContext.startActivity(StorageScope.createConfigActivityIntent(getPackageName()));
        return true;
    }

    private String getPackageName() {
        return mParent.getPackageInfo().packageName;
    }
}

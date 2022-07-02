package com.android.settings.applications.appinfo;

import android.app.StorageScope;
import android.content.Context;
import android.content.pm.GosPackageState;

import com.android.settings.ext.AppInfoPreferenceControllerBase2;

public class AppStorageScopesPreferenceController extends AppInfoPreferenceControllerBase2 {
    public AppStorageScopesPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return hasGosPackageStateFlags(GosPackageState.FLAG_STORAGE_SCOPES_ENABLED) ?
                AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void onPreferenceClick(String packageName) {
        mContext.startActivity(StorageScope.createConfigActivityIntent(packageName));
    }
}

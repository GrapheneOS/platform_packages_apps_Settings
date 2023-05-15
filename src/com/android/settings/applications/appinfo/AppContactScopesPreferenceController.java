package com.android.settings.applications.appinfo;

import android.content.Context;
import android.content.pm.GosPackageState;
import android.ext.cscopes.ContactScopesApi;

import com.android.settings.ext.AppInfoPreferenceControllerBase2;

public class AppContactScopesPreferenceController extends AppInfoPreferenceControllerBase2 {
    public AppContactScopesPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return hasGosPackageStateFlags(GosPackageState.FLAG_CONTACT_SCOPES_ENABLED) ?
                AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void onPreferenceClick(String packageName) {
        mContext.startActivity(ContactScopesApi.createConfigActivityIntent(packageName));
    }
}

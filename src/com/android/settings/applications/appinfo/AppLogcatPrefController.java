package com.android.settings.applications.appinfo;

import android.content.Context;
import android.content.Intent;
import android.ext.LogViewerApp;

import com.android.settings.ext.AppInfoPreferenceControllerBase2;

public class AppLogcatPrefController extends AppInfoPreferenceControllerBase2 {

    public AppLogcatPrefController(Context ctx, String key) {
        super(ctx, key);
    }

    @Override
    public void onPreferenceClick(String packageName) {
        Intent i = LogViewerApp.getPackageLogcatIntent(packageName);
        mContext.startActivity(i);
    }
}

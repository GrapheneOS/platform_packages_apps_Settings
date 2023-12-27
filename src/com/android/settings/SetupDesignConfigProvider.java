package com.android.settings;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

public class SetupDesignConfigProvider extends ContentProvider {
    private static final String TAG = SetupDesignConfigProvider.class.getSimpleName();

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        Log.d(TAG, "method: " + method + ", caller: " + getCallingPackage());

        var res = new Bundle();
        switch (method) {
            case "suwDefaultThemeString" ->
                res.putString(method, "glif_v4_light");

            case
                    "applyGlifThemeControlledTransition",
                    "isDynamicColorEnabled",
                    "isEmbeddedActivityOnePaneEnabled",
                    "isFullDynamicColorEnabled",
                    "IsMaterialYouStyleEnabled",
                    "isNeutralButtonStyleEnabled",
                    "isSuwDayNightEnabled" ->
                res.putBoolean(method, true);

            case "getDeviceName" -> {
                String name = Settings.Global.getString(getContext().getContentResolver(), Settings.Global.DEVICE_NAME);
                if (TextUtils.isEmpty(name)) {
                    name = Build.MODEL;
                }
                res.putCharSequence(method, name);
            }
        }
        return res;
    }

    @Override
    public Cursor query(Uri uri, String[] strings, String s, String[] strings1, String s1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getType(Uri uri) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(Uri uri, String s, String[] strings) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String s, String[] strings) {
        throw new UnsupportedOperationException();
    }
}

package com.android.settings.display;

import android.content.Context;
import android.hardware.display.ColorDisplayManager;
import android.provider.Settings;
import android.util.Log;

public final class DisplayColorModeFixup {
    private static final String TAG = "DisplayColorModeFixup";

    private DisplayColorModeFixup() {
    }

    public static void run(Context context) {
        try {
            runInner(context);
        } catch (Throwable e) {
            Log.e(TAG, "unable to initialize display color mode", e);
        }
    }

    private static void runInner(Context context) {
        final int userId = context.getUserId();
        if (Settings.System.getStringForUser(context.getContentResolver(),
                Settings.System.DISPLAY_COLOR_MODE, userId) != null) {
            return;
        }

        if (!Settings.System.putIntForUser(context.getContentResolver(),
                Settings.System.DISPLAY_COLOR_MODE,
                ColorDisplayManager.COLOR_MODE_NATURAL,
                userId)) {
            Log.w(TAG, "unable to set the default display color mode for user " + userId);
        }
    }
}

/*
 * Copyright (C) 2026 GrapheneOS
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.gestures;

import android.content.Context;
import android.ext.settings.ExtSettings;

import com.android.settings.ext.BoolSettingPrefController;

/**
 * Configures behaviour of hiding the navigation hint pill.
 */
public class HideNavigationHandleController extends BoolSettingPrefController {

    public HideNavigationHandleController(Context ctx, String key) {
        super(ctx, key, ExtSettings.HIDE_NAVIGATION_HANDLE);
    }

    @Override
    public int getAvailabilityStatus() {
        return SystemNavigationPreferenceController.isGestureAvailable(mContext)
                ? super.getAvailabilityStatus() : UNSUPPORTED_ON_DEVICE;
    }
}

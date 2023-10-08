/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.settings.biometrics.fingerprint;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.os.UserManager;
import android.provider.Settings;

import com.android.settings.Utils;
import com.android.settings.biometrics.activeunlock.ActiveUnlockStatusUtils;
import com.android.settings.core.TogglePreferenceController;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtilsInternal;

import static android.provider.Settings.Secure.BIOMETRIC_KEYGUARD_ENABLED;

// based on src/com/android/settings/biometrics/combination/BiometricSettingsKeyguardPreferenceController.java
// from android-14.0.0_r1
public class FingerprintSettingsKeyguardPreferenceController extends TogglePreferenceController {
    private static final int ON = 1;
    private static final int OFF = 0;
    private static final int DEFAULT = ON;

    private int mUserId;

    public FingerprintSettingsKeyguardPreferenceController(Context context, String key) {
        super(context, key);
    }

    protected RestrictedLockUtils.EnforcedAdmin getRestrictingAdmin() {
        return RestrictedLockUtilsInternal.checkIfKeyguardFeaturesDisabled(mContext,
                DevicePolicyManager.KEYGUARD_DISABLE_BIOMETRICS, mUserId);
    }

    public void setUserId(int userId) {
        mUserId = userId;
    }

    @Override
    public boolean isChecked() {
        return Settings.Secure.getIntForUser(mContext.getContentResolver(),
                BIOMETRIC_KEYGUARD_ENABLED, DEFAULT, mUserId) == ON;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.Secure.putIntForUser(mContext.getContentResolver(),
                BIOMETRIC_KEYGUARD_ENABLED, isChecked ? ON : OFF, mUserId);
    }

    @Override
    public int getAvailabilityStatus() {
        if (UserManager.get(mContext).isManagedProfile(mUserId)) {
            return DISABLED_FOR_USER;
        }

        return getAvailabilityFromRestrictingAdmin();
    }

    private int getAvailabilityFromRestrictingAdmin() {
        return getRestrictingAdmin() != null ? DISABLED_FOR_USER : AVAILABLE;
    }

    @Override
    public final boolean isSliceable() {
        return false;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        // not needed since it's not sliceable
        return NO_RES;
    }
}

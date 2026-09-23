/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.settings.network;

import android.content.Context;
import android.ext.settings.ExtSettings;

import androidx.annotation.VisibleForTesting;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.ext.BoolSettingPrefController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnDestroy;

/** Controls the opt-in authentication requirement for disabling airplane mode. */
public class AirplaneModeAuthenticationPreferenceController
        extends BoolSettingPrefController implements LifecycleObserver, OnDestroy {
    private final LockPatternUtils mLockPatternUtils;
    private AirplaneModeAuthenticationHelper mAuthenticationHelper;

    public AirplaneModeAuthenticationPreferenceController(Context context, String key) {
        super(context, key, ExtSettings.REQUIRE_AUTHENTICATION_TO_DISABLE_AIRPLANE_MODE);
        mLockPatternUtils = new LockPatternUtils(context);
    }

    @Override
    public int getAvailabilityStatus() {
        if (!mLockPatternUtils.isSecure(mContext.getUserId())) {
            return BasePreferenceController.CONDITIONALLY_UNAVAILABLE;
        }
        return super.getAvailabilityStatus();
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        if (!isChecked && isChecked()) {
            getAuthenticationHelper().runAfterAuthentication(this::disableAfterAuthentication);
            return false;
        }
        return super.setChecked(isChecked);
    }

    private void disableAfterAuthentication() {
        if (isChecked()) {
            super.setChecked(false);
        }
    }

    private AirplaneModeAuthenticationHelper getAuthenticationHelper() {
        if (mAuthenticationHelper == null) {
            mAuthenticationHelper = new AirplaneModeAuthenticationHelper(mContext);
        }
        return mAuthenticationHelper;
    }

    @VisibleForTesting
    void setAuthenticationHelper(AirplaneModeAuthenticationHelper authenticationHelper) {
        mAuthenticationHelper = authenticationHelper;
    }

    @Override
    public void onDestroy() {
        if (mAuthenticationHelper != null) {
            mAuthenticationHelper.cancel();
        }
    }
}

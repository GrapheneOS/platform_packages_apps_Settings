/*
 * Copyright (C) 2023 The Android Open Source Project
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
package com.android.settings.security;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;

import androidx.annotation.StringRes;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.passwordDuress.DuressPasswordUi;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.core.AbstractPreferenceController;

public class DuressPreferenceController extends AbstractPreferenceController implements
        PreferenceControllerMixin {

    private static final String KEY_DURESS_UPDATE_OR_CHANGE = "duress_password";

    protected final UserManager mUm;
    protected final LockPatternUtils mLockPatternUtils;

    protected final int mUserId = UserHandle.myUserId();

    public DuressPreferenceController(Context context, SettingsPreferenceFragment host) {
        super(context);
        mUm = context.getSystemService(UserManager.class);
        mLockPatternUtils = FeatureFactory.getFactory(context)
                .getSecurityFeatureProvider()
                .getLockPatternUtils(context);
    }

    @Override
    public boolean isAvailable() {
        return mUm.isAdminUser();
    }

    @Override
    public String getPreferenceKey() {
        return KEY_DURESS_UPDATE_OR_CHANGE;
    }

    @Override
    public void updateState(Preference preference) {
        preference.setEnabled(mLockPatternUtils.isSecure(mUserId));
        preference.setSummary(getLockStatusSummary(preference.isEnabled()));
    }

    @StringRes
    private int getLockStatusSummary(boolean isEnabled ){

        if (!isEnabled) {
            return R.string.add_a_lock_screen_first;
        }

        boolean duressCredentialsExist = mLockPatternUtils.validDuressCredentialsExist();
        if (duressCredentialsExist) {
            return R.string.duress_pin_and_password_exist;
        } else {
            return R.string.added_a_duress_pin_and_password;
        }
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return super.handlePreferenceTreeClick(preference);
        }
        new AlertDialog.Builder(preference.getContext())
                .setTitle(R.string.duress_title)
                .setMessage(R.string.duress_descriptions)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> launchSettingsFragment())
                .setNegativeButton(android.R.string.cancel,null)
                .show();
        return true;
    }

    private boolean launchSettingsFragment() {
        mContext.startActivity(new Intent(mContext, DuressPasswordUi.class));
        return true;
    }

}

/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.connecteddevice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.nfc.NfcAdapter;
import android.os.UserManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.android.settingslib.core.lifecycle.events.OnStop;

/**
 * Controller that used to show NFC and payment features
 */
public class NfcAndPaymentFragmentController extends BasePreferenceController
        implements LifecycleObserver, OnResume, OnStop {
    private final NfcAdapter mNfcAdapter;
    private final PackageManager mPackageManager;
    private final UserManager mUserManager;
    private final IntentFilter mIntentFilter;
    private Preference mPreference;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mPreference == null) {
                return;
            }

            final String action = intent.getAction();
            if (NfcAdapter.ACTION_ADAPTER_STATE_CHANGED.equals(action)) {
                refreshSummary(mPreference);
            }
        }
    };

    public NfcAndPaymentFragmentController(Context context, String preferenceKey) {
        super(context, preferenceKey);

        mPackageManager = context.getPackageManager();
        mUserManager = context.getSystemService(UserManager.class);
        mNfcAdapter = NfcAdapter.getDefaultAdapter(context);

        mIntentFilter = isNfcAvailable()
                ? new IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED) : null;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public int getAvailabilityStatus() {
        if (!mPackageManager.hasSystemFeature(PackageManager.FEATURE_NFC)
                || !mPackageManager.hasSystemFeature(
                PackageManager.FEATURE_NFC_HOST_CARD_EMULATION)) {
            return UNSUPPORTED_ON_DEVICE;
        }
        return AVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        if (mNfcAdapter != null) {
            if (mNfcAdapter.isEnabled()) {
                return mContext.getText(R.string.nfc_setting_on);
            } else {
                return mContext.getText(R.string.nfc_setting_off);
            }
        }
        return null;
    }

    @Override
    public void onStop() {
        if (!isNfcAvailable()) {
            return;
        }

        mContext.unregisterReceiver(mReceiver);
    }

    @Override
    public void onResume() {
        if (!isNfcAvailable()) {
            return;
        }

        mContext.registerReceiver(mReceiver, mIntentFilter);
    }

    private boolean isNfcAvailable() {
        return mNfcAdapter != null;
    }
}

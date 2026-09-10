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

package com.android.settings.network;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class AdaptiveConnectivityPreferenceControllerTest {

    private static final String PREF_KEY = "adaptive_connectivity";

    private Context mContext;
    @Mock private Resources mResources;
    @Mock private TelephonyManager mTelephonyManager;
    private AdaptiveConnectivityPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        doReturn(mResources).when(mContext).getResources();
        doReturn(mTelephonyManager).when(mContext).getSystemService(TelephonyManager.class);
        when(mResources.getBoolean(R.bool.config_show_sim_info)).thenReturn(true);
        when(mTelephonyManager.isDataCapable()).thenReturn(true);

        mController = new AdaptiveConnectivityPreferenceController(mContext, PREF_KEY);
        // Clear settings before each test
        Settings.Secure.putString(mContext.getContentResolver(),
                Settings.Secure.ADAPTIVE_CONNECTIVITY_ENABLED, null);
        Settings.Secure.putString(mContext.getContentResolver(),
                Settings.Secure.ADAPTIVE_CONNECTIVITY_WIFI_ENABLED, null);
        Settings.Secure.putString(mContext.getContentResolver(),
                Settings.Secure.ADAPTIVE_CONNECTIVITY_MOBILE_NETWORK_ENABLED, null);
    }

    @Test
    public void isAvailable_supportAdaptiveConnectivity_shouldReturnTrue() {
        when(mResources.getBoolean(R.bool.config_show_adaptive_connectivity))
                .thenReturn(true);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_mobileOptimizationNotSupported_shouldReturnTrue() {
        when(mResources.getBoolean(R.bool.config_show_adaptive_connectivity))
                .thenReturn(false);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_mobileDataNotCapable_shouldReturnFalse() {
        when(mResources.getBoolean(R.bool.config_show_adaptive_connectivity))
                .thenReturn(true);
        when(mTelephonyManager.isDataCapable()).thenReturn(false);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void getSummary_wifiOn_mobileOff_shouldShowOn() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ADAPTIVE_CONNECTIVITY_WIFI_ENABLED, 1);
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ADAPTIVE_CONNECTIVITY_MOBILE_NETWORK_ENABLED, 0);

        assertThat(mController.getSummary()).isEqualTo(
                mContext.getString(R.string.adaptive_connectivity_switch_on));
    }

    @Test
    public void getSummary_wifiOff_mobileOn_shouldShowOn() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ADAPTIVE_CONNECTIVITY_WIFI_ENABLED, 0);
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ADAPTIVE_CONNECTIVITY_MOBILE_NETWORK_ENABLED, 1);

        assertThat(mController.getSummary()).isEqualTo(
                mContext.getString(R.string.adaptive_connectivity_switch_on));
    }

    @Test
    public void getSummary_wifiOn_mobileOn_shouldShowOn() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ADAPTIVE_CONNECTIVITY_WIFI_ENABLED, 1);
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ADAPTIVE_CONNECTIVITY_MOBILE_NETWORK_ENABLED, 1);

        assertThat(mController.getSummary()).isEqualTo(
                mContext.getString(R.string.adaptive_connectivity_switch_on));
    }

    @Test
    public void getSummary_wifiOff_mobileOff_shouldShowOff() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ADAPTIVE_CONNECTIVITY_WIFI_ENABLED, 0);
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ADAPTIVE_CONNECTIVITY_MOBILE_NETWORK_ENABLED, 0);

        assertThat(mController.getSummary()).isEqualTo(
                mContext.getString(R.string.adaptive_connectivity_switch_off));
    }

    @Test
    public void getSummary_wifiOn_mobileNotSet_shouldShowOn() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ADAPTIVE_CONNECTIVITY_WIFI_ENABLED, 1);

        assertThat(mController.getSummary()).isEqualTo(
                mContext.getString(R.string.adaptive_connectivity_switch_on));
    }

    @Test
    public void getSummary_wifiNotSet_mobileOn_shouldShowOn() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ADAPTIVE_CONNECTIVITY_MOBILE_NETWORK_ENABLED, 1);

        assertThat(mController.getSummary()).isEqualTo(
                mContext.getString(R.string.adaptive_connectivity_switch_on));
    }

    @Test
    public void getSummary_wifiOff_mobileNotSet_shouldShowOff() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ADAPTIVE_CONNECTIVITY_WIFI_ENABLED, 0);

        assertThat(mController.getSummary()).isEqualTo(
                mContext.getString(R.string.adaptive_connectivity_switch_off));
    }

    @Test
    public void getSummary_wifiNotSet_mobileOff_shouldShowOff() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ADAPTIVE_CONNECTIVITY_MOBILE_NETWORK_ENABLED, 0);

        assertThat(mController.getSummary()).isEqualTo(
                mContext.getString(R.string.adaptive_connectivity_switch_off));
    }

    @Test
    public void getSummary_newSettingsNotSet_fallbackToEnabled_shouldShowOn() {
        // When the new settings are not set, the controller should fall back to the old global
        // setting. Set the old setting to ON.
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ADAPTIVE_CONNECTIVITY_ENABLED, 1);

        // Verify the summary shows "On".
        assertThat(mController.getSummary()).isEqualTo(
                mContext.getString(R.string.adaptive_connectivity_switch_on));
    }

    @Test
    public void getSummary_newSettingsNotSet_fallbackToDisabled_shouldShowOff() {
        // When the new settings are not set, the controller should fall back to the old global
        // setting. Set the old setting to OFF.
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ADAPTIVE_CONNECTIVITY_ENABLED, 0);

        // Verify the summary shows "Off".
        assertThat(mController.getSummary()).isEqualTo(
                mContext.getString(R.string.adaptive_connectivity_switch_off));
    }
}

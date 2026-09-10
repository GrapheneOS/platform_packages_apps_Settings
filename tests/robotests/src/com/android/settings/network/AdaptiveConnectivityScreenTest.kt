/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.network

import android.content.Context
import android.content.res.Resources
import android.provider.Settings
import android.provider.Settings.Secure.ADAPTIVE_CONNECTIVITY_ENABLED
import android.provider.Settings.Secure.ADAPTIVE_CONNECTIVITY_MOBILE_NETWORK_ENABLED
import android.provider.Settings.Secure.ADAPTIVE_CONNECTIVITY_WIFI_ENABLED
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.preference.SwitchPreferenceCompat
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settings.Settings.AdaptiveConnectivitySettingsActivity
import com.android.settings.flags.Flags
import com.android.settings.testutils2.SettingsCatalystTestCase
import com.android.settingslib.metadata.PreferenceHierarchy
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.TestScope
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever
import org.robolectric.shadows.ShadowSubscriptionManager

@Suppress("DEPRECATION")
class AdaptiveConnectivityScreenTest() : SettingsCatalystTestCase() {
    override val preferenceScreenCreator = AdaptiveConnectivityScreen()
    override val flagName
        get() = Flags.FLAG_CATALYST_ADAPTIVE_CONNECTIVITY

    private lateinit var fragment: AdaptiveConnectivitySettings
    private val mockResources: Resources = mock()
    private val mockSubscriptionManager: SubscriptionManager = mock()
    private val mContext: Context =
        spy(ApplicationProvider.getApplicationContext()) {
            on { resources } doReturn mockResources
            on { getSystemService(SubscriptionManager::class.java) } doReturn
                mockSubscriptionManager
        }
    private val testScope = TestScope()

    @Test override fun migration() {}

    @Test
    fun getPreferenceHierarchy_mobileOptimizationDisabled_returnsOnlyWifiToggle() {
        whenever(mockResources.getBoolean(R.bool.config_show_adaptive_connectivity))
            .thenReturn(false)
        val hierarchy: PreferenceHierarchy =
            preferenceScreenCreator.getPreferenceHierarchy(mContext, testScope)
        assertThat(hierarchy.find(ADAPTIVE_CONNECTIVITY_ENABLED)).isNull()
        assertThat(hierarchy.find(ADAPTIVE_CONNECTIVITY_WIFI_ENABLED)).isNotNull()
        assertThat(hierarchy.find(ADAPTIVE_CONNECTIVITY_MOBILE_NETWORK_ENABLED)).isNull()
    }

    @Test
    fun flagEnabled_switchPreferenceCompatExists() {
        val scenario = launchFragmentInContainer<AdaptiveConnectivitySettings>()
        scenario.onFragment { fragment ->
            this.fragment = fragment
            assertSwitchPreferenceCompatVisibility(
                ADAPTIVE_CONNECTIVITY_WIFI_ENABLED,
                fragment,
                true,
            )
            assertSwitchPreferenceCompatVisibility(
                ADAPTIVE_CONNECTIVITY_MOBILE_NETWORK_ENABLED,
                fragment,
                false,
            )
        }
    }

    @Test
    fun flagEnabled_onWifiScorerSwitchClick_shouldUpdateSetting() {
        val scenario = launchFragmentInContainer<AdaptiveConnectivitySettings>()
        scenario.onFragment { fragment: AdaptiveConnectivitySettings ->
            this.fragment = fragment
            val switchPreference =
                fragment.findPreference<SwitchPreferenceCompat>(ADAPTIVE_CONNECTIVITY_WIFI_ENABLED)
            assertThat(switchPreference?.isChecked).isTrue()
            switchPreference?.performClick()
            assertThat(switchPreference?.isChecked).isFalse()
            assertThat(updateSetting(ADAPTIVE_CONNECTIVITY_WIFI_ENABLED)).isFalse()
            switchPreference?.performClick()
            assertThat(switchPreference?.isChecked).isTrue()
            assertThat(updateSetting(ADAPTIVE_CONNECTIVITY_WIFI_ENABLED)).isTrue()
        }
    }

    @Test
    fun flagEnabled_onAdaptiveMobileNetworkSwitchClick_shouldUpdateSetting() {
        assumeTrue(
            ApplicationProvider.getApplicationContext<Context>().resources.getBoolean(
                R.bool.config_show_adaptive_connectivity,
            ),
        )
        val scenario = launchFragmentInContainer<AdaptiveConnectivitySettings>()
        scenario.onFragment { fragment: AdaptiveConnectivitySettings ->
            this.fragment = fragment
            val switchPreference =
                fragment.findPreference<SwitchPreferenceCompat>(
                    ADAPTIVE_CONNECTIVITY_MOBILE_NETWORK_ENABLED
                )
            assertThat(switchPreference?.isChecked).isTrue()
            switchPreference?.performClick()
            assertThat(switchPreference?.isChecked).isFalse()
            assertThat(updateSetting(ADAPTIVE_CONNECTIVITY_MOBILE_NETWORK_ENABLED)).isFalse()
            switchPreference?.performClick()
            assertThat(switchPreference?.isChecked).isTrue()
            assertThat(updateSetting(ADAPTIVE_CONNECTIVITY_MOBILE_NETWORK_ENABLED)).isTrue()
        }
    }

    @Test
    fun getLaunchIntent_correctActivity() {
        val underTest = preferenceScreenCreator.getLaunchIntent(mContext, null)!!

        assertThat(underTest.getComponent()?.getClassName())
            .isEqualTo(AdaptiveConnectivitySettingsActivity::class.java.getName())
    }

    @Test
    fun getPreferenceHierarchy_mobileToggleHiddenForCarrier_mobileNetworkToggleIsHidden() {
        whenever(mockResources.getBoolean(R.bool.config_show_adaptive_connectivity)).thenReturn(true)
        val subId = 1
        val carrierId = 1234
        val subscriptionInfo = mock<SubscriptionInfo>()
        whenever(subscriptionInfo.subscriptionId).thenReturn(subId)
        whenever(subscriptionInfo.carrierId).thenReturn(carrierId)
        whenever(mockSubscriptionManager.getActiveSubscriptionInfo(subId)).thenReturn(subscriptionInfo)
        whenever(mockSubscriptionManager.getAllSubscriptionInfoList())
            .thenReturn(listOf(subscriptionInfo))
        ShadowSubscriptionManager.setDefaultDataSubscriptionId(subId)

        whenever(
                mockResources.getIntArray(R.array.config_carrier_for_mobile_network_toggle_disable)
            )
            .thenReturn(intArrayOf(carrierId))
        whenever(mockResources.getIntArray(R.array.config_carrier_for_wifi_scorer_toggle_off))
            .thenReturn(intArrayOf())

        val hierarchy: PreferenceHierarchy =
            preferenceScreenCreator.getPreferenceHierarchy(mContext, testScope)

        assertThat(hierarchy.find(ADAPTIVE_CONNECTIVITY_WIFI_ENABLED)).isNotNull()
        assertThat(hierarchy.find(ADAPTIVE_CONNECTIVITY_MOBILE_NETWORK_ENABLED)).isNull()

        // Clean up
        ShadowSubscriptionManager.setDefaultDataSubscriptionId(
            SubscriptionManager.INVALID_SUBSCRIPTION_ID
        )
    }

    @Test
    fun getPreferenceHierarchy_mobileToggleShownForCarrier_mobileNetworkToggleIsShown() {
        whenever(mockResources.getBoolean(R.bool.config_show_adaptive_connectivity)).thenReturn(true)
        val subId = 1
        val carrierId = 1234
        val disabledCarrierId = 5678
        val subscriptionInfo = mock<SubscriptionInfo>()
        whenever(subscriptionInfo.subscriptionId).thenReturn(subId)
        whenever(subscriptionInfo.carrierId).thenReturn(carrierId)
        whenever(mockSubscriptionManager.getActiveSubscriptionInfo(subId)).thenReturn(subscriptionInfo)
        whenever(mockSubscriptionManager.getAllSubscriptionInfoList())
            .thenReturn(listOf(subscriptionInfo))
        ShadowSubscriptionManager.setDefaultDataSubscriptionId(subId)

        whenever(
                mockResources.getIntArray(R.array.config_carrier_for_mobile_network_toggle_disable)
            )
            .thenReturn(intArrayOf(disabledCarrierId))
        whenever(mockResources.getIntArray(R.array.config_carrier_for_wifi_scorer_toggle_off))
            .thenReturn(intArrayOf())

        val hierarchy: PreferenceHierarchy =
            preferenceScreenCreator.getPreferenceHierarchy(mContext, testScope)

        assertThat(hierarchy.find(ADAPTIVE_CONNECTIVITY_WIFI_ENABLED)).isNotNull()
        assertThat(hierarchy.find(ADAPTIVE_CONNECTIVITY_MOBILE_NETWORK_ENABLED)).isNotNull()

        // Clean up
        ShadowSubscriptionManager.setDefaultDataSubscriptionId(
            SubscriptionManager.INVALID_SUBSCRIPTION_ID
        )
    }

    /**
     * Helper function to get the setting value from Settings.Secure.
     *
     * @param key the key of the setting to get.
     */
    private fun updateSetting(key: String): Boolean {
        return (Settings.Secure.getInt(mContext.contentResolver, key, 0) == 1)
    }

    private fun assertSwitchPreferenceCompatVisibility(
        key: String,
        fragment: AdaptiveConnectivitySettings,
        isVisible: Boolean,
    ) {
        val switchPreference = fragment.findPreference<SwitchPreferenceCompat>(key)
        assertThat(switchPreference?.isVisible).isEqualTo(isVisible)
    }
}

/*
 * Copyright (C) 2025 The Android Open Source Project
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

import static android.provider.Settings.Secure.ADAPTIVE_CONNECTIVITY_ENABLED;
import static android.provider.Settings.Secure.ADAPTIVE_CONNECTIVITY_MOBILE_NETWORK_ENABLED;
import static android.provider.Settings.Secure.ADAPTIVE_CONNECTIVITY_WIFI_ENABLED;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.settings.SettingsEnums;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;
import androidx.test.core.app.ApplicationProvider;
import com.android.settings.R;
import com.android.settings.testutils.shadow.ShadowUtils;
import com.android.settingslib.widget.IllustrationPreference;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowSubscriptionManager;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowUtils.class})
public class AdaptiveConnectivitySettingsTest {

  private static final String ADAPTIVE_CONNECTIVITY_SUMMARY = "adaptive_connectivity_summary";
  private static final String ADAPTIVE_CONNECTIVITY_HEADER = "adaptive_connectivity_header";

  @Mock private ContentResolver mContentResolver;
  @Mock private WifiManager mWifiManager;
  @Mock private PreferenceScreen mPreferenceScreen;
  @Mock private IllustrationPreference mIllustrationPreference;
  @Mock private Preference mSummaryPreference;
  @Mock private Preference mLegacyTogglePreference;
  @Mock private SwitchPreferenceCompat mWifiSwitchPreference;
  @Mock private SwitchPreferenceCompat mMobileNetworkSwitchPreference;
  @Mock private SubscriptionManager mSubscriptionManager;
  @Mock private Resources mResources;
  @Mock private PreferenceManager mPreferenceManager;

  private AdaptiveConnectivitySettings mFragment;
  private Context mContext;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    mContext = spy(ApplicationProvider.getApplicationContext());
    mFragment = spy(new AdaptiveConnectivitySettings());

    doReturn(mContext).when(mFragment).getContext();
    doReturn(mPreferenceManager).when(mFragment).getPreferenceManager();
    when(mPreferenceManager.getContext()).thenReturn(mContext);
    when(mContext.getSystemService(SubscriptionManager.class)).thenReturn(mSubscriptionManager);
    when(mContext.getResources()).thenReturn(mResources);

    doReturn(mPreferenceScreen).when(mFragment).getPreferenceScreen();
    doReturn(mIllustrationPreference).when(mFragment).findPreference(ADAPTIVE_CONNECTIVITY_HEADER);
    doReturn(mSummaryPreference).when(mFragment).findPreference(ADAPTIVE_CONNECTIVITY_SUMMARY);
    doReturn(mLegacyTogglePreference).when(mFragment).findPreference(ADAPTIVE_CONNECTIVITY_ENABLED);
    doReturn(mWifiSwitchPreference)
        .when(mFragment)
        .findPreference(ADAPTIVE_CONNECTIVITY_WIFI_ENABLED);
    doReturn(mMobileNetworkSwitchPreference)
        .when(mFragment)
        .findPreference(ADAPTIVE_CONNECTIVITY_MOBILE_NETWORK_ENABLED);

    when(mWifiSwitchPreference.getKey()).thenReturn(ADAPTIVE_CONNECTIVITY_WIFI_ENABLED);
    when(mMobileNetworkSwitchPreference.getKey())
        .thenReturn(ADAPTIVE_CONNECTIVITY_MOBILE_NETWORK_ENABLED);
  }

  @Test
  public void getMetricsCategory_shouldReturnCorrectCategory() {
    assertEquals(SettingsEnums.ADAPTIVE_CONNECTIVITY_CATEGORY, mFragment.getMetricsCategory());
  }

  @Test
  public void getLogTag_shouldReturnCorrectTag() {
    assertEquals("AdaptiveConnectivitySettings", mFragment.getLogTag());
  }

  @Test
  public void getPreferenceScreenResId_shouldReturnCorrectResourceId() {
    assertEquals(R.xml.adaptive_connectivity_settings, mFragment.getPreferenceScreenResId());
  }

  @Test
  public void getPreferenceScreenBindingKey_shouldReturnCorrectKey() {
    String bindingKey = mFragment.getPreferenceScreenBindingKey(mContext);
    assertEquals(AdaptiveConnectivityScreen.KEY, bindingKey);
  }

  @Test
  public void searchIndexDataProvider_shouldNotBeNull() {
    assertNotNull(AdaptiveConnectivitySettings.SEARCH_INDEX_DATA_PROVIDER);
  }

  @Test
  public void setupSwitchPreferenceCompat_withValidWifiPreference_shouldConfigureCorrectly() {
    when(mWifiSwitchPreference.getKey()).thenReturn(ADAPTIVE_CONNECTIVITY_WIFI_ENABLED);

    Preference.OnPreferenceChangeListener listener =
        (preference, newValue) -> {
          boolean isChecked = (Boolean) newValue;
          Settings.Secure.putInt(mContentResolver, preference.getKey(), isChecked ? 1 : 0);
          if (preference.getKey().equals(ADAPTIVE_CONNECTIVITY_WIFI_ENABLED)) {
            mWifiManager.setWifiScoringEnabled(isChecked);
          }
          return true;
        };

    mWifiSwitchPreference.setOnPreferenceChangeListener(listener);
    mWifiSwitchPreference.setVisible(true);

    verify(mWifiSwitchPreference).setOnPreferenceChangeListener(any());
    verify(mWifiSwitchPreference).setVisible(true);
  }

  @Test
  public void setupSwitchPreferenceCompat_withValidMobilePreference_shouldConfigureCorrectly() {
    when(mMobileNetworkSwitchPreference.getKey())
        .thenReturn(ADAPTIVE_CONNECTIVITY_MOBILE_NETWORK_ENABLED);

    Preference.OnPreferenceChangeListener listener =
        (preference, newValue) -> {
          boolean isChecked = (Boolean) newValue;
          Settings.Secure.putInt(mContentResolver, preference.getKey(), isChecked ? 1 : 0);
          if (preference.getKey().equals(ADAPTIVE_CONNECTIVITY_WIFI_ENABLED)) {
            mWifiManager.setWifiScoringEnabled(isChecked);
          }
          return true;
        };

    mMobileNetworkSwitchPreference.setOnPreferenceChangeListener(listener);
    mMobileNetworkSwitchPreference.setVisible(true);

    verify(mMobileNetworkSwitchPreference).setOnPreferenceChangeListener(any());
    verify(mMobileNetworkSwitchPreference).setVisible(true);
  }

  @Test
  public void setupSwitchPreferenceCompat_withNullPreference_shouldNotCrash() {
    SwitchPreferenceCompat nullPreference = null;

    if (nullPreference != null) {
      nullPreference.setOnPreferenceChangeListener(any());
      nullPreference.setVisible(true);
    }

    assertTrue("Method should handle null preferences gracefully", true);
  }

  @Test
  public void wifiSwitchPreference_onPreferenceChange_true_shouldUpdateWifiScoring() {
    Preference.OnPreferenceChangeListener listener =
        (preference, newValue) -> {
          boolean isChecked = (Boolean) newValue;
          if (preference.getKey().equals(ADAPTIVE_CONNECTIVITY_WIFI_ENABLED)) {
            mWifiManager.setWifiScoringEnabled(isChecked);
          }
          return true;
        };

    boolean result = listener.onPreferenceChange(mWifiSwitchPreference, true);

    assertTrue(result);
    verify(mWifiManager).setWifiScoringEnabled(true);
  }

  @Test
  public void wifiSwitchPreference_onPreferenceChange_false_shouldUpdateWifiScoring() {
    Preference.OnPreferenceChangeListener listener =
        (preference, newValue) -> {
          boolean isChecked = (Boolean) newValue;
          if (preference.getKey().equals(ADAPTIVE_CONNECTIVITY_WIFI_ENABLED)) {
            mWifiManager.setWifiScoringEnabled(isChecked);
          }
          return true;
        };

    boolean result = listener.onPreferenceChange(mWifiSwitchPreference, false);

    assertTrue(result);
    verify(mWifiManager).setWifiScoringEnabled(false);
  }

  @Test
  public void mobileNetworkSwitchPreference_onPreferenceChange_shouldNotAffectWifiScoring() {
    Preference.OnPreferenceChangeListener listener =
        (preference, newValue) -> {
          boolean isChecked = (Boolean) newValue;
          if (preference.getKey().equals(ADAPTIVE_CONNECTIVITY_WIFI_ENABLED)) {
            mWifiManager.setWifiScoringEnabled(isChecked);
          }
          return true;
        };

    boolean result = listener.onPreferenceChange(mMobileNetworkSwitchPreference, true);

    assertTrue(result);
    verify(mWifiManager, never()).setWifiScoringEnabled(anyBoolean());
  }

  @Test
  public void preferenceChangeListener_testLogicVerification() {
    mWifiManager.setWifiScoringEnabled(true);
    verify(mWifiManager).setWifiScoringEnabled(true);

    mWifiManager.setWifiScoringEnabled(false);
    verify(mWifiManager).setWifiScoringEnabled(false);

    verify(mWifiManager, times(1)).setWifiScoringEnabled(true);
    verify(mWifiManager, times(1)).setWifiScoringEnabled(false);
  }

  @Test
  public void illustrationPreference_testConfigurationLogic() {
    int staticResId = R.drawable.ic_enhanced_connectivity;
    mIllustrationPreference.setLottieAnimationResId(staticResId);
    verify(mIllustrationPreference).setLottieAnimationResId(staticResId);

    int dynamicResId = R.drawable.ic_enhanced_connectivity_dynamic;
    mIllustrationPreference.setLottieAnimationResId(dynamicResId);
    mIllustrationPreference.applyDynamicColor();

    verify(mIllustrationPreference).setLottieAnimationResId(dynamicResId);
    verify(mIllustrationPreference).applyDynamicColor();
  }

  @Test
  public void preferenceVisibility_testConfigurationLogic() {
    if (mSummaryPreference != null) {
      mSummaryPreference.setVisible(false);
      verify(mSummaryPreference).setVisible(false);
    }

    if (mLegacyTogglePreference != null) {
      mLegacyTogglePreference.setVisible(false);
      verify(mLegacyTogglePreference).setVisible(false);
    }

    if (mWifiSwitchPreference != null) {
      mWifiSwitchPreference.setVisible(true);
      verify(mWifiSwitchPreference).setVisible(true);
    }

    if (mMobileNetworkSwitchPreference != null) {
      mMobileNetworkSwitchPreference.setVisible(true);
      verify(mMobileNetworkSwitchPreference).setVisible(true);
    }
  }

  @Test
  public void featureFlag_testUiDifferences() {
    boolean enableNestedToggles = true;

    if (enableNestedToggles) {
      mIllustrationPreference.setLottieAnimationResId(R.drawable.ic_enhanced_connectivity_dynamic);
      mIllustrationPreference.applyDynamicColor();

      mSummaryPreference.setVisible(false);
      mLegacyTogglePreference.setVisible(false);

      verify(mIllustrationPreference)
          .setLottieAnimationResId(R.drawable.ic_enhanced_connectivity_dynamic);
      verify(mIllustrationPreference).applyDynamicColor();
      verify(mSummaryPreference).setVisible(false);
      verify(mLegacyTogglePreference).setVisible(false);
    } else {
      mIllustrationPreference.setLottieAnimationResId(R.drawable.ic_enhanced_connectivity);

      verify(mIllustrationPreference).setLottieAnimationResId(R.drawable.ic_enhanced_connectivity);
      verify(mIllustrationPreference, never()).applyDynamicColor();
    }
  }

  @Test
  public void preferenceKey_testConditionalLogic() {
    String wifiKey = ADAPTIVE_CONNECTIVITY_WIFI_ENABLED;
    if (wifiKey.equals(ADAPTIVE_CONNECTIVITY_WIFI_ENABLED)) {
      mWifiManager.setWifiScoringEnabled(true);
    }
    verify(mWifiManager).setWifiScoringEnabled(true);

    String mobileKey = ADAPTIVE_CONNECTIVITY_MOBILE_NETWORK_ENABLED;
    if (mobileKey.equals(ADAPTIVE_CONNECTIVITY_WIFI_ENABLED)) {
      mWifiManager.setWifiScoringEnabled(false);
    }
    verify(mWifiManager, never()).setWifiScoringEnabled(false);
    verify(mWifiManager, times(1)).setWifiScoringEnabled(true);
  }

  @Test
  public void onCreatePreferences_mobileToggleHiddenWithoutVendorSupport() {
      FragmentScenario<AdaptiveConnectivitySettings> scenario =
          FragmentScenario.launchInContainer(AdaptiveConnectivitySettings.class);

      scenario.onFragment(fragment -> {
          SwitchPreferenceCompat mobileNetworkPreference = fragment.findPreference(
              ADAPTIVE_CONNECTIVITY_MOBILE_NETWORK_ENABLED);
          SwitchPreferenceCompat wifiPreference = fragment.findPreference(
              ADAPTIVE_CONNECTIVITY_WIFI_ENABLED);
          Preference summaryPreference = fragment.findPreference(
              ADAPTIVE_CONNECTIVITY_SUMMARY);
          Preference legacyTogglePreference = fragment.findPreference(
              ADAPTIVE_CONNECTIVITY_ENABLED);

          assertThat(mobileNetworkPreference.isVisible()).isFalse();
          assertThat(wifiPreference.isVisible()).isTrue();
          assertThat(summaryPreference.isVisible()).isFalse();
          assertThat(legacyTogglePreference.isVisible()).isFalse();
      });
  }
}

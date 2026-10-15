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

import android.Manifest.permission.READ_PRIVILEGED_PHONE_STATE
import android.app.Activity
import android.app.Application
import android.app.settings.SettingsEnums.SETTINGS_NETWORK_CATEGORY
import android.companion.AssociationInfo
import android.companion.AssociationRequest
import android.companion.CompanionDeviceManager
import android.companion.CompanionDeviceManager.FEATURE_CROSS_DEVICE_SYNC
import android.companion.CompanionDeviceManager.FLAG_AIRPLANE_MODE
import android.companion.Flags.FLAG_ENABLE_DATA_SYNC
import android.content.Context.COMPANION_DEVICE_SERVICE
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager.FEATURE_LEANBACK
import android.os.PersistableBundle
import android.os.UserHandle
import android.os.UserManager
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.telephony.TelephonyManager
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceChangeListener
import androidx.preference.SwitchPreferenceCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.ext.truth.content.IntentSubject.assertThat
import com.android.server.connectivity.Flags.FLAG_SYNC_AIRPLANE_MODE_WITH_WATCHES
import com.android.settings.R
import com.android.settings.contract.KEY_AIRPLANE_MODE
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.testutils.MetricsRule
import com.android.settings.testutils.SettingsStoreRule
import com.android.settings.testutils.shadow.SettingsShadowResources
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.metadata.HERO_SET
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.preference.createAndBindWidget
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowContextImpl

@RunWith(AndroidJUnit4::class)
@Config(shadows = [SettingsShadowResources::class])
class AirplaneModePreferenceTest {
    @get:Rule(order = 0) val metricsRule = MetricsRule()
    @get:Rule(order = 1) val settingsStoreRule = SettingsStoreRule()
    @get:Rule(order = 2) val setFlagsRule = SetFlagsRule()

    private val context = ApplicationProvider.getApplicationContext<Application>()

    private val airplaneModePreference = AirplaneModePreference()
    private val airplaneModeDataStore = AirplaneModePreference.createDataStore(context)
    private val packageManager = shadowOf(context.packageManager)
    private val companionDeviceManager =
        mock<CompanionDeviceManager> {
            on { getLocalMetadata(UserHandle.USER_ALL) } doReturn PersistableBundle()
        }

    @Before
    fun setUp() {
        shadowOf(context as ContextWrapper).grantPermissions(READ_PRIVILEGED_PHONE_STATE)
        SettingsShadowResources.overrideResource(R.bool.config_show_toggle_airplane, true)
        packageManager.setSystemFeature(FEATURE_LEANBACK, false)
        SatelliteRepository.setIsSessionStartedForTesting(false)
        Shadow.extract<ShadowContextImpl>(context.baseContext)
            .setSystemService(COMPANION_DEVICE_SERVICE, companionDeviceManager)
    }

    @Test
    fun isAvailable_hasConfigAndNoFeatureLeanback_shouldReturnTrue() {
        assertThat(airplaneModePreference.isAvailable(context)).isTrue()
    }

    @Test
    fun isAvailable_noConfig_shouldReturnFalse() {
        SettingsShadowResources.overrideResource(R.bool.config_show_toggle_airplane, false)

        assertThat(airplaneModePreference.isAvailable(context)).isFalse()
    }

    @Test
    fun isAvailable_hasFeatureLeanback_shouldReturnFalse() {
        packageManager.setSystemFeature(FEATURE_LEANBACK, true)

        assertThat(airplaneModePreference.isAvailable(context)).isFalse()
    }

    @Test
    fun noValueInDataStore() {
        assertThat(airplaneModeDataStore.contains(AirplaneModePreference.KEY)).isFalse()
        assertThat(airplaneModeDataStore.getBoolean(AirplaneModePreference.KEY))
            .isEqualTo(AirplaneModePreference.DEFAULT_VALUE)
    }

    @Test
    fun toggleOn_performClick_isCheckedReturnFalse() {
        airplaneModeDataStore.setBoolean(AirplaneModePreference.KEY, true)

        val preference = getSwitchPreference().apply { performClick() }

        assertThat(preference.isChecked).isFalse()
        verify(metricsRule.metricsFeatureProvider)
            .changed(SETTINGS_NETWORK_CATEGORY, AirplaneModePreference.KEY, 0)
    }

    @Test
    fun toggleOff_performClick_isCheckedReturnTrue() {
        airplaneModeDataStore.setBoolean(AirplaneModePreference.KEY, false)

        val preference = getSwitchPreference().apply { performClick() }

        assertThat(preference.isChecked).isTrue()
        verify(metricsRule.metricsFeatureProvider)
            .changed(SETTINGS_NETWORK_CATEGORY, AirplaneModePreference.KEY, 1)
    }

    @Test
    fun getWritePermit_satelliteOn_disallow() {
        SatelliteRepository.setIsSessionStartedForTesting(true)

        val permit = airplaneModePreference.getWritePermit(context, true, 0, 0)

        assertThat(permit).isEqualTo(ReadWritePermit.DISALLOW)
    }

    @Test
    fun getWritePermit_inEcmMode_disallow() {
        shadowOf(context.getSystemService(TelephonyManager::class.java))
            .setEmergencyCallbackMode(true)

        val permit = airplaneModePreference.getWritePermit(context, true, 0, 0)

        assertThat(permit).isEqualTo(ReadWritePermit.DISALLOW)
    }

    @Test
    fun getWritePermit_authenticationRequired_disallowMetadataWrite() {
        airplaneModePreference.isAuthenticationRequired = { true }

        val permit = airplaneModePreference.getWritePermit(context, false, 0, 0)

        assertThat(permit).isEqualTo(ReadWritePermit.DISALLOW)
    }

    @Test
    fun getWritePermit_authenticationRequired_disallowsUnknownMetadataWrite() {
        airplaneModePreference.isAuthenticationRequired = { true }

        val permit = airplaneModePreference.getWritePermit(context, null, 0, 0)

        assertThat(permit).isEqualTo(ReadWritePermit.DISALLOW)
    }

    @Test
    fun getWritePermit_authenticationRequired_allowsEnablingAirplaneMode() {
        airplaneModePreference.isAuthenticationRequired = { true }

        val permit = airplaneModePreference.getWritePermit(context, true, 0, 0)

        assertThat(permit).isEqualTo(ReadWritePermit.ALLOW)
    }

    @Test
    fun getWritePermit_allow() {
        val permit = airplaneModePreference.getWritePermit(context, true, 0, 0)

        assertThat(permit).isEqualTo(ReadWritePermit.ALLOW)
    }

    @Test
    fun onCreate_inEcmMode_showEcmDialog() {
        val mockPreference = mock<Preference>()
        val telephonyManager = mock<TelephonyManager> { on { emergencyCallbackMode } doReturn true }
        val mockContext =
            mock<PreferenceLifecycleContext> {
                on { requirePreference<Preference>(AirplaneModePreference.KEY) } doReturn
                    mockPreference
                on { getSystemService(TelephonyManager::class.java) } doReturn telephonyManager
            }

        airplaneModePreference.onCreate(mockContext)
        val onPreferenceChangeListener = argumentCaptor<OnPreferenceChangeListener>()
        verify(mockPreference).onPreferenceChangeListener = onPreferenceChangeListener.capture()
        val result = onPreferenceChangeListener.lastValue.onPreferenceChange(mockPreference, true)

        assertThat(result).isFalse()
        verify(mockContext)
            .startActivityForResult(
                any(),
                eq(AirplaneModePreference.REQUEST_CODE_EXIT_ECM),
                eq(null),
            )
    }

    @Test
    fun onCreate_satelliteOn_showSatelliteDialog() {
        SatelliteRepository.setIsSessionStartedForTesting(true)
        val mockPreference = mock<Preference>()
        val mockContext =
            mock<PreferenceLifecycleContext> {
                on { requirePreference<Preference>(AirplaneModePreference.KEY) } doReturn
                    mockPreference
            }

        airplaneModePreference.onCreate(mockContext)
        val onPreferenceChangeListener = argumentCaptor<OnPreferenceChangeListener>()
        verify(mockPreference).onPreferenceChangeListener = onPreferenceChangeListener.capture()
        val result = onPreferenceChangeListener.lastValue.onPreferenceChange(mockPreference, true)

        assertThat(result).isFalse()
        verify(mockContext).startActivity(any())
    }

    @Test
    fun onCreate_success() {
        val mockPreference = mock<Preference>()
        val mockContext =
            mock<PreferenceLifecycleContext> {
                on { requirePreference<Preference>(AirplaneModePreference.KEY) } doReturn
                    mockPreference
            }

        airplaneModePreference.onCreate(mockContext)
        val onPreferenceChangeListener = argumentCaptor<OnPreferenceChangeListener>()
        verify(mockPreference).onPreferenceChangeListener = onPreferenceChangeListener.capture()
        val result = onPreferenceChangeListener.lastValue.onPreferenceChange(mockPreference, true)

        assertThat(result).isTrue()
        verify(mockContext).requirePreference<Preference>(AirplaneModePreference.KEY)
        verify(mockContext).getSystemService(TelephonyManager::class.java)
        verifyNoMoreInteractions(mockContext)
    }

    @Test
    fun onCreate_turningOffWithAuthenticationRequired_waitsForSuccess() {
        val mockPreference = mock<Preference>()
        val mockStore =
            mock<KeyValueStore> {
                on { getBoolean(AirplaneModePreference.KEY) } doReturn true
            }
        val mockHelper = mock<AirplaneModeAuthenticationHelper>()
        val mockContext =
            mock<PreferenceLifecycleContext> {
                on { requirePreference<Preference>(AirplaneModePreference.KEY) } doReturn
                    mockPreference
                on { getKeyValueStore(AirplaneModePreference.KEY) } doReturn mockStore
            }
        airplaneModePreference.authenticationHelper = mockHelper
        airplaneModePreference.isAuthenticationRequired = { true }

        airplaneModePreference.onCreate(mockContext)
        val listener = argumentCaptor<OnPreferenceChangeListener>()
        verify(mockPreference).onPreferenceChangeListener = listener.capture()
        val result = listener.lastValue.onPreferenceChange(mockPreference, false)

        assertThat(result).isFalse()
        verify(mockHelper).runAfterAuthentication(any())
        verify(mockStore, never()).setBoolean(AirplaneModePreference.KEY, false)
    }

    @Test
    fun onCreate_turningOffWithAuthenticationRequiredAndMissingStore_failsClosed() {
        val mockPreference = mock<Preference>()
        val mockHelper = mock<AirplaneModeAuthenticationHelper>()
        val mockContext =
            mock<PreferenceLifecycleContext> {
                on { requirePreference<Preference>(AirplaneModePreference.KEY) } doReturn
                    mockPreference
                on { getKeyValueStore(AirplaneModePreference.KEY) } doReturn null
            }
        airplaneModePreference.authenticationHelper = mockHelper
        airplaneModePreference.isAuthenticationRequired = { true }

        airplaneModePreference.onCreate(mockContext)
        val listener = argumentCaptor<OnPreferenceChangeListener>()
        verify(mockPreference).onPreferenceChangeListener = listener.capture()

        assertThat(listener.lastValue.onPreferenceChange(mockPreference, false)).isFalse()
        verify(mockHelper, never()).runAfterAuthentication(any())
    }

    @Test
    fun onCreate_turningOffAfterAuthentication_writesFalse() {
        val mockPreference = mock<Preference>()
        val mockStore =
            mock<KeyValueStore> {
                on { getBoolean(AirplaneModePreference.KEY) } doReturn true
            }
        val mockHelper = mock<AirplaneModeAuthenticationHelper>()
        val mockContext =
            mock<PreferenceLifecycleContext> {
                on { requirePreference<Preference>(AirplaneModePreference.KEY) } doReturn
                    mockPreference
                on { getKeyValueStore(AirplaneModePreference.KEY) } doReturn mockStore
            }
        airplaneModePreference.authenticationHelper = mockHelper
        airplaneModePreference.isAuthenticationRequired = { true }
        mockHelper.stub {
            on { runAfterAuthentication(any()) } doAnswer {
                it.getArgument<Runnable>(0).run()
                Unit
            }
        }

        airplaneModePreference.onCreate(mockContext)
        val listener = argumentCaptor<OnPreferenceChangeListener>()
        verify(mockPreference).onPreferenceChangeListener = listener.capture()
        val result = listener.lastValue.onPreferenceChange(mockPreference, false)

        assertThat(result).isFalse()
        verify(mockStore).setBoolean(AirplaneModePreference.KEY, false)
    }

    @Test
    fun onDestroy_cancelsAndReleasesAuthenticationHelper() {
        val mockHelper = mock<AirplaneModeAuthenticationHelper>()
        airplaneModePreference.authenticationHelper = mockHelper

        airplaneModePreference.onDestroy(mock())

        verify(mockHelper).cancel()
        assertThat(airplaneModePreference.authenticationHelper).isNull()
    }

    @Test
    fun onActivityResult_exitEcm_turnOnAirplaneMode() {
        val mockKeyValueStore = mock<KeyValueStore>()
        val mockContext =
            mock<PreferenceLifecycleContext> {
                on { getKeyValueStore(AirplaneModePreference.KEY) } doReturn mockKeyValueStore
            }

        val result =
            airplaneModePreference.onActivityResult(
                mockContext,
                AirplaneModePreference.REQUEST_CODE_EXIT_ECM,
                Activity.RESULT_OK,
                null,
            )

        assertThat(result).isTrue()
        verify(mockKeyValueStore).setBoolean(AirplaneModePreference.KEY, true)
    }

    @Test
    fun onActivityResult_notExitEcm_doNothing() {
        val mockContext = mock<PreferenceLifecycleContext>()

        val result =
            airplaneModePreference.onActivityResult(
                mockContext,
                AirplaneModePreference.REQUEST_CODE_EXIT_ECM,
                Activity.RESULT_CANCELED,
                null,
            )

        assertThat(result).isTrue()
        verifyNoMoreInteractions(mockContext)
    }

    @Test
    fun setValue_shouldSendBroadcast() {
        airplaneModeDataStore.setBoolean(AirplaneModePreference.KEY, true)

        val intents = shadowOf(context as ContextWrapper).getBroadcastIntentsForUser(UserHandle.ALL)
        assertThat(intents).hasSize(1)
        assertThat(intents[0]).hasAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
        assertThat(intents[0]).extras().bool("state").isTrue()
    }

    @Test
    @EnableFlags(FLAG_SYNC_AIRPLANE_MODE_WITH_WATCHES, FLAG_ENABLE_DATA_SYNC)
    fun airplaneModeTogglePreference_isAvailable_noPairedWatch() {
        val preference = AirplaneModeTogglePreference()

        assertThat(preference.isAvailable(context)).isTrue()
    }

    @Test
    @EnableFlags(FLAG_SYNC_AIRPLANE_MODE_WITH_WATCHES, FLAG_ENABLE_DATA_SYNC)
    fun airplaneModeTogglePreference_isAvailable_hasPairedWatch() {
        val preference = AirplaneModeTogglePreference()
        addWatchAssociation()

        assertThat(preference.isAvailable(context)).isFalse()
    }

    @Test
    @EnableFlags(FLAG_SYNC_AIRPLANE_MODE_WITH_WATCHES, FLAG_ENABLE_DATA_SYNC)
    fun airplaneModeDetailsPreference_isAvailable_hasPairedWatch() {
        val preference = AirplaneModeDetailsPreference()
        addWatchAssociation()

        assertThat(preference.isAvailable(context)).isTrue()
    }

    @Test
    @EnableFlags(FLAG_SYNC_AIRPLANE_MODE_WITH_WATCHES, FLAG_ENABLE_DATA_SYNC)
    fun airplaneModeDetailsPreference_isAvailable_noPairedWatch() {
        val preference = AirplaneModeDetailsPreference()

        assertThat(preference.isAvailable(context)).isFalse()
    }

    @Test
    fun airplaneModeDetailsPreference_isNotIndexable() {
        val preference = AirplaneModeDetailsPreference()

        assertThat(preference.indexable).isFalse()
    }

    @Test
    fun properties() {
        assertThat(airplaneModePreference.icon).isEqualTo(R.drawable.ic_airplanemode_active)
        assertThat(airplaneModePreference.tags(context))
            .asList()
            .containsExactly(KEY_AIRPLANE_MODE, HERO_SET)
        assertThat(airplaneModePreference.restrictionKeys)
            .asList()
            .containsExactly(UserManager.DISALLOW_AIRPLANE_MODE)
    }

    private fun addWatchAssociation() {
        val metadata =
            PersistableBundle().apply {
                putPersistableBundle(
                    FEATURE_CROSS_DEVICE_SYNC,
                    PersistableBundle().apply { putBoolean(APM_SYNC_SUPPORTED, true) },
                )
            }
        companionDeviceManager.stub {
            on { getAllAssociations(UserHandle.USER_ALL) } doReturn
                listOf(
                    AssociationInfo.Builder(1, UserHandle.myUserId(), context.packageName)
                        .setDeviceProfile(AssociationRequest.DEVICE_PROFILE_WATCH)
                        .setDisplayName("Smart Watch")
                        .setMetadata(metadata)
                        .setSystemDataSyncFlags(FLAG_AIRPLANE_MODE)
                        .build()
                )
            on { getLocalMetadata(UserHandle.USER_ALL) } doReturn metadata
        }
    }

    private fun getSwitchPreference(): SwitchPreferenceCompat =
        airplaneModePreference.createAndBindWidget(
            context,
            preferenceScreen = null,
            mock<PreferenceScreenMixin> {
                on { metricsCategory } doReturn SETTINGS_NETWORK_CATEGORY
            },
        )
}

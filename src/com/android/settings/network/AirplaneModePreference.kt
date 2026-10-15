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

import android.app.Activity
import android.app.settings.SettingsEnums.ACTION_AIRPLANE_TOGGLE
import android.content.Context
import android.content.Intent
import android.ext.settings.ExtSettings
import android.os.UserHandle
import android.os.UserManager
import android.provider.Settings
import android.telephony.TelephonyManager
import androidx.annotation.DrawableRes
import androidx.annotation.VisibleForTesting
import androidx.preference.Preference
import com.android.settings.AirplaneModeEnabler
import com.android.settings.R
import com.android.settings.Utils
import com.android.settings.contract.KEY_AIRPLANE_MODE
import com.android.settings.metrics.PreferenceActionMetricsProvider
import com.android.settings.network.SatelliteRepository.Companion.isSatelliteOn
import com.android.settings.restriction.PreferenceRestrictionMixin
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyValueStoreDelegate
import com.android.settingslib.datastore.SettingsGlobalStore
import com.android.settingslib.metadata.HERO_SET
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.SwitchPreference
import com.android.settingslib.metadata.UI_ONLY_PREFERENCE
import com.android.settingslib.widget.MainSwitchPreferenceBinding

// LINT.IfChange
open class AirplaneModePreference :
    SwitchPreference(
        KEY,
        R.string.airplane_mode_settings_airplane_mode_on_purpose,
        R.string.airplane_mode,
    ),
    PreferenceActionMetricsProvider,
    PreferenceAvailabilityProvider,
    PreferenceLifecycleProvider,
    PreferenceRestrictionMixin {

    @VisibleForTesting
    internal var authenticationHelper: AirplaneModeAuthenticationHelper? = null

    @VisibleForTesting
    internal var isAuthenticationRequired: (Context) -> Boolean = {
        ExtSettings.REQUIRE_AUTHENTICATION_TO_DISABLE_AIRPLANE_MODE.get(it)
    }

    override val icon: Int
        @DrawableRes get() = R.drawable.ic_airplanemode_active

    override fun tags(context: Context) = arrayOf(KEY_AIRPLANE_MODE, HERO_SET)

    override val availabilityDescription = "The device must support configuring airplane mode."

    override fun getAvailabilityStability() = PreconditionStability.STABLE_UNTIL_APK_UPDATE

    override fun isAvailable(context: Context) = context.isAirplaneModeEligible()

    override fun getEnabledDescription(): String = "This setting must not be restricted by a device administrator. Airplane mode cannot be changed during an emergency call. Airplane mode cannot be changed while satellite messaging is active."

    override fun getEnabledStability() = PreconditionStability.UNSTABLE

    override fun isEnabled(context: Context) = super<PreferenceRestrictionMixin>.isEnabled(context)

    override val restrictionKeys
        get() = arrayOf(UserManager.DISALLOW_AIRPLANE_MODE)

    override fun getReadPermissions(context: Context) = SettingsGlobalStore.getReadPermissions()

    override fun getWritePermissions(context: Context) = SettingsGlobalStore.getWritePermissions()

    override fun getReadPermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getWritePermit(
        context: Context,
        value: Boolean?,
        callingPid: Int,
        callingUid: Int,
    ) =
        when {
            value != true && isAuthenticationRequired(context) -> ReadWritePermit.DISALLOW
            isSatelliteOn(context) || isInEcmMode(context) -> ReadWritePermit.DISALLOW
            else -> ReadWritePermit.ALLOW
        }

    override val sensitivityLevel
        get() = SensitivityLevel.MUST_PROVIDE_UNDO

    override val preferenceActionMetrics: Int
        get() = ACTION_AIRPLANE_TOGGLE

    override fun storage(context: Context) = createDataStore(context)

    override fun onCreate(context: PreferenceLifecycleContext) {
        context.requirePreference<Preference>(key).onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _: Preference, newValue: Any ->
                if (isInEcmMode(context)) {
                    showEcmDialog(context)
                    return@OnPreferenceChangeListener false
                }
                if (isSatelliteOn(context)) {
                    showSatelliteDialog(context)
                    return@OnPreferenceChangeListener false
                }
                if (newValue == false && isAuthenticationRequired(context)) {
                    val store = context.getKeyValueStore(KEY)
                        ?: return@OnPreferenceChangeListener false
                    if (store.getBoolean(KEY) != true) {
                        return@OnPreferenceChangeListener false
                    }
                    getAuthenticationHelper(context).runAfterAuthentication {
                        if (store.getBoolean(KEY) == true) {
                            store.setBoolean(KEY, false)
                        }
                    }
                    return@OnPreferenceChangeListener false
                }
                return@OnPreferenceChangeListener true
            }
    }

    override fun onDestroy(context: PreferenceLifecycleContext) {
        super.onDestroy(context)
        authenticationHelper?.cancel()
        authenticationHelper = null
    }

    private fun getAuthenticationHelper(context: Context): AirplaneModeAuthenticationHelper =
        authenticationHelper ?: AirplaneModeAuthenticationHelper(context).also {
            authenticationHelper = it
        }

    override fun onActivityResult(
        context: PreferenceLifecycleContext,
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
    ): Boolean {
        if (requestCode == REQUEST_CODE_EXIT_ECM && resultCode == Activity.RESULT_OK) {
            context.getKeyValueStore(KEY)?.setBoolean(KEY, true)
        }
        return true
    }

    private fun isInEcmMode(context: Context) =
        AirplaneModeEnabler.isInEcmMode(
            context,
            context.getSystemService(TelephonyManager::class.java),
        )

    private fun showEcmDialog(context: PreferenceLifecycleContext) {
        val intent =
            Intent(TelephonyManager.ACTION_SHOW_NOTICE_ECM_BLOCK_OTHERS, null)
                .setPackage(Utils.PHONE_PACKAGE_NAME)
        context.startActivityForResult(intent, REQUEST_CODE_EXIT_ECM, null)
    }

    private fun showSatelliteDialog(context: PreferenceLifecycleContext) {
        val intent =
            Intent(context, SatelliteWarningDialogActivity::class.java)
                .putExtra(
                    SatelliteWarningDialogActivity.EXTRA_TYPE_OF_SATELLITE_WARNING_DIALOG,
                    SatelliteWarningDialogActivity.TYPE_IS_AIRPLANE_MODE,
                )
        context.startActivity(intent)
    }

    companion object {
        const val KEY = Settings.Global.AIRPLANE_MODE_ON
        const val DEFAULT_VALUE = false
        const val REQUEST_CODE_EXIT_ECM = 1

        fun createDataStore(context: Context): KeyValueStore = AirplaneModeStorage(context)

        @Suppress("UNCHECKED_CAST")
        private class AirplaneModeStorage(private val context: Context) : KeyValueStoreDelegate {

            private val settingsStore =
                SettingsGlobalStore.get(context).apply { setDefaultValue(KEY, DEFAULT_VALUE) }

            override val keyValueStoreDelegate
                get() = settingsStore

            override fun contains(key: String): Boolean = settingsStore.contains(KEY)

            override fun <T : Any> getValue(key: String, valueType: Class<T>) =
                settingsStore.getValue(KEY, valueType)

            override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
                settingsStore.setValue(KEY, valueType, value)

                val intent = Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED)
                intent.putExtra("state", getBoolean(KEY)!!)
                context.sendBroadcastAsUser(intent, UserHandle.ALL)
            }
        }
    }
}

// LINT.ThenChange(AirplaneModePreferenceController.java)

/** Preference for the Airplane Mode toggle in the Network & Internet screen. */
class AirplaneModeTogglePreference : AirplaneModePreference() {

    override val availabilityDescription = "The device must support configuring airplane mode and must not have a paired watch."

    override fun getAvailabilityStability() = PreconditionStability.UNSTABLE

    override fun isAvailable(context: Context) =
        context.isAirplaneModeEligible() && !context.hasPairedWatchForAirplaneModeSync()
}

/** Preference for the Airplane Mode toggle in the Airplane Mode Settings screen. */
class AirplaneModeDetailsPreference : AirplaneModePreference(), MainSwitchPreferenceBinding {

    override val availabilityDescription = "The device must support configuring airplane mode and must not have a paired watch."

    override fun getAvailabilityStability() = PreconditionStability.UNSTABLE

    override fun isAvailable(context: Context) =
        context.isAirplaneModeEligible() && context.hasPairedWatchForAirplaneModeSync()

    override fun tags(context: Context) = arrayOf(UI_ONLY_PREFERENCE)

    // Since the AirplaneModeSettingsScreen is indexed and already points to this main switch, we
    // don't want this to also be indexed causing 2 results for Settings search.
    override val indexable: Boolean
        get() = false
}

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
package com.android.settings.network

import android.app.KeyguardManager
import android.content.Context
import android.ext.settings.ExtSettings
import androidx.preference.Preference
import com.android.settings.R
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.SettingsGlobalStore
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.SwitchPreference
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.preference.SwitchPreferenceBinding

/** Catalyst metadata for the opt-in airplane-mode authentication requirement. */
class AirplaneModeAuthenticationPreference :
    SwitchPreference(
        key = KEY,
        purpose = R.string.airplane_mode_authentication_purpose,
        title = R.string.airplane_mode_authentication_setting_title,
    ),
    SwitchPreferenceBinding,
    PreferenceAvailabilityProvider,
    PreferenceLifecycleProvider,
    PreferenceSummaryProvider {

    internal var authenticationHelper: AirplaneModeAuthenticationHelper? = null

    internal var isDeviceSecure: (Context) -> Boolean = {
        it.getSystemService(KeyguardManager::class.java).isDeviceSecure
    }

    internal var isAuthenticationRequired: (Context) -> Boolean = {
        ExtSettings.REQUIRE_AUTHENTICATION_TO_DISABLE_AIRPLANE_MODE.get(it)
    }

    override fun storage(context: Context): KeyValueStore =
        SettingsGlobalStore.get(context).apply { setDefaultValue(KEY, false) }

    override fun getSummary(context: Context): CharSequence =
        context.getText(R.string.airplane_mode_authentication_summary)

    override val availabilityDescription = "The current user must have a secure screen lock."

    override fun getAvailabilityStability() = PreconditionStability.UNSTABLE

    override fun isAvailable(context: Context) = isDeviceSecure(context)

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
        if (value != true && isAuthenticationRequired(context)) {
            ReadWritePermit.DISALLOW
        } else {
            ReadWritePermit.ALLOW
        }

    override val sensitivityLevel
        get() = SensitivityLevel.MUST_PROVIDE_UNDO

    override fun onCreate(context: PreferenceLifecycleContext) {
        context.requirePreference<Preference>(key).onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                if (newValue == false && isAuthenticationRequired(context)) {
                    getAuthenticationHelper(context).runAfterAuthentication {
                        if (isAuthenticationRequired(context)) {
                            storage(context).setBoolean(KEY, false)
                        }
                    }
                    return@OnPreferenceChangeListener false
                }
                true
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

    companion object {
        val KEY: String = ExtSettings.REQUIRE_AUTHENTICATION_TO_DISABLE_AIRPLANE_MODE.key
    }
}

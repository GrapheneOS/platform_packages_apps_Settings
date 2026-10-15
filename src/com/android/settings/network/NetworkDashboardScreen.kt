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

import android.app.settings.SettingsEnums.SETTINGS_NETWORK_CATEGORY
import android.content.Context
import androidx.fragment.app.Fragment
import com.android.server.connectivity.Flags as ConnectivityFlags
import com.android.settings.R
import com.android.settings.Settings.NetworkDashboardActivity
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.datausage.DataSaverScreen
import com.android.settings.flags.Flags
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.metadata.PreferenceIconProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen.Companion.APP_FUNCTION_MOBILE_DATA
import com.android.settingslib.widget.SettingsThemeHelper.isExpressiveTheme
import kotlinx.coroutines.CoroutineScope

@ProvidePreferenceScreen(NetworkDashboardScreen.KEY)
open class NetworkDashboardScreen : PreferenceScreenMixin, PreferenceIconProvider {
    override fun tags(context: Context) = arrayOf(APP_FUNCTION_MOBILE_DATA)

    override val key: String
        get() = KEY

    // TODO(b/462618020) Catalyst-purpose: replace default purpose with 2 line description
    override val purpose: Int
        get() = R.string.network_provider_and_internet_screen_purpose

    override val title: Int
        get() = R.string.network_dashboard_title

    override fun getIcon(context: Context) =
        when {
            isExpressiveTheme(context) -> R.drawable.ic_homepage_network
            else -> R.drawable.ic_settings_wireless_filled
        }

    override fun isFlagEnabled(context: Context) = Flags.catalystNetworkProviderAndInternetScreen()

    override fun hasCompleteHierarchy() = false

    override fun fragmentClass(): Class<out Fragment>? = NetworkDashboardFragment::class.java

    override fun getMetricsCategory() = SETTINGS_NETWORK_CATEGORY

    override val highlightMenuKey: Int
        get() = R.string.menu_key_network

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        makeLaunchIntent(context, NetworkDashboardActivity::class.java, metadata?.key)

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {
            if (Flags.catalystMobileNetworkList()) +MobileNetworkListScreen.KEY order -15
            if (ConnectivityFlags.syncAirplaneModeWithWatches()) {
                // Note : The following 2 preferences are mutually exclusive and only at most one of
                // them can be shown at a time. This is because they use different Preferences under
                // the hood and are shown based on whether or not a watch is connected to the phone.
                +AirplaneModeTogglePreference() order -5
                +AirplaneModeSettingsScreen.KEY order 0
            } else {
                +AirplaneModePreference() order -5
            }
            +AirplaneModeAuthenticationPreference() order -4
            if (Flags.catalystRestrictBackgroundParentEntry()) +DataSaverScreen.KEY order 10
        }

    companion object {
        const val KEY = "network_provider_and_internet_screen"
    }
}

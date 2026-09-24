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

import android.app.settings.SettingsEnums
import android.content.Context
import android.content.Intent
import android.telephony.SubscriptionManager
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.Settings.AdaptiveConnectivitySettingsActivity
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.flags.Flags
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import kotlinx.coroutines.CoroutineScope
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen.Companion.APP_FUNCTION_UNCATEGORIZED

@ProvidePreferenceScreen(AdaptiveConnectivityScreen.KEY)
open class AdaptiveConnectivityScreen : PreferenceScreenMixin {
    override fun tags(context: Context) = arrayOf(APP_FUNCTION_UNCATEGORIZED)

    override val key
        get() = KEY

    // TODO(b/462618020) Catalyst-purpose: replace default purpose with 2 line description
    override val purpose: Int
        get() = R.string.adaptive_connectivity_purpose

    override val title
        get() = R.string.adaptive_connectivity_title

    override fun getMetricsCategory() = SettingsEnums.ADAPTIVE_CONNECTIVITY_CATEGORY

    override val highlightMenuKey
        get() = R.string.menu_key_network

    override fun isFlagEnabled(context: Context) = Flags.catalystAdaptiveConnectivity()

    override fun fragmentClass(): Class<out Fragment>? = AdaptiveConnectivitySettings::class.java

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {
            +WifiScorerTogglePreference()
            val subscriptionManager = context.getSystemService(SubscriptionManager::class.java)
            val shouldHideMobileNetworkToggle =
                subscriptionManager != null &&
                    SubscriptionUtil.hasSubscriptionForMobileNetworkToggleDisable(
                        context,
                        subscriptionManager,
                    )
            if (context.resources.getBoolean(R.bool.config_show_adaptive_connectivity) &&
                !shouldHideMobileNetworkToggle) {
                +AdaptiveMobileNetworkTogglePreference()
            }
        }

    override fun hasCompleteHierarchy() = false

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?): Intent? =
        makeLaunchIntent(context, AdaptiveConnectivitySettingsActivity::class.java, metadata?.key)

    companion object {
        const val KEY = "adaptive_connectivity"
    }
}

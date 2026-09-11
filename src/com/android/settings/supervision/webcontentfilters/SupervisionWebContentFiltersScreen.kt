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
package com.android.settings.supervision.webcontentfilters

import android.Manifest.permission.MANAGE_USERS
import android.app.settings.SettingsEnums
import android.app.supervision.SupervisionManager
import android.app.supervision.flags.Flags
import android.content.Context
import android.content.pm.PackageManager.NameNotFoundException
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.preference.PreferenceGroup
import androidx.preference.SwitchPreferenceCompat
import com.android.settings.CatalystSettingsActivity
import com.android.settings.R
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.supervision.SupervisionDashboardActivity
import com.android.settings.supervision.SupervisionSupportedAppPreference
import com.android.settings.supervision.ipc.SupervisionMessengerClient
import com.android.settings.supervision.ipc.SupportedApp
import com.android.settings.supervision.shared.widget.NonIndexablePreferenceCategory
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen.Companion.APP_FUNCTION_UNCATEGORIZED
import com.android.settingslib.supervision.SupervisionLog.TAG
import com.android.settingslib.utils.StringUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Activity to display [SupervisionWebContentFiltersScreen]. */
class SupervisionWebContentFiltersActivity :
    CatalystSettingsActivity(SupervisionWebContentFiltersScreen.KEY) {
    @RequiresPermission(MANAGE_USERS)
    public override fun onResume() {
        super.onResume()
        if (getSystemService(SupervisionManager::class.java)?.isSupervisionEnabled != true) {
            startActivity(
                makeLaunchIntent(
                    this,
                    SupervisionDashboardActivity::class.java,
                    SupervisionWebContentFiltersScreen.KEY,
                )
            )
            finish()
            return
        }
    }
}

/** Web content filters landing page (Settings > Supervision > Web content filters). */
@ProvidePreferenceScreen(SupervisionWebContentFiltersScreen.KEY)
open class SupervisionWebContentFiltersScreen : PreferenceScreenMixin, PreferenceLifecycleProvider {
    override fun tags(context: Context) = arrayOf(APP_FUNCTION_UNCATEGORIZED)

    private var supervisionClient: SupervisionMessengerClient? = null

    override val key: String
        get() = KEY

    override val purpose: Int
        get() = R.string.supervision_web_content_filters_purpose

    override val title: Int
        get() = R.string.supervision_web_content_filters_title

    override val indexable
        get() = true

    override val keywords: Int
        get() = R.string.supervision_web_content_filters_keywords

    override val icon: Int
        get() = R.drawable.ic_globe

    override fun getMetricsCategory() = SettingsEnums.SUPERVISION_WEB_CONTENT_FILTERS

    override val highlightMenuKey: Int
        get() = R.string.menu_key_supervision

    override fun onCreate(context: PreferenceLifecycleContext) {
        if (isContainer(context)) {
            supervisionClient = getSupervisionClient(context)
            if (!Flags.enableSupervisionSettingsUiUpdates()) {
                addSupportedApps(context)
            }
        }
    }

    override fun onDestroy(context: PreferenceLifecycleContext) {
        if (isContainer(context)) {
            supervisionClient?.close()
        }
    }

    override fun hasCompleteHierarchy() = true

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {
            +SupervisionWebContentFiltersTopIntroPreference()
            +NonIndexablePreferenceCategory(
                BROWSER_FILTERS_GROUP,
                R.string.browser_filters_group_purpose,
                R.string.supervision_web_content_filters_browser_title,
            ) +=
                {
                    val dataStore = SupervisionSafeSitesDataStore(context)
                    +SupervisionSafeSitesSwitchPreference(dataStore)
                    if (Flags.enableSupervisionSettingsUiUpdates()) {
                        +SupervisionWebContentFiltersBrowserSupportedAppsScreen.KEY
                    }
                }
            +NonIndexablePreferenceCategory(
                SEARCH_FILTERS_GROUP,
                R.string.search_filters_group_purpose,
                R.string.supervision_web_content_filters_search_title,
            ) +=
                {
                    val dataStore = SupervisionSafeSearchDataStore(context)
                    +SupervisionSafeSearchSwitchPreference(dataStore)
                    if (Flags.enableSupervisionSettingsUiUpdates()) {
                        +SupervisionWebContentFiltersSearchSupportedAppsScreen.KEY
                    }
                }
            +SupervisionWebContentFiltersFooterPreference()
        }

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        makeLaunchIntent(context, SupervisionWebContentFiltersActivity::class.java, metadata?.key)

    private fun addSupportedApps(context: PreferenceLifecycleContext) {
        context.lifecycleScope.launch {
            val supportedAppsMap =
                withContext(Dispatchers.IO) {
                    supervisionClient?.getSupportedApps(
                        listOf(BROWSER_FILTERS_SUPPORTED_APPS, SEARCH_FILTERS_SUPPORTED_APPS)
                    )
                }

            createSupportedAppPreference(
                context,
                BROWSER_FILTERS_GROUP,
                SupervisionSafeSitesSwitchPreference.KEY,
                BROWSER_FILTERS_SUPPORTED_APPS,
                supportedAppsMap,
            )
            createSupportedAppPreference(
                context,
                SEARCH_FILTERS_GROUP,
                SupervisionSafeSearchSwitchPreference.KEY,
                SEARCH_FILTERS_SUPPORTED_APPS,
                supportedAppsMap,
            )
        }
    }

    private fun createSupportedAppPreference(
        context: PreferenceLifecycleContext,
        filterGroup: String,
        filterSwitchPreferenceKey: String,
        filterType: String,
        supportedAppsMap: Map<String, List<SupportedApp>>?,
    ) {
        val supportedApps = supportedAppsMap?.get(filterType) ?: emptyList()
        context.findPreference<SwitchPreferenceCompat>(filterSwitchPreferenceKey)?.apply {
            setSummaryOn(
                StringUtil.getIcuPluralsString(
                    context,
                    supportedApps.size,
                    R.string.supervision_web_content_filters_switch_summary,
                )
            )
            setSummaryOff(
                StringUtil.getIcuPluralsString(
                    context,
                    supportedApps.size,
                    R.string.supervision_web_content_filters_switch_summary,
                )
            )
        }
        context.findPreference<PreferenceGroup>(filterGroup)?.apply {
            for (supportedApp in supportedApps) {
                val packageName = supportedApp.packageName
                if (packageName != null) {
                    try {
                        SupervisionSupportedAppPreference(
                                supportedApp.title,
                                supportedApp.summary,
                                packageName,
                            )
                            .createWidget(context)
                            .let { addPreference(it) }
                    } catch (e: NameNotFoundException) {
                        Log.d(TAG, "Package not found for supported app, skipping: $packageName", e)
                    }
                }
            }
        }
    }

    private fun getSupervisionClient(context: Context) =
        supervisionClient ?: SupervisionMessengerClient(context).also { supervisionClient = it }

    companion object {
        const val KEY = "supervision_web_content_filters"
        internal const val BROWSER_FILTERS_GROUP = "browser_filters_group"
        internal const val BROWSER_FILTERS_SUPPORTED_APPS = "browser_filters_supported_apps"
        internal const val SEARCH_FILTERS_GROUP = "search_filters_group"
        internal const val SEARCH_FILTERS_SUPPORTED_APPS = "search_filters_supported_apps"
    }
}

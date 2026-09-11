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
package com.android.settings.supervision.appstorefilters

import android.Manifest
import android.app.settings.SettingsEnums
import android.app.supervision.flags.Flags
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.preference.Preference
import androidx.preference.PreferenceGroup
import com.android.settings.CatalystSettingsActivity
import com.android.settings.R
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.supervision.SupervisionLog.TAG
import com.android.settingslib.widget.UntitledPreferenceCategoryMetadata
import kotlinx.coroutines.CoroutineScope

class SupervisionAppStoreFiltersActivity :
    CatalystSettingsActivity(SupervisionAppStoreFiltersScreen.KEY)

@ProvidePreferenceScreen(SupervisionAppStoreFiltersScreen.KEY)
open class SupervisionAppStoreFiltersScreen :
    PreferenceScreenMixin, PreferenceAvailabilityProvider, PreferenceLifecycleProvider {

    override val availabilityDescription = "The device must support the app store filters screen."

    override fun getAvailabilityStability() = PreconditionStability.STABLE_UNTIL_APK_UPDATE

    override fun isAvailable(context: Context) = Flags.enableAppStoreFiltersScreen()

    override val key: String
        get() = KEY

    override val purpose: Int
        get() = R.string.supervision_app_store_filters_purpose

    override val title: Int
        get() = R.string.supervision_app_store_filters_title

    override val indexable: Boolean
        get() = true

    override val keywords: Int
        get() = R.string.supervision_app_store_filters_keywords

    override val summary: Int
        get() = R.string.supervision_app_store_filters_summary

    override val icon: Int
        get() = R.drawable.ic_apps_library

    override val highlightMenuKey: Int
        get() = R.string.menu_key_supervision

    override fun getMetricsCategory() = SettingsEnums.SUPERVISION_APP_STORE_FILTERS

    override fun hasCompleteHierarchy() = true

    override fun onCreate(context: PreferenceLifecycleContext) {
        if (isContainer(context)) {
            val preferenceGroup =
                context.findPreference<PreferenceGroup>(SUPERVISION_APP_STORE_FILTERS_GROUP)
            preferenceGroup?.let { group ->
                group.removeAll()
                val appStorePrefs = getAppStoreFiltersEntries(context)
                appStorePrefs.forEach { pref -> group.addPreference(pref) }
            }
        }
    }

    @VisibleForTesting
    internal fun getAppStoreFiltersEntries(context: PreferenceLifecycleContext): List<Preference> {
        val packageManager = context.packageManager
        val intent = Intent(ACTION_VIEW_APP_FILTER_SETTINGS)

        val resolveInfos =
            packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)

        val appStorePreferences = mutableListOf<Preference>()

        for (resolveInfo in resolveInfos) {
            val activityInfo = resolveInfo.activityInfo
            if (activityInfo == null || !activityInfo.enabled) continue

            val packageName = activityInfo.packageName
            try {
                val applicationInfo = activityInfo.getApplicationInfo()
                if (
                    packageManager.checkPermission(
                        Manifest.permission.INSTALL_PACKAGES,
                        packageName,
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    val appLabel = applicationInfo.loadLabel(packageManager)
                    val appIcon = applicationInfo.loadIcon(packageManager)
                    val preference =
                        Preference(context).apply {
                            this.title = appLabel
                            this.icon = appIcon
                            this.key = packageName
                            this.widgetLayoutResource = R.layout.preference_widget_open_in_new

                            setOnPreferenceClickListener { pref ->
                                val intent = Intent().setClassName(packageName, activityInfo.name)
                                context.startActivityForResult(intent, 0, null)
                                true
                            }
                        }
                    appStorePreferences.add(preference)
                } else {
                    Log.w(
                        TAG,
                        "$packageName does not have the necessary INSTALL_PACKAGES permission.",
                    )
                }
            } catch (e: Exception) {
                Log.d(TAG, "Exception thrown for application, skipping: $packageName", e)
            }
        }
        return appStorePreferences
    }

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {
            +SupervisionAppStoreFiltersTopIntroPreference() order -100
            +UntitledPreferenceCategoryMetadata(SUPERVISION_APP_STORE_FILTERS_GROUP, purpose = R.string.supervision_app_store_filters_group_purpose) order 0
            +SupervisionAppStoreFiltersFooterPreference() order 100
        }

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        makeLaunchIntent(context, SupervisionAppStoreFiltersActivity::class.java, metadata?.key)

    companion object {
        const val KEY = "supervision_app_store_filters"
        internal const val SUPERVISION_APP_STORE_FILTERS_GROUP =
            "supervision_app_store_filters_group"
        internal const val ACTION_VIEW_APP_FILTER_SETTINGS =
            "android.app.supervision.action.VIEW_APP_FILTER_SETTINGS"
    }
}

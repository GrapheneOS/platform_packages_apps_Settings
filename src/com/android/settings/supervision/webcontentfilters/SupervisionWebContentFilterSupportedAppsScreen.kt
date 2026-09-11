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

import android.app.supervision.flags.Flags
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.preference.Preference
import androidx.preference.PreferenceGroup
import com.android.settings.CatalystFragment
import com.android.settings.R
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.supervision.SupervisionSupportedAppPreference
import com.android.settings.supervision.ipc.SupervisionMessengerClient
import com.android.settings.supervision.ipc.SupportedApp
import com.android.settingslib.HelpUtils
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.datastore.SettingsStore
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceTitleProvider
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.preference.PreferenceBinding
import com.android.settingslib.supervision.SupervisionLog.TAG
import com.android.settingslib.utils.StringUtil
import com.android.settingslib.widget.UntitledPreferenceCategoryMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Abstract base class for screens displaying supported apps for web content filters. */
abstract class SupervisionWebContentFilterSupportedAppsScreen :
    PreferenceScreenMixin,
    PreferenceLifecycleProvider,
    PreferenceTitleProvider,
    PreferenceBinding,
    PreferenceAvailabilityProvider {
    protected var supportedApps: List<SupportedApp> = emptyList()
    private var supervisionClient: SupervisionMessengerClient? = null
    private var settingsStore: SettingsStore? = null
    private var lifecycleContext: PreferenceLifecycleContext? = null

    override fun getTitle(context: Context): CharSequence? =
        StringUtil.getIcuPluralsString(
            context,
            supportedApps.size,
            R.string.supervision_web_content_filters_switch_summary,
        )

    /** The key used to fetch the list of supported apps from [SupervisionMessengerClient]. */
    abstract val supportedAppsKey: String

    /** The key for whether the filter is enabled or not in settings store. */
    abstract val settingsKey: String

    override fun isEnabled(context: Context): Boolean = isFilterEnabled()

    override val availabilityDescription = "The device must support the new supervision settings UI."

    override fun getAvailabilityStability() = PreconditionStability.STABLE_UNTIL_APK_UPDATE

    override fun isAvailable(context: Context) = Flags.enableSupervisionSettingsUiUpdates()

    override val indexable
        get() = true

    override val highlightMenuKey: Int
        get() = R.string.menu_key_supervision

    override fun onCreate(context: PreferenceLifecycleContext) {
        lifecycleContext = context
        settingsStore = settingsStore ?: SettingsSecureStore.get(context)

        if (Flags.enableSupportedAppsRetrievalUpdates()) {
            if (!isContainer(context)) {
                supervisionClient = supervisionClient ?: SupervisionMessengerClient(context)
                context.lifecycleScope.launch {
                    supportedApps =
                        withContext(Dispatchers.IO) { filterSupportedApps(context, supportedApps) }
                    context.notifyPreferenceChange(key)
                }
            }
        } else {
            supervisionClient = supervisionClient ?: SupervisionMessengerClient(context)
            context.lifecycleScope.launch {
                supportedApps =
                    withContext(Dispatchers.IO) { filterSupportedApps(context, supportedApps) }
                addSupportedAppsPreferences(context)

                context.notifyPreferenceChange(key)
            }
        }
    }

    override fun onDestroy(context: PreferenceLifecycleContext) {
        supervisionClient?.close()
    }

    override fun createWidget(context: Context): Preference =
        SupervisionWebContentFiltersSupportedAppsEntryPointPreference(context, supportedApps)

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        if (preference is SupervisionWebContentFiltersSupportedAppsEntryPointPreference) {
            preference.updateSupportedApps(supportedApps)
        }
    }

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {
            +UntitledPreferenceCategoryMetadata(
                SUPPORTED_APPS_GROUP,
                R.string.supported_apps_group_purpose,
            ) +=
                {
                    if (Flags.enableSupportedAppsRetrievalUpdates()) {
                        addAsync(coroutineScope, Dispatchers.Default) {
                            supervisionClient =
                                supervisionClient ?: SupervisionMessengerClient(context)
                            supportedApps = filterSupportedApps(context, supportedApps)
                            for ((index, supportedApp) in supportedApps.withIndex()) {
                                +SupervisionSupportedAppPreference(
                                    supportedApp.title,
                                    supportedApp.summary,
                                    supportedApp.packageName!!,
                                    SupervisionSupportedAppPreference.KEY + index.toString(),
                                    supportedApp.learnMoreLink,
                                )
                            }
                            lifecycleContext?.notifyPreferenceChange(key)
                        }
                    }
                }
            +SupervisionWebContentFiltersFooterPreference()
        }

    private suspend fun filterSupportedApps(
        context: Context,
        supportedApps: List<SupportedApp>,
    ): List<SupportedApp> =
        supportedApps.ifEmpty {
            (supervisionClient?.getSupportedApps(listOf(supportedAppsKey))?.get(supportedAppsKey)
                    ?: emptyList())
                .filter { it.packageName != null }
                .filter { supportedApp ->
                    val packageName = supportedApp.packageName!!
                    try {
                        context.packageManager.getApplicationInfo(packageName, 0)
                        true
                    } catch (e: PackageManager.NameNotFoundException) {
                        Log.d(TAG, "Package not found for supported app, skipping: $packageName", e)
                        false
                    } catch (e: Exception) {
                        Log.d(TAG, "Exception thrown for supported app, skipping: $packageName", e)
                        false
                    }
                }
        }

    private fun addSupportedAppsPreferences(context: PreferenceLifecycleContext) {
        val fragmentInstance = context.lifecycleOwner
        context.findPreference<PreferenceGroup>(SUPPORTED_APPS_GROUP)?.apply {
            for ((index, supportedApp) in supportedApps.withIndex()) {
                SupervisionSupportedAppPreference(
                        supportedApp.title,
                        supportedApp.summary,
                        supportedApp.packageName!!,
                        SupervisionSupportedAppPreference.KEY + index.toString(),
                    )
                    .createWidget(context)
                    .let {
                        addPreference(it)
                        if (
                            Flags.enableSupervisionSettingsUiUpdates() &&
                                (supportedApp.learnMoreLink != null) &&
                                fragmentInstance is CatalystFragment
                        ) {
                            fragmentInstance.attachFooter(
                                it.key,
                                context.getString(
                                    R.string.supervision_web_content_filters_learn_more_title
                                ),
                            ) {
                                val intent =
                                    HelpUtils.getHelpIntent(
                                        context,
                                        supportedApp.learnMoreLink,
                                        context::class.java.name,
                                    )
                                if (intent != null) {
                                    context.startActivity(intent)
                                } else {
                                    Log.w(TAG, "HelpIntent is null")
                                }
                            }
                        }
                    }
            }
        }
    }

    private fun isFilterEnabled(): Boolean {
        val settingValue = settingsStore?.getInt(settingsKey)
        return settingValue != null && (settingValue > 0)
    }

    companion object {
        const val SUPPORTED_APPS_GROUP = "supported_apps_group"
    }
}

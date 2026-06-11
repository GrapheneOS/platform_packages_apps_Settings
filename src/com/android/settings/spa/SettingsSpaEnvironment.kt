/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.spa

import android.content.Context
import android.util.FeatureFlagUtils
import com.android.settings.network.apn.ApnEditPageProvider
import com.android.settings.print.PrintSettingsPageProvider
import com.android.settings.spa.about.AboutPhonePageProvider
import com.android.settings.spa.accessibility.ForceDarkAppExceptionsPageProvider
import com.android.settings.spa.app.AllAppListPageProvider
import com.android.settings.spa.app.AppsMainPageProvider
import com.android.settings.spa.app.appcompat.UserAspectRatioAppsPageProvider
import com.android.settings.spa.app.appinfo.AppInfoSettingsProvider
import com.android.settings.spa.app.appinfo.CloneAppInfoSettingsProvider
import com.android.settings.spa.app.backgroundinstall.BackgroundInstalledAppsPageProvider
import com.android.settings.spa.app.battery.BatteryOptimizationModeAppListPageProvider
import com.android.settings.spa.app.specialaccess.AlarmsAndRemindersAppListProvider
import com.android.settings.spa.app.specialaccess.AllFilesAccessAppListProvider
import com.android.settings.spa.app.specialaccess.ComputerControlAppInfoPageProvider
import com.android.settings.spa.app.specialaccess.ComputerControlAutomationAppListProvider
import com.android.settings.spa.app.specialaccess.ComputerControlTargetAppPageProvider
import com.android.settings.spa.app.specialaccess.DisplayOverOtherAppsAppListProvider
import com.android.settings.spa.app.specialaccess.HidAccessAppListProvider
import com.android.settings.spa.app.specialaccess.InstallUnknownAppsListProvider
import com.android.settings.spa.app.specialaccess.LongBackgroundTasksAppListProvider
import com.android.settings.spa.app.specialaccess.MediaManagementAppsAppListProvider
import com.android.settings.spa.app.specialaccess.MediaRoutingControlAppListProvider
import com.android.settings.spa.app.specialaccess.ModifySystemSettingsAppListProvider
import com.android.settings.spa.app.specialaccess.NfcTagAppsSettingsProvider
import com.android.settings.spa.app.specialaccess.PictureInPictureListProvider
import com.android.settings.spa.app.specialaccess.SpecialAppAccessPageProvider
import com.android.settings.spa.app.specialaccess.TurnScreenOnAppsAppListProvider
import com.android.settings.spa.app.specialaccess.UsageDataAppListProvider
import com.android.settings.spa.app.specialaccess.UseFullScreenIntentAppListProvider
import com.android.settings.spa.app.specialaccess.WifiControlAppListProvider
import com.android.settings.spa.app.specialaccess.WriteSystemPreferencesAppListProvider
import com.android.settings.spa.app.specialaccess.runIfComputerControlEnabled
import com.android.settings.spa.app.specialaccess.runIfHidAccessEnabled
import com.android.settings.spa.app.storage.StorageAppListPageProvider
import com.android.settings.spa.core.instrumentation.SpaLogMetricsProvider
import com.android.settings.spa.development.UsageStatsPageProvider
import com.android.settings.spa.development.compat.PlatformCompatAppListPageProvider
import com.android.settings.spa.home.HomePageProvider
import com.android.settings.spa.network.NetworkAndInternetPageProvider
import com.android.settings.spa.network.NetworkCellularGroupProvider
import com.android.settings.spa.network.SimOnboardingPageProvider
import com.android.settings.spa.notification.AppListNotificationsPageProvider
import com.android.settings.spa.notification.NotificationMainPageProvider
import com.android.settings.spa.system.AppLanguagesPageProvider
import com.android.settings.spa.system.LanguageAndInputPageProvider
import com.android.settings.spa.system.SystemMainPageProvider
import com.android.settings.wifi.details2.WifiPrivacyPageProvider
import com.android.settingslib.spa.framework.common.SettingsPageProviderRepository
import com.android.settingslib.spa.framework.common.SpaEnvironment
import com.android.settingslib.spa.framework.common.SpaLogger
import com.android.settingslib.spa.framework.common.createSettingsPage
import com.android.settingslib.spaprivileged.template.app.TogglePermissionAppListProvider
import com.android.settingslib.spaprivileged.template.app.TogglePermissionAppListTemplate
import com.android.settingslib.widget.theme.flags.Flags

open class SettingsSpaEnvironment(context: Context) : SpaEnvironment(context) {
    open fun getTogglePermissionAppListProviders(): List<TogglePermissionAppListProvider> {
        return listOf(
                AllFilesAccessAppListProvider,
                DisplayOverOtherAppsAppListProvider,
                MediaManagementAppsAppListProvider,
                MediaRoutingControlAppListProvider,
                ModifySystemSettingsAppListProvider,
                UseFullScreenIntentAppListProvider,
                PictureInPictureListProvider,
                InstallUnknownAppsListProvider,
                AlarmsAndRemindersAppListProvider,
                WifiControlAppListProvider,
                NfcTagAppsSettingsProvider,
                LongBackgroundTasksAppListProvider,
                TurnScreenOnAppsAppListProvider,
                WriteSystemPreferencesAppListProvider,
                UsageDataAppListProvider,
            )
            .runIfHidAccessEnabled { plus(HidAccessAppListProvider) }
    }

    override val pageProviderRepository = lazy {
        val togglePermissionAppListTemplate =
            TogglePermissionAppListTemplate(allProviders = getTogglePermissionAppListProviders())
        SettingsPageProviderRepository(
            allPageProviders =
                settingsPageProviders() + togglePermissionAppListTemplate.createPageProviders(),
            rootPages = listOf(HomePageProvider.createSettingsPage()),
        )
    }

    open fun settingsPageProviders() =
        listOf(
                HomePageProvider,
                AppsMainPageProvider,
                AllAppListPageProvider,
                AppInfoSettingsProvider,
                SpecialAppAccessPageProvider,
                NotificationMainPageProvider,
                AppListNotificationsPageProvider.AllApps,
                AppListNotificationsPageProvider.ExcludeClassification,
                AppListNotificationsPageProvider.ExcludeSummarization,
                SystemMainPageProvider,
                LanguageAndInputPageProvider,
                AppLanguagesPageProvider,
                UsageStatsPageProvider,
                PlatformCompatAppListPageProvider,
                BackgroundInstalledAppsPageProvider,
                UserAspectRatioAppsPageProvider,
                CloneAppInfoSettingsProvider,
                NetworkAndInternetPageProvider,
                AboutPhonePageProvider,
                StorageAppListPageProvider.Apps,
                StorageAppListPageProvider.Games,
                ApnEditPageProvider,
                SimOnboardingPageProvider,
                BatteryOptimizationModeAppListPageProvider,
                NetworkCellularGroupProvider(),
                WifiPrivacyPageProvider,
                PrintSettingsPageProvider,
                ForceDarkAppExceptionsPageProvider,
            )
            .runIfComputerControlEnabled {
                plus(
                    arrayOf(
                        ComputerControlAutomationAppListProvider,
                        ComputerControlAppInfoPageProvider,
                        ComputerControlTargetAppPageProvider,
                    )
                )
            }
            .plus(
                arrayOf(
                    com.android.settings.applications.AswAdapterUseHardenedMalloc.makeAppListPageProvider(),
                    com.android.settings.applications.AswAdapterUseExtendedVaSpace.makeAppListPageProvider(),
                    com.android.settings.applications.AswAdapterNativeDebugging.makeAppListPageProvider(),
                    com.android.settings.applications.AswAdapterUseMemoryTagging.makeAppListPageProvider(),
                    com.android.settings.applications.AswAdapterWebViewDynCodeLoading.makeAppListPageProvider(),
                    com.android.settings.applications.AswAdapterMemoryDynCodeLoading.makeAppListPageProvider(),
                    com.android.settings.applications.AswAdapterStorageDynCodeLoading.makeAppListPageProvider(),
                    com.android.settings.applications.AswAdapterManagePlayIntegrityApi.makeAppListPageProvider(),
                    com.android.settings.applications.AswAdapterUseExecSpawning.makeAppListPageProvider(),
                    com.android.settings.network.telephony.carriersettingsoverride.CarrierSettingsOverridesProvider,
                )
            )

    override val logger =
        if (FeatureFlagUtils.isEnabled(context, FeatureFlagUtils.SETTINGS_ENABLE_SPA_METRICS))
            SpaLogMetricsProvider // ToDo: Implement 'SpaLogProvider' for SPA settings.
        else object : SpaLogger {}

    override val isSpaExpressiveEnabled by lazy {
        super.isSpaExpressiveEnabled || Flags.isExpressiveDesignEnabled()
    }
}

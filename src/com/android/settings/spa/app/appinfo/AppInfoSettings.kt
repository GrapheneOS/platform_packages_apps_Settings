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

package com.android.settings.spa.app.appinfo

import android.app.Activity
import android.app.settings.SettingsEnums
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Bundle
import android.os.UserHandle
import android.os.UserManager
import android.util.FeatureFlagUtils
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.Fragment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.android.settings.R
import com.android.settings.applications.AppInfoBase
import com.android.settings.applications.appinfo.AppInfoDashboardFragment
import com.android.settings.flags.Flags
import com.android.settings.personalcontext.PersonalContextAppPreference
import com.android.settings.spa.SpaActivity.Companion.startSpaActivity
import com.android.settings.spa.app.appcompat.UserAspectRatioAppPreference
import com.android.settings.spa.app.specialaccess.AlarmsAndRemindersAppListProvider
import com.android.settings.spa.app.specialaccess.ComputerControlAutomationAppListProvider
import com.android.settings.spa.app.specialaccess.ComputerControlTargetAppPageProvider
import com.android.settings.spa.app.specialaccess.DisplayOverOtherAppsAppListProvider
import com.android.settings.spa.app.specialaccess.InstallUnknownAppsListProvider
import com.android.settings.spa.app.specialaccess.ModifySystemSettingsAppListProvider
import com.android.settings.spa.app.specialaccess.PictureInPictureListProvider
import com.android.settings.spa.app.specialaccess.UsageDataAppListProvider
import com.android.settings.spa.app.specialaccess.WriteSystemPreferencesAppListProvider
import com.android.settings.utils.HsuUtils
import com.android.settingslib.spa.framework.common.SettingsPageProvider
import com.android.settingslib.spa.framework.compose.LifecycleEffect
import com.android.settingslib.spa.framework.compose.navigator
import com.android.settingslib.spa.widget.scaffold.RegularScaffold
import com.android.settingslib.spa.widget.ui.Category
import com.android.settingslib.spaprivileged.model.app.toRoute
import com.android.settingslib.spaprivileged.template.app.AppInfoProvider
import kotlinx.coroutines.flow.MutableStateFlow

private const val PACKAGE_NAME = "packageName"
private const val USER_ID = "userId"

object AppInfoSettingsProvider : SettingsPageProvider {
    override val name = "AppInfoSettings"

    override val parameter =
        listOf(
            navArgument(PACKAGE_NAME) { type = NavType.StringType },
            navArgument(USER_ID) { type = NavType.IntType },
        )

    const val METRICS_CATEGORY = SettingsEnums.APPLICATIONS_INSTALLED_APP_DETAILS

    @Composable
    override fun Page(arguments: Bundle?) {
        val packageName = arguments!!.getString(PACKAGE_NAME)!!
        val userId = arguments.getInt(USER_ID)
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        val packageInfoPresenter = remember {
            PackageInfoPresenter(context, packageName, userId, coroutineScope)
        }
        AppInfoSettings(packageInfoPresenter)
        packageInfoPresenter.PackageFullyRemovedEffect()
    }

    @Composable fun navigator(app: ApplicationInfo) = navigator(route = "$name/${app.toRoute()}")

    /**
     * Gets the route to the App Info Settings page.
     *
     * Expose route to enable enter from non-SPA pages.
     */
    fun getRoute(packageName: String, userId: Int): String = "$name/$packageName/$userId"

    /**
     * Starts the App Info Settings page from non-SPA.
     *
     * Will starts SPA version if flag [FeatureFlagUtils.SETTINGS_ENABLE_SPA] is true.
     */
    @JvmStatic
    fun startAppInfoSettings(
        packageName: String,
        uid: Int,
        source: Fragment,
        request: Int,
        sourceMetricsCategory: Int,
    ) {
        val context = source.context ?: return
        if (FeatureFlagUtils.isEnabled(context, FeatureFlagUtils.SETTINGS_ENABLE_SPA)) {
            context.startSpaActivity(getRoute(packageName, UserHandle.getUserId(uid)))
        } else {
            AppInfoBase.startAppInfoFragment(
                AppInfoDashboardFragment::class.java,
                context.getString(R.string.application_info_label),
                packageName,
                uid,
                source,
                request,
                sourceMetricsCategory,
            )
        }
    }
}

@Composable
private fun AppInfoSettings(packageInfoPresenter: PackageInfoPresenter) {
    // needed to refresh AppSwitch items after returning from their fragments
    LifecycleEffect(onStart = { check(packageInfoPresenter.manualRefreshFlow.tryEmit(Any())) })
    val packageInfoState = packageInfoPresenter.flow.collectAsStateWithLifecycle()
    val context = LocalContext.current
    RegularScaffold(
        title = stringResource(R.string.application_info_label),
        actions = {
            packageInfoState.value?.applicationInfo?.let { app ->
                if (isArchivingEnabled()) TopBarAppLaunchButton(packageInfoPresenter, app)
                AppInfoSettingsMoreOptions(packageInfoPresenter, app)
            }
        },
    ) {
        val packageInfo = packageInfoState.value ?: return@RegularScaffold
        val app = packageInfo.applicationInfo ?: return@RegularScaffold

        // In Headless System User Mode, non-admin users are restricted from controlling HSU apps
        // to prevent system-wide impact. Block access to the App Info page for these users.
        if (android.multiuser.Flags.hsuAppManagement() && !HsuUtils.canControlHsuApp(context, app)) {
            val activity = context as? Activity
            activity?.finish()
            return@RegularScaffold
        }

        val appInfoProvider = remember(packageInfo) { AppInfoProvider(packageInfo) }
        val isHibernationSwitchEnabledStateFlow = MutableStateFlow(false)
        val isContinueAcrossDevicesSwitchEnabledStateFlow = MutableStateFlow(false)

        appInfoProvider.AppInfo()

        AppButtons(packageInfoPresenter, isHibernationSwitchEnabledStateFlow)

        Category {
            AppSettingsPreference(app)
            AppAllServicesPreference(app)
            AppNotificationPreference(app)
            AppPermissionPreference(app)
            ManageTargetAppFunctionAccessPreference(app)
            ManageAgentAppFunctionAccessPreference(app)
            AppStorageScopesPreference(app)
            AppContactScopesPreference(app)
            AppStoragePreference(app)
            InstantAppDomainsPreference(app)
            AppDataUsagePreference(app)
            AppTimeSpentPreference(app)
            AppBatteryPreference(app)
            AppLocalePreference(app)
            AppOpenByDefaultPreference(app)
            DefaultAppShortcuts(app)
            AppLogcatPreference(app)
        }

        if (com.android.window.flags.Flags.virtualGamepadOverride()) {
            Category(title = stringResource(R.string.app_info_experience_category)) {
                VirtualGamepadPreference(app)
            }
        }

        Category(title = stringResource(R.string.unused_apps_category)) {
            HibernationSwitchPreference(app, isHibernationSwitchEnabledStateFlow)
        }

        if (android.companion.Flags.taskContinuity()) {
            Category(title = stringResource(R.string.task_continuity_category)) {
                ContinueAcrossDevicesSwitchPreference(
                    app,
                    isContinueAcrossDevicesSwitchEnabledStateFlow,
                )
            }
        }

        Category(title = stringResource(R.string.ai_assist_category)) {
            PersonalContextAppPreference(app)
            if (
                android.companion.virtualdevice.flags.Flags.computerControlAccess() &&
                    android.companion.virtualdevice.flags.Flags.computerControlPerAppConsent()
            ) {
                ComputerControlTargetAppPageProvider.InfoPageEntryItem(app)
            }
        }

        Category(title = stringResource(R.string.advanced_apps)) {
            com.android.settings.applications.AppManagePlayIntegrityApiPreference(app)
            if (android.companion.virtualdevice.flags.Flags.computerControlAccess()) {
                ComputerControlAutomationAppListProvider.InfoPageEntryItem(app)
            }
            UserAspectRatioAppPreference(app)
            DisplayOverOtherAppsAppListProvider.InfoPageEntryItem(app)
            ModifySystemSettingsAppListProvider.InfoPageEntryItem(app)
            PictureInPictureListProvider.InfoPageEntryItem(app)
            InstallUnknownAppsListProvider.InfoPageEntryItem(app)
            InteractAcrossProfilesDetailsPreference(app)
            AlarmsAndRemindersAppListProvider.InfoPageEntryItem(app)
            WriteSystemPreferencesAppListProvider.InfoPageEntryItem(app)
            Enable16KbAppCompatPreference(app, packageInfoPresenter)
            UsageDataAppListProvider.InfoPageEntryItem(app)
        }

        Category(title = stringResource(R.string.exploit_protection_category_title)) {
            AppExploitProtectionCompatModeSwitchPreference(app, packageInfoPresenter)
            com.android.settings.applications.AppHardenedMallocPreference(app)
            com.android.settings.applications.AppMemtagPreference(app)
            com.android.settings.applications.AppExtendedVaSpacePreference(app)
            com.android.settings.applications.AppExecSpawningPreference(app)
            com.android.settings.applications.AppNativeDebuggingPreference(app)
            com.android.settings.applications.AppWebViewDynCodeLoadingPreference(app)
            com.android.settings.applications.AppMemoryDynCodeLoadingPreference(app)
            com.android.settings.applications.AppStorageDynCodeLoadingPreference(app)
        }

        Category(title = stringResource(R.string.app_install_details_group_title)) {
            AppInstallerInfoPreference(app)
        }

        appInfoProvider.FooterAppVersion()
    }
}

fun isArchivingEnabled() =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM || Flags.appArchiving()

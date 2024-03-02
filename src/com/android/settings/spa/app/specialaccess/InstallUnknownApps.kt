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

package com.android.settings.spa.app.specialaccess

import android.Manifest
import android.app.AppGlobals
import android.app.AppOpsManager
import android.app.AppOpsManager.MODE_DEFAULT
import android.app.AppOpsManager.OP_REQUEST_INSTALL_PACKAGES
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.GosPackageState
import android.content.pm.PackageInfo
import android.os.UserManager
import androidx.compose.runtime.Composable
import com.android.settings.R
import com.android.settingslib.spa.livedata.observeAsCallback
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.res.stringResource
import com.android.settingslib.spa.framework.compose.stateOf
import com.android.settingslib.spa.widget.preference.SwitchPreference
import com.android.settingslib.spa.widget.preference.SwitchPreferenceModel
import com.android.settingslib.spaprivileged.model.app.AppOpsController
import com.android.settingslib.spaprivileged.model.app.AppRecord
import com.android.settingslib.spaprivileged.model.app.userId
import com.android.settingslib.spaprivileged.template.app.TogglePermissionAppListModel
import com.android.settingslib.spaprivileged.template.app.TogglePermissionAppListProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

object InstallUnknownAppsListProvider : TogglePermissionAppListProvider {
    override val permissionType = "InstallUnknownApps"
    override fun createModel(context: Context) = InstallUnknownAppsListModel(context)
}

data class InstallUnknownAppsRecord(
    override val app: ApplicationInfo,
    val appOpsController: AppOpsController,
) : AppRecord {
    val isObbFlagSet = mutableStateOf(isObbFlagSet())

    fun isObbFlagSet(): Boolean {
        return GosPackageState.get(app.packageName)?.hasFlag(GosPackageState.FLAG_ALLOW_ACCESS_TO_OBB_DIRECTORY) == true
    }

    fun setObbFlagState(state: Boolean): Boolean {
        GosPackageState.edit(app.packageName).run {
            setFlagsState(GosPackageState.FLAG_ALLOW_ACCESS_TO_OBB_DIRECTORY, state)
            killUidAfterApply()
            if (apply()) {
                isObbFlagSet.value = state
                return true
            } else {
                return false
            }
        }
    }
}

class InstallUnknownAppsListModel(private val context: Context) :
    TogglePermissionAppListModel<InstallUnknownAppsRecord> {
    override val pageTitleResId = com.android.settingslib.R.string.install_other_apps
    override val switchTitleResId = R.string.external_source_switch_title
    override val footerResId = R.string.install_all_warning
    override val switchRestrictionKeys =
        listOf(
            UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES,
            UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES_GLOBALLY,
        )
    override val enhancedConfirmationKey: String = AppOpsManager.OPSTR_REQUEST_INSTALL_PACKAGES

    override fun transformItem(app: ApplicationInfo) =
        InstallUnknownAppsRecord(
            app = app,
            appOpsController =
                AppOpsController(
                    context = context,
                    app = app,
                    op = OP_REQUEST_INSTALL_PACKAGES,
                ),
        )

    override fun filter(
        userIdFlow: Flow<Int>,
        recordListFlow: Flow<List<InstallUnknownAppsRecord>>,
    ) =
        userIdFlow.map(::getPotentialPackageNames).combine(recordListFlow) {
            potentialPackageNames,
            recordList ->
            recordList.filter { record -> isChangeable(record, potentialPackageNames) }
        }

    @Composable
    override fun isAllowed(record: InstallUnknownAppsRecord) =
        record.appOpsController.isAllowed.observeAsCallback()

    override fun isChangeable(record: InstallUnknownAppsRecord) =
        isChangeable(record, getPotentialPackageNames(record.app.userId))

    override fun setAllowed(record: InstallUnknownAppsRecord, newAllowed: Boolean) {
        record.appOpsController.setAllowed(newAllowed)
        if (!newAllowed) {
            record.setObbFlagState(false)
        }
    }

    @Composable
    override fun extContent(record: InstallUnknownAppsRecord, pkgInfo: PackageInfo) {
        SwitchPreference(object : SwitchPreferenceModel {
            override val title = stringResource(R.string.allow_access_to_obb_directory_title)
            override val summary = stateOf(stringResource(R.string.allow_access_to_obb_directory_summary))

            override val changeable = record.appOpsController.isAllowed.observeAsState(false)
            override val checked = record.isObbFlagSet
            override val onCheckedChange = { newChecked: Boolean ->
                record.setObbFlagState(newChecked)
                Unit
            }
        })
    }

    companion object {
        private fun isChangeable(
            record: InstallUnknownAppsRecord,
            potentialPackageNames: Set<String>,
        ) =
            record.appOpsController.getMode() != MODE_DEFAULT ||
                record.app.packageName in potentialPackageNames

        private fun getPotentialPackageNames(userId: Int): Set<String> =
            AppGlobals.getPackageManager()
                .getAppOpPermissionPackages(Manifest.permission.REQUEST_INSTALL_PACKAGES, userId)
                .toSet()
    }
}

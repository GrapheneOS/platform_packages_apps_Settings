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
package com.android.settings.supervision.credentialmanagement

import android.app.settings.SettingsEnums
import android.app.settings.SettingsEnums.ACTION_SUPERVISION_MANAGE_PIN
import android.app.supervision.SupervisionManager
import android.app.supervision.SupervisionRecoveryInfo.STATE_PENDING
import android.app.supervision.flags.Flags
import android.content.Context
import com.android.settings.CatalystSettingsActivity
import com.android.settings.R
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.metrics.PreferenceActionMetricsProvider
import com.android.settings.supervision.shared.isSupervisingCredentialSet
import com.android.settings.supervision.shared.shouldDisplayPinRecoveryReminders
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.metadata.PreferenceIconProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.widget.UntitledPreferenceCategoryMetadata
import kotlinx.coroutines.CoroutineScope
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen.Companion.APP_FUNCTION_NONE

/** Activity to display [SupervisionPinManagementScreen]. */
class SupervisionPinManagementActivity :
    CatalystSettingsActivity(SupervisionPinManagementScreen.KEY)

/** Pin Management landing page (Settings > Supervision > Manage Pin). */
@ProvidePreferenceScreen(SupervisionPinManagementScreen.KEY)
class SupervisionPinManagementScreen :
    PreferenceScreenMixin,
    PreferenceAvailabilityProvider,
    PreferenceSummaryProvider,
    PreferenceActionMetricsProvider,
    PreferenceIconProvider {
    override fun tags(context: Context) = arrayOf(APP_FUNCTION_NONE)

    override val key: String
        get() = KEY

    // TODO(b/462618020) Catalyst-purpose: replace default purpose with 2 line description
    override val purpose: Int
        get() = R.string.supervision_pin_management_purpose

    override val preferenceActionMetrics: Int
        get() = ACTION_SUPERVISION_MANAGE_PIN

    override val title: Int
        get() =
            if (Flags.enableSupervisionSettingsUiUpdates())
                R.string.supervision_manage_pin_preference_title
            else R.string.supervision_pin_management_preference_title

    override val screenTitle: Int
        get() = R.string.supervision_pin_management_screen_title

    override val indexable
        get() = true

    override val keywords: Int
        get() = R.string.supervision_pin_management_preference_keywords

    override val highlightMenuKey: Int
        get() = R.string.menu_key_supervision

    override fun getMetricsCategory() = SettingsEnums.SUPERVISION_MANAGE_PIN_SCREEN

    // There is an implicit dependency on SupervisionSetupRecoveryPreference due to `getSummary`,
    // which can be removed if `SupervisionManager.supervisionRecoveryInfo` supports
    // observer/listener mechanism on change. Similarly, the `isAvailable` is dependent on
    // `SupervisionPinRecoveryPreference` and `SupervisionDeletePinPreference`.
    override fun dependencies(context: Context) =
        arrayOf(
            SupervisionSetupRecoveryPreference.KEY,
            SupervisionPinRecoveryPreference.KEY,
            SupervisionDeletePinPreference.KEY,
        )

    override val availabilityDescription =
        "The device must have a supervising credential."

    override fun getAvailabilityStability() = PreconditionStability.UNSTABLE

    override fun isAvailable(context: Context) = context.isSupervisingCredentialSet()

    override fun getSummary(context: Context): CharSequence? {
        if (
            !Flags.enableSupervisionPinRecoveryScreen() ||
                (Flags.enableSupervisionSettingsUiUpdates() &&
                    !context.shouldDisplayPinRecoveryReminders())
        ) {
            return null
        }
        val recoveryInfo =
            context.getSystemService(SupervisionManager::class.java)?.getSupervisionRecoveryInfo()
        return when {
            recoveryInfo == null -> {
                context.getString(R.string.supervision_pin_management_preference_summary_add)
            }
            recoveryInfo.state == STATE_PENDING -> {
                context.getString(
                    R.string.supervision_pin_management_preference_summary_verify_recovery
                )
            }
            else -> null
        }
    }

    // TODO(b/409837094): get icon with dynamic color.
    override fun getIcon(context: Context): Int {
        if (Flags.enableSupervisionSettingsUiUpdates()) {
            return if (context.shouldDisplayPinRecoveryReminders()) R.drawable.exclamation_icon
            else R.drawable.ic_pin_outline
        }
        if (Flags.enableSupervisionPinRecoveryScreen()) {
            val recoveryInfo =
                context
                    .getSystemService(SupervisionManager::class.java)
                    ?.getSupervisionRecoveryInfo()
            if (recoveryInfo == null || recoveryInfo.state == STATE_PENDING) {
                // if recovery is not fully setup.
                return R.drawable.exclamation_icon
            }
        }
        return R.drawable.ic_pin_outline
    }

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {
            +UntitledPreferenceCategoryMetadata(
                key = GROUP_KEY,
                purpose = R.string.pin_management_group_purpose,
            ) +=
                {
                    +SupervisionPinRecoveryPreference()
                    +SupervisionChangePinPreference()
                    +SupervisionSetupRecoveryPreference()
                    +SupervisionUpdateRecoveryEmailPreference()
                }
            +UntitledPreferenceCategoryMetadata(
                key = "delete_pin_group",
                purpose = R.string.delete_pin_group_purpose,
            ) +=
                {
                    +SupervisionDeletePinPreference()
                }
            +SupervisionPinManagementFooterPreference()
        }

    override fun hasCompleteHierarchy() = true

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        makeLaunchIntent(context, SupervisionPinManagementActivity::class.java, metadata?.key)

    companion object {
        const val KEY = "supervision_pin_management"
        internal const val GROUP_KEY = "pin_management_group"
    }
}

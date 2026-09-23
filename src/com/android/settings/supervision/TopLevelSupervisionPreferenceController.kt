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
package com.android.settings.supervision

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.settings.SettingsActivity.EXTRA_IS_SECOND_LAYER_PAGE
import com.android.settings.activityembedding.ActivityEmbeddingRulesController
import com.android.settings.core.BasePreferenceController
import com.android.settings.supervision.shared.isSupervisionSupportedOnDevice

/** Controller for the top level Supervision settings Preference item. */
class TopLevelSupervisionPreferenceController(context: Context, key: String) :
    BasePreferenceController(context, key) {

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        ActivityEmbeddingRulesController.registerTwoPanePairRuleForSettingsHome(
            mContext,
            ComponentName(mContext, SupervisionDashboardActivity::class.java),
            /* secondaryIntentAction= */ null,
            /* clearTop= */ true,
        )
    }

    override fun handlePreferenceTreeClick(preference: Preference): Boolean {
        if (preference.key == preferenceKey) {
            val intent = Intent(mContext, SupervisionDashboardActivity::class.java)
            intent.putExtra(EXTRA_IS_SECOND_LAYER_PAGE, true)
            mContext.startActivity(intent)
            return true
        }
        return super.handlePreferenceTreeClick(preference)
    }

    override fun getAvailabilityStatus(): Int {
        // Hide the supervision entry in settings if the necessary supervision component is not
        // available and can't be fixed by user.
        return if (mContext.isSupervisionSupportedOnDevice()) AVAILABLE else UNSUPPORTED_ON_DEVICE
    }
}

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

import android.app.KeyguardManager
import android.app.supervision.ISupervisionManager
import android.app.supervision.SupervisionManager
import android.app.supervision.SupervisionRecoveryInfo
import android.app.supervision.SupervisionRecoveryInfo.STATE_PENDING
import android.app.supervision.SupervisionRecoveryInfo.STATE_VERIFIED
import android.app.supervision.flags.Flags
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.UserInfo
import android.net.Uri
import android.os.UserManager
import android.os.UserManager.USER_TYPE_PROFILE_SUPERVISING
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.provider.Settings.Global
import android.text.Spanned
import android.text.style.ClickableSpan
import android.view.View
import android.widget.TextView
import androidx.preference.PreferenceGroupAdapter
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.settings.R
import com.android.settingslib.preference.launchFragmentScenario
import com.android.settingslib.widget.FooterPreference
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowServiceManager

@RunWith(AndroidJUnit4::class)
class SupervisionPinManagementScreenTest {
    private val mockKeyguardManager = mock<KeyguardManager>()
    private val mockUserManager = mock<UserManager>()
    private val mockSupervisionManager = mock<SupervisionManager>()
    private val mockISupervisionManager = mock<ISupervisionManager>()

    @get:Rule val setFlagsRule = SetFlagsRule()

    private val context: Context =
        object : ContextWrapper(ApplicationProvider.getApplicationContext()) {
            override fun getSystemService(name: String): Any? =
                when (name) {
                    Context.KEYGUARD_SERVICE -> mockKeyguardManager
                    Context.USER_SERVICE -> mockUserManager
                    Context.SUPERVISION_SERVICE -> mockSupervisionManager
                    else -> super.getSystemService(name)
                }
        }

    private val supervisionPinManagementScreen = SupervisionPinManagementScreen()

    @Before
    fun setUp() {
        ShadowServiceManager.addBinderService(
            Context.SUPERVISION_SERVICE,
            ISupervisionManager::class.java,
            mockISupervisionManager,
        )
    }

    @Test
    fun key() {
        assertThat(supervisionPinManagementScreen.key).isEqualTo(SupervisionPinManagementScreen.KEY)
    }

    @Test
    fun isAvailable() {
        whenever(mockUserManager.users).thenReturn(listOf(SUPERVISING_USER_INFO))
        whenever(mockKeyguardManager.isDeviceSecure(SUPERVISING_USER_ID)).thenReturn(true)

        assertThat(supervisionPinManagementScreen.isAvailable(context)).isTrue()
    }

    @Test
    fun isAvailable_noSupervisingUser_returnsFalse() {
        whenever(mockUserManager.users).thenReturn(emptyList())
        whenever(mockKeyguardManager.isDeviceSecure(SUPERVISING_USER_ID)).thenReturn(true)

        assertThat(supervisionPinManagementScreen.isAvailable(context)).isFalse()
    }

    @Test
    fun isAvailable_noSupervisingCredential_returnsFalse() {
        whenever(mockUserManager.users).thenReturn(listOf(SUPERVISING_USER_INFO))
        whenever(mockKeyguardManager.isDeviceSecure(SUPERVISING_USER_ID)).thenReturn(false)

        assertThat(supervisionPinManagementScreen.isAvailable(context)).isFalse()
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun getTitle_withoutUiUpdates() {
        assertThat(supervisionPinManagementScreen.title)
            .isEqualTo(R.string.supervision_pin_management_preference_title)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun getTitle() {
        assertThat(supervisionPinManagementScreen.title)
            .isEqualTo(R.string.supervision_manage_pin_preference_title)
    }

    @Test
    fun getKeywords() {
        assertThat(supervisionPinManagementScreen.keywords)
            .isEqualTo(R.string.supervision_pin_management_preference_keywords)
    }

    @Test
    fun isIndexable() {
        assertThat(supervisionPinManagementScreen.indexable).isFalse()
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_PIN_RECOVERY_SCREEN)
    @DisableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun getDescription_recoveryInfoMissing_addPinRecovery() {
        whenever(mockSupervisionManager.getSupervisionRecoveryInfo()).thenReturn(null)

        assertThat(supervisionPinManagementScreen.getSummary(context))
            .isEqualTo(
                context.getString(R.string.supervision_pin_management_preference_summary_add)
            )
        assertThat(supervisionPinManagementScreen.getIcon(context))
            .isEqualTo(R.drawable.exclamation_icon)
    }

    @Test
    @EnableFlags(
        Flags.FLAG_ENABLE_SUPERVISION_PIN_RECOVERY_SCREEN,
        Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES,
    )
    fun getDescription_shouldShowPinRecoveryRemindersAndrecoveryInfoMissing_addPinRecovery() {
        whenever(mockISupervisionManager.hasValidRecoveryMethod(any())).thenReturn(false)
        whenever(mockSupervisionManager.getSupervisionRecoveryInfo()).thenReturn(null)

        assertThat(supervisionPinManagementScreen.getSummary(context))
            .isEqualTo(
                context.getString(R.string.supervision_pin_management_preference_summary_add)
            )
        assertThat(supervisionPinManagementScreen.getIcon(context))
            .isEqualTo(R.drawable.exclamation_icon)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_PIN_RECOVERY_SCREEN)
    @DisableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun getDescription_recoveryNotVerified_verifyPinRecovery() {
        val recoveryInfo =
            SupervisionRecoveryInfo(
                /* accountName */ "email",
                /* accountType */ "default",
                /* state */ STATE_PENDING,
                /* accountData */ null,
            )
        whenever(mockSupervisionManager.getSupervisionRecoveryInfo()).thenReturn(recoveryInfo)

        assertThat(supervisionPinManagementScreen.getSummary(context))
            .isEqualTo(
                context.getString(
                    R.string.supervision_pin_management_preference_summary_verify_recovery
                )
            )
        assertThat(supervisionPinManagementScreen.getIcon(context))
            .isEqualTo(R.drawable.exclamation_icon)
    }

    @Test
    @EnableFlags(
        Flags.FLAG_ENABLE_SUPERVISION_PIN_RECOVERY_SCREEN,
        Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES,
    )
    fun getDescription_shouldShowPinRecoveryRemindersAndRecoveryNotVerified_verifyPinRecovery() {
        whenever(mockISupervisionManager.hasValidRecoveryMethod(any())).thenReturn(false)
        val recoveryInfo =
            SupervisionRecoveryInfo(
                /* accountName */ "email",
                /* accountType */ "default",
                /* state */ STATE_PENDING,
                /* accountData */ null,
            )
        whenever(mockSupervisionManager.getSupervisionRecoveryInfo()).thenReturn(recoveryInfo)

        assertThat(supervisionPinManagementScreen.getSummary(context))
            .isEqualTo(
                context.getString(
                    R.string.supervision_pin_management_preference_summary_verify_recovery
                )
            )
        assertThat(supervisionPinManagementScreen.getIcon(context))
            .isEqualTo(R.drawable.exclamation_icon)
    }

    @Test
    @EnableFlags(
        Flags.FLAG_ENABLE_SUPERVISION_PIN_RECOVERY_SCREEN,
        Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES,
    )
    fun getDescription_shouldNotShowPinRecoveryReminders_noSummary() {
        whenever(mockISupervisionManager.hasValidRecoveryMethod(any())).thenReturn(true)

        assertThat(supervisionPinManagementScreen.getSummary(context)).isNull()
        assertThat(supervisionPinManagementScreen.getIcon(context))
            .isEqualTo(R.drawable.ic_pin_outline)
    }

    @Test
    @DisableFlags(
        Flags.FLAG_ENABLE_SUPERVISION_PIN_RECOVERY_SCREEN,
        Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES,
    )
    fun getDescription_recoveryFlagDisabled_noSummary() {
        whenever(mockSupervisionManager.getSupervisionRecoveryInfo()).thenReturn(null)

        assertThat(supervisionPinManagementScreen.getSummary(context)).isNull()
        assertThat(supervisionPinManagementScreen.getIcon(context))
            .isEqualTo(R.drawable.ic_pin_outline)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_PIN_RECOVERY_SCREEN)
    @DisableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun getDescription_recoverySetup_noSummary() {
        val recoveryInfo =
            SupervisionRecoveryInfo(
                /* accountName */ "email",
                /* accountType */ "default",
                /* state */ STATE_VERIFIED,
                /* accountData */ null,
            )
        whenever(mockSupervisionManager.getSupervisionRecoveryInfo()).thenReturn(recoveryInfo)

        assertThat(supervisionPinManagementScreen.getSummary(context)).isNull()
        assertThat(supervisionPinManagementScreen.getIcon(context))
            .isEqualTo(R.drawable.ic_pin_outline)
    }

    @Test
    fun footerPreference() {
        supervisionPinManagementScreen.launchFragmentScenario().onFragment { fragment ->
            val footerPreference: FooterPreference =
                fragment.findPreference(SupervisionPinManagementFooterPreference.KEY)!!
            val context = footerPreference.context
            val learnMoreLink = context.getString(R.string.supervision_pin_learn_more_link)

            // Setups the HelpUtils.getHelpIntent
            Global.putInt(context.contentResolver, Global.DEVICE_PROVISIONED, 1)
            shadowOf(context.packageManager).apply {
                val componentName = ComponentName(context, "browser")
                val intentFilter =
                    IntentFilter(Intent.ACTION_VIEW).apply {
                        addCategory(Intent.CATEGORY_DEFAULT)
                        addDataScheme(Uri.parse(learnMoreLink).scheme)
                    }
                addActivityIfNotPresent(componentName)
                addIntentFilterForActivity(componentName, intentFilter)
            }

            // Checks that the footer preference is visible.
            val recyclerView = fragment.listView
            val adapter = recyclerView.adapter as PreferenceGroupAdapter
            val position = adapter.getPreferenceAdapterPosition(footerPreference)
            recyclerView.scrollToPosition(position)
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            val viewHolder = recyclerView.findViewHolderForAdapterPosition(position)!!
            val learnMoreView =
                viewHolder.itemView.findViewById<TextView>(
                    com.android.settingslib.widget.preference.footer.R.id.settingslib_learn_more
                )
            assertThat(learnMoreView.visibility).isEqualTo(View.VISIBLE)

            val text = learnMoreView.text
            (text as Spanned).getSpans(0, text.length, ClickableSpan::class.java).apply {
                assertThat(this).hasLength(1)
                get(0).onClick(learnMoreView)
            }

            val intent = shadowOf(fragment.activity).nextStartedActivity
            assertThat(intent.dataString).isEqualTo(learnMoreLink)
            assertThat(intent.action).isEqualTo(Intent.ACTION_VIEW)
        }
    }

    private companion object {
        const val SUPERVISING_USER_ID = 5
        val SUPERVISING_USER_INFO =
            UserInfo(
                SUPERVISING_USER_ID,
                /* name */ "supervising",
                /* iconPath */ "",
                /* flags */ 0,
                USER_TYPE_PROFILE_SUPERVISING,
            )
    }
}

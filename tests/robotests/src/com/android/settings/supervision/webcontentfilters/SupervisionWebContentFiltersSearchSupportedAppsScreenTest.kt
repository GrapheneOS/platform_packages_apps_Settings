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

import android.app.Application
import android.app.role.RoleManager
import android.app.role.RoleManager.ROLE_SYSTEM_SUPERVISION
import android.app.settings.SettingsEnums
import android.app.supervision.flags.Flags
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
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
import com.android.settings.supervision.TestSupervisionMessengerService
import com.android.settingslib.ipc.MessengerServiceClient
import com.android.settingslib.ipc.MessengerServiceRule
import com.android.settingslib.preference.launchFragmentScenario
import com.android.settingslib.widget.FooterPreference
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.LooperMode
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowContextImpl

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@LooperMode(LooperMode.Mode.INSTRUMENTATION_TEST)
class SupervisionWebContentFiltersSearchSupportedAppsScreenTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val searchSupportedAppsScreen = SupervisionWebContentFiltersSearchSupportedAppsScreen()
    private val packageName: String = "com.android.supervision"
    private val mockRoleManager: RoleManager = mock {
        on { getRoleHolders(ROLE_SYSTEM_SUPERVISION) }.thenReturn(listOf(packageName))
    }

    @get:Rule(order = 0) val setFlagsRule = SetFlagsRule()
    @get:Rule(order = 1)
    val serviceRule =
        MessengerServiceRule<MessengerServiceClient>(TestSupervisionMessengerService::class.java)

    @Before
    fun setUp() {
        (Shadow.extract((context as Application).baseContext) as ShadowContextImpl).apply {
            setSystemService(Context.ROLE_SERVICE, mockRoleManager)
        }
    }

    @Test
    fun key() {
        assertThat(searchSupportedAppsScreen.key)
            .isEqualTo(SupervisionWebContentFiltersSearchSupportedAppsScreen.KEY)
    }

    @Test
    fun screenTitle() {
        assertThat(searchSupportedAppsScreen.screenTitle)
            .isEqualTo(R.string.supervision_web_content_filters_search_filter_title)
    }

    @Test
    fun isIndexable() {
        assertThat(searchSupportedAppsScreen.indexable).isFalse()
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun flagEnabled() {
        assertThat(searchSupportedAppsScreen.isAvailable(context)).isTrue()
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun flagDisabled() {
        assertThat(searchSupportedAppsScreen.isAvailable(context)).isFalse()
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun getMetricsCategory() {
        assertThat(searchSupportedAppsScreen.getMetricsCategory())
            .isEqualTo(SettingsEnums.SUPERVISION_WEB_CONTENT_FILTERS_SEARCH_SUPPORTED_APPS)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun footerPreference() {
        searchSupportedAppsScreen.launchFragmentScenario().onFragment { fragment ->
            val footerPreference: FooterPreference =
                fragment.findPreference(SupervisionWebContentFiltersFooterPreference.KEY)!!
            val context = footerPreference.context
            val learnMoreLink =
                context.getString(R.string.supervision_web_content_filters_learn_more_link)

            // setup for HelpUtils.getHelpIntent
            Global.putInt(context.contentResolver, Global.DEVICE_PROVISIONED, 1)
            shadowOf(context.packageManager).apply {
                val componentName = ComponentName(context, "search")
                val intentFilter =
                    IntentFilter(Intent.ACTION_VIEW).apply {
                        addCategory(Intent.CATEGORY_DEFAULT)
                        addDataScheme(Uri.parse(learnMoreLink).scheme)
                    }
                addActivityIfNotPresent(componentName)
                addIntentFilterForActivity(componentName, intentFilter)
            }

            // ensure the footer preference is visible
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
}

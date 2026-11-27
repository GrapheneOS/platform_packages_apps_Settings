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
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.net.Uri
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.provider.Settings.Global
import android.text.Spanned
import android.text.style.ClickableSpan
import android.view.View
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceGroupAdapter
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.settings.R
import com.android.settingslib.metadata.getPreferenceSummary
import com.android.settingslib.metadata.getPreferenceTitle
import com.android.settingslib.preference.launchFragmentScenario
import com.android.settingslib.widget.FooterPreference
import com.android.settingslib.widget.TopIntroPreference
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowPackageManager

@RunWith(AndroidJUnit4::class)
class SupervisionAppStoreFiltersScreenTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val supervisionAppStoreFiltersScreen = SupervisionAppStoreFiltersScreen()
    private val shadowPackageManager: ShadowPackageManager = shadowOf(context.packageManager)

    @get:Rule val setFlagsRule = SetFlagsRule()

    @Test
    fun key() {
        assertThat(supervisionAppStoreFiltersScreen.key)
            .isEqualTo(SupervisionAppStoreFiltersScreen.KEY)
    }

    @Test
    fun getTitle() {
        assertThat(supervisionAppStoreFiltersScreen.getPreferenceTitle(context))
            .isEqualTo("App store filters")
    }

    @Test
    fun getKeywords() {
        assertThat(supervisionAppStoreFiltersScreen.keywords)
            .isEqualTo(R.string.supervision_app_store_filters_keywords)
    }

    @Test
    fun isIndexable() {
        assertThat(supervisionAppStoreFiltersScreen.indexable).isFalse()
    }

    @Test
    fun getSummary() {
        assertThat(supervisionAppStoreFiltersScreen.getPreferenceSummary(context))
            .isEqualTo("Filter apps by content ratings")
    }

    @Test
    fun getMetricsCategory() {
        assertThat(supervisionAppStoreFiltersScreen.getMetricsCategory())
            .isEqualTo(SettingsEnums.SUPERVISION_APP_STORE_FILTERS)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_APP_STORE_FILTERS_SCREEN)
    fun flagEnabled() {
        assertThat(supervisionAppStoreFiltersScreen.isFlagEnabled(context)).isTrue()
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_APP_STORE_FILTERS_SCREEN)
    fun topIntroExists() {
        supervisionAppStoreFiltersScreen.launchFragmentScenario().onFragment { fragment ->
            val topIntroPreference =
                fragment.findPreference<TopIntroPreference>(
                    SupervisionAppStoreFiltersTopIntroPreference.KEY
                )
            assertThat(topIntroPreference).isNotNull()
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_APP_STORE_FILTERS_SCREEN)
    fun preferenceGroupExists() {
        supervisionAppStoreFiltersScreen.launchFragmentScenario().onFragment { fragment ->
            val preferenceGroup =
                fragment.findPreference<PreferenceGroup>(
                    SupervisionAppStoreFiltersScreen.SUPERVISION_APP_STORE_FILTERS_GROUP
                )
            assertThat(preferenceGroup).isNotNull()
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_APP_STORE_FILTERS_SCREEN)
    fun footerPreference() {
        supervisionAppStoreFiltersScreen.launchFragmentScenario().onFragment { fragment ->
            val footerPreference: FooterPreference =
                fragment.findPreference(SupervisionAppStoreFiltersFooterPreference.KEY)!!

            assertThat(footerPreference).isNotNull()
            val context = footerPreference.context
            val learnMoreLink =
                context.getString(R.string.supervision_app_store_filters_learn_more_link)

            // setup for HelpUtils.getHelpIntent
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

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_APP_STORE_FILTERS_SCREEN)
    fun getAppStoreFiltersEntries_findsAppsWithPermission() {
        val appWithPermissionPackage = "com.example.goodapp"
        val appWithPermissionLabel = "Good App Store"
        val appWithPermissionClass = "com.example.goodapp.AppFilterSettingsActivity"
        val appWithPermissionIconId = R.drawable.ic_apps_library

        val appWithoutPermissionPackage = "com.example.badapp"
        val appWithoutPermissionLabel = "Bad App Store"
        val appWithoutPermissionClass = "com.example.badapp.AppFilterSettingsActivity"
        val appWithoutPermissionIconId = R.drawable.ic_apps

        val anotherAppWithPermissionPackage = "com.example.anothergoodapp"
        val anotherAppWithPermissionLabel = "Another Good App Store"
        val anotherAppWithPermissionClass = "com.example.anothergoodapp.AppFilterSettingsActivity"
        val anotherAppWithPermissionIconId = R.drawable.ic_apps_alt

        addAppStoreActivity(
            componentName = ComponentName(appWithPermissionPackage, appWithPermissionClass),
            label = appWithPermissionLabel,
            hasInstallPackagesPermission = true,
            iconId = appWithPermissionIconId,
        )
        addAppStoreActivity(
            componentName = ComponentName(appWithoutPermissionPackage, appWithoutPermissionClass),
            label = appWithoutPermissionLabel,
            hasInstallPackagesPermission = false,
            iconId = appWithoutPermissionIconId,
        )
        addAppStoreActivity(
            componentName =
                ComponentName(anotherAppWithPermissionPackage, anotherAppWithPermissionClass),
            label = anotherAppWithPermissionLabel,
            hasInstallPackagesPermission = true,
            iconId = anotherAppWithPermissionIconId,
        )

        // check to see that correct app stores are added to the UI
        supervisionAppStoreFiltersScreen.launchFragmentScenario().onFragment { fragment ->
            // Assert: Check the contents of the PreferenceGroup
            val preferenceGroup =
                fragment.findPreference<PreferenceGroup>(
                    SupervisionAppStoreFiltersScreen.SUPERVISION_APP_STORE_FILTERS_GROUP
                )
            assertThat(preferenceGroup).isNotNull()

            assertThat(preferenceGroup!!.preferenceCount).isEqualTo(2)

            val pref1 = preferenceGroup.findPreference<Preference>(appWithPermissionPackage)
            assertThat(pref1).isNotNull()
            assertThat(pref1!!.title.toString()).isEqualTo(appWithPermissionLabel)
            assertThat(pref1.key.toString()).isEqualTo(appWithPermissionPackage)
            assertThat(pref1.icon).isNotNull()

            val pref2 = preferenceGroup.findPreference<Preference>(anotherAppWithPermissionPackage)
            assertThat(pref2).isNotNull()
            assertThat(pref2!!.title.toString()).isEqualTo(anotherAppWithPermissionLabel)
            assertThat(pref2.key.toString()).isEqualTo(anotherAppWithPermissionPackage)
            assertThat(pref2.icon).isNotNull()

            val badPref = preferenceGroup.findPreference<Preference>(appWithoutPermissionPackage)
            assertThat(badPref).isNull()
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_APP_STORE_FILTERS_SCREEN)
    fun onAppStorePreferenceClick_launchesIntent() {
        val appPackage = "com.example.appstore"
        val appClass = "com.example.appstore.SettingsActivity"
        val appLabel = "My App Store"

        addAppStoreActivity(
            componentName = ComponentName(appPackage, appClass),
            label = appLabel,
            hasInstallPackagesPermission = true,
            iconId = R.drawable.ic_apps_library,
        )

        supervisionAppStoreFiltersScreen.launchFragmentScenario().onFragment { fragment ->
            val preferenceGroup =
                fragment.findPreference<PreferenceGroup>(
                    SupervisionAppStoreFiltersScreen.SUPERVISION_APP_STORE_FILTERS_GROUP
                )
            assertThat(preferenceGroup).isNotNull()

            val pref = preferenceGroup!!.findPreference<Preference>(appPackage)
            assertThat(pref).isNotNull()

            pref!!.performClick()

            val shadowActivity = shadowOf(fragment.requireActivity())
            val nextIntentResult = shadowActivity.nextStartedActivityForResult

            assertThat(nextIntentResult).isNotNull()
            val startedIntent = nextIntentResult.intent
            assertThat(startedIntent).isNotNull()

            assertThat(startedIntent.component?.packageName).isEqualTo(appPackage)
            assertThat(startedIntent.component?.className).isEqualTo(appClass)
        }
    }

    private fun addAppStoreActivity(
        componentName: ComponentName,
        label: String,
        hasInstallPackagesPermission: Boolean,
        iconId: Int,
    ) {
        val packageName = componentName.packageName
        val packageInfo =
            PackageInfo().apply {
                this.packageName = packageName
                applicationInfo =
                    ApplicationInfo().apply {
                        this.packageName = packageName
                        name = label
                        icon = iconId
                    }
                if (hasInstallPackagesPermission) {
                    requestedPermissions = arrayOf(Manifest.permission.INSTALL_PACKAGES)
                } else {
                    requestedPermissions = emptyArray()
                }
            }

        shadowPackageManager.installPackage(packageInfo)

        shadowPackageManager.addActivityIfNotPresent(componentName)
        val intentFilter =
            IntentFilter(SupervisionAppStoreFiltersScreen.ACTION_VIEW_APP_FILTER_SETTINGS).apply {
                addCategory(Intent.CATEGORY_DEFAULT)
            }
        shadowPackageManager.addIntentFilterForActivity(componentName, intentFilter)
    }
}

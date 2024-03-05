package com.android.settings.spa.app.appinfo

import android.content.Context
import android.content.pm.ApplicationInfo
import android.ext.settings.app.AppSwitch
import androidx.compose.runtime.Composable
import com.android.settings.applications.appinfo.AppInfoDashboardFragment
import com.android.settings.applications.appinfo.AswAdapter
import com.android.settingslib.spa.framework.compose.stateOf
import com.android.settingslib.spa.widget.preference.Preference
import com.android.settingslib.spa.widget.preference.PreferenceModel
import com.android.settingslib.spaprivileged.model.app.installed

@Composable
fun AswPreference(context: Context, app: ApplicationInfo, adapter: AswAdapter<out AppSwitch>) {
    if (!app.installed) {
        return
    }

}

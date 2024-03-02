package com.android.settings.spa.app.appinfo

import android.content.pm.ApplicationInfo
import android.ext.LogViewerApp
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.android.settings.R
import com.android.settingslib.spa.widget.preference.Preference
import com.android.settingslib.spa.widget.preference.PreferenceModel
import com.android.settingslib.spaprivileged.model.app.installed

@Composable
fun AppLogcatPreference(app: ApplicationInfo) {
    if (!app.installed) {
        return
    }

    val context = LocalContext.current

    Preference(object : PreferenceModel {
        override val title = stringResource(R.string.view_logs)
        override val onClick = {
            val intent = LogViewerApp.getPackageLogcatIntent(app.packageName)
            context.startActivity(intent)
        }
    })
}

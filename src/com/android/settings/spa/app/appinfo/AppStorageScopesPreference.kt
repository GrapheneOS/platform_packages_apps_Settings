package com.android.settings.spa.app.appinfo

import android.app.StorageScope
import android.content.pm.ApplicationInfo
import android.content.pm.GosPackageState
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.android.settings.R
import com.android.settingslib.spa.widget.preference.Preference
import com.android.settingslib.spa.widget.preference.PreferenceModel

@Composable
fun AppStorageScopesPreference(app: ApplicationInfo) {
    val pkgName = app.packageName
    if (GosPackageState.get(pkgName)?.hasFlags(GosPackageState.FLAG_STORAGE_SCOPES_ENABLED) != true) {
        return
    }

    val context = LocalContext.current

    Preference(object : PreferenceModel {
        override val title = stringResource(R.string.storage_scopes)
        override val onClick = {
            val intent = StorageScope.createConfigActivityIntent(pkgName)
            context.startActivity(intent)
        }
    })
}

package com.android.settings.applications.appinfo

import android.content.Context
import android.ext.settings.app.AppSwitch

abstract class AswPrefController<T : AppSwitch>(ctx: Context, key: String,
        val adapter: AswAdapter<T>,
) : AppInfoPreferenceControllerBase(ctx, key) {

    override fun getSummary() = adapter.getPreferenceSummary(mAppEntry.info)
}

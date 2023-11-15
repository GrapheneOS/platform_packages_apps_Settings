package com.android.settings.applications.appinfo

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.GosPackageState
import android.ext.settings.app.AppSwitch
import androidx.annotation.StringRes
import com.android.settings.R

abstract class AswAdapter<T : AppSwitch>(val context: Context, val userId: Int = context.userId) {
    abstract fun getAppSwitch(): T

    fun getPreferenceSummary(appInfo: ApplicationInfo): CharSequence {
        val asw = getAppSwitch()
        val si = AppSwitch.StateInfo()
        val isOn = asw.get(context, userId, appInfo, GosPackageState.get(appInfo.packageName, userId), si)
        return if (si.isUsingDefaultValue) {
            getDefaultTitle(isOn)
        } else {
            if (isOn) getOnTitle() else getOffTitle()
        }
    }

    abstract fun getAswTitle(): CharSequence

    fun getDefaultTitle(isOn: Boolean): CharSequence {
        return context.getString(R.string.aep_default,
            if (isOn) getOnTitle() else getOffTitle())
    }

    open fun getOnTitle(): CharSequence = getText(R.string.aep_enabled)

    open fun getOffTitle(): CharSequence = getText(R.string.aep_disabled)

    protected fun getText(@StringRes id: Int): CharSequence = context.getText(id)

    protected fun getString(@StringRes id: Int): String = context.getString(id)
}

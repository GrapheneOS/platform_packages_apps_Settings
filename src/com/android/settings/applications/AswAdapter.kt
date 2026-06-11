package com.android.settings.applications

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.GosPackageState
import android.ext.settings.app.AppSwitch
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment
import com.android.settings.applications.appinfo.AppInfoDashboardFragment
import com.android.settings.applications.appinfo.RadioButtonAppInfoFragment
import com.android.settings.spa.SpaActivity.Companion.startSpaActivity
import com.android.settings.spa.app.appinfo.AppInfoSettingsProvider
import com.android.settings.spa.app.appinfo.AswAppListPageProvider
import com.android.settingslib.spaprivileged.model.app.userId
import kotlin.reflect.KClass

abstract class AswAdapter<T : AppSwitch> {
    abstract fun getAppSwitch(): T

    enum class Category {
        ExploitProtection,
    }

    fun getCategory(): Category = Category.ExploitProtection

    fun getPreferenceSummary(ctx: Context, appInfo: ApplicationInfo): CharSequence {
        val asw = getAppSwitch()
        val si = AppSwitch.StateInfo()
        val userId = appInfo.userId
        val isOn = asw.get(ctx, userId, appInfo, GosPackageState.get(appInfo.packageName, userId), si)
        return if (si.isUsingDefaultValue) {
            getDefaultTitle(ctx, isOn)
        } else {
            if (isOn) getOnTitle(ctx) else getOffTitle(ctx)
        }
    }

    open fun getNotificationToggleTitle(ctx: Context): CharSequence? = null
    open fun getNotificationToggleSummary(ctx: Context): CharSequence? = null

    abstract fun getAswTitle(ctx: Context): CharSequence

    open fun getShortAswTitle(ctx: Context) = getAswTitle(ctx)

    fun getDefaultTitle(ctx: Context, isOn: Boolean): CharSequence {
        return ctx.getString(R.string.aep_default,
            if (isOn) getOnTitle(ctx) else getOffTitle(ctx)
        )
    }

    open fun getOnTitle(ctx: Context): CharSequence = ctx.getText(R.string.aep_enabled)

    open fun getOffTitle(ctx: Context): CharSequence = ctx.getText(R.string.aep_disabled)

    abstract fun getDetailFragmentClass(): KClass<out SettingsPreferenceFragment>

    fun openAppPage(context: Context, app: ApplicationInfo) {
        AppInfoDashboardFragment.startAppInfoFragment(
            getDetailFragmentClass().java,
            app, context, AppInfoSettingsProvider.METRICS_CATEGORY,
        )
    }

    fun makeAppListPageProvider() = AswAppListPageProvider(this)

    fun getAppListPageProviderName() = this::class.simpleName!!

    open fun shouldIncludeInAppListPage(app: ApplicationInfo, gosPs: GosPackageState) = true

    fun openAppListPage(ctx: Context) {
        ctx.startSpaActivity(getAppListPageProviderName())
    }

    @JvmOverloads
    fun addAppListPageLink(screen: PreferenceScreen,
                           title: CharSequence = screen.context.getText(R.string.default_see_all_apps_title)) {
        val ctx = screen.context
        val pref = Preference(ctx).apply {
            this.title = title
            setOnPreferenceClickListener { p ->
                p.context.startSpaActivity(getAppListPageProviderName())
                true
            }
        }
        screen.addPreference(pref)
    }
}

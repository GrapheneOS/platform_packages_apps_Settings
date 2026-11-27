package com.android.settings.applications

import android.content.Context
import android.content.pm.ApplicationInfo
import android.ext.settings.ExtSettings
import android.ext.settings.app.VpnDisguise
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.ext.BoolSettingFragment
import com.android.settings.ext.BoolSettingFragmentPrefController
import com.android.settings.ext.ExtSettingControllerHelper
import com.android.settings.spa.app.appinfo.AswPreference
import com.android.settingslib.widget.FooterPreference

object AswAdapterVpnDisguise : AswAdapter<VpnDisguise>() {

    override fun getAppSwitch() = VpnDisguise.I

    override fun getAswTitle(ctx: Context) = ctx.getText(R.string.vpn_disguise)

    override fun getOnTitle(ctx: Context) = ctx.getText(R.string.vpn_disguise_enabled)
    override fun getOffTitle(ctx: Context) = ctx.getText(R.string.vpn_disguise_disabled)

    override fun getDetailFragmentClass() = AppVpnDisguiseFragment::class
}

@Composable
fun AppVpnDisguisePreference(app: ApplicationInfo) {
    val context = LocalContext.current
    AswPreference(context, app, AswAdapterVpnDisguise)
}

class AppVpnDisguiseFragment : AswAppInfoFragment<VpnDisguise>() {

    override fun getAswAdapter() = AswAdapterVpnDisguise

    override fun getTitle() = getText(R.string.vpn_disguise)

    override fun updateFooter(fp: FooterPreference) {
        fp.setTitle(R.string.vpn_disguise_footer)
    }
}

class AppDefaultVpnDisguisePrefController(ctx: Context, key: String) :
        BoolSettingFragmentPrefController(ctx, key, ExtSettings.VPN_DISGUISE_BY_DEFAULT) {

    override fun getSummaryOn() = resText(R.string.vpn_disguise_enabled_for_nonsystem)
    override fun getSummaryOff() = resText(R.string.vpn_disguise_disabled)
}

class AppDefaultVpnDisguiseFragment : BoolSettingFragment() {

    override fun getSetting() = ExtSettings.VPN_DISGUISE_BY_DEFAULT

    override fun getTitle() = resText(R.string.vpn_disguise)

    override fun getMainSwitchTitle() = resText(R.string.vpn_disguise)

    override fun addExtraPrefs(screen: PreferenceScreen) {
        AswAdapterVpnDisguise.addAppListPageLink(screen)
    }

    override fun makeFooterPref(builder: FooterPreference.Builder): FooterPreference {
        return builder.setTitle(R.string.vpn_disguise_footer).build()
    }
}

class VpnDisguiseAppListPrefController(context: Context, preferenceKey: String) :
    AswAppListPrefController(context, preferenceKey, AswAdapterVpnDisguise) {

    override fun getAvailabilityStatus() = ExtSettingControllerHelper
        .getSecondaryUserOnlySettingAvailability(mContext)
}

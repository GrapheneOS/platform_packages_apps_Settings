package com.android.settings.applications

import android.content.Context
import android.content.pm.ApplicationInfo
import android.ext.settings.ExtSettings
import android.ext.settings.app.AswUseExecSpawning
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.ext.BoolSettingFragment
import com.android.settings.ext.BoolSettingFragmentPrefController
import com.android.settings.ext.ExtSettingControllerHelper
import com.android.settings.spa.app.appinfo.AswPreference
import com.android.settingslib.widget.FooterPreference

object AswAdapterUseExecSpawning : AswAdapter<AswUseExecSpawning>() {
    override fun getAppSwitch() = AswUseExecSpawning.I
    override fun getAswTitle(ctx: Context) = ctx.getText(R.string.aep_exec_spawning)
    override fun getDetailFragmentClass() = AppExecSpawningFragment::class
}

@Composable
fun AppExecSpawningPreference(app: ApplicationInfo) {
    val context = LocalContext.current
    AswPreference(context, app, AswAdapterUseExecSpawning)
}

class AppExecSpawningFragment : AswExploitProtectionFragment<AswUseExecSpawning>() {
    override fun getAswAdapter() = AswAdapterUseExecSpawning

    override fun updateFooter(fp: FooterPreference) {
        fp.setTitle(R.string.aep_exec_spawning_footer)
    }
}

class AppDefaultExecSpawningPrefController(ctx: Context, key: String) :
    BoolSettingFragmentPrefController(ctx, key, ExtSettings.EXEC_SPAWNING) {

    override fun getSummaryOn() = resText(R.string.aep_default_exec_spawning_summary_on)
    override fun getSummaryOff() = resText(R.string.aep_default_summary_disabled)
}

class AppDefaultExecSpawningFragment : BoolSettingFragment() {

    override fun getSetting() = ExtSettings.EXEC_SPAWNING

    override fun getTitle() = resText(R.string.aep_exec_spawning)

    override fun getMainSwitchTitle() = resText(R.string.aep_default_exec_spawning_main_switch_title)

    override fun addExtraPrefs(screen: PreferenceScreen) {
        AswAdapterUseExecSpawning.addAppListPageLink(screen)
    }

    override fun makeFooterPref(builder: FooterPreference.Builder): FooterPreference {
        val p = builder.setTitle(R.string.aep_default_exec_spawning_footer).build()
        setFooterPrefLearnMoreUri(p, Uri.parse("https://grapheneos.org/usage#exec-spawning"))
        return p
    }
}

class ExecSpawningAppListPrefController(context: Context, preferenceKey: String) :
    AswAppListPrefController(context, preferenceKey, AswAdapterUseExecSpawning) {

    override fun getAvailabilityStatus() = ExtSettingControllerHelper
        .getSecondaryUserOnlySettingAvailability(mContext)
}

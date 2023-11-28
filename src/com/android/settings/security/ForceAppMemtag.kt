package com.android.settings.security

import android.content.Context
import android.ext.settings.ExtSettings
import com.android.internal.os.Zygote
import com.android.settings.R
import com.android.settings.ext.AppPrefUtils
import com.android.settings.ext.BoolSettingFragment
import com.android.settings.ext.BoolSettingFragmentPrefController
import com.android.settingslib.widget.FooterPreference

class ForceAppMemtagPrefController(ctx: Context, key: String) :
        BoolSettingFragmentPrefController(ctx, key, ExtSettings.FORCE_APP_MEMTAG_BY_DEFAULT) {

    private val isSupported = Zygote.nativeSupportsMemoryTagging()

    override fun getAvailabilityStatus(): Int {
        var res = super.getAvailabilityStatus()

        if (res == AVAILABLE && !isSupported) {
            res = UNSUPPORTED_ON_DEVICE
        }

        return res
    }

    override fun getSummaryOn() = resText(R.string.memtag_in_3p_apps_enabled_by_default)
    override fun getSummaryOff() = resText(R.string.memtag_in_3p_apps_disabled_by_default)
}

class ForceAppMemtagFragment : BoolSettingFragment() {

    override fun getSetting() = ExtSettings.FORCE_APP_MEMTAG_BY_DEFAULT

    override fun getTitle() = resText(R.string.memtag_in_3p_apps_title)

    override fun getMainSwitchTitle() = resText(R.string.memtag_in_3p_apps_main_switch_title)
    override fun getMainSwitchSummary() = resText(R.string.memtag_in_3p_apps_main_switch_summary)

    override fun makeFooterPref(builder: FooterPreference.Builder): FooterPreference {
        val text = AppPrefUtils.getFooterForDefaultHardeningSetting(requireContext(),
                R.string.memtag_in_3p_apps_footer)
        return builder.setTitle(text).build()
    }
}

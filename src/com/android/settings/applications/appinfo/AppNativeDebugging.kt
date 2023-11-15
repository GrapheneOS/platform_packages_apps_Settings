package com.android.settings.applications.appinfo

import android.content.Context
import android.ext.settings.app.AswDenyNativeDebug
import android.ext.settings.app.AppSwitch
import com.android.settings.R
import com.android.settingslib.widget.FooterPreference

class AswAdapterNativeDebugging(ctx: Context) : AswAdapter<AswDenyNativeDebug>(ctx) {

    override fun getAppSwitch() = AswDenyNativeDebug.I

    override fun getAswTitle() = getText(R.string.native_debugging_title)

    override fun getOnTitle() = getText(R.string.aep_blocked)
    override fun getOffTitle() = getText(R.string.aep_allowed)
}

class AppNativeDebuggingPrefController(ctx: Context, key: String) :
        AswPrefController<AswDenyNativeDebug>(ctx, key, AswAdapterNativeDebugging(ctx)) {

    override fun getDetailFragmentClass() = AppNativeDebuggingFragment::class.java
}

class AppNativeDebuggingFragment : AswExploitProtectionFragment<AswDenyNativeDebug>() {

    override fun createAswAdapter(ctx: Context) = AswAdapterNativeDebugging(ctx)

    override fun getSummaryForImmutabilityReason(ir: Int): CharSequence? {
        val id = when (ir) {
            AppSwitch.IR_IS_SYSTEM_APP -> R.string.native_debug_dvr_is_system_app
            else -> return null
        }
        return getText(id)
    }

    override fun getSummaryForDefaultValueReason(dvr: Int): CharSequence? {
        val id = when (dvr) {
            AppSwitch.DVR_DEFAULT_SETTING -> R.string.aep_dvr_default_security_setting
            else -> return null
        }
        return getText(id)
    }

    override fun updateFooter(fp: FooterPreference) {
        fp.setTitle(R.string.native_debugging_footer)
    }
}

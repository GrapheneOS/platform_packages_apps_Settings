package com.android.settings.applications.appinfo

import android.content.Context
import android.ext.settings.app.AppSwitch
import android.ext.settings.app.AswUseExtendedVaSpace
import com.android.settings.R
import com.android.settings.ext.ExtSettingControllerHelper
import com.android.settingslib.widget.FooterPreference

class AswAdapterUseExtendedVaSpace(ctx: Context) : AswAdapter<AswUseExtendedVaSpace>(ctx) {

    override fun getAppSwitch() = AswUseExtendedVaSpace.I

    override fun getAswTitle() = getText(R.string.aep_ext_va_space)
}

class AppExtendedVaSpacePrefController(ctx: Context, key: String) :
        AswPrefController<AswUseExtendedVaSpace>(ctx, key, AswAdapterUseExtendedVaSpace(ctx)) {

    override fun getDetailFragmentClass() = AppExtendedVaSpaceFragment::class.java

    override fun getAvailabilityStatus() = ExtSettingControllerHelper.getDevModeSettingAvailability(mContext)
}

class AppExtendedVaSpaceFragment : AswExploitProtectionFragment<AswUseExtendedVaSpace>() {

    override fun createAswAdapter(ctx: Context) = AswAdapterUseExtendedVaSpace(ctx)

    override fun getSummaryForImmutabilityReason(ir: Int): CharSequence? {
        val id = when (ir) {
            AppSwitch.IR_REQUIRED_BY_HARDENED_MALLOC -> R.string.aep_ext_va_space_ir_hardened_malloc
            AppSwitch.IR_NON_64_BIT_NATIVE_CODE -> R.string.aep_ext_va_space_ir_32_bit_native_code
            else -> return null
        }
        return getText(id)
    }

    override fun updateFooter(fp: FooterPreference) {
        fp.setTitle(R.string.aep_ext_va_space_footer)
    }
}

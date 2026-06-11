package com.android.settings.applications

import android.content.Context
import android.content.pm.ApplicationInfo
import android.ext.settings.app.AppSwitch
import android.ext.settings.app.AswUseExtendedVaSpace
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.android.settings.R
import com.android.settings.core.BasePreferenceController
import com.android.settings.ext.ExtSettingControllerHelper
import com.android.settings.spa.app.appinfo.AswPreference
import com.android.settingslib.widget.FooterPreference

object AswAdapterUseExtendedVaSpace : AswAdapter<AswUseExtendedVaSpace>() {

    override fun getAppSwitch() = AswUseExtendedVaSpace.I

    override fun getAswTitle(ctx: Context) = ctx.getText(R.string.aep_ext_va_space)

    override fun getDetailFragmentClass() = AppExtendedVaSpaceFragment::class
}

@Composable
fun AppExtendedVaSpacePreference(app: ApplicationInfo) {
    if (!AswUseExtendedVaSpace.isAvailable()) {
        return
    }

    val context = LocalContext.current
    if (ExtSettingControllerHelper.getDevModeSettingAvailability(context) != BasePreferenceController.AVAILABLE) {
        return
    }

    AswPreference(context, app, AswAdapterUseExtendedVaSpace)
}

class AppExtendedVaSpaceFragment : AswExploitProtectionFragment<AswUseExtendedVaSpace>() {

    override fun getAswAdapter() = AswAdapterUseExtendedVaSpace

    override fun getSummaryForImmutabilityReason(ir: Int): CharSequence? {
        val id = when (ir) {
            AppSwitch.IR_REQUIRED_BY_HARDENED_MALLOC -> R.string.aep_ext_va_space_ir_hardened_malloc
            AppSwitch.IR_NON_64_BIT_NATIVE_CODE -> R.string.aep_ext_va_space_ir_32_bit_native_code
            AppSwitch.IR_REQUIRED_BY_ZYGOTE_SPAWNING -> R.string.aep_ext_va_space_ir_zygote_spawning
            else -> return null
        }
        return getText(id)
    }

    override fun updateFooter(fp: FooterPreference) {
        fp.setTitle(R.string.aep_ext_va_space_footer)
    }
}

class ExtendedVaSpaceAppListPrefController(context: Context, preferenceKey: String) :
    AswAppListPrefController(context, preferenceKey, AswAdapterUseExtendedVaSpace) {

    override fun getAvailabilityStatus() = if (AswUseExtendedVaSpace.isAvailable())
        ExtSettingControllerHelper.getDevModeSettingAvailability(mContext) else UNSUPPORTED_ON_DEVICE
}

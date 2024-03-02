package com.android.settings.applications.appinfo

import android.content.Context
import android.content.pm.ApplicationInfo
import android.ext.settings.app.AppSwitch
import android.ext.settings.app.AswUseHardenedMalloc
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.android.settings.R
import com.android.settings.spa.app.appinfo.AswPreference
import com.android.settingslib.widget.FooterPreference

class AswAdapterUseHardenedMalloc(ctx: Context) : AswAdapter<AswUseHardenedMalloc>(ctx) {

    override fun getAppSwitch() = AswUseHardenedMalloc.I

    override fun getAswTitle() = getText(R.string.aep_hmalloc)

    override fun getDetailFragmentClass() = AppHardenedMallocFragment::class
}

class AppHardenedMallocPrefController(ctx: Context, key: String) :
        AswPrefController<AswUseHardenedMalloc>(ctx, key, AswAdapterUseHardenedMalloc(ctx)) {

    override fun getDetailFragmentClass() = AppHardenedMallocFragment::class.java
}

@Composable
fun AppHardenedMallocPreference(app: ApplicationInfo) {
    val context = LocalContext.current
    AswPreference(context, app, AswAdapterUseHardenedMalloc(context))
}

class AppHardenedMallocFragment : AswExploitProtectionFragment<AswUseHardenedMalloc>() {

    override fun createAswAdapter(ctx: Context) = AswAdapterUseHardenedMalloc(ctx)

    override fun getSummaryForImmutabilityReason(ir: Int): CharSequence? {
        val id = when (ir) {
            AppSwitch.IR_IS_SYSTEM_APP -> R.string.aep_hmalloc_ir_preinstalled_app
            AppSwitch.IR_NO_NATIVE_CODE -> R.string.aep_hmalloc_ir_no_native_code
            AppSwitch.IR_NON_64_BIT_NATIVE_CODE -> R.string.aep_hmalloc_ir_32_bit_native_code
            AppSwitch.IR_IS_DEBUGGABLE_APP -> R.string.aep_hmalloc_ir_debuggable_app
            else -> return null
        }
        return getText(id)
    }

    override fun updateFooter(fp: FooterPreference) {
        fp.setTitle(R.string.aep_hmalloc_footer)
        setLearnMoreLink(fp, "https://grapheneos.org/features#exploit-mitigations")
    }
}

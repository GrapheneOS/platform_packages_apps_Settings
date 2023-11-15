package com.android.settings.applications.appinfo

import android.content.Context
import android.ext.settings.app.AswUseMemoryTagging
import android.ext.settings.app.AppSwitch
import com.android.internal.os.Zygote
import com.android.settings.R
import com.android.settingslib.widget.FooterPreference

class AswAdapterUseMemoryTagging(ctx: Context) : AswAdapter<AswUseMemoryTagging>(ctx) {

    override fun getAppSwitch() = AswUseMemoryTagging.I

    override fun getAswTitle() = getText(R.string.aep_memtag)
}

class AppMemtagPrefController(ctx: Context, key: String) :
        AswPrefController<AswUseMemoryTagging>(ctx, key, AswAdapterUseMemoryTagging(ctx)) {

    override fun getDetailFragmentClass() = AppMemtagFragment::class.java

    private val isSupported = Zygote.nativeSupportsMemoryTagging()
    override fun getAvailabilityStatus() = if (isSupported) AVAILABLE else UNSUPPORTED_ON_DEVICE
}

class AppMemtagFragment : AswExploitProtectionFragment<AswUseMemoryTagging>() {

    override fun createAswAdapter(ctx: Context) = AswAdapterUseMemoryTagging(ctx)

    override fun getSummaryForImmutabilityReason(ir: Int): CharSequence? {
        val id = when (ir) {
            AppSwitch.IR_IS_SYSTEM_APP -> R.string.aep_memtag_dvr_is_system_app
            AppSwitch.IR_NO_NATIVE_CODE -> R.string.aep_memtag_dvr_no_native_code
            AppSwitch.IR_OPTED_IN_VIA_MANIFEST -> R.string.aep_memtag_dvr_manifest_opt_in
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
        fp.setTitle(R.string.aep_memtag_footer)
    }
}

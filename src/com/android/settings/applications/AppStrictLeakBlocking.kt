package com.android.settings.applications

import android.content.Context
import android.ext.settings.app.AppSwitch
import android.ext.settings.app.AswStrictLeakBlocking
import com.android.settings.R
import com.android.settingslib.widget.FooterPreference

object AswAdapterStrictLeakBlocking : AswAdapter<AswStrictLeakBlocking>() {

    // TODO: This doesn't fit in the ExploitProtection category. Override getCategory() after
    // deciding on new category.

    override fun getAppSwitch() = AswStrictLeakBlocking.I

    override fun getAswTitle(ctx: Context) = ctx.getText(R.string.app_strict_leak_blocking_title)

    override fun getNotificationToggleTitle(ctx: Context) = ctx.getText(R.string.app_strict_leak_blocking_notif_toggle_title)

    override fun getDetailFragmentClass() = AppStrictLeakBlockingFragment::class
}

class AppStrictLeakBlockingFragment : AswStrictLeakBlockingFragment<AswStrictLeakBlocking>() {

    override fun getAswAdapter() = AswAdapterStrictLeakBlocking

    override fun getSummaryForImmutabilityReason(ir: Int): CharSequence? {
        val id = when (ir) {
            AppSwitch.IR_IS_SYSTEM_APP -> R.string.app_strict_leak_blocking_ir_system_app
            else -> return null
        }
        return getText(id)
    }

    override fun updateFooter(fp: FooterPreference) {
        fp.setTitle(R.string.app_strict_leak_blocking_footer)
    }
}
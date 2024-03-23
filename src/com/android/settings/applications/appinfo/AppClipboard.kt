package com.android.settings.applications.appinfo

import android.content.Context
import android.content.pm.ApplicationInfo
import android.ext.settings.app.AppSwitch
import android.ext.settings.app.AswDenyClipboardRead
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.android.settings.R
import com.android.settings.spa.app.appinfo.AswPreference
import com.android.settingslib.widget.FooterPreference

class AswAdapterDenyClipboardRead(ctx: Context) : AswAdapter<AswDenyClipboardRead>(ctx) {

    override fun getAppSwitch() = AswDenyClipboardRead.I

    override fun getAswTitle() = getText(R.string.clipboard_title)

    override fun getOnTitle() = getText(R.string.aep_blocked)
    override fun getOffTitle() = getText(R.string.aep_allowed)

    override fun getDetailFragmentClass() = AppClipboardFragment::class
}

@Composable
fun AppClipboardPreference(app: ApplicationInfo) {
    val context = LocalContext.current
    AswPreference(context, app, AswAdapterDenyClipboardRead(context))
}

class AppClipboardFragment : AswAppInfoFragment<AswDenyClipboardRead>() {

    override fun createAswAdapter(ctx: Context) = AswAdapterDenyClipboardRead(ctx)

    override fun getSummaryForImmutabilityReason(ir: Int): CharSequence? {
        val id = when (ir) {
            AppSwitch.IR_IS_SYSTEM_APP -> R.string.clipboard_ir_preinstalled_app
            else -> return null
        }
        return getText(id)
    }

    override fun getSummaryForDefaultValueReason(dvr: Int): CharSequence? {
        val id = when (dvr) {
            AppSwitch.DVR_DEFAULT_SETTING -> R.string.dvr_default_privacy_setting
            else -> return null
        }
        return getText(id)
    }

    override fun updateFooter(fp: FooterPreference) {
        fp.setTitle(R.string.clipboard_footer)
    }
}

package com.android.settings.applications

import android.app.settings.SettingsEnums
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.ext.settings.app.AppSwitch
import android.ext.settings.app.AswAllowClipboardRead
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.android.settings.R
import com.android.settings.dashboard.DashboardFragment
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settings.spa.app.appinfo.AswPreference
import com.android.settingslib.search.SearchIndexable
import com.android.settingslib.widget.FooterPreference

object AswAdapterClipboardRead : AswAdapter<AswAllowClipboardRead>() {

    override fun getAppSwitch() = AswAllowClipboardRead.I

    override fun getAswTitle(ctx: Context) = ctx.getText(R.string.app_clipboard_read_title)
    override fun getOnTitle(ctx: Context) = ctx.getText(R.string.app_clipboard_read_allowed)
    override fun getOffTitle(ctx: Context) = ctx.getText(R.string.app_clipboard_read_blocked)

    override fun getDetailFragmentClass() = AppClipboardReadFragment::class
}

@Composable
fun AppClipboardReadPreference(app: ApplicationInfo) {
    AswPreference(LocalContext.current, app, AswAdapterClipboardRead)
}

class AppClipboardReadFragment : AswAppInfoFragment<AswAllowClipboardRead>() {

    override fun getAswAdapter() = AswAdapterClipboardRead

    override fun shouldKillUidAfterChange() = false

    override fun getSummaryForDefaultValueReason(dvr: Int): CharSequence? {
        val id = when (dvr) {
            AppSwitch.DVR_DEFAULT_SETTING -> R.string.app_clipboard_read_dvr_default_setting
            else -> return null
        }
        return getText(id)
    }

    override fun getSummaryForImmutabilityReason(ir: Int): CharSequence? {
        val id = when (ir) {
            AppSwitch.IR_IS_SYSTEM_APP -> R.string.app_clipboard_read_ir_is_system_app
            AppSwitch.IR_IS_DEFAULT_IME -> R.string.app_clipboard_read_ir_is_default_ime
            else -> return null
        }
        return getText(id)
    }

    override fun updateFooter(fp: FooterPreference) {
        val footer = getText(R.string.app_clipboard_read_footer)
        val packageManager = requireContext().packageManager
        val uidPackages = packageManager.getPackagesForUid(getAppInfo().uid) ?: emptyArray()
        if (uidPackages.size < 2) {
            fp.title = footer
            return
        }

        val packageList = uidPackages.joinToString("\n") { packageName ->
            val label = try {
                packageManager.getApplicationInfoAsUser(packageName, 0, mUserId)
                    .loadLabel(packageManager)
            } catch (_: PackageManager.NameNotFoundException) {
                packageName
            }
            "• $label"
        }
        fp.title = footer.toString() + "\n\n" +
            getString(R.string.app_clipboard_read_shared_uid_warning, packageList)
    }
}

@SearchIndexable
class ClipboardAccessFragment : DashboardFragment() {
    override fun getPreferenceScreenResId() = R.xml.app_clipboard_access

    override fun getMetricsCategory() = SettingsEnums.PAGE_UNKNOWN

    override fun getLogTag() = "ClipboardAccess"

    companion object {
        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER = BaseSearchIndexProvider(R.xml.app_clipboard_access)
    }
}

class ClipboardReadAppListPrefController(context: Context, preferenceKey: String) :
    AswAppListPrefController(context, preferenceKey, AswAdapterClipboardRead) {

    override fun getAvailabilityStatus() = AVAILABLE
}

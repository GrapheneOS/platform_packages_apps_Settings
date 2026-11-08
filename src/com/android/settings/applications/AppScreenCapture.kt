package com.android.settings.applications

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.GosPackageState
import android.ext.settings.app.AppSwitch
import android.ext.settings.app.AswAllowScreenRecording
import android.ext.settings.app.AswAllowScreenshots
import android.ext.settings.app.AswBlockScreenRecordingDetection
import android.ext.settings.app.AswBlockScreenshotDetection
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.applications.appinfo.RadioButtonAppInfoFragment.Entry
import com.android.settings.applications.appinfo.RadioButtonAppInfoFragment.PrefModel
import com.android.settings.applications.appinfo.RadioButtonAppInfoFragment.TogglePrefModel
import com.android.settings.spa.app.appinfo.AswPreference
import com.android.settingslib.widget.FooterPreference

abstract class AswAdapterScreenCapture<T : AppSwitch> : AswAdapter<T>() {
    override fun getOnTitle(ctx: Context) = ctx.getText(R.string.app_screen_capture_allowed)
    override fun getOffTitle(ctx: Context) = ctx.getText(R.string.app_screen_capture_blocked)

    // the default depends on whether the app uses FLAG_SECURE
    override fun getDefaultTitle(ctx: Context, isOn: Boolean) =
        ctx.getText(R.string.app_screen_capture_default)
}

object AswAdapterScreenshots : AswAdapterScreenCapture<AswAllowScreenshots>() {
    override fun getAppSwitch() = AswAllowScreenshots.I

    override fun getAswTitle(ctx: Context) = ctx.getText(R.string.app_screenshots_title)

    override fun getDetailFragmentClass() = AppScreenshotsFragment::class
}

object AswAdapterScreenRecording : AswAdapterScreenCapture<AswAllowScreenRecording>() {
    override fun getAppSwitch() = AswAllowScreenRecording.I

    override fun getAswTitle(ctx: Context) = ctx.getText(R.string.app_screen_recording_title)

    override fun getDetailFragmentClass() = AppScreenRecordingFragment::class
}

@Composable
fun AppScreenshotsPreference(app: ApplicationInfo) {
    AswPreference(LocalContext.current, app, AswAdapterScreenshots)
}

@Composable
fun AppScreenRecordingPreference(app: ApplicationInfo) {
    AswPreference(LocalContext.current, app, AswAdapterScreenRecording)
}

abstract class AppScreenCaptureFragment<T : AppSwitch> : AswAppInfoFragment<T>() {
    private val detectionToggle = TogglePrefModel()

    protected abstract val blockDetectionSwitch: AppSwitch
    protected abstract fun getDetectionToggleTitle(): CharSequence
    protected abstract fun getDetectionToggleSummary(): CharSequence

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        detectionToggle.title = getDetectionToggleTitle()
        detectionToggle.summary = getDetectionToggleSummary()
        detectionToggle.onChangeListener = Preference.OnPreferenceChangeListener { _, value ->
            val ed = GosPackageState.edit(mPackageName, mUserId)
            blockDetectionSwitch.set(ed, !(value as Boolean))
            ed.apply()
        }
    }

    override fun getEntries(): Array<Entry> {
        detectionToggle.isChecked = !blockDetectionSwitch.get(requireContext(), mUserId,
            getAppInfo(), GosPackageState.get(mPackageName, mUserId))
        return super.getEntries()
    }

    override fun getExtraPrefModels(): List<PrefModel> =
        listOf(detectionToggle) + super.getExtraPrefModels()

    override fun getSummaryForDefaultValueReason(dvr: Int): CharSequence =
        getText(R.string.app_screen_capture_default_summary)

    // changes don't require restarting the app
    override fun shouldKillUidAfterChange() = false
}

class AppScreenshotsFragment : AppScreenCaptureFragment<AswAllowScreenshots>() {
    override fun getAswAdapter() = AswAdapterScreenshots

    override val blockDetectionSwitch get() = AswBlockScreenshotDetection.I
    override fun getDetectionToggleTitle() = getText(R.string.app_screenshot_detection_title)
    override fun getDetectionToggleSummary() = getText(R.string.app_screenshot_detection_summary)

    override fun getOnSummary() = getText(R.string.app_screenshots_allowed_summary)
    override fun getOffSummary() = getText(R.string.app_screenshots_blocked_summary)

    override fun updateFooter(fp: FooterPreference) {
        fp.setTitle(R.string.app_screenshots_footer)
    }
}

class AppScreenRecordingFragment : AppScreenCaptureFragment<AswAllowScreenRecording>() {
    override fun getAswAdapter() = AswAdapterScreenRecording

    override val blockDetectionSwitch get() = AswBlockScreenRecordingDetection.I
    override fun getDetectionToggleTitle() = getText(R.string.app_screen_recording_detection_title)
    override fun getDetectionToggleSummary() =
        getText(R.string.app_screen_recording_detection_summary)

    override fun getOnSummary() = getText(R.string.app_screen_recording_allowed_summary)
    override fun getOffSummary() = getText(R.string.app_screen_recording_blocked_summary)

    override fun updateFooter(fp: FooterPreference) {
        fp.setTitle(R.string.app_screen_recording_footer)
    }
}

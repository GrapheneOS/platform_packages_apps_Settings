package com.android.settings.sudconfig

import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import com.android.settings.R;

/**
 * Provides system-wide config for setup wizard screens.
 */
class SudConfigProvider : NonRelationalProvider() {
    companion object {
        private const val TAG = "SudConfigProvider"

        // resources to be forwarded via overlay config
        private val overlayConfigResources = arrayOf(
            R.dimen.setup_design_card_view_intrinsic_height,
            R.dimen.setup_design_card_view_intrinsic_width,
            R.bool.setup_compat_light_navigation_bar,
            R.bool.setup_compat_light_status_bar
        )
    }

    override fun onCreate(): Boolean {
        return true
    }

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle {
        Log.d(TAG, "method: $method, caller: $callingPackage")
        val bundle = Bundle()
        when (method) {
            "suwDefaultThemeString" -> bundle.putString(method, "glif_v4_light")

            "applyGlifThemeControlledTransition",
            "isDynamicColorEnabled",
            "isEmbeddedActivityOnePaneEnabled",
            "isFullDynamicColorEnabled",
            "IsMaterialYouStyleEnabled",
            "isNeutralButtonStyleEnabled",
            "isSuwDayNightEnabled" -> bundle.putBoolean(method, true)

            "getDeviceName" -> bundle.putCharSequence(method, getDeviceName())

            "getOverlayConfig" -> fillOverlayConfig(bundle)
        }
        return bundle
    }

    private fun getDeviceName(): String {
        var name = Settings.Global.getString(
            requireContext().contentResolver,
            Settings.Global.DEVICE_NAME
        )
        if (TextUtils.isEmpty(name)) {
            name = Build.MODEL
        }
        return name
    }

    private fun fillOverlayConfig(bundle: Bundle) {
        overlayConfigResources.forEach { resId ->
            val context = requireContext()
            val resName = context.resources.getResourceEntryName(resId)
            val config = Bundle()
            config.putString("packageName", context.packageName)
            config.putString("resourceName", resName)
            config.putInt("resourceId", resId)
            bundle.putBundle(resName, config)
        }
    }
}

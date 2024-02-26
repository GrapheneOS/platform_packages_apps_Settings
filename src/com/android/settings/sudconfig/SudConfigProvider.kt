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

        // keys inside overlay config bundle
        private const val KEY_PACKAGE_NAME = "packageName"
        private const val KEY_RESOURCE_NAME = "resourceName"
        private const val KEY_RESOURCE_ID = "resourceId"

        // methods
        private const val METHOD_GET_SUW_DEFAULT_THEME_STRING = "suwDefaultThemeString"
        const val METHOD_APPLY_GLIF_THEME_CONTROLLED_TRANSITION =
            "applyGlifThemeControlledTransition"
        const val METHOD_IS_DYNAMIC_COLOR_ENABLED = "isDynamicColorEnabled"
        const val METHOD_IS_EMBEDDED_ACTIVITY_ONE_PANE_ENABLED = "isEmbeddedActivityOnePaneEnabled"
        const val METHOD_IS_FULL_DYNAMIC_COLOR_ENABLED = "isFullDynamicColorEnabled"
        const val METHOD_IS_MATERIAL_YOU_STYLE_ENABLED = "IsMaterialYouStyleEnabled"
        const val METHOD_IS_NEUTRAL_BUTTON_STYLE_ENABLED = "isNeutralButtonStyleEnabled"
        const val METHOD_IS_SUW_DAY_NIGHT_ENABLED = "isSuwDayNightEnabled"
        const val METHOD_GET_DEVICE_NAME = "getDeviceName"
        const val METHOD_GET_OVERLAY_CONFIG = "getOverlayConfig"

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
            METHOD_GET_SUW_DEFAULT_THEME_STRING -> bundle.putString(method, "glif_v4_light")

            METHOD_APPLY_GLIF_THEME_CONTROLLED_TRANSITION,
            METHOD_IS_DYNAMIC_COLOR_ENABLED,
            METHOD_IS_EMBEDDED_ACTIVITY_ONE_PANE_ENABLED,
            METHOD_IS_FULL_DYNAMIC_COLOR_ENABLED,
            METHOD_IS_MATERIAL_YOU_STYLE_ENABLED,
            METHOD_IS_NEUTRAL_BUTTON_STYLE_ENABLED,
            METHOD_IS_SUW_DAY_NIGHT_ENABLED -> bundle.putBoolean(
                method,
                true
            )

            METHOD_GET_DEVICE_NAME -> {
                bundle.putCharSequence(method, getDeviceName())
            }

            METHOD_GET_OVERLAY_CONFIG -> {
                fillOverlayConfig(bundle)
            }
        }
        return bundle
    }

    private fun getDeviceName(): String {
        var name = Settings.Global.getString(
            context.contentResolver,
            Settings.Global.DEVICE_NAME
        )
        if (TextUtils.isEmpty(name)) {
            name = Build.MODEL
        }
        return name
    }

    private fun fillOverlayConfig(bundle: Bundle) {
        overlayConfigResources.forEach { resId ->
            val resName = context.resources.getResourceEntryName(resId)
            val config = Bundle()
            config.putString(KEY_PACKAGE_NAME, context.packageName)
            config.putString(KEY_RESOURCE_NAME, resName)
            config.putInt(KEY_RESOURCE_ID, resId)
            bundle.putBundle(resName, config)
        }
    }
}

package com.android.settings.network;

import android.app.compat.gms.GmsCompat;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Process;
import android.os.UserHandle;

import com.android.internal.gmscompat.GmsInfo;
import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

import static android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
import static android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED;

public class EuiccCompatController extends TogglePreferenceController {
    private final PackageManager packageManager;

    public EuiccCompatController(Context context, String key) {
        super(context, key);
        packageManager = context.getPackageManager();
    }

    @Override
    public boolean isChecked() {
        try {
            for (String pkg : GmsInfo.EUICC_PACKAGES) {
                int state = packageManager.getApplicationEnabledSetting(pkg);
                if (state != COMPONENT_ENABLED_STATE_ENABLED) {
                    return false;
                }
            }
            return true;
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public int getAvailabilityStatus() {
        if (!Process.myUserHandle().isSystem()) {
            // eUICC compat packages have a <install-in user-type="SYSTEM" /> directive
            return DISABLED_FOR_USER;
        }

        return checkDependencies() ?
            AVAILABLE :
            DISABLED_DEPENDENT_SETTING;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        int state = isChecked && checkDependencies() ?
                COMPONENT_ENABLED_STATE_ENABLED :
                COMPONENT_ENABLED_STATE_DISABLED;
        try {
            for (String pkg : GmsInfo.EUICC_PACKAGES) {
                packageManager.setApplicationEnabledSetting(pkg, state, 0);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean checkDependencies() {
        for (String pkg : GmsInfo.DEPENDENCIES_OF_EUICC_PACKAGES) {
            // all dependencies are a part of the GMS suite
            if (GmsCompat.isGmsApp(pkg, UserHandle.USER_SYSTEM)) {
                continue;
            }
            return false;
        }
        return true;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_network;
    }
}

package com.android.settings.network;

import android.Manifest;
import android.annotation.Nullable;
import android.app.compat.gms.GmsCompat;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.PatternMatcher;
import android.os.Process;
import android.os.UserHandle;
import android.permission.PermissionManager;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;

import com.android.internal.gmscompat.GmsInfo;
import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

import static android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
import static android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED;

public class EuiccCompatController extends TogglePreferenceController implements LifecycleObserver {
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

        return checkDependencies() ? AVAILABLE : DISABLED_DEPENDENT_SETTING;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        preference.setEnabled(getAvailabilityStatus() == AVAILABLE);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        int state = isChecked && checkDependencies() ?
                COMPONENT_ENABLED_STATE_ENABLED :
                COMPONENT_ENABLED_STATE_DISABLED;

        PermissionManager permissionManager = mContext.getSystemService(PermissionManager.class);

        try {
            for (String pkg : GmsInfo.EUICC_PACKAGES) {
                // Previously, Camera permission was auto-granted with the FLAG_PERMISSION_SYSTEM_FIXED,
                // which made it unchangeable by the user.
                // Removing FLAG_PERMISSION_USER_FIXED is needed to make sure that the app is always
                // able to show a permission request dialog after being enabled
                if (state == COMPONENT_ENABLED_STATE_ENABLED && "com.google.android.euicc".equals(pkg)) {
                    UserHandle user = mContext.getUser();
                    String perm = Manifest.permission.CAMERA;
                    permissionManager.revokeRuntimePermission(pkg, perm, user, null);
                    int permFlagsToRemove = PackageManager.FLAG_PERMISSION_SYSTEM_FIXED
                            | PackageManager.FLAG_PERMISSION_USER_FIXED;
                    permissionManager.updatePermissionFlags(pkg, perm, permFlagsToRemove, 0, user);
                }
                packageManager.setApplicationEnabledSetting(pkg, state, 0);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean checkDependencies() {
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

    @Nullable
    TwoStatePreference preference;

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        preference = screen.findPreference(getPreferenceKey());
    }

    private final BroadcastReceiver packageChangeListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            var p = preference;
            if (p != null) {
                p.setEnabled(getAvailabilityStatus() == AVAILABLE);
                p.setChecked(isChecked());
            }
        }
    };

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public void onResume() {
        var f = new IntentFilter();
        f.addAction(Intent.ACTION_PACKAGE_ADDED);
        f.addAction(Intent.ACTION_PACKAGE_CHANGED);
        f.addAction(Intent.ACTION_PACKAGE_REMOVED);
        f.addAction(Intent.ACTION_PACKAGE_FULLY_REMOVED);

        f.addDataScheme("package");
        for (String pkg : GmsInfo.DEPENDENCIES_OF_EUICC_PACKAGES) {
            f.addDataSchemeSpecificPart(pkg, PatternMatcher.PATTERN_LITERAL);
        }
        for (String pkg : GmsInfo.EUICC_PACKAGES) {
            f.addDataSchemeSpecificPart(pkg, PatternMatcher.PATTERN_LITERAL);
        }
        mContext.registerReceiver(packageChangeListener, f);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    public void onPause() {
        mContext.unregisterReceiver(packageChangeListener);
    }
}

package com.android.settings.network;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.PatternMatcher;
import android.os.Process;
import android.os.UserHandle;
import android.permission.PermissionManager;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;

import com.android.internal.util.GoogleEuicc;
import com.android.settings.ext.AbstractTogglePrefController;

import static android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
import static android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED;

public class GoogleEuiccLpaController extends AbstractTogglePrefController implements DefaultLifecycleObserver {
    private final PackageManager packageManager;

    public GoogleEuiccLpaController(Context context, String key) {
        super(context, key);
        packageManager = context.getPackageManager();
    }

    @Override
    public boolean isChecked() {
        try {
            return packageManager.getApplicationEnabledSetting(GoogleEuicc.LPA_PKG_NAME)
                    == COMPONENT_ENABLED_STATE_ENABLED;
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public int getAvailabilityStatus() {
        if (!Process.myUserHandle().isSystem()) {
            return DISABLED_FOR_USER;
        }

        return GoogleEuicc.checkLpaDependencies() ? AVAILABLE : DISABLED_DEPENDENT_SETTING;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        preference.setEnabled(getAvailabilityStatus() == AVAILABLE);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        int state = isChecked && GoogleEuicc.checkLpaDependencies() ?
                COMPONENT_ENABLED_STATE_ENABLED :
                COMPONENT_ENABLED_STATE_DISABLED;

        PermissionManager permissionManager = mContext.getSystemService(PermissionManager.class);

        try {
            // Previously, Camera permission was auto-granted with the FLAG_PERMISSION_SYSTEM_FIXED,
            // which made it unchangeable by the user.
            // Removing FLAG_PERMISSION_USER_FIXED is needed to make sure that the app is always
            // able to show a permission request dialog after being enabled
            String pkg = GoogleEuicc.LPA_PKG_NAME;
            if (state == COMPONENT_ENABLED_STATE_ENABLED) {
                UserHandle user = mContext.getUser();
                String perm = Manifest.permission.CAMERA;
                permissionManager.revokeRuntimePermission(pkg, perm, user, null);
                int permFlagsToRemove = PackageManager.FLAG_PERMISSION_SYSTEM_FIXED
                        | PackageManager.FLAG_PERMISSION_USER_FIXED;
                permissionManager.updatePermissionFlags(pkg, perm, permFlagsToRemove, 0, user);
            }
            packageManager.setApplicationEnabledSetting(pkg, state, 0);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private final BroadcastReceiver packageChangeListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (preference != null) {
                updateState(preference);
            }
        }
    };

    @Override
    public void onResume(@NonNull LifecycleOwner owner) {
        var f = new IntentFilter();
        f.addAction(Intent.ACTION_PACKAGE_ADDED);
        f.addAction(Intent.ACTION_PACKAGE_CHANGED);
        f.addAction(Intent.ACTION_PACKAGE_REMOVED);
        f.addAction(Intent.ACTION_PACKAGE_FULLY_REMOVED);

        f.addDataScheme("package");
        for (String pkg : GoogleEuicc.getLpaDependencies()) {
            f.addDataSchemeSpecificPart(pkg, PatternMatcher.PATTERN_LITERAL);
        }

        f.addDataSchemeSpecificPart(GoogleEuicc.LPA_PKG_NAME, PatternMatcher.PATTERN_LITERAL);

        mContext.registerReceiver(packageChangeListener, f);
    }

    @Override
    public void onPause(@NonNull LifecycleOwner owner) {
        mContext.unregisterReceiver(packageChangeListener);
    }
}

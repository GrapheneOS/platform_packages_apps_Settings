package com.android.settings.network;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.PatternMatcher;
import android.os.PowerManager;
import android.os.Process;
import android.os.UserHandle;
import android.permission.PermissionManager;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;

import com.android.internal.util.GoogleEuicc;
import com.android.settings.R;
import com.android.settings.ext.AbstractTogglePrefController;

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

        boolean isPresent = false;
        try {
            var ai = packageManager.getApplicationInfo(GoogleEuicc.LPA_PKG_NAME, 0);
            isPresent = ai.isSystemApp();
        } catch (PackageManager.NameNotFoundException e) {
        }

        if (!isPresent) {
            return UNSUPPORTED_ON_DEVICE;
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
        if (!isChecked) {
            var b = new AlertDialog.Builder(mContext);
            b.setMessage(R.string.privileged_euicc_management_restart_to_disable_dialog);
            b.setPositiveButton(R.string.privileged_euicc_management_restart_button, (dialogInterface, btn) -> {
                var pm = mContext.getSystemService(PowerManager.class);
                pm.reboot(null);
            });
            b.show();
            return false;
        }

        if (!GoogleEuicc.checkLpaDependencies()) {
            // this is a race condition, toggle hasn't been grayed out yet
            return false;
        }

        ContentResolver cr = mContext.getContentResolver();

        if (Settings.Global.getInt(cr, Settings.Global.BOOT_COUNT, 0) == 1) {
            // When BOOT_COUNT is 1, Google's LPA assumes that it was started together with Google's
            // SetupWizard, and changes its behavior in unwanted ways.
            //
            // To avoid this, require a reboot, which will increment BOOT_COUNT

            var b = new AlertDialog.Builder(mContext);
            b.setMessage(R.string.privileged_euicc_management_restart_due_to_first_boot);
            b.setPositiveButton(R.string.privileged_euicc_management_restart_button, (dialogInterface, btn) -> {
                var pm = mContext.getSystemService(PowerManager.class);
                pm.reboot(null);
            });
            b.show();
            return false;
        }

        PermissionManager permissionManager = mContext.getSystemService(PermissionManager.class);

        try {
            String pkg = GoogleEuicc.LPA_PKG_NAME;

            UserHandle user = mContext.getUser();
            String perm = Manifest.permission.CAMERA;
            permissionManager.revokeRuntimePermission(pkg, perm, user, null);
            // Previously, Camera permission was auto-granted with the FLAG_PERMISSION_SYSTEM_FIXED,
            // which made it unchangeable by the user.
            // Removing FLAG_PERMISSION_USER_FIXED is needed to make sure that the app is always
            // able to show a permission request dialog after being enabled
            int permFlagsToRemove = PackageManager.FLAG_PERMISSION_SYSTEM_FIXED
                    | PackageManager.FLAG_PERMISSION_USER_FIXED;
            permissionManager.updatePermissionFlags(pkg, perm, permFlagsToRemove, 0, user);

            packageManager.setApplicationEnabledSetting(pkg, COMPONENT_ENABLED_STATE_ENABLED, 0);
            {
                // Google's LPA expects to be enabled at boot, when ACTION_LOCKED_BOOT_COMPLETED
                // is normally sent.
                // Upon receiving this broadcast, it performs additional initialization that is
                // skipped otherwise, see RestoreUiccSlotSettingsOnBootReceiver and CompleteBootService
                var intent = new Intent(Intent.ACTION_LOCKED_BOOT_COMPLETED);
                intent.setPackage(pkg);
                mContext.sendBroadcast(intent);
            }
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

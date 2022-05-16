package com.android.settings.backup;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

import static android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
import static android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED;

public class EnableBackupAppPreferenceController extends TogglePreferenceController {
    private final PackageManager packageManager;
    private final String backupPkgName;
    private final int userId;

    public EnableBackupAppPreferenceController(Context context, String key) {
        super(context, key);
        packageManager = context.getPackageManager();
        BackupSettingsHelper settingsHelper = new BackupSettingsHelper(context);
        Intent backupAppIntent = settingsHelper.getIntentForBackupSettings();
        backupPkgName = backupAppIntent.getComponent().getPackageName();
        userId = context.getUserId();
    }

    @Override
    public boolean isChecked() {
        try {
            int state = packageManager.getApplicationEnabledSetting(backupPkgName);
            if (state != COMPONENT_ENABLED_STATE_DISABLED) {
                return true;
            }
            return false;
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public int getAvailabilityStatus() {
        if (backupPkgName == null) {
            return DISABLED_FOR_USER;
        }
        return AVAILABLE;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        int state = isChecked ? COMPONENT_ENABLED_STATE_ENABLED :
                COMPONENT_ENABLED_STATE_DISABLED;
        try {
            packageManager.setApplicationEnabledSetting(backupPkgName, state, userId);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_system;
    }
}

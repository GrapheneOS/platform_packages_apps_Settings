/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.backup;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.preference.PreferenceScreen;

import com.android.settings.core.TogglePreferenceController;
import com.android.settingslib.PrimarySwitchPreference;

import java.util.ArrayList;
import java.util.List;

public class BackupPrefController extends TogglePreferenceController {
    private static final String BACKUP_APP = "com.stevesoltys.seedvault";
    private final PackageManager pm;
    private PrimarySwitchPreference pref;

    public BackupPrefController(Context ctx, String key) {
        super(ctx, key);
        pm = ctx.getPackageManager();
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        PrimarySwitchPreference p = screen.findPreference(getPreferenceKey());
        if (p != null) {
            pref = p;
            pref.setEnabled(isChecked());
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public boolean isChecked() {
        int state = pm.getApplicationEnabledSetting(BACKUP_APP);
        if (state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
            return true;
        }
        if (state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT) {
            try {
                var backupAppInfo = pm.getApplicationInfo(BACKUP_APP, PackageManager.ApplicationInfoFlags.of(0));
                return backupAppInfo.enabled;
            } catch (PackageManager.NameNotFoundException e) {
                return false;
            }
        }
        return false;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        int newState = isChecked ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        List<PackageManager.ComponentEnabledSetting> changedComponents = new ArrayList<>();
        changedComponents.add(new PackageManager.ComponentEnabledSetting(BACKUP_APP, newState, 0));
        changedComponents.add(new PackageManager.ComponentEnabledSetting(
                new ComponentName(mContext.getPackageName(), UserBackupSettingsActivity.class.getName()),
                newState,
                PackageManager.DONT_KILL_APP));
        pm.setComponentEnabledSettings(changedComponents);
        if (pref != null) {
            mContext.getMainThreadHandler().postDelayed(() -> pref.setEnabled(isChecked), isChecked ? 1000 : 0);
        }
        return true;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return NO_RES;
    }
}

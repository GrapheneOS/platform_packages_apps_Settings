/*
 * Copyright (C) 2022 GrapheneOS
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

package com.android.settings.applications.appinfo;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.GosPackageState;
import android.os.Bundle;
import android.provider.Settings;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.settings.R;
import com.android.settings.applications.AppInfoBase;
import com.android.settings.applications.AppInfoWithHeader;
import com.android.settingslib.applications.AppUtils;

import dalvik.system.VMRuntime;

public class AppRelaxHardeningPreferenceController extends AppInfoPreferenceControllerBase
    implements Preference.OnPreferenceChangeListener {

    private final boolean devMode;

    public AppRelaxHardeningPreferenceController(Context ctx, String key) {
        super(ctx, key);

        devMode = Settings.Global.getInt(ctx.getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) != 0;
    }

    @Override
    public int getAvailabilityStatus() {
        if (!AppUtils.isAppInstalled(mAppEntry)) {
            // not installed for the current user, App info page is still shown in Owner in this case
            return CONDITIONALLY_UNAVAILABLE;
        }

        ApplicationInfo ai = mParent.getPackageInfo().applicationInfo;
        return GosPackageState.eligibleForRelaxHardeningFlag(ai) ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    private boolean addedDevPreference;

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);

        final int flags = GosPackageState.FLAG_DISABLE_HARDENED_MALLOC | GosPackageState.FLAG_ENABLE_COMPAT_VA_39_BIT;

        GosPackageState ps = GosPackageState.get(getPackageName());
        boolean checked = ps != null && ps.hasFlags(flags);

        ((SwitchPreference) preference).setChecked(checked);

        if (!devMode || addedDevPreference || getAvailabilityStatus() != AVAILABLE) {
            return;
        }

        Preference p = new Preference(preference.getContext());
        p.setTitle(R.string.app_hardening_config);

        p.setOnPreferenceClickListener(pref -> {
            AppInfoBase.startAppInfoFragment(DevModeFragment.class,
                    mContext.getString(R.string.app_hardening_config),
                    getPackageName(), mParent.getPackageInfo().applicationInfo.uid,
                    mParent, -1, mParent.getMetricsCategory());
            return true;
        });

        p.setOrder(preference.getOrder() - 1);
        preference.getParent().addPreference(p);

        addedDevPreference = true;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean checked = (boolean) newValue;

        GosPackageState.edit(getPackageName())
                .setFlagsState(GosPackageState.FLAG_DISABLE_HARDENED_MALLOC
                        | GosPackageState.FLAG_ENABLE_COMPAT_VA_39_BIT, checked)
                .killUidAfterApply()
                .apply();

        return true;
    }

    private String getPackageName() {
        return mParent.getPackageInfo().packageName;
    }

    public static class DevModeFragment extends AppInfoWithHeader {
        private static final String KEY_FLAG = "flag";

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            Context ctx = requireContext();
            PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(ctx);

            String[] flags = { "DISABLE_HARDENED_MALLOC", "ENABLE_COMPAT_VA_39_BIT" };

            for (String flag : flags) {
                SwitchPreference p = new SwitchPreference(ctx);
                p.setTitle(flag);

                int psFlag;
                try {
                    psFlag = GosPackageState.class.getDeclaredField("FLAG_" + flag).getInt(null);
                }  catch (ReflectiveOperationException e) {
                    throw new RuntimeException(e);
                }
                p.getExtras().putInt(KEY_FLAG, psFlag);

                p.setOnPreferenceChangeListener((preference, newValue) -> {
                    boolean state = (boolean) newValue;

                    GosPackageState.edit(mPackageName)
                            .setFlagsState(psFlag, state)
                            .killUidAfterApply()
                            .apply();

                    return true;
                });

                screen.addPreference(p);
            }

            setPreferenceScreen(screen);
        }

        @Override
        protected boolean refreshUi() {
            GosPackageState ps = GosPackageState.get(mPackageName);

            PreferenceScreen s = getPreferenceScreen();
            for (int i = 0, m = s.getPreferenceCount(); i < m; ++i) {
                Preference p = s.getPreference(i);
                if (!(p instanceof SwitchPreference)) {
                    continue;
                }
                SwitchPreference sp = (SwitchPreference) p;
                int psFlag = sp.getExtras().getInt(KEY_FLAG);
                sp.setChecked(ps != null && ps.hasFlag(psFlag));
            }

            return true;
        }

        @Override
        protected AlertDialog createDialog(int id, int errorCode) {
            return null;
        }

        @Override
        public int getMetricsCategory() {
            return METRICS_CATEGORY_UNKNOWN;
        }
    }
}

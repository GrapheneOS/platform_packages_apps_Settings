package com.android.settings.applications.appinfo;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.GosPackageState;
import android.ext.settings.app.AppSwitch;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.android.settings.R;

public abstract class AswAppInfoFragment<T extends AppSwitch>
        extends RadioButtonAppInfoFragment {

    protected static final int ID_DEFAULT = 0;
    protected static final int ID_ON = 1;
    protected static final int ID_OFF = 2;

    public abstract AswAdapter<T> createAswAdapter(Context ctx);

    protected AswAdapter<T> adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        adapter = createAswAdapter(requireContext());
        super.onCreate(savedInstanceState);
    }

    @Override
    protected CharSequence getTitle() {
        return adapter.getAswTitle();
    }

    @Override
    public Entry[] getEntries() {
        Context ctx = adapter.getContext();
        AppSwitch asw = adapter.getAppSwitch();

        int userId = mUserId;
        var ps = GosPackageState.get(mPackageName, userId);
        ApplicationInfo appInfo = getAppInfo();
        var si = new AppSwitch.StateInfo();
        boolean state = asw.get(ctx, userId, appInfo, ps, si);

        boolean isDefault = si.isUsingDefaultValue();
        boolean isImmutable = si.isImmutable();

        var defaultSi = new AppSwitch.StateInfo();
        boolean defaultValue = asw.getDefaultValue(ctx, userId, appInfo, ps, defaultSi);

        var def = createEntry(ID_DEFAULT, adapter.getDefaultTitle(defaultValue));
        def.isChecked = isDefault;
        def.isEnabled = !isImmutable;
        if (def.isEnabled) {
            int dvr = defaultSi.getDefaultValueReason();
            CharSequence summary = getSummaryForDefaultValueReason(dvr);
            if (summary == null) {
                summary = switch (dvr) {
                    case AppSwitch.DVR_APP_COMPAT_CONFIG_HARDENING_OPT_IN ->
                        getText(R.string.aep_dvr_compat_config_hardening_opt_in);
                    case AppSwitch.DVR_APP_COMPAT_CONFIG_HARDENING_OPT_OUT -> {
                        var s = defaultValue ? adapter.getOnTitle() : adapter.getOffTitle();
                        yield getString(R.string.aep_dvr_compat_config_hardening_opt_out, s.toString());
                    }
                    default -> null;
                };
            }
            def.summary = summary;
        }

        var enabled = createEntry(ID_ON, adapter.getOnTitle());
        enabled.isChecked = !isDefault && state;
        enabled.isEnabled = enabled.isChecked || !isImmutable;

        var disabled = createEntry(ID_OFF, adapter.getOffTitle());
        disabled.isChecked = !isDefault && !state;
        disabled.isEnabled = disabled.isChecked || !isImmutable;

        if (isImmutable) {
            int immutabilityReason = si.getImmutabilityReason();
            CharSequence summary = getSummaryForImmutabilityReason(immutabilityReason);
            if (summary == null) {
                if (immutabilityReason == AppSwitch.IR_EXPLOIT_PROTECTION_COMPAT_MODE) {
                    summary = getString(R.string.aep_ir_exploit_protection_compat_mode,
                            getString(R.string.aep_compat_mode_title));
                }
            }
            if (enabled.isChecked) {
                enabled.summary = summary;
            }
            if (disabled.isChecked) {
                disabled.summary = summary;
            }
        }

        return new Entry[] { def, enabled, disabled };
    }

    @Nullable
    protected CharSequence getSummaryForDefaultValueReason(int dvr) {
        return null;
    }

    @Nullable
    protected CharSequence getSummaryForImmutabilityReason(int ir) {
        return null;
    }

    @Override
    public final void onEntrySelected(int id) {
        Context ctx = requireContext();
        AppSwitch asw = adapter.getAppSwitch();
        int userId = mUserId;
        String pkgName = mPackageName;
        GosPackageState ps = GosPackageState.get(pkgName, userId);
        ApplicationInfo appInfo = getAppInfo();

        boolean isImmutable = asw.isImmutable(ctx, userId, appInfo, ps);

        if (isImmutable) {
            return;
        }

        Runnable r = () -> {
            GosPackageState.Editor ed = GosPackageState.edit(pkgName, userId);

            switch (id) {
                case ID_DEFAULT -> asw.setUseDefaultValue(ed);
                case ID_ON, ID_OFF -> asw.set(ed, id == ID_ON);
                default -> throw new IllegalStateException();
            }

            ed.setKillUidAfterApply(shouldKillUidAfterChange());

            if (!ed.apply()) {
                finish();
            }

            if (!refreshUi()) {
                setIntentAndFinish(true);
            }
        };

        completeStateChange(id, asw.get(ctx, userId, appInfo, ps), r);
    }

    protected void completeStateChange(int newEntryId, boolean curValue, Runnable stateChangeAction) {
        stateChangeAction.run();
    }

    protected boolean shouldKillUidAfterChange() {
        return true;
    }
}

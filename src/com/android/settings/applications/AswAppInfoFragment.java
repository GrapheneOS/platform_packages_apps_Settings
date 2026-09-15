package com.android.settings.applications;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.GosPackageState;
import android.ext.settings.app.AppSwitch;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.android.settings.R;
import com.android.settings.applications.appinfo.RadioButtonAppInfoFragment;

import java.util.ArrayList;
import java.util.List;

public abstract class AswAppInfoFragment<T extends AppSwitch>
        extends RadioButtonAppInfoFragment {

    protected static final int ID_DEFAULT = 0;
    protected static final int ID_ON = 1;
    protected static final int ID_OFF = 2;

    public abstract AswAdapter<T> getAswAdapter();

    protected AswAdapter<T> adapter;
    private final ArrayList<PrefModel> extraPrefs = new ArrayList<>();
    @Nullable
    private TogglePrefModel notifTogglePref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        adapter = getAswAdapter();
        super.onCreate(savedInstanceState);

        if (adapter.getAppSwitch().hasNotification()) {
            Context ctx = requireContext();
            var p = new TogglePrefModel();
            p.title = adapter.getNotificationToggleTitle(ctx);
            p.summary = adapter.getNotificationToggleSummary(ctx);
            p.onChangeListener = (pref, state) -> {
                var ed = GosPackageState.edit(mPackageName, mUserId);
                adapter.getAppSwitch().setNotificationEnabled(ed, (boolean) state);
                return ed.apply();
            };
            extraPrefs.add(p);
            notifTogglePref = p;
        }
        {
            var p = new LinkPrefModel();
            p.title = getText(R.string.default_see_all_apps_title);
            p.onClick = () -> adapter.openAppListPage(requireContext());
            extraPrefs.add(p);
        }
    }

    @Override
    protected CharSequence getTitle() {
        return adapter.getAswTitle(requireContext());
    }

    @Override
    public Entry[] getEntries() {
        Context ctx = requireContext();
        AppSwitch asw = adapter.getAppSwitch();

        int userId = mUserId;
        GosPackageState ps = GosPackageState.get(mPackageName, userId);
        ApplicationInfo appInfo = getAppInfo();
        var si = new AppSwitch.StateInfo();
        boolean state = asw.get(ctx, userId, appInfo, ps, si);

        boolean isDefault = si.isUsingDefaultValue();
        boolean isImmutable = si.isImmutable();

        var defaultSi = new AppSwitch.StateInfo();
        boolean defaultValue = asw.getDefaultValue(ctx, userId, appInfo, ps, defaultSi);

        var def = createEntry(ID_DEFAULT, adapter.getDefaultTitle(ctx, defaultValue));
        def.isChecked = isDefault;
        def.isEnabled = !isImmutable;
        if (def.isEnabled) {
            int dvr = defaultSi.getDefaultValueReason();
            CharSequence summary = getSummaryForDefaultValueReason(dvr);
            if (summary == null) {
                summary = switch (dvr) {
                    case AppSwitch.DVR_DEFAULT_SETTING -> {
                        if (!ctx.getUser().isSystem()) {
                            yield null;
                        }
                        yield getString(R.string.aep_dvr_default_security_setting,
                                getAppDefaultSettingPathForCategory(adapter.getCategory()));
                    }
                    case AppSwitch.DVR_APP_COMPAT_CONFIG_HARDENING_OPT_IN ->
                        getText(R.string.aep_dvr_compat_config_hardening_opt_in);
                    case AppSwitch.DVR_APP_COMPAT_CONFIG_HARDENING_OPT_OUT -> {
                        if (!ctx.getUser().isSystem()) {
                            yield null;
                        }
                        var s = defaultValue ? adapter.getOnTitle(ctx) : adapter.getOffTitle(ctx);
                        yield getString(R.string.aep_dvr_compat_config_hardening_opt_out,
                                s.toString(), getSettingPath(R.string.safety_center_title,
                                                  R.string.more_security_privacy_settings));
                    }
                    case AppSwitch.DVR_APP_IS_CLIENT_OF_GMSCORE -> {
                        var s = defaultValue ? adapter.getOnTitle(ctx) : adapter.getOffTitle(ctx);
                        yield getString(R.string.aep_dvr_app_is_client_of_gmscore, s.toString());
                    }
                    default -> null;
                };
            }
            def.summary = summary;
        }

        var enabled = createEntry(ID_ON, adapter.getOnTitle(ctx));
        enabled.isChecked = !isDefault && state;
        enabled.isEnabled = enabled.isChecked || !isImmutable;

        var disabled = createEntry(ID_OFF, adapter.getOffTitle(ctx));
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

        if (notifTogglePref != null) {
            notifTogglePref.isChecked = adapter.getAppSwitch().isNotificationEnabled(ps);
            notifTogglePref.isEnabled = adapter.isNotificationToggleEnabled(state);
        }

        return new Entry[] { def, enabled, disabled };
    }

    private String getAppDefaultSettingPathForCategory(AswAdapter.Category category) {
        switch (category) {
            case ExploitProtection:
                return getSettingPath(R.string.safety_center_title, R.string.exploit_protection_settings);
            default:
                throw new IllegalArgumentException(category.toString());
        }
    }

    private String getSettingPath(int... parts) {
        var sb = new StringBuilder();
        for (int i = 0; i < parts.length; ++i) {
            if (i != 0) {
                sb.append(" > ");
            }
            sb.append(getString(parts[i]));
        }
        return sb.toString();
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
    protected List<PrefModel> getExtraPrefModels() {
        return extraPrefs;
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

package com.android.settings.users;

import static com.android.settings.users.UserDetailsSettings.EXTRA_USER_ID;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.utils.CandidateInfoExtra;
import com.android.settings.widget.RadioButtonPickerFragment;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.widget.CandidateInfo;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class UserAppsInstallSettings extends RadioButtonPickerFragment {

    private static final String INSTALL_ENABLED = "install_apps_enabled";
    private static final String INSTALL_FIRST_PARTY_ENABLED = "install_apps_first_party_enabled";
    private static final String INSTALL_DISABLED = "install_apps_disabled";
    private UserRestrictions userRestrictions;

    static void launch(Preference preference, int userId) {

        Bundle extras = preference.getExtras();
        extras.putInt(EXTRA_USER_ID, userId);

        new SubSettingLauncher(preference.getContext())
                .setDestination(UserAppsInstallSettings.class.getName())
                .setSourceMetricsCategory(extras.getInt(DashboardFragment.CATEGORY,
                        SettingsEnums.PAGE_UNKNOWN))
                .setTitleText(preference.getTitle())
                .setArguments(extras)
                .launch();
    }

    private static String getCurrentInstallRestriction(UserRestrictions userRestrictions) {
        if (userRestrictions.isSet(UserManager.DISALLOW_INSTALL_APPS)) {
            return INSTALL_DISABLED;
        }

        if (userRestrictions.isSet(UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES)) {
            return INSTALL_FIRST_PARTY_ENABLED;
        }

        return INSTALL_ENABLED;
    }

    static String getDescription(Context context, UserRestrictions userRestrictions) {
        switch (getCurrentInstallRestriction(userRestrictions)) {
            case INSTALL_ENABLED:
                return context.getString(R.string.user_app_install_enabled);
            case INSTALL_FIRST_PARTY_ENABLED:
                return context.getString(R.string.user_app_install_enabled_first_party_sources);
            case INSTALL_DISABLED:
                return context.getString(R.string.user_app_install_disabled);
            default:
                throw new IllegalStateException();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Bundle args = requireArguments();
        int userId = args.getInt(EXTRA_USER_ID, UserHandle.USER_NULL);

        Context ctx = requireContext();
        UserManager userManager = ctx.getSystemService(UserManager.class);
        UserInfo userInfo = userManager.getUserInfo(userId);
        userRestrictions = new UserRestrictions(userManager, userInfo);

        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        PreferenceScreen ps = getPreferenceManager().createPreferenceScreen(requireContext());
        setPreferenceScreen(ps);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return -1;
    }

    @Override
    protected List<? extends CandidateInfo> getCandidates() {
        Context ctx = requireContext();
        ArrayList<CandidateInfoExtra> candidates = new ArrayList<>(3);
        if (!userRestrictions.userInfo.isGuest()) {
            candidates.add(new CandidateInfoExtra(ctx.getString(R.string.user_app_install_enabled),
                    ctx.getString(R.string.user_app_install_enabled_desc),
                    INSTALL_ENABLED, true));
        }
        candidates.add(new CandidateInfoExtra(ctx.getString(R.string.user_app_install_enabled_first_party_sources),
                        ctx.getString(R.string.user_app_install_enabled_first_party_sources_desc),
                INSTALL_FIRST_PARTY_ENABLED, true));
        candidates.add(new CandidateInfoExtra(ctx.getString(R.string.user_app_install_disabled),
                        ctx.getString(R.string.user_app_install_disabled_desc),
                INSTALL_DISABLED, true));
        return candidates;
    }

    @Override
    public void bindPreferenceExtra(SelectorWithWidgetPreference pref, String key, CandidateInfo info,
                                    String defaultKey, String systemDefaultKey) {
        pref.setSingleLineTitle(false);

        if (info instanceof CandidateInfoExtra) {
            var cie = (CandidateInfoExtra) info;
            pref.setSummary(cie.loadSummary());
        }
    }

    @Override
    protected String getDefaultKey() {
        return getCurrentInstallRestriction(userRestrictions);
    }

    @Override
    protected boolean setDefaultKey(String key) {
        if (key == null) {
            return false;
        }

        switch (key) {
            case INSTALL_ENABLED:
                userRestrictions.set(UserManager.DISALLOW_INSTALL_APPS, false);
                userRestrictions.set(UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES, false);
                return true;
            case INSTALL_FIRST_PARTY_ENABLED:
                userRestrictions.set(UserManager.DISALLOW_INSTALL_APPS, false);
                userRestrictions.set(UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES, true);
                return true;
            case INSTALL_DISABLED:
                userRestrictions.set(UserManager.DISALLOW_INSTALL_APPS, true);
                userRestrictions.set(UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES, true);
                return true;
            default:
                return false;
        }
    }

    @Override
    public int getMetricsCategory() {
        return METRICS_CATEGORY_UNKNOWN;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateCandidates();
    }
}

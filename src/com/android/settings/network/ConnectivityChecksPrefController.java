package com.android.settings.network;

import android.content.Context;
import android.ext.settings.ConnChecksSetting;
import android.ext.settings.ExtSettings;
import android.os.UserManager;

import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.ext.IntSettingPrefController;
import com.android.settings.ext.RadioButtonPickerFragment2;
import com.android.settingslib.RestrictedLockUtilsInternal;

public class ConnectivityChecksPrefController extends IntSettingPrefController {

    public ConnectivityChecksPrefController(Context ctx, String key) {
        super(ctx, key, ExtSettings.CONNECTIVITY_CHECKS);
    }

    @Override
    public int getAvailabilityStatus() {
        if (hasUserManagerRestriction()) {
            return DISABLED_FOR_USER;
        }
        // ExtSettings.CONNECTIVITY_CHECKS uses sysprop, hence the availability is originally
        // disabled for user when not in system user. Check for the UserManager.DISALLOW_CONFIG_PRIVATE_DNS
        // whether to hide the settings or not.
        if (super.getAvailabilityStatus() == DISABLED_FOR_USER) {
            UserManager userManager = mContext.getSystemService(UserManager.class);
            var userHandle = android.os.Process.myUserHandle();
            String key = UserManager.DISALLOW_CONFIG_PRIVATE_DNS;
            if (!userHandle.isSystem() && userManager.hasUserRestriction(key, userHandle)) {
                return DISABLED_FOR_USER;
            }
        }
        return AVAILABLE;
    }

    @Override
    protected void getEntries(Entries entries) {
        entries.add(R.string.conn_checks_grapheneos_server, ConnChecksSetting.VAL_GRAPHENEOS);
        entries.add(R.string.conn_checks_google_server, ConnChecksSetting.VAL_STANDARD);
        entries.add(R.string.conn_checks_disabled, ConnChecksSetting.VAL_DISABLED);
    }

    @Override
    public void addPrefsAfterList(RadioButtonPickerFragment2 fragment, PreferenceScreen screen) {
        addFooterPreference(screen, R.string.conn_checks_footer);
    }

    private boolean hasUserManagerRestriction() {
        String restriction = UserManager.DISALLOW_CONFIG_PRIVATE_DNS;
        Context ctx = mContext;
        return RestrictedLockUtilsInternal
                .checkIfRestrictionEnforced(ctx, restriction, ctx.getUserId()) != null;
    }
}

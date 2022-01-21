package com.android.settings.applications;

import android.app.compat.gms.GmsCompat;
import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import android.text.TextUtils;

import androidx.preference.Preference;

import com.android.internal.gmscompat.GmsCompatApp;
import com.android.internal.gmscompat.GmsInfo;
import com.android.settings.core.BasePreferenceController;

public class GmsCompatAppController extends BasePreferenceController {

    public GmsCompatAppController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        UserHandle workProfile = getWorkProfileUser();
        int userId = workProfile != null ?
                workProfile.getIdentifier() :
                UserHandle.myUserId();

        return GmsCompat.isEnabledFor(GmsInfo.PACKAGE_GMS_CORE, userId) ?
                AVAILABLE : DISABLED_FOR_USER;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return false;
        }
        Intent intent = new Intent(GmsCompatApp.PKG_NAME + ".SETTINGS_LINK");
        intent.setPackage(GmsCompatApp.PKG_NAME);

        UserHandle workProfile = getWorkProfileUser();
        if (workProfile != null) {
            mContext.startActivityAsUser(intent, workProfile);
        } else {
            mContext.startActivity(intent);
        }
        return true;
    }
}

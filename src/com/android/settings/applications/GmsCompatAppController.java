/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.applications;

import android.app.compat.gms.GmsCompat;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.UserHandle;
import android.text.TextUtils;

import androidx.preference.Preference;

import com.android.internal.gmscompat.GmsCompatApp;
import com.android.internal.gmscompat.GmsInfo;
import com.android.settings.core.BasePreferenceController;

public class GmsCompatAppController extends BasePreferenceController {
    private final Context context;

    public GmsCompatAppController(Context context, String key) {
        super(context, key);
        this.context = context;
    }

    @Override
    public int getAvailabilityStatus() {
        UserHandle workProfile = getWorkProfileUser();
        int userId = workProfile != null ?
                workProfile.getIdentifier() :
                UserHandle.myUserId();

        return GmsCompat.isGmsApp(GmsInfo.PACKAGE_GMS_CORE, userId) ?
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
            context.startActivityAsUser(intent, workProfile);
        } else {
            context.startActivity(intent);
        }
        return true;
    }
}

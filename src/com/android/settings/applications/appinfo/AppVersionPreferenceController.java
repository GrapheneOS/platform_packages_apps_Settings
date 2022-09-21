/*
 * Copyright (C) 2017 The Android Open Source Project
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
import android.content.pm.PackageInfo;
import android.text.BidiFormatter;

import com.android.settings.R;

import java.text.DateFormat;
import java.util.Date;

public class AppVersionPreferenceController extends AppInfoPreferenceControllerBase {

    public AppVersionPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public CharSequence getSummary() {
        // TODO(b/168333280): Review the null case in detail since this is just a quick
        // workaround to fix NPE.
        final PackageInfo packageInfo = mParent.getPackageInfo();
        if (packageInfo == null) {
            return null;
        }

        Context ctx = mContext;

        DateFormat dateFormat = android.text.format.DateFormat.getMediumDateFormat(ctx);
        DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(ctx);

        String times = null;
        if (packageInfo.firstInstallTime != 0) {
            String s = formatDate(packageInfo.firstInstallTime, dateFormat, timeFormat);
            times = ctx.getString(R.string.app_info_install_time, s);
        }

        if (packageInfo.lastUpdateTime != 0 && packageInfo.lastUpdateTime != packageInfo.firstInstallTime) {
            String s = formatDate(packageInfo.lastUpdateTime, dateFormat, timeFormat);
            String updateTime = ctx.getString(R.string.app_info_update_time, s);
            if (times != null) {
                times += '\n' + updateTime;
            } else {
                times = updateTime;
            }
        }

        return ctx.getString(R.string.version_text,
                BidiFormatter.getInstance().unicodeWrap(packageInfo.versionName))
                + "\n\n" + packageInfo.packageName
                + "\nversionCode " + packageInfo.getLongVersionCode()
                + "\n\ntargetSdk " + packageInfo.applicationInfo.targetSdkVersion
                + "\nminSdk " + packageInfo.applicationInfo.minSdkVersion
                + (times != null ? ("\n\n" + times) : "")
        ;
    }

    private static String formatDate(long unixTs, DateFormat dateFormat, DateFormat timeFormat) {
        Date d = new Date(unixTs);
        return dateFormat.format(d) + "; " + timeFormat.format(d);
    }
}

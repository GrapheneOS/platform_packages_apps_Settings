package com.android.settings.applications;

import android.ext.settings.app.AppSwitch;

import com.android.settings.R;

public abstract class AswStrictLeakBlockingFragment<T extends AppSwitch>
        extends AswWarnOnDisableFragment<T> {

    public int getDisableWarningMessageId() {
        return R.string.app_strict_leak_blocking_confirm_disable_warning_msg;
    }
}

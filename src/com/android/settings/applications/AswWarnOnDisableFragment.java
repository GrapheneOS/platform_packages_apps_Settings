package com.android.settings.applications;

import android.content.Context;
import android.content.pm.GosPackageState;
import android.ext.settings.app.AppSwitch;

import androidx.appcompat.app.AlertDialog;

import com.android.settings.R;

public abstract class AswWarnOnDisableFragment<T extends AppSwitch> extends
        AswAppInfoFragment<T> {

    @Override
    protected void completeStateChange(int newEntryId, boolean curValue, Runnable stateChangeAction) {
        Context ctx = requireContext();

        boolean showWarning = false;
        if (curValue) {
            if (newEntryId == ID_OFF) {
                showWarning = true;
            } else if (newEntryId == ID_DEFAULT) {
                AppSwitch asw = adapter.getAppSwitch();
                int userId = mUserId;
                var ps = GosPackageState.get(mPackageName, userId);
                showWarning = !asw.getDefaultValue(ctx, userId, getAppInfo(), ps);
            }
        }
        if (showWarning) {
            var d = getWarningOnDisable(ctx, stateChangeAction);
            d.show();
        } else {
            stateChangeAction.run();
        }
    }

    public abstract int getDisableWarningMessageId();

    public AlertDialog.Builder getWarningOnDisable(Context ctx, Runnable action) {
        return getWarningOnDisable(ctx, action, getDisableWarningMessageId());
    }

    public static AlertDialog.Builder getWarningOnDisable(Context ctx, Runnable action,
            int warningMessageId) {
        var b = new AlertDialog.Builder(ctx);
        b.setTitle(R.string.aep_confirm_disable_title);
        b.setMessage(warningMessageId);
        b.setNegativeButton(R.string.cancel, null);
        b.setPositiveButton(R.string.aep_confirm_disable_proceed_btn, (d, w) -> action.run());
        return b;
    }

}

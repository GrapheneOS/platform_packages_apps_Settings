package com.android.settings.security;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.ext.settings.LockAction;
import android.util.Log;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.Nullable;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.ext.BoolSettingPrefController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.password.ChooseLockSettingsHelper;

public class LockActionPrefController extends BoolSettingPrefController {
    private static final String TAG = LockActionPrefController.class.getSimpleName();

    @Nullable private Activity activity;
    @Nullable private ActivityResultLauncher<Intent> credentialConfirmationLauncher;
    private boolean awaitingCredentialConfirmation;

    public LockActionPrefController(Context ctx, String key) {
        super(ctx, key, LockAction.ENABLED_SETTING);
    }

    public void setHost(Activity activity, ActivityResultLauncher<Intent> launcher) {
        this.activity = activity;
        this.credentialConfirmationLauncher = launcher;
    }

    @Override
    public int getAvailabilityStatus() {
        int status = super.getAvailabilityStatus();
        if (status != AVAILABLE) {
            return status;
        }
        return isScreenLockSecure() ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        if (isChecked || !isScreenLockSecure()) {
            return super.setChecked(isChecked);
        }

        if (awaitingCredentialConfirmation) {
            return false;
        }

        if (activity == null || credentialConfirmationLauncher == null) {
            Log.e(TAG, "credential confirmation is unavailable, refusing to disable");
            return false;
        }

        awaitingCredentialConfirmation = true;

        var builder = new ChooseLockSettingsHelper.Builder(activity);
        builder.setActivityResultLauncher(credentialConfirmationLauncher);
        builder.setForegroundOnly(true);
        if (!builder.show()) {
            awaitingCredentialConfirmation = false;
        }

        return false;
    }

    public void onCredentialConfirmationResult(@Nullable ActivityResult result) {
        if (!awaitingCredentialConfirmation) {
            return;
        }
        awaitingCredentialConfirmation = false;

        if (result != null && result.getResultCode() == Activity.RESULT_OK) {
            super.setChecked(false);
            if (preference != null) {
                updateState(preference);
            }
        }
    }

    private boolean isScreenLockSecure() {
        LockPatternUtils lpu = FeatureFactory.getFeatureFactory()
                .getSecurityFeatureProvider()
                .getLockPatternUtils(mContext);
        return lpu.isSecure(mContext.getUserId());
    }
}
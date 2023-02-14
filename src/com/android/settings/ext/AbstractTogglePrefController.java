package com.android.settings.ext;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.preference.Preference;

import com.android.settings.core.TogglePreferenceController;

public abstract class AbstractTogglePrefController extends TogglePreferenceController {

    protected AbstractTogglePrefController(Context ctx, String key) {
        super(ctx, key);
    }

    @Nullable protected Preference preference;

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);

        if (preference != this.preference) {
            preference.setSingleLineTitle(false);
            preference.setPersistent(false);
            this.preference = preference;
        }
    }

    @Override
    public boolean isSliceable() {
        return false;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return NO_RES;
    }
}

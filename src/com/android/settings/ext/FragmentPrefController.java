package com.android.settings.ext;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.android.settings.core.BasePreferenceController;

public abstract class FragmentPrefController<FragmentType extends PreferenceFragmentCompat>
        extends BasePreferenceController {

    protected FragmentPrefController(Context ctx, String key) {
        super(ctx, key);
    }

    @Nullable
    protected Preference preference;

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);

        if (preference != this.preference) {
            preference.setSingleLineTitle(false);
            preference.setPersistent(false);

            this.preference = preference;
        }
    }

    protected final CharSequence resText(int res) {
        return mContext.getText(res);
    }
}

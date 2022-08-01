package com.android.settings.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.provider.Settings;
import android.text.format.DateUtils;

import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settingslib.widget.CandidateInfo;
import com.android.settingslib.widget.SelectorWithWidgetPreference;
import java.util.ArrayList;
import java.util.List;

public abstract class TimeoutPickerFragment extends RadioButtonPickerFragment {

    private String toHumanFriendly(long duration) {
        if (duration == 0) {
            return  getContext().getString(R.string.timeout_never);
        }
        return DateUtils.formatDuration(duration).toString();
    }

    protected abstract long[] getSelectableTimeouts();

    protected abstract String getSettingsKey();

    @Override
    protected String getDefaultKey() {
        return Long.toString(
                Settings.Global.getLong(
                        getContext().getContentResolver(),
                        getSettingsKey(),
                        0)
        );
    }

    @Override
    protected boolean setDefaultKey(String key) {
        return Settings.Global.putLong(
                getContext().getContentResolver(),
                getSettingsKey(),
                Long.parseLong(key)
        );
    }

    @Override
    protected List<? extends CandidateInfo> getCandidates() {
        final List<CandidateInfo> candidates = new ArrayList<>();
        final long[] CANDIDATES = getSelectableTimeouts();

        for (long timeout : CANDIDATES) {
            candidates.add(
                    new TimeoutCandidateInfo(
                            toHumanFriendly(timeout),
                            String.valueOf(timeout)
                    )
            );
        }
        return candidates;
    }

    @Override
    public void updateCandidates() {
        final PreferenceScreen screen = getPreferenceScreen();
        screen.removeAll();

        final List<? extends CandidateInfo> candidateList = getCandidates();
        String defaultKey = getDefaultKey();
        for (CandidateInfo info : candidateList) {
            SelectorWithWidgetPreference pref = new SelectorWithWidgetPreference(getPrefContext());
            bindPreference(pref, info.getKey(), info, defaultKey);
            screen.addPreference(pref);
        }
    }

    private static class TimeoutCandidateInfo extends CandidateInfo {
        private final CharSequence mLabel;
        private final String mKey;

        TimeoutCandidateInfo(CharSequence label, String key) {
            super(true);
            mLabel = label;
            mKey = key;
        }

        @Override
        public CharSequence loadLabel() {
            return mLabel;
        }

        @Override
        public Drawable loadIcon() {
            return null;
        }

        @Override
        public String getKey() {
            return mKey;
        }
    }
}

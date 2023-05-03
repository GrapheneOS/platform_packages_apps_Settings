/*
 * Copyright (C) 2022 GrapheneOS
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.ext;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.UserHandle;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.SparseIntArray;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.utils.CandidateInfoExtra;
import com.android.settingslib.widget.CandidateInfo;
import com.android.settingslib.widget.FooterPreference;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public abstract class AbstractListPreferenceController extends BasePreferenceController
    implements DefaultLifecycleObserver {

    private Preference preference;
    private Entries entries;

    @Nullable
    public RadioButtonPickerFragment2 fragment;

    protected AbstractListPreferenceController(Context ctx, String key) {
        super(ctx, key);
    }

    // call entries.add(entryName, entryValue) to add entries.
    // entryValues can be mapped from other values or sets of values, as long as getCurrentValue()
    // and setValue() methods are consistent
    protected abstract void getEntries(Entries entries);

    public void getEntriesAsCandidates(ArrayList<CandidateInfo> dst) {
        Entries e = new Entries(mContext);
        getEntries(e);

        dst.addAll(e.list);
    }

    protected abstract int getCurrentValue();
    protected abstract boolean setValue(int val);

    @Override
    public void updateState(Preference p) {
        if (entries == null) {
            entries = new Entries(mContext);
            getEntries(entries);
        }

        if (p != preference) {
            p.setSingleLineTitle(false);
            p.setPersistent(false);
            this.preference = p;
        }

        updatePreference();
    }

    void updatePreference() {
        if (fragment != null) {
            fragment.updateCandidates();
        }

        Preference p = preference;
        if (p == null) {
            return;
        }

        int idx = entries.getIndexForValue(getCurrentValue());
        if (idx >= 0) {
            p.setSummary(entries.list.get(idx).loadLabel());
        } else {
            p.setSummary(null);
        }
    }

    public static class Entries {
        private final Context context;
        private final ArrayList<CandidateInfoExtra> list = new ArrayList<>();
        private final SparseIntArray valueToIndexMap = new SparseIntArray();

        Entries(Context context) {
            this.context = context;
        }

        public void add(@StringRes int title, int value) {
            add(context.getText(title), value);
        }

        public void add(@StringRes int title, @StringRes int summary, int value) {
            add(context.getText(title), context.getText(summary), value);
        }

        public void add(int duration, TimeUnit timeUnit) {
            long durationMillis = timeUnit.toMillis(duration);
            if (durationMillis > Integer.MAX_VALUE) {
                throw new IllegalArgumentException();
            }

            add(DateUtils.formatDuration(durationMillis), (int) durationMillis);
        }

        public void add(CharSequence title, int value) {
            add(title, null, value, true);
        }

        public void add(CharSequence title, CharSequence summary, int value) {
            add(title, summary, value, true);
        }

        public void add(CharSequence title, @Nullable CharSequence summary, int value, boolean enabled) {
            String prefKey = Integer.toString(value);
            list.add(new CandidateInfoExtra(title, summary, prefKey, enabled));
            valueToIndexMap.put(value, list.size() - 1);
        }

        public int getIndexForValue(int val) {
            return valueToIndexMap.get(val, -1);
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

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return super.handlePreferenceTreeClick(preference);
        }

        if (this.preference instanceof ListPreference) {
            return super.handlePreferenceTreeClick(preference);
        }

        UserHandle workProfileUser = getWorkProfileUser();
        boolean isForWork = workProfileUser != null;

        RadioButtonPickerFragment2.fillArgs(preference, this, isForWork);

        new SubSettingLauncher(preference.getContext())
                .setDestination(RadioButtonPickerFragment2.class.getName())
                .setSourceMetricsCategory(preference.getExtras().getInt(DashboardFragment.CATEGORY,
                                                                        SettingsEnums.PAGE_UNKNOWN))
                .setTitleText(preference.getTitle())
                .setArguments(preference.getExtras())
                .setUserHandle(workProfileUser)
                .launch();
        return true;
    }

    public void addPrefsBeforeList(RadioButtonPickerFragment2 fragment, PreferenceScreen screen) {

    }

    public void addPrefsAfterList(RadioButtonPickerFragment2 fragment, PreferenceScreen screen) {

    }

    public FooterPreference addFooterPreference(PreferenceScreen screen, @StringRes int text) {
        Context ctx = screen.getContext();
        return addFooterPreference(screen, ctx.getText(text), null, null);
    }

    public FooterPreference addFooterPreference(PreferenceScreen screen,
                                                @StringRes int text, String learnMoreUrl) {
        return addFooterPreference(screen, text, R.string.learn_more, learnMoreUrl);
    }

    public FooterPreference addFooterPreference(
            PreferenceScreen screen, @StringRes int text,
            @StringRes int learnMoreText, String learnMoreUrl) {
        Context ctx = screen.getContext();
        Runnable learnMoreAction = () -> {
            var intent = new Intent(Intent.ACTION_VIEW, Uri.parse(learnMoreUrl));
            ctx.startActivity(intent);
        };
        return addFooterPreference(screen, ctx.getText(text),
                ctx.getText(learnMoreText), learnMoreAction);
    }

    public FooterPreference addFooterPreference(
            PreferenceScreen screen, @StringRes int text,
            @StringRes int learnMoreText, Runnable learnMoreAction) {
        Context ctx = screen.getContext();
        return addFooterPreference(screen, ctx.getText(text), ctx.getText(learnMoreText), learnMoreAction);
    }

    public FooterPreference addFooterPreference(PreferenceScreen screen, CharSequence text,
                                                @Nullable CharSequence learnMoreText,
                                                @Nullable Runnable learnMoreAction) {
        var p = new FooterPreference(screen.getContext());
        p.setSelectable(false);
        p.setSummary(text);
        if (learnMoreText != null) {
            p.setLearnMoreText(learnMoreText);
            Objects.requireNonNull(learnMoreAction);
            p.setLearnMoreAction(v -> learnMoreAction.run());
        }
        p.setOrder(Preference.DEFAULT_ORDER);
        screen.addPreference(p);
        return p;
    }

    protected final CharSequence getText(@StringRes int resId) {
        return mContext.getText(resId);
    }
}

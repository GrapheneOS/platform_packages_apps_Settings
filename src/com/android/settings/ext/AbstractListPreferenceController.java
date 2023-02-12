/*
 * Copyright (C) 2022 GrapheneOS
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.ext;

import android.content.Context;
import android.text.format.DateUtils;
import android.util.SparseIntArray;

import androidx.annotation.StringRes;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public abstract class AbstractListPreferenceController extends BasePreferenceController
    implements Preference.OnPreferenceChangeListener {

    private ListPreference preference;
    private Entries entries;
    // current preference value is prepended to the baseSummary
    private CharSequence baseSummary;

    protected AbstractListPreferenceController(Context ctx, String key) {
        super(ctx, key);
    }

    // call entries.add(entryName, entryValue) to add entries.
    // entryValues can be mapped from other values or sets of values, as long as getCurrentValue()
    // and setValue() methods are consistent
    protected abstract void getEntries(Entries entries);

    protected abstract int getCurrentValue();
    protected abstract boolean setValue(int val);

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        ListPreference p = screen.findPreference(mPreferenceKey);
        if (p == null) {
            return;
        }

        this.preference = p;

        if (p.getEntries() == null) {
            if (entries == null) {
                entries = new Entries(mContext);
                getEntries(entries);
            }

            baseSummary = p.getSummary();

            p.setSingleLineTitle(false);
            p.setEntries(entries.getTitles());
            p.setEntryValues(entries.getValues());
            p.setPersistent(false);
            p.setOnPreferenceChangeListener(this);
        }

        updatePreference();
    }

    void updatePreference() {
        ListPreference p = preference;
        if (p == null) {
            return;
        }

        int idx = entries.getIndexForValue(getCurrentValue());
        if (idx >= 0) {
            p.setValueIndex(idx);

            var summary = new StringBuilder();
            summary.append("[ ");
            summary.append(p.getEntries()[idx]);
            summary.append(" ]");
            if (baseSummary != null) {
                summary.append("\n\n");
                summary.append(baseSummary);
            }
            p.setSummary(summary.toString());
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object o) {
        int val = Integer.parseInt((String) o);
        return setValue(val);
    }

    public static class Entries {
        private final Context context;
        private final ArrayList<CharSequence> titles = new ArrayList<>();
        private final ArrayList<String> values = new ArrayList<>();
        private final SparseIntArray valueToIndexMap = new SparseIntArray();

        Entries(Context context) {
            this.context = context;
        }

        public void add(@StringRes int title, int value) {
            add(context.getText(title), value);
        }

        public void add(int duration, TimeUnit timeUnit) {
            long durationMillis = timeUnit.toMillis(duration);
            if (durationMillis > Integer.MAX_VALUE) {
                throw new IllegalArgumentException();
            }

            add(DateUtils.formatDuration(durationMillis), (int) durationMillis);
        }

        public void add(CharSequence title, int value) {
            titles.add(title);
            values.add(Integer.toString(value));
            valueToIndexMap.put(value, values.size() - 1);
        }

        public CharSequence[] getTitles() {
            return titles.toArray(CharSequence[]::new);
        }

        public String[] getValues() {
            return values.toArray(String[]::new);
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
}

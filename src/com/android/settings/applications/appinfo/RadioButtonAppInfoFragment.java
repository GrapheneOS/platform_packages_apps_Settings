package com.android.settings.applications.appinfo;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.applications.AppInfoWithHeader;
import com.android.settingslib.widget.FooterPreference;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

public abstract class RadioButtonAppInfoFragment extends AppInfoWithHeader implements SelectorWithWidgetPreference.OnClickListener {

    public static class Entry {
        public final int id;
        public CharSequence title;
        @Nullable
        public CharSequence summary;
        public boolean isChecked;
        public boolean isEnabled;

        public Entry(int id, CharSequence title, @Nullable CharSequence summary, boolean isChecked, boolean isEnabled) {
            this.id = id;
            this.title = title;
            this.summary = summary;
            this.isChecked = isChecked;
            this.isEnabled = isEnabled;
        }
    }

    public static Entry createEntry(int id, CharSequence title) {
        return new Entry(id, title, null, false, true);
    }

    public Entry createEntry(int id, @StringRes int title) {
        return createEntry(id, getText(title));
    }

    public Entry createEntry(@StringRes int title) {
        return createEntry(title, title);
    }

    public abstract Entry[] getEntries();

    public boolean hasFooter() {
        return true;
    }

    public abstract void updateFooter(FooterPreference fp);

    public void setLearnMoreLink(FooterPreference p, String url) {
        p.setLearnMoreAction(v -> {
            var i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(i);
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(getPrefContext());
        setPreferenceScreen(screen);
        requireActivity().setTitle(getTitle());
    }

    protected abstract CharSequence getTitle();

    @Nullable
    private SelectorWithWidgetPreference[] radioButtons;
    @Nullable
    private FooterPreference footer;

    @Override
    protected boolean refreshUi() {
        Entry[] entries = getEntries();
        int entryCnt = entries.length;

        Context ctx = getPrefContext();
        PreferenceScreen screen = getPreferenceScreen();

        if (radioButtons == null || radioButtons.length != entryCnt) {
            if (radioButtons != null) {
                for (Preference p : radioButtons) {
                    screen.removePreference(p);
                }
            }

            radioButtons = new SelectorWithWidgetPreference[entryCnt];

            for (int i = 0; i < entryCnt; ++i) {
                var p = new SelectorWithWidgetPreference(ctx);
                p.setOnClickListener(this);
                screen.addPreference(p);
                radioButtons[i] = p;
            }
        }

        for (int i = 0; i < entryCnt; ++i) {
            SelectorWithWidgetPreference p = radioButtons[i];
            Entry e = entries[i];
            p.setKey(Integer.toString(e.id));
            p.setTitle(e.title);
            p.setSummary(e.summary);
            p.setEnabled(e.isEnabled);
            p.setChecked(e.isChecked);
        }

        if (hasFooter()) {
            if (footer == null) {
                footer = new FooterPreference(ctx);
                screen.addPreference(footer);
            }
            updateFooter(footer);
        } else {
            if (footer != null) {
                screen.removePreference(footer);
                footer = null;
            }
        }

        return true;
    }

    @Override
    public final void onRadioButtonClicked(SelectorWithWidgetPreference emiter) {
        int id = Integer.parseInt(emiter.getKey());
        onEntrySelected(id);
        if (!refreshUi()) {
            setIntentAndFinish(true);
        }
    }

    public abstract void onEntrySelected(int id);

    @Override
    protected AlertDialog createDialog(int id, int errorCode) {
        return null;
    }

    @Override
    public int getMetricsCategory() {
        return METRICS_CATEGORY_UNKNOWN;
    }

    protected ApplicationInfo getAppInfo() {
        return mPackageInfo.applicationInfo;
    }
}

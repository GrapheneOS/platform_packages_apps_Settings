package com.android.settings.connecteddevice;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.support.actionbar.HelpResourceProvider;
import com.android.settings.widget.RadioButtonPickerFragment;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.widget.CandidateInfo;
import com.android.settingslib.widget.RadioButtonPreference;

import java.util.ArrayList;
import java.util.List;
@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class NfcTimeoutSettings extends RadioButtonPickerFragment implements
        HelpResourceProvider {
    private CharSequence[] mInitialEntries;
    private CharSequence[] mInitialValues;
    Context mContext;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
        mInitialEntries = getResources().getStringArray(R.array.nfc_timeout_entries);
        mInitialValues = getResources().getStringArray(R.array.nfc_timeout_values);
    }

    @Override
    protected List<? extends CandidateInfo> getCandidates() {
        final List<CandidateInfo> candidates = new ArrayList<>();
        for (int i = 0; i < mInitialValues.length; ++i) {
            candidates.add(new TimeoutCandidateInfo(mInitialEntries[i],
                    mInitialValues[i].toString(), true));
        }
        return candidates;
    }

    @Override
    public void updateCandidates() {
        final String defaultKey = getDefaultKey();
        final PreferenceScreen screen = getPreferenceScreen();
        screen.removeAll();

        final List<? extends CandidateInfo> candidateList = getCandidates();
        if (candidateList == null) {
            return;
        }

        for (CandidateInfo info : candidateList) {
            RadioButtonPreference pref =
                    new RadioButtonPreference(getPrefContext());
            bindPreference(pref, info.getKey(), info, defaultKey);
            screen.addPreference(pref);
        }
    }

    @Override
    protected String getDefaultKey() {
        return getCurrentNfcTimeout(getContext());
    }

    @Override
    protected boolean setDefaultKey(String key) {
        setCurrentNfcTimeout(getContext(), key);
        return true;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.NFC_PAYMENT;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.nfc_timeout_settings;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_adaptive_sleep;
    }

    private String getCurrentNfcTimeout(Context context) {
        return Long.toString(
                Settings.Global.getLong(
                        context.getContentResolver(),
                        Settings.Global.NFC_OFF_TIMEOUT,
                        0
                )
        );
    }

    private void setCurrentNfcTimeout(Context context, String key) {
        Settings.Global.putLong(
                context.getContentResolver(),
                Settings.Global.NFC_OFF_TIMEOUT,
                Long.parseLong(key)
        );
    }

    private static class TimeoutCandidateInfo extends CandidateInfo {
        private final CharSequence mLabel;
        private final String mKey;

        TimeoutCandidateInfo(CharSequence label, String key, boolean enabled) {
            super(enabled);
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

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.nfc_and_payment_settings) {
                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    final PackageManager pm = context.getPackageManager();
                    return pm.hasSystemFeature(PackageManager.FEATURE_NFC);
                }
            };

}

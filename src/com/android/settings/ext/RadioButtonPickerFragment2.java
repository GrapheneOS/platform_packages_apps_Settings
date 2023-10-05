package com.android.settings.ext;

import android.content.Context;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.utils.CandidateInfoExtra;
import com.android.settings.widget.RadioButtonPickerFragment;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.widget.CandidateInfo;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import java.util.ArrayList;
import java.util.List;

public class RadioButtonPickerFragment2 extends RadioButtonPickerFragment {

    private final ArrayList<CandidateInfo> candidates = new ArrayList<>();

    static final String KEY_PREF_CONTROLLER_CLASS = "pref_controller";
    static final String KEY_PREF_KEY = "pref_key";

    private AbstractListPreferenceController prefController;

    public static void fillArgs(Preference pref, AbstractListPreferenceController pc, boolean isForWork) {
        Bundle args = pref.getExtras();
        args.putString(KEY_PREF_CONTROLLER_CLASS, pc.getClass().getName());
        args.putString(KEY_PREF_KEY, pc.getPreferenceKey());
        args.putBoolean(EXTRA_FOR_WORK, isForWork);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Bundle args = requireArguments();
        String prefControllerClass = args.getString(KEY_PREF_CONTROLLER_CLASS);
        String prefKey = args.getString(KEY_PREF_KEY);
        boolean forWork = args.getBoolean(EXTRA_FOR_WORK);

        Context ctx = requireContext();

        prefController = (AbstractListPreferenceController) BasePreferenceController
                .createInstance(ctx,prefControllerClass, prefKey, forWork);
        prefController.fragment = this;

        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        PreferenceScreen ps = getPreferenceManager().createPreferenceScreen(requireContext());
        setPreferenceScreen(ps);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return -1;
    }

    @Override
    protected List<? extends CandidateInfo> getCandidates() {
        candidates.clear();
        prefController.getEntriesAsCandidates(candidates);
        return candidates;
    }

    @Override
    protected void addPrefsBeforeList(PreferenceScreen screen) {
        prefController.addPrefsBeforeList(this, screen);
    }

    @Override
    protected void addPrefsAfterList(PreferenceScreen screen) {
        prefController.addPrefsAfterList(this, screen);
    }

    @Override
    protected String getDefaultKey() {
        return Integer.toString(prefController.getCurrentValue());
    }

    @Override
    protected boolean setDefaultKey(String key) {
        return prefController.setValue(Integer.parseInt(key));
    }

    @Override
    public void bindPreferenceExtra(SelectorWithWidgetPreference pref, String key, CandidateInfo info,
                                    String defaultKey, String systemDefaultKey) {
        pref.setSingleLineTitle(false);

        if (info instanceof CandidateInfoExtra) {
            var cie = (CandidateInfoExtra) info;
            pref.setSummary(cie.loadSummary());
        }
    }

    @Override
    public int getMetricsCategory() {
        return METRICS_CATEGORY_UNKNOWN;
    }

    @Override
    public void onPause() {
        super.onPause();
        prefController.onPause(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        prefController.onResume(this);
        updateCandidates();
    }
}

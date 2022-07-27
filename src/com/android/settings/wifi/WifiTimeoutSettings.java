package com.android.settings.wifi;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.pm.PackageManager;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.widget.TimeoutPickerFragment;
import com.android.settingslib.search.SearchIndexable;

import java.util.concurrent.TimeUnit;

@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class WifiTimeoutSettings extends TimeoutPickerFragment {
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.wifi_timeout_settings) {
                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return context.getPackageManager().hasSystemFeature(
                            PackageManager.FEATURE_WIFI);
                }
            };
    static final long[] CANDIDATES = {
            0,
            TimeUnit.SECONDS.toMillis(15),
            TimeUnit.SECONDS.toMillis(30),
            TimeUnit.MINUTES.toMillis(1),
            TimeUnit.MINUTES.toMillis(2),
            TimeUnit.MINUTES.toMillis(5),
            TimeUnit.MINUTES.toMillis(10),
            TimeUnit.MINUTES.toMillis(30),
            TimeUnit.HOURS.toMillis(1),
            TimeUnit.HOURS.toMillis(2),
            TimeUnit.HOURS.toMillis(4),
            TimeUnit.HOURS.toMillis(8),
    };

    @Override
    protected long[] getSelectableTimeouts() {
        return CANDIDATES;
    }

    @Override
    protected String getSettingsKey() {
        return Settings.Global.WIFI_OFF_TIMEOUT;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.WIFI;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.wifi_timeout_settings;
    }

}
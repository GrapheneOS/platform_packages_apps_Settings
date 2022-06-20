package com.android.settings.display;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.internal.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

import vendor.google.wireless_charger.ReverseWirelessCharger;

public class BatterySharePreferenceController extends BasePreferenceController implements
        PreferenceControllerMixin, Preference.OnPreferenceChangeListener, LifecycleObserver,
        OnStart, OnStop {

    private static final String KEY_BATTERY_SHARE = "battery_share";
    private final ReverseWirelessCharger wirelessCharger;
    private final Context mContext;
    private Preference mPreference;

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ((Activity) mContext).runOnUiThread(() -> update());
        }
    };


    public BatterySharePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mContext = context;
        wirelessCharger = ReverseWirelessCharger.getInstance();
    }

    public boolean isPlugged(Context context) {
        Intent intent = context.registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        return plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        update();
    }

    @Override
    public String getPreferenceKey() {
        return KEY_BATTERY_SHARE;
    }

    private void update() {
        if (mPreference == null) return;
        boolean enabled = !isPlugged(mContext) && wirelessCharger.isRtxSupported();
        mPreference.setEnabled(enabled);
        ((SwitchPreference) mPreference).setChecked(wirelessCharger.isRtxModeOn());
    }

    @Override
    public int getAvailabilityStatus() {
        return wirelessCharger.isRtxSupported() ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void updateState(Preference preference) {
        update();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        wirelessCharger.setRtxMode((Boolean) newValue);
        return true;
    }

    @Override
    public void onStart() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        mContext.registerReceiver(mBroadcastReceiver, filter);
    }

    @Override
    public void onStop() {
        mContext.unregisterReceiver(mBroadcastReceiver);
    }
}

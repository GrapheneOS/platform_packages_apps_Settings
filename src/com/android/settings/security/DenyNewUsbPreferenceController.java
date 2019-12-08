package com.android.settings.security;

import android.content.Context;

import android.os.UserHandle;
import android.os.UserManager;
import android.os.SystemProperties;

import android.provider.Settings;


import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;


import com.android.internal.widget.LockPatternUtils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.events.OnResume;

public class DenyNewUsbPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, OnResume, Preference.OnPreferenceChangeListener {

    private static final String KEY_DENY_NEW_USB = "deny_new_usb";
    private static final String DENY_NEW_USB_PROP = "security.deny_new_usb";
    private static final String DENY_NEW_USB_PERSIST_PROP = "persist.security.deny_new_usb";
    private static final String PREF_KEY_SECURITY_CATEGORY = "security_category";

    private PreferenceCategory mSecurityCategory;
    private ListPreference mDenyNewUsb;
    private boolean mIsAdmin;
    private final UserManager mUm;

    public DenyNewUsbPreferenceController(Context context) {
        super(context);
        mUm = UserManager.get(context);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mSecurityCategory = screen.findPreference(PREF_KEY_SECURITY_CATEGORY);
        updatePreferenceState();
    }

    @Override
    public boolean isAvailable() {
        mIsAdmin = mUm.isAdminUser();
        return mIsAdmin;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_DENY_NEW_USB;
    }

    // TODO: should we use onCreatePreferences() instead?
    private void updatePreferenceState() {
        if (mSecurityCategory == null) {
            return;
        }

        if (mIsAdmin) {
            mDenyNewUsb = (ListPreference) mSecurityCategory.findPreference(KEY_DENY_NEW_USB);
            mDenyNewUsb.setValue(SystemProperties.get(DENY_NEW_USB_PERSIST_PROP, "disabled"));
        } else {
            mSecurityCategory.removePreference(mSecurityCategory.findPreference(KEY_DENY_NEW_USB));
        }
    }

    @Override
    public void onResume() {
        updatePreferenceState();

        if (mDenyNewUsb != null) {
            String mode = mDenyNewUsb.getValue();
            if (mode.equals("dynamic") || mode.equals("disabled")) {
                SystemProperties.set(DENY_NEW_USB_PROP, "0");
            } else {
                SystemProperties.set(DENY_NEW_USB_PROP, "1");
            }
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        final String key = preference.getKey();
        if (KEY_DENY_NEW_USB.equals(key)) {
            String mode = (String) value;
            SystemProperties.set(DENY_NEW_USB_PERSIST_PROP, mode);
            // The dynamic mode defaults to the disabled state
            if (mode.equals("dynamic") || mode.equals("disabled")) {
                SystemProperties.set(DENY_NEW_USB_PROP, "0");
            } else {
                SystemProperties.set(DENY_NEW_USB_PROP, "1");
            }
        }
        return true;
    }
}

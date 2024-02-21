package com.android.settings.security;

import android.content.Context;
import android.ext.settings.UsbPortSecurity;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbPort;
import android.hardware.usb.ext.PortSecurityState;

import com.android.settings.R;
import com.android.settings.ext.IntSettingPrefController;

import java.util.List;

public class UsbPortSecurityPrefController extends IntSettingPrefController {

    public UsbPortSecurityPrefController(Context ctx, String key) {
        super(ctx, key, UsbPortSecurity.MODE_SETTING);
    }

    @Override
    public int getAvailabilityStatus() {
        int res = super.getAvailabilityStatus();
        if (res == AVAILABLE) {
            int config = com.android.internal.R.bool.config_usbPortSecuritySupported;
            if (!mContext.getResources().getBoolean(config)) {
                res = UNSUPPORTED_ON_DEVICE;
            }
        }
        return res;
    }

    @Override
    protected void getEntries(Entries entries) {
        entries.add(R.string.usbc_port_off_title, R.string.usbc_port_off_summary,
                UsbPortSecurity.MODE_DISABLED);
        entries.add(R.string.usbc_port_charging_only_title,
                UsbPortSecurity.MODE_CHARGING_ONLY);
        entries.add(R.string.usbc_port_charging_only_when_locked_title, R.string.usbc_port_charging_only_when_locked_summary,
                UsbPortSecurity.MODE_CHARGING_ONLY_WHEN_LOCKED);
        entries.add(R.string.usbc_port_charging_only_when_locked_afu_title, R.string.usbc_port_charging_only_when_locked_afu_summary,
                UsbPortSecurity.MODE_CHARGING_ONLY_WHEN_LOCKED_AFU);
        entries.add(R.string.usbc_port_on_title,
                UsbPortSecurity.MODE_ENABLED);
    }

    private void setState(@android.hardware.usb.ext.PortSecurityState int state) {
        var um = mContext.getSystemService(UsbManager.class);
        List<UsbPort> ports = um.getPorts();

        for (UsbPort port : ports) {
            um.setPortSecurityState(port, state);
        }
    }

    @Override
    protected boolean setValue(int val) {
        boolean res = super.setValue(val);
        if (!res) {
            return false;
        }

        int pss = switch (val) {
            case UsbPortSecurity.MODE_DISABLED -> PortSecurityState.DISABLED;
            case UsbPortSecurity.MODE_CHARGING_ONLY -> PortSecurityState.CHARGING_ONLY_IMMEDIATE;
            case UsbPortSecurity.MODE_CHARGING_ONLY_WHEN_LOCKED -> PortSecurityState.ENABLED;
            case UsbPortSecurity.MODE_CHARGING_ONLY_WHEN_LOCKED_AFU -> PortSecurityState.ENABLED;
            case UsbPortSecurity.MODE_ENABLED -> PortSecurityState.ENABLED;
            default -> throw new IllegalArgumentException(Integer.toString(val));
        };
        setState(pss);
        return true;
    }
}

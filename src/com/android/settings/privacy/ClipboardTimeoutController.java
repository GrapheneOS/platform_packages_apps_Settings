package com.android.settings.privacy;

import android.content.Context;
import android.ext.settings.ExtSettings;

import com.android.settings.ext.IntSettingPrefController;
import com.android.settings.R;

import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

public class ClipboardTimeoutController extends IntSettingPrefController {

    public ClipboardTimeoutController(Context ctx, String key) {
        super(ctx, key, ExtSettings.CLIPBOARD_AUTO_CLEAR_TIMEOUT);
    }

    @Override
    protected void getEntries(Entries entries) {
        entries.add(R.string.switch_off_text, 0);
        entries.add(10, SECONDS);
        entries.add(30, SECONDS);
        entries.add(1, MINUTES);
        entries.add(5, MINUTES);
        entries.add(10, MINUTES);
        entries.add(30, MINUTES);
        entries.add(1, HOURS);
        entries.add(2, HOURS);
        entries.add(4, HOURS);
        entries.add(8, HOURS);
    }
}

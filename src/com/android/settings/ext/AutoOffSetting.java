package com.android.settings.ext;

import com.android.settings.R;

import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

public class AutoOffSetting {

    public static void getEntries(AbstractListPreferenceController.Entries entries) {
        entries.add(R.string.auto_off_never, 0);
        entries.add(15, SECONDS);
        entries.add(30, SECONDS);
        entries.add(1,  MINUTES);
        entries.add(2,  MINUTES);
        entries.add(5,  MINUTES);
        entries.add(10, MINUTES);
        entries.add(30, MINUTES);
        entries.add(1,  HOURS);
        entries.add(2,  HOURS);
        entries.add(4,  HOURS);
        entries.add(8,  HOURS);
    }
}

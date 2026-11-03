package com.example.daycalendar;

import android.content.*;

/** Restore an explicit opt-in after reboot or an application update. */
public class UpdateRestartReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) || Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) {
            MinuteUpdateService.startIfEnabled(context);
            DayWidget.refreshAll(context);
        }
    }
}

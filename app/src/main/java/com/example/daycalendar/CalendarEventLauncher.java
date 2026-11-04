package com.example.daycalendar;

import android.app.Activity;
import android.content.*;
import android.provider.CalendarContract;

final class CalendarEventLauncher {
    static final String GOOGLE_CALENDAR = "com.google.android.calendar";
    private CalendarEventLauncher() { }
    static Intent intent(long id,long begin,long end,boolean allDay) {
        if(id<=0 || end<begin)throw new IllegalArgumentException("Invalid calendar event");
        return new Intent(Intent.ACTION_VIEW,ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI,id))
                .putExtra(CalendarContract.EXTRA_EVENT_ID,id)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME,begin)
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME,end)
                .putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY,allDay);
    }
    static boolean open(Context context,long id,long begin,long end,boolean allDay) {
        Intent view=intent(id,begin,end,allDay);
        if(!(context instanceof Activity))view.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {context.startActivity(new Intent(view).setPackage(GOOGLE_CALENDAR));return true;}
        catch(ActivityNotFoundException | SecurityException missingGoogle) {
            try {context.startActivity(view);return true;}
            catch(ActivityNotFoundException | SecurityException missingCalendar){return false;}
        }
    }
    static boolean open(Context context,CalendarEvent event) {return open(context,event.id(),event.begin(),event.end(),event.allDay());}
}

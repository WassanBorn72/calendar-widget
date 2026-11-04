package com.example.daycalendar;

import static org.junit.Assert.*;
import android.app.Activity;
import android.content.*;
import android.provider.CalendarContract;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import java.util.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35)
public class EventLaunchTest {
    public static class CalendarHost extends Activity {
        final List<Intent> attempts=new ArrayList<>();boolean googleMissing,allMissing;
        @Override public void startActivity(Intent intent){attempts.add(new Intent(intent));if(allMissing || googleMissing&&CalendarEventLauncher.GOOGLE_CALENDAR.equals(intent.getPackage()))throw new ActivityNotFoundException();}
    }
    @Test public void googleIsPreferredAndRecurringInstanceTimesAreForwarded(){
        CalendarHost activity=Robolectric.buildActivity(CalendarHost.class).setup().get();
        assertTrue(CalendarEventLauncher.open(activity,73,1762218000000L,1762221600000L,false));
        Intent view=activity.attempts.get(0);
        assertEquals(CalendarEventLauncher.GOOGLE_CALENDAR,view.getPackage());assertEquals(Intent.ACTION_VIEW,view.getAction());
        assertEquals("content://com.android.calendar/events/73",view.getDataString());
        assertEquals(1762218000000L,view.getLongExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME,-1));
        assertEquals(1762221600000L,view.getLongExtra(CalendarContract.EXTRA_EVENT_END_TIME,-1));
        assertFalse(view.getBooleanExtra(CalendarContract.EXTRA_EVENT_ALL_DAY,true));
    }
    @Test public void missingGoogleFallsBackWithoutLosingEventOrAllDay(){
        CalendarHost activity=Robolectric.buildActivity(CalendarHost.class).setup().get();activity.googleMissing=true;
        assertTrue(CalendarEventLauncher.open(activity,22,1762214400000L,1762300800000L,true));
        assertEquals(2,activity.attempts.size());Intent fallback=activity.attempts.get(1);
        assertNull(fallback.getPackage());assertEquals("content://com.android.calendar/events/22",fallback.getDataString());
        assertTrue(fallback.getBooleanExtra(CalendarContract.EXTRA_EVENT_ALL_DAY,false));
    }
    @Test public void noCalendarHandlerReturnsFailureInsteadOfCrashing(){
        CalendarHost activity=Robolectric.buildActivity(CalendarHost.class).setup().get();activity.allMissing=true;
        assertFalse(CalendarEventLauncher.open(activity,22,100,200,false));assertEquals(2,activity.attempts.size());
    }
    @Test public void invalidIdsAndReversedRangesAreRejected(){
        assertThrows(IllegalArgumentException.class,()->CalendarEventLauncher.intent(-1,100,200,false));
        assertThrows(IllegalArgumentException.class,()->CalendarEventLauncher.intent(1,200,100,false));
    }
}

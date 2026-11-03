package com.example.daycalendar;

import static org.junit.Assert.*;
import android.content.Context;
import android.view.View;
import android.widget.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk={26,35})
public class WidgetClockTest {
    @Test public void clockIsSupportedInRemoteViewsOnOldAndCurrentAndroid() {
        Context context=RuntimeEnvironment.getApplication();
        RemoteViews remote=new RemoteViews(context.getPackageName(),R.layout.widget);
        View view=remote.apply(context,new FrameLayout(context));
        TextClock clock=view.findViewById(R.id.clock);
        assertNotNull(clock);
        assertEquals("HH:mm",clock.getFormat12Hour().toString());
        assertEquals("HH:mm",clock.getFormat24Hour().toString());
    }
}

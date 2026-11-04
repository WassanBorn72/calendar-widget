package com.example.daycalendar;

import static org.junit.Assert.*;
import android.content.Context;
import android.graphics.*;
import android.os.Parcel;
import android.view.*;
import android.widget.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import java.time.*;
import java.util.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,qualifiers="xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class WidgetReapplyTest {
    private final Context context=RuntimeEnvironment.getApplication();
    private final LocalDate day=LocalDate.of(2026,11,4);
    private final ZoneId zone=ZoneId.systemDefault();
    private CalendarEvent event(String title,String location) {
        return new CalendarEvent(1,title,location,day.atTime(10,0).atZone(zone).toInstant().toEpochMilli(),day.atTime(11,0).atZone(zone).toInstant().toEpochMilli(),false,0xff769885);
    }
    private RemoteViews snapshot(int minute,List<CalendarEvent> events) {
        return snapshot(320,20,day,day.atTime(10,minute).atZone(zone),events);
    }
    private RemoteViews snapshot(int width,int slice,LocalDate date,ZonedDateTime now,List<CalendarEvent> events) {
        return WidgetNativeTimeline.slice(context,width,slice,date,DayTimeline.layout(events,date,zone),now);
    }
    private View apply(RemoteViews views,int width) {
        View result=views.apply(context,new FrameLayout(context));layout(result,width);return result;
    }
    private static void layout(View view,int width) {
        view.measure(View.MeasureSpec.makeMeasureSpec(width*3,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(view.getLayoutParams().height,View.MeasureSpec.EXACTLY));
        view.layout(0,0,width*3,view.getMeasuredHeight());
    }
    private static List<String> text(View v) {
        List<String> out=new ArrayList<>();
        if(v instanceof TextView)out.add(((TextView)v).getText().toString());
        if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)out.addAll(text(((ViewGroup)v).getChildAt(i)));
        return out;
    }
    @Test public void repeatedMinuteAndManualUpdatesReplaceChildrenInsteadOfAppending() {
        List<CalendarEvent> events=Collections.singletonList(event("Meeting","Room A"));
        View reused=apply(snapshot(15,events),320);
        for(int minute=16;minute<25;minute++) {
            RemoteViews update=snapshot(minute,events);View fresh=apply(update,320);
            update.reapply(context,reused);layout(reused,320);
            assertEquals("Each refresh must replace the old children",((ViewGroup)fresh).getChildCount(),((ViewGroup)reused).getChildCount());
            assertEquals(text(fresh),text(reused));
        }
    }
    @Test public void editedAndDeletedAppointmentsDoNotRemainInRecycledRows() {
        View reused=apply(snapshot(15,Collections.singletonList(event("OLD TITLE","OLD PLACE"))),320);
        RemoteViews edit=snapshot(16,Collections.singletonList(event("NEW TITLE","NEW PLACE")));
        edit.reapply(context,reused);layout(reused,320);
        assertFalse(text(reused).contains("OLD TITLE"));assertFalse(text(reused).contains("OLD PLACE"));
        assertTrue(text(reused).contains("NEW TITLE"));
        snapshot(17,Collections.emptyList()).reapply(context,reused);layout(reused,320);
        assertFalse(text(reused).contains("NEW TITLE"));assertFalse(text(reused).contains("NEW PLACE"));
    }
    @Test public void scrollingResizingAndDateChangeReuseOnlyTheLatestRow() {
        View reused=apply(snapshot(15,Collections.singletonList(event("Yesterday meeting","Room A"))),320);
        LocalDate tomorrow=day.plusDays(1);
        RemoteViews next=snapshot(240,44,tomorrow,tomorrow.atTime(22,30).atZone(zone),Collections.emptyList());
        next.reapply(context,reused);layout(reused,240);
        View fresh=apply(next,240);
        assertEquals(text(fresh),text(reused));assertTrue(text(reused).contains("22:00"));assertFalse(text(reused).contains("10:00"));
        assertEquals(((ViewGroup)fresh).getChildCount(),((ViewGroup)reused).getChildCount());
    }
    @Test public void transportedUpdateRendersExactlyLikeFreshInflation() {
        View reused=apply(snapshot(15,Collections.singletonList(event("Old title","Old room"))),320);
        RemoteViews update=snapshot(16,Collections.singletonList(event("Replacement","New room")));
        Parcel parcel=Parcel.obtain();RemoteViews transported;
        try {update.writeToParcel(parcel,0);parcel.setDataPosition(0);transported=RemoteViews.CREATOR.createFromParcel(parcel);}finally{parcel.recycle();}
        transported.reapply(context,reused);layout(reused,320);View fresh=apply(transported,320);
        Bitmap actual=Bitmap.createBitmap(reused.getWidth(),reused.getHeight(),Bitmap.Config.ARGB_8888);
        Bitmap expected=Bitmap.createBitmap(fresh.getWidth(),fresh.getHeight(),Bitmap.Config.ARGB_8888);
        reused.draw(new Canvas(actual));fresh.draw(new Canvas(expected));
        assertTrue("Reapplying a Binder-transported update must not leave ghost text or lines",expected.sameAs(actual));
    }
    @Test public void nearHourCurrentTimeNeverOverprintsTheFixedHourLabels() {
        int[][] cases={{0,1,0},{10,0,10},{10,1,10},{10,59,11},{23,58,24}};
        for(int[] entry:cases) {
            int minutes=entry[0]*60+entry[1];
            int slice=(int)((12+minutes*TimelineView.HOUR/60f)/WidgetRaster.SLICE_HEIGHT);
            View row=apply(snapshot(320,slice,day,day.atTime(entry[0],entry[1]).atZone(zone),Collections.emptyList()),320);
            String expected=String.format(Locale.ROOT,"%02d:00",entry[2]);
            assertEquals("Only the fixed hour label belongs on the axis",Collections.singletonList(expected),text(row));
        }
    }

}

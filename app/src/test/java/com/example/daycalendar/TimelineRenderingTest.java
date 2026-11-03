package com.example.daycalendar;

import static org.junit.Assert.*;
import android.graphics.*;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import java.io.*;
import java.time.*;
import java.util.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, qualifiers = "xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class TimelineRenderingTest {
    private static final LocalDate DAY = LocalDate.of(2020, 1, 2);
    private static final ZoneId ZONE = ZoneId.of("Asia/Tokyo");
    private TimeZone originalZone;
    @Before public void useFixtureTimeZone() { originalZone = TimeZone.getDefault(); TimeZone.setDefault(TimeZone.getTimeZone(ZONE)); }
    @After public void restoreTimeZone() { TimeZone.setDefault(originalZone); }

    private static CalendarEvent event(long id, int hour, int minute, int endHour, int endMinute, String title) {
        return new CalendarEvent(id, title, "Meeting room A",
                DAY.atTime(hour, minute).atZone(ZONE).toInstant().toEpochMilli(),
                DAY.atTime(endHour, endMinute).atZone(ZONE).toInstant().toEpochMilli(), false, 0xff5d8a6a);
    }
    private static List<CalendarEvent> events() {
        return Arrays.asList(event(1,9,30,10,0,"Team stand-up"), event(2,10,0,11,0,"Design review"));
    }
    private static List<DayTimeline.Block> blocks() { return DayTimeline.layout(events(), DAY, ZONE); }
    private static ZonedDateTime refreshed() { return DAY.plusDays(1).atStartOfDay(ZONE); }
    private static int brightness(int color) { return (Color.red(color)+Color.green(color)+Color.blue(color))/3; }

    @Test public void phoneBitmapsUsePhysicalPixelsAndFullColor() {
        Bitmap bitmap = TimelineView.sliceBitmap(320, 3, 20, DAY, blocks(), refreshed());
        assertEquals(960, bitmap.getWidth());
        assertEquals(108, bitmap.getHeight());
        assertEquals(Bitmap.Config.ARGB_8888, bitmap.getConfig());
        assertEquals(Bitmap.DENSITY_NONE, bitmap.getDensity());
    }
    @Test public void denseWideWidgetsStayBelowPerRowTransferBudget() {
        for(int width : new int[]{128,320,600,1024,2048}) for(float density : new float[]{1,2,3,4}) {
            Bitmap bitmap = TimelineView.sliceBitmap(width, density, 20, DAY, blocks(), refreshed());
            assertTrue(bitmap.getAllocationByteCount() <= WidgetRaster.MAX_BITMAP_BYTES);
            assertTrue(bitmap.getWidth() <= width*density);
            bitmap.recycle();
        }
    }
    @Test public void adjacentSameColorEventsHaveVisibleBoundaryWithoutTimeGap() {
        Bitmap bitmap = TimelineView.sliceBitmap(320, 3, 20, DAY, blocks(), refreshed());
        // 10:00 falls 12dp into this slice. Use an empty text area inside both cards.
        int edge = bitmap.getPixel(220*3, 12*3);
        int fillBefore = bitmap.getPixel(220*3, 8*3);
        int fillAfter = bitmap.getPixel(220*3, 18*3);
        assertTrue(brightness(edge)+40 < brightness(fillBefore));
        assertTrue(brightness(edge)+40 < brightness(fillAfter));
        var placed = blocks();
        assertEquals(placed.get(0).end, placed.get(1).start, 0);
        assertEquals(30, placed.get(0).end-placed.get(0).start, 0);
        assertEquals(60, placed.get(1).end-placed.get(1).start, 0);
    }
    @Test public void slicedWidgetAndAppHaveMatchingBordersAndHourPositions() {
        TimelineView view = new TimelineView(RuntimeEnvironment.getApplication(), DAY, events(), e -> { });
        int width = 320*3;
        view.measure(android.view.View.MeasureSpec.makeMeasureSpec(width, android.view.View.MeasureSpec.EXACTLY), 0);
        view.layout(0,0,width,view.getMeasuredHeight());
        Bitmap app = Bitmap.createBitmap(width,view.getMeasuredHeight(),Bitmap.Config.ARGB_8888);
        view.draw(new Canvas(app));
        Bitmap row = TimelineView.sliceBitmap(320,3,20,DAY,blocks(),refreshed());
        for(int y : new int[]{8,12,18,35})
            assertEquals(app.getPixel(220*3, (20*36+y)*3), row.getPixel(220*3,y*3));
        int total=0;for(int i=0;i<WidgetRaster.SLICE_COUNT;i++)total+=WidgetRaster.sliceHeight(i);
        assertEquals(24*TimelineView.HOUR+24,total);
        // The 09:30 grid line belongs to the previous hour but crosses into slice 19.
        Bitmap empty=TimelineView.sliceBitmap(320,3,19,DAY,Collections.emptyList(),refreshed());
        assertTrue(brightness(empty.getPixel(220*3,12*3))<240);
    }
    @Test public void currentTimeLineKeepsItsPositionInHighDensitySlices() {
        ZonedDateTime now=DAY.atTime(10,15).atZone(ZONE);
        Bitmap row=TimelineView.sliceBitmap(320,3,20,DAY,blocks(),now);
        // 10:15 is y=30dp in slice 20; the red line crosses even occupied events.
        int color=row.getPixel(300*3,30*3);
        assertTrue(Color.red(color)>170 && Color.green(color)<130);
    }
    @Test public void renderPreview() throws IOException {
        File dir=new File(System.getProperty("calendar.widget.screenshots", "build/render-previews"));
        if(!dir.isDirectory() && !dir.mkdirs())throw new IOException("Cannot create screenshot directory");
        Bitmap image=Bitmap.createBitmap(960,36*3*7,Bitmap.Config.ARGB_8888);
        Canvas c=new Canvas(image);
        for(int i=18;i<25;i++) {
            Bitmap slice=TimelineView.sliceBitmap(320,3,i,DAY,blocks(),DAY.atTime(10,15).atZone(ZONE));
            c.drawBitmap(slice,0,(i-18)*36*3,null);slice.recycle();
        }
        try(OutputStream out=new FileOutputStream(new File(dir,"android-timeline-3x.png"))) {
            assertTrue(image.compress(Bitmap.CompressFormat.PNG,100,out));
        }
    }
}
